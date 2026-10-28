package com.mdframe.forge.plugin.ai.knowledge.service;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.mdframe.forge.plugin.ai.knowledge.mapper.AiKnowledgeDocumentMapper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises actual Spring transactions and production Mapper XML, not production MySQL migrations. */
class DocumentProcessStateTransactionTest {
    private JdbcTemplate jdbc;
    private TransactionTemplate transaction;
    private AiKnowledgeDocumentMapper mapper;
    private DocumentProcessState state;

    @BeforeEach
    void setup() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:document_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE ai_knowledge_document (id BIGINT PRIMARY KEY, tenant_id BIGINT, "
                + "process_status VARCHAR(20), process_attempt VARCHAR(36), process_error VARCHAR(500), "
                + "chunk_count INT, process_deadline TIMESTAMP, update_time TIMESTAMP, del_flag BIGINT DEFAULT 0)");
        DataSourceTransactionManager manager = new DataSourceTransactionManager(dataSource);
        transaction = new TransactionTemplate(manager);
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                return new LongValue(TenantContextHolder.getTenantId());
            }

            @Override
            public boolean ignoreTable(String tableName) {
                return TenantContextHolder.isIgnore();
            }
        }));
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPlugins(interceptor);
        factory.setMapperLocations(new ClassPathResource("mapper/AiKnowledgeDocumentMapper.xml"));
        mapper = new SqlSessionTemplate(factory.getObject()).getMapper(AiKnowledgeDocumentMapper.class);
        ProxyFactory proxy = new ProxyFactory(new DocumentProcessState(mapper));
        proxy.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
        state = (DocumentProcessState) proxy.getProxy();
        TenantContextHolder.setTenantId(7L);
    }

    @AfterEach
    void clear() {
        jdbc.execute("DROP ALL OBJECTS");
        TenantContextHolder.clear();
    }

    @Test
    void rejectionAfterCommitActuallyCommitsNewTransaction() {
        DocumentProcessDispatcher stopped = new DocumentProcessDispatcher(1, 1, 0);
        stopped.shutdown();
        transaction.executeWithoutResult(status -> {
            insert(1, 7);
            assertEquals(1, mapper.requestProcessing(1L, "pending", "one", 1800));
            stopped.dispatch(() -> fail("stopped executor ran work"), () -> assertTrue(state.reject(1L, "one", "full")));
            assertEquals("pending", status(1));
        });
        assertEquals("failed", status(1));
        assertEquals("full", jdbc.queryForObject("SELECT process_error FROM ai_knowledge_document WHERE id=1", String.class));
    }

    @Test
    void rollbackDoesNotRunWorkOrPersistRejection() {
        DocumentProcessDispatcher dispatcher = new DocumentProcessDispatcher(1, 1, 0);
        AtomicInteger callbacks = new AtomicInteger();
        try {
            transaction.executeWithoutResult(status -> {
                insert(1, 7);
                mapper.requestProcessing(1L, "pending", "one", 1800);
                dispatcher.dispatch(callbacks::incrementAndGet, callbacks::incrementAndGet);
                status.setRollbackOnly();
            });
            assertEquals(0, callbacks.get());
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM ai_knowledge_document", Integer.class));
        } finally {
            dispatcher.shutdown();
        }
    }

    @Test
    void duplicateRequestAndStaleCompletionCannotReplaceCurrentAttempt() {
        insert(1, 7);
        assertEquals(1, mapper.requestProcessing(1L, "pending", "old", 1800));
        assertEquals(0, mapper.requestProcessing(1L, "pending", "duplicate", 1800));
        assertTrue(state.reject(1L, "old", "full"));
        assertEquals(1, mapper.requestProcessing(1L, "failed", "new", 1800));
        assertFalse(state.reject(1L, "old", "late rejection"));
        assertEquals(0, mapper.claimPendingDocument(1L, "old", 1800));
        assertEquals(1, mapper.claimPendingDocument(1L, "new", 1800));
        assertFalse(state.reject(1L, "new", "already running"));
        assertEquals(0, mapper.finishAttempt(1L, "old", "processing", "success", null, 99));
        assertEquals(1, mapper.finishAttempt(1L, "new", "processing", "success", null, 2));
        assertEquals("success", status(1));
        assertEquals(2, jdbc.queryForObject("SELECT chunk_count FROM ai_knowledge_document WHERE id=1", Integer.class));
    }

    @Test
    void tenantAndUnconfirmedDocumentRemainProtected() {
        insert(1, 8);
        insert(2, 7);
        assertEquals(0, mapper.requestProcessing(1L, "pending", "foreign", 1800));
        assertEquals(0, mapper.claimPendingDocument(2L, "unrequested", 1800));
        assertFalse(state.reject(2L, "unrequested", "must not touch confirmation"));
        assertEquals("pending", status(1));
        assertEquals("pending", status(2));
    }

    @Test
    void recoveryOnlyExpiresRecordedAttemptsAndRestoresCallerTenant() {
        insert(1, 7);
        insert(2, 8);
        insert(3, 7);
        insert(4, 7);
        insert(5, 7);
        mapper.requestProcessing(1L, "pending", "expired-queued", 1800);
        TenantContextHolder.setTenantId(8L);
        mapper.requestProcessing(2L, "pending", "expired-running", 1800);
        mapper.claimPendingDocument(2L, "expired-running", 1800);
        TenantContextHolder.setTenantId(7L);
        mapper.requestProcessing(3L, "pending", "live-other-instance", 1800);
        mapper.claimPendingDocument(3L, "live-other-instance", 1800);
        jdbc.update("UPDATE ai_knowledge_document SET process_deadline=TIMESTAMPADD(SECOND,-1,CURRENT_TIMESTAMP) WHERE id IN(1,2,4)");
        jdbc.update("UPDATE ai_knowledge_document SET process_status='processing' WHERE id=5");
        assertEquals("recovered=2", new DocumentProcessRecovery(mapper).recoverExpired());
        assertEquals(7L, TenantContextHolder.getTenantId());
        assertFalse(TenantContextHolder.isIgnore());
        assertEquals("failed", status(1));
        assertEquals("failed", status(2));
        assertEquals("processing", status(3));
        assertEquals("pending", status(4));
        assertEquals("processing", status(5));
        assertEquals(1, mapper.requestProcessing(1L, "failed", "new-retry", 1800));
        assertEquals(0, mapper.expireAttempt(1L, "expired-queued"));
        assertEquals(0, mapper.claimPendingDocument(1L, "expired-queued", 1800));
        assertEquals(1, mapper.claimPendingDocument(1L, "new-retry", 1800));
    }

    @Test
    void expiredQueueCannotBeginWorkBeforeRecoveryJobRuns() {
        insert(1, 7);
        mapper.requestProcessing(1L, "pending", "expired", 1800);
        jdbc.update("UPDATE ai_knowledge_document SET process_deadline=TIMESTAMPADD(SECOND,-1,CURRENT_TIMESTAMP) WHERE id=1");
        assertEquals(0, mapper.claimPendingDocument(1L, "expired", 1800));
        assertEquals("recovered=1", new DocumentProcessRecovery(mapper).recoverExpired());
        assertEquals("recovered=0", new DocumentProcessRecovery(mapper).recoverExpired());
    }

    private void insert(long id, long tenant) {
        jdbc.update("INSERT INTO ai_knowledge_document(id,tenant_id,process_status) VALUES(?,?,'pending')", id, tenant);
    }

    private String status(long id) {
        return jdbc.queryForObject("SELECT process_status FROM ai_knowledge_document WHERE id=?", String.class, id);
    }
}
