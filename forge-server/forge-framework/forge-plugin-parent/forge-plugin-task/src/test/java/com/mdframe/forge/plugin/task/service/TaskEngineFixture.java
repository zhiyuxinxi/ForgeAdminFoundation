package com.mdframe.forge.plugin.task.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.task.mapper.BusinessTaskMapper;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.identity.TenantMember;
import com.mdframe.forge.starter.core.identity.TenantMemberDirectory;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.datascope.config.DataScopeProperties;
import com.mdframe.forge.starter.datascope.context.DataScopeContextHolder;
import com.mdframe.forge.starter.datascope.handler.DataScopeInterceptor;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import com.mdframe.forge.starter.tenant.config.TenantProperties;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import com.mdframe.forge.starter.tenant.handler.DefaultTenantLineHandler;
import com.mdframe.forge.task.api.BusinessTask;
import com.mdframe.forge.task.api.TaskSourceHandler;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Real SQL/interceptor/transaction setup shared by this task engine's behavioral tests. */
class TaskEngineFixture {
    protected JdbcTemplate jdbc;
    protected DefaultBusinessTaskEngine engine;
    protected DataSourceTransactionManager transactionManager;
    protected Source source;
    protected volatile boolean memberEnabled = true;

    @BeforeEach
    void prepare() throws Exception {
        JdbcDataSource datasource = new JdbcDataSource();
        datasource.setURL("jdbc:h2:mem:tasks_" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000");
        jdbc = new JdbcTemplate(datasource);
        String ddl = Files.readString(Path.of("../../../db/migration/V1.0.2026101501__public_business_task_engine.sql"));
        // H2 cannot represent MySQL's per-column utf8mb4 binary collation.
        ddl = ddl.replace(" CHARACTER SET utf8mb4 COLLATE utf8mb4_bin", "")
                .replace(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4", "");
        for (String statement : ddl.split(";")) {
            if (!statement.isBlank()) {
                jdbc.execute(statement);
            }
        }
        jdbc.execute("CREATE TABLE task_test_source(tenant_id BIGINT,source_key VARCHAR(128),"
                + "writes INT,state VARCHAR(24),PRIMARY KEY(tenant_id,source_key))");
        jdbc.update("INSERT INTO task_test_source VALUES (7,'record-1',0,'OPEN'),(8,'record-1',0,'OPEN')");

        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        TenantProperties tenant = new TenantProperties();
        tenant.setIgnoreTables(List.of());
        tenant.setStrictMode(true);
        tenant.setAutoDetectTenantColumn(false);
        MybatisPlusInterceptor interceptors = new MybatisPlusInterceptor();
        interceptors.addInnerInterceptor(new TenantLineInnerInterceptor(new DefaultTenantLineHandler(tenant)));
        DataScopeProperties scope = new DataScopeProperties();
        scope.setUnconfiguredPolicy(DataScopeProperties.UnconfiguredPolicy.DENY);
        interceptors.addInnerInterceptor(new DataScopeInterceptor(mock(IDataScopeService.class), scope));
        configuration.addInterceptor(interceptors);
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(datasource);
        factory.setConfiguration(configuration);
        factory.setMapperLocations(new ClassPathResource("mapper/BusinessTaskMapper.xml"));
        BusinessTaskMapper mapper = new SqlSessionTemplate(factory.getObject()).getMapper(BusinessTaskMapper.class);
        transactionManager = new DataSourceTransactionManager(datasource);
        source = new Source();
        TaskPersistence store = new TaskPersistence(mapper);
        TaskSources sources = new TaskSources(List.of(source));
        TaskCatalog catalog = new TaskCatalog(store, sources, new Members(), transactionManager);
        engine = new DefaultBusinessTaskEngine(store, sources, catalog, new ObjectMapper(), transactionManager,
                mock(TaskPeriodConstraints.class));
    }

    protected <T> T as(long tenant, long user, Supplier<T> action) {
        LoginUser login = new LoginUser();
        login.setTenantId(tenant);
        login.setUserId(user);
        login.setUserType(2);
        login.setPermissions(user == 10 ? Set.of("task:definition:write") : Set.of());
        ExecutionIdentity identity = new ExecutionIdentity(login, "USER", user, null, 1L, "test", "test", Set.of());
        Long previous = TenantContextHolder.getTenantId();
        TenantContextHolder.setTenantId(tenant);
        try (var ignored = ExecutionIdentityContextHolder.open(identity)) {
            return action.get();
        } finally {
            if (previous == null) {
                TenantContextHolder.clear();
            } else {
                TenantContextHolder.setTenantId(previous);
            }
        }
    }

    protected class Members implements TenantMemberDirectory {
        @Override
        public List<TenantMember> findActiveMembers(Collection<Long> ids, Long org) {
            return ids.stream().filter(id -> memberEnabled && (id == 10 || id == 20 || id == 30))
                    .map(id -> new TenantMember(id, "member" + id, "Member " + id)).toList();
        }

        @Override
        public List<TenantMember> findActiveMembersByOrg(Long org, String keyword, Long after, int limit) {
            throw new UnsupportedOperationException("Task engine only requires stable-ID membership lookup");
        }

        @Override
        public List<TenantMember> findActiveMembersByPost(Long post, Long after, int limit) {
            throw new UnsupportedOperationException("Task engine only requires stable-ID membership lookup");
        }
    }

    protected class Source implements TaskSourceHandler {
        protected volatile boolean failWriteBack;

        @Override
        public String sourceType() { return "test.source"; }

        @Override
        public String transactionDataSource() { return "master"; }

        @Override
        public void authorize(String key, Actor actor, Operation operation) {
            assertThat(DataScopeContextHolder.isSkip()).isFalse();
            Integer found = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM task_test_source WHERE tenant_id=? AND source_key=?",
                    Integer.class, actor.tenantId(), key);
            if (found == null || found != 1 || ((operation == Operation.CREATE
                    || operation == Operation.DISPATCH || operation == Operation.CANCEL) && actor.userId() != 10)) {
                throw new BusinessException(403, "source denied");
            }
        }

        @Override
        public void validateCompletion(BusinessTask task, String result, Actor actor) {
            if (!result.contains("\"ok\":true")) {
                throw new BusinessException(400, "completion evidence required");
            }
        }

        @Override
        public void validateCreation(com.mdframe.forge.task.api.TaskDefinition definition,
                                     com.mdframe.forge.task.api.BusinessTaskEngine.Create command, Actor actor) {
            if (command.assigneeId() != 20) {
                throw new BusinessException(403, "source assignee denied");
            }
        }

        @Override
        public void onTerminal(BusinessTask task, Actor actor) {
            assertThat(DataScopeContextHolder.isSkip()).isFalse();
            jdbc.update("UPDATE task_test_source SET writes=writes+1,state=? WHERE tenant_id=? AND source_key=?",
                    task.state().name(), actor.tenantId(), task.sourceKey());
            if (failWriteBack) {
                throw new IllegalStateException("source rejected");
            }
        }
    }
}
