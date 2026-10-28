package com.mdframe.forge.plugin.ai.knowledge.service;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.mdframe.forge.plugin.ai.knowledge.chunker.dto.ChunkResult;
import com.mdframe.forge.plugin.ai.knowledge.mapper.AiKnowledgeChunkMapper;
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

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DocumentIndexPublisherTransactionTest {
    private JdbcTemplate jdbc;
    private DocumentIndexPublisher publisher;
    private AiKnowledgeChunkMapper chunks;

    @BeforeEach
    void setup() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:publication_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(dataSource);
        String audit = ",create_by BIGINT,create_time TIMESTAMP,create_dept BIGINT,update_by BIGINT,update_time TIMESTAMP,del_flag BIGINT DEFAULT 0)";
        jdbc.execute("CREATE TABLE ai_knowledge_document(id BIGINT PRIMARY KEY,tenant_id BIGINT,knowledge_id BIGINT,"
                + "file_id VARCHAR,doc_name VARCHAR,doc_type VARCHAR,source_type VARCHAR,source_url VARCHAR,content_hash VARCHAR,"
                + "chunk_count INT,process_status VARCHAR,process_attempt VARCHAR,process_deadline TIMESTAMP,process_error VARCHAR" + audit);
        jdbc.execute("CREATE TABLE ai_knowledge_chunk(id BIGINT PRIMARY KEY,tenant_id BIGINT,knowledge_id BIGINT,document_id BIGINT,"
                + "chunk_index INT,content VARCHAR(64),title VARCHAR,token_count INT,vector_id VARCHAR,ref_count INT,"
                + "content_hash VARCHAR,retrieval_count INT" + audit);
        jdbc.update("INSERT INTO ai_knowledge_document(id,tenant_id,knowledge_id,process_status,process_attempt,process_deadline,chunk_count) VALUES(2,7,1,'processing','current',TIMESTAMPADD(SECOND,1800,CURRENT_TIMESTAMP),1)");
        jdbc.update("INSERT INTO ai_knowledge_chunk(id,tenant_id,knowledge_id,document_id,chunk_index,content,vector_id) VALUES(10,7,1,2,0,'old content','old-vector')");
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                return new LongValue(TenantContextHolder.getTenantId());
            }
        }));
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPlugins(interceptor);
        factory.setMapperLocations(new ClassPathResource("mapper/AiKnowledgeDocumentMapper.xml"),
                new ClassPathResource("mapper/AiKnowledgeChunkMapper.xml"));
        SqlSessionTemplate session = new SqlSessionTemplate(factory.getObject());
        chunks = session.getMapper(AiKnowledgeChunkMapper.class);
        ProxyFactory proxy = new ProxyFactory(new DocumentIndexPublisher(session.getMapper(AiKnowledgeDocumentMapper.class), chunks));
        proxy.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(dataSource), new AnnotationTransactionAttributeSource()));
        publisher = (DocumentIndexPublisher) proxy.getProxy();
        TenantContextHolder.setTenantId(7L);
    }

    @AfterEach
    void clear() {
        jdbc.execute("DROP ALL OBJECTS");
        TenantContextHolder.clear();
    }

    @Test
    void publishesChunksAndSuccessTogetherAndRetainsOldIdsForCleanup() {
        var result = publisher.publish(2L, "current", List.of(ChunkResult.of(0, "new content", 2)), List.of("new-vector"));
        assertTrue(result.published());
        assertEquals("old-vector", result.supersededChunks().get(0).getVectorId());
        assertEquals("success", status());
        assertEquals("new content", chunks.selectByDocumentId(2L).get(0).getContent());
        assertEquals(1, chunks.selectSearchableByVectorIds(1L, List.of("new-vector", "old-vector")).size());
        assertTrue(chunks.selectSearchableByVectorIds(1L, List.of("old-vector")).isEmpty());
        assertEquals(10L, jdbc.queryForObject("SELECT del_flag FROM ai_knowledge_chunk WHERE id=10", Long.class));
    }

    @Test
    void failedChunkWriteRollsBackDeletionAndDocumentState() {
        assertThrows(RuntimeException.class, () -> publisher.publish(2L, "current",
                List.of(ChunkResult.of(0, "x".repeat(100), 1)), List.of("new-vector")));
        assertEquals("processing", status());
        assertEquals("old-vector", chunks.selectByDocumentId(2L).get(0).getVectorId());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM ai_knowledge_chunk", Integer.class));
    }

    @Test
    void staleAttemptAndForeignTenantCannotReplaceLiveChunks() {
        assertFalse(publisher.publish(2L, "old", List.of(), List.of()).published());
        TenantContextHolder.setTenantId(8L);
        assertFalse(publisher.publish(2L, "current", List.of(), List.of()).published());
        assertEquals("processing", status());
        assertEquals(0L, jdbc.queryForObject("SELECT del_flag FROM ai_knowledge_chunk WHERE id=10", Long.class));
    }

    @Test
    void alreadyPublishedAttemptIsNotReportedAsObsolete() {
        jdbc.update("UPDATE ai_knowledge_document SET process_status='success' WHERE id=2");
        var result = publisher.publish(2L, "current", List.of(), List.of());
        assertTrue(result.published());
        assertTrue(result.supersededChunks().isEmpty());
        assertEquals("old-vector", chunks.selectByDocumentId(2L).get(0).getVectorId());
    }

    @Test
    void expiredRunningAttemptCannotPublishEvenBeforeRecoveryChangesStatus() {
        jdbc.update("UPDATE ai_knowledge_document SET process_deadline=TIMESTAMPADD(SECOND,-1,CURRENT_TIMESTAMP) WHERE id=2");
        assertThrows(IllegalStateException.class, () -> publisher.publish(2L, "current",
                List.of(ChunkResult.of(0, "late content", 2)), List.of("late-vector")));
        assertEquals("processing", status());
        assertEquals("old-vector", chunks.selectByDocumentId(2L).get(0).getVectorId());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM ai_knowledge_chunk", Integer.class));
    }

    private String status() {
        return jdbc.queryForObject("SELECT process_status FROM ai_knowledge_document WHERE id=2", String.class);
    }
}
