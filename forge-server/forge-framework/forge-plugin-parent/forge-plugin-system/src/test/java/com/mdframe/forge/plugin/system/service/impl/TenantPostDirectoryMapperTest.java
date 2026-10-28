package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.mdframe.forge.plugin.system.mapper.TenantPostDirectoryMapper;
import com.mdframe.forge.starter.core.identity.TenantPost;
import com.mdframe.forge.starter.core.identity.TenantPostDirectory;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.datascope.config.DataScopeProperties;
import com.mdframe.forge.starter.datascope.context.DataScopeContextHolder;
import com.mdframe.forge.starter.datascope.handler.DataScopeInterceptor;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import com.mdframe.forge.starter.tenant.config.TenantProperties;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import com.mdframe.forge.starter.tenant.handler.DefaultTenantLineHandler;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/** Production XML, strict interceptors, actual datasource routing and transaction advice on H2. */
class TenantPostDirectoryMapperTest {
    private MockedStatic<SessionHelper> login;
    private DynamicRoutingDataSource routing;
    private DataSourceTransactionManager transactions;
    private JdbcTemplate master;
    private JdbcTemplate business;
    private TenantPostDirectory directory;

    @BeforeEach
    void prepare() throws IOException {
        JdbcDataSource masterSource = source("post_master_");
        JdbcDataSource businessSource = source("post_business_");
        master = new JdbcTemplate(masterSource);
        business = new JdbcTemplate(businessSource);
        createPosts(master);
        createPosts(business);
        business.update("UPDATE sys_post SET post_name='业务库同ID' WHERE id=101");
        routing = new DynamicRoutingDataSource(List.of());
        routing.setPrimary("master");
        routing.setStrict(true);
        routing.addDataSource("master", masterSource);
        routing.addDataSource("business-db", businessSource);
        transactions = new DataSourceTransactionManager(routing);
        MybatisConfiguration configuration = configuration();
        SqlSessionTemplate sessions = new SqlSessionTemplate(new MybatisSqlSessionFactoryBuilder().build(configuration));
        SystemTenantPostDirectory target = new SystemTenantPostDirectory(
                sessions.getMapper(TenantPostDirectoryMapper.class));
        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(transactions);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.addAdvice(interceptor);
        directory = (TenantPostDirectory) proxy.getProxy();
        TenantContextHolder.setTenantId(2L);
        login = SystemTenantPostDirectoryTest.login();
    }

    private MybatisConfiguration configuration() throws IOException {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setEnvironment(new Environment("test", new SpringManagedTransactionFactory(), routing));
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
        Path xml = Path.of("src/main/resources/mapper/TenantPostDirectoryMapper.xml");
        try (var input = Files.newInputStream(xml)) {
            new XMLMapperBuilder(input, configuration, xml.toString(), configuration.getSqlFragments()).parse();
        }
        return configuration;
    }

    private JdbcDataSource source(String prefix) {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:" + prefix + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        return source;
    }

    private void createPosts(JdbcTemplate jdbc) {
        jdbc.execute("CREATE TABLE sys_tenant(id BIGINT PRIMARY KEY,tenant_status INT,del_flag BIGINT)");
        jdbc.execute("CREATE TABLE sys_org(id BIGINT PRIMARY KEY,tenant_id BIGINT,parent_id BIGINT,org_status INT,del_flag BIGINT)");
        jdbc.execute("CREATE TABLE sys_post(id BIGINT PRIMARY KEY,tenant_id BIGINT,org_id BIGINT,post_code VARCHAR(40),post_name VARCHAR(80),post_status INT,del_flag BIGINT)");
        jdbc.execute("INSERT INTO sys_tenant VALUES(2,1,0),(3,1,0),(4,0,0),(5,1,5)");
        jdbc.execute("INSERT INTO sys_org VALUES(10,2,0,1,0),(11,2,10,1,0),(12,2,0,0,0),(13,2,0,1,13),(20,3,0,1,0)");
        jdbc.execute("INSERT INTO sys_post VALUES(101,2,10,'A','同名岗位',1,0),(102,2,10,'B','同名岗位',1,0),"
                + "(103,2,NULL,'GLOBAL','租户级岗位',1,0),(104,2,11,'CHILD','子部门岗位',1,0),"
                + "(105,2,10,'DISABLED','停用岗位',0,0),(106,2,10,'DELETED','删除岗位',1,106),"
                + "(107,3,20,'FOREIGN','外租户岗位',1,0),(108,2,20,'BAD_ORG','外租户组织',1,0),"
                + "(109,2,12,'OFF_ORG','停用组织',1,0),(110,2,13,'DEL_ORG','删除组织',1,0),"
                + "(111,2,999,'LOST_ORG','缺失组织',1,0),(112,4,NULL,'OFF_TENANT','停用租户',1,0),"
                + "(113,5,NULL,'DEL_TENANT','删除租户',1,0),(114,3,NULL,'FOREIGN_GLOBAL','外租户全局',1,0)");
    }

    @AfterEach
    void close() throws Exception {
        if (login != null) {
            login.close();
        }
        if (master != null) {
            master.execute("SHUTDOWN");
        }
        if (business != null) {
            business.execute("SHUTDOWN");
        }
        if (routing != null) {
            routing.destroy();
        }
        TenantContextHolder.clear();
        DataScopeContextHolder.clearSkip();
        DynamicDataSourceContextHolder.clear();
    }

    @Test
    void batchUsesStableIdsAndFiltersInactiveForeignAndInvalidOrganizationPosts() {
        List<Long> requested = List.of(101L,102L,103L,104L,105L,106L,107L,108L,109L,110L,111L,112L,113L,114L,999L);
        assertThat(directory.findActivePosts(requested, null)).extracting(TenantPost::postId)
                .containsExactly(101L,102L,103L,104L);
        assertThat(directory.findActivePosts(List.of(101L,101L,102L), 10L)).containsExactly(
                new TenantPost(101L,10L,"A","同名岗位"), new TenantPost(102L,10L,"B","同名岗位"));
        assertThat(directory.findActivePosts(List.of(), null)).isEmpty();
        assertThat(directory.findActivePosts(List.of(103L), null)).containsExactly(
                new TenantPost(103L,null,"GLOBAL","租户级岗位"));
        assertThat(directory.findActivePosts(requested, 10L)).extracting(TenantPost::postId)
                .containsExactly(101L,102L);
        for (Long orgId : List.of(12L,13L,20L,999L)) {
            assertThat(directory.findActivePosts(requested, orgId)).isEmpty();
        }
    }

    @Test
    void directOrganizationPagesNeverIncludeTenantWideOrDescendantPosts() {
        assertThat(directory.findActivePostsByOrg(10L, "同名", null, 1))
                .extracting(TenantPost::postId).containsExactly(101L);
        assertThat(directory.findActivePostsByOrg(10L, "同名", 101L, 1))
                .extracting(TenantPost::postId).containsExactly(102L);
        assertThat(directory.findActivePostsByOrg(10L, null, 102L, 200)).isEmpty();
        assertThat(directory.findActivePostsByOrg(10L, "B", null, 200))
                .extracting(TenantPost::postId).containsExactly(102L);
        assertThat(directory.findActivePostsByOrg(11L, null, null, 200))
                .extracting(TenantPost::postId).containsExactly(104L);
        for (Long orgId : List.of(12L,13L,20L,999L)) {
            assertThat(directory.findActivePostsByOrg(orgId, null, null, 200)).isEmpty();
        }
        assertThat(directory.findActivePostsByOrg(10L, "' OR 1=1 --", null, 200)).isEmpty();
    }

    @Test
    void currentNamesAndRevokedPostOrganizationOrTenantBypassNoQueryCache() {
        assertThat(directory.findActivePosts(List.of(101L), 10L)).hasSize(1);
        master.update("UPDATE sys_post SET post_name='新名称' WHERE id=101");
        assertThat(directory.findActivePosts(List.of(101L), 10L).get(0).postName()).isEqualTo("新名称");
        master.update("UPDATE sys_post SET post_status=0 WHERE id=101");
        assertThat(directory.findActivePosts(List.of(101L), null)).isEmpty();
        master.update("UPDATE sys_org SET org_status=0 WHERE id=10");
        assertThat(directory.findActivePosts(List.of(102L), null)).isEmpty();
        assertThat(directory.findActivePostsByOrg(10L, null, null, 200)).isEmpty();
        assertThat(directory.findActivePosts(List.of(103L), null)).hasSize(1);
        master.update("UPDATE sys_tenant SET tenant_status=0 WHERE id=2");
        assertThat(directory.findActivePosts(List.of(103L), null)).isEmpty();
        master.update("UPDATE sys_tenant SET tenant_status=1,del_flag=2 WHERE id=2");
        assertThat(directory.findActivePosts(List.of(103L), null)).isEmpty();
    }

    @Test
    void explicitTenantWidePagesExcludeDepartmentPostsAndOtherOrDisabledTenants() {
        assertThat(directory.findActiveTenantWidePosts(null, null, 1))
                .containsExactly(new TenantPost(103L, null, "GLOBAL", "租户级岗位"));
        assertThat(directory.findActiveTenantWidePosts(null, 103L, 200)).isEmpty();
        assertThat(directory.findActiveTenantWidePosts("GLOBAL", null, 20)).hasSize(1);
        assertThat(directory.findActiveTenantWidePosts("同名", null, 20)).isEmpty();
        master.update("UPDATE sys_post SET post_status=0 WHERE id=103");
        assertThat(directory.findActiveTenantWidePosts(null, null, 200)).isEmpty();
    }

    @Test
    void transactionProxySuspendsBusinessTransactionAndRestoresItAfterSuccessAndFailure() {
        DynamicDataSourceContextHolder.push("business-db");
        JdbcTemplate routed = new JdbcTemplate(routing);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            routed.update("UPDATE sys_post SET post_name='业务未提交' WHERE id=101");
            assertThat(directory.findActivePosts(List.of(101L), 10L).get(0).postName()).isEqualTo("同名岗位");
            assertThat(DynamicDataSourceContextHolder.peek()).isEqualTo("business-db");
            master.execute("DROP TABLE sys_post");
            assertThatThrownBy(() -> directory.findActivePosts(List.of(101L), 10L)).isInstanceOf(RuntimeException.class);
            assertThat(DynamicDataSourceContextHolder.peek()).isEqualTo("business-db");
            assertThat(DataScopeContextHolder.isSkip()).isFalse();
            assertThat(TenantContextHolder.getTenantId()).isEqualTo(2L);
            assertThat(routed.queryForObject("SELECT post_name FROM sys_post WHERE id=101", String.class)).isEqualTo("业务未提交");
            status.setRollbackOnly();
        });
        assertThat(business.queryForObject("SELECT post_name FROM sys_post WHERE id=101", String.class)).isEqualTo("业务库同ID");
    }
}
