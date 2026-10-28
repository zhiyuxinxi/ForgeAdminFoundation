package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.mdframe.forge.plugin.system.mapper.TenantOrganizationDirectoryMapper;
import com.mdframe.forge.starter.core.identity.TenantOrganization;
import com.mdframe.forge.starter.core.identity.TenantOrganizationDirectory;
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
import static org.mockito.Mockito.mock;

/** Production XML, strict interceptors, actual datasource routing and transaction advice on H2. */
class TenantOrganizationDirectoryMapperTest {
    private MockedStatic<SessionHelper> login;
    private DynamicRoutingDataSource routing;
    private DataSourceTransactionManager transactions;
    private JdbcTemplate master;
    private JdbcTemplate business;
    private TenantOrganizationDirectory directory;

    @BeforeEach
    void prepare() throws IOException {
        JdbcDataSource masterSource = source("org_master_");
        JdbcDataSource businessSource = source("org_business_");
        master = new JdbcTemplate(masterSource);
        business = new JdbcTemplate(businessSource);
        createOrganizations(master);
        createOrganizations(business);
        business.update("UPDATE sys_org SET org_name='业务库同ID' WHERE id=10");
        routing = new DynamicRoutingDataSource(List.of());
        routing.setPrimary("master");
        routing.setStrict(true);
        routing.addDataSource("master", masterSource);
        routing.addDataSource("business-db", businessSource);
        transactions = new DataSourceTransactionManager(routing);
        MybatisConfiguration configuration = configuration();
        SqlSessionTemplate sessions = new SqlSessionTemplate(new MybatisSqlSessionFactoryBuilder().build(configuration));
        SystemTenantOrganizationDirectory target = new SystemTenantOrganizationDirectory(
                sessions.getMapper(TenantOrganizationDirectoryMapper.class));
        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(transactions);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.addAdvice(interceptor);
        directory = (TenantOrganizationDirectory) proxy.getProxy();
        TenantContextHolder.setTenantId(2L);
        login = SystemTenantMemberDirectoryTest.login();
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
        Path xml = Path.of("src/main/resources/mapper/TenantOrganizationDirectoryMapper.xml");
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

    private void createOrganizations(JdbcTemplate jdbc) {
        jdbc.execute("CREATE TABLE sys_tenant(id BIGINT PRIMARY KEY,tenant_status INT,del_flag BIGINT)");
        jdbc.execute("CREATE TABLE sys_org(id BIGINT PRIMARY KEY,tenant_id BIGINT,parent_id BIGINT,org_name VARCHAR(80),org_status INT,del_flag BIGINT)");
        jdbc.execute("INSERT INTO sys_tenant VALUES(2,1,0),(3,1,0),(4,0,0),(5,1,5)");
        jdbc.execute("INSERT INTO sys_org VALUES(10,2,0,'同名组织',1,0),(11,2,0,'同名组织',1,0),"
                + "(12,2,0,'停用',0,0),(13,2,0,'删除',1,13),(20,3,0,'其他租户',1,0),"
                + "(30,2,10,'直属子组织',1,0),(31,2,10,'另一子组织',1,0),(32,2,30,'孙组织',1,0),"
                + "(40,2,20,'错误外租户父',1,0),(41,2,12,'停用父',1,0),(42,2,13,'删除父',1,0),"
                + "(43,2,999,'缺失父',1,0),(50,4,0,'停用租户',1,0),(51,5,0,'删除租户',1,0)");
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
    void batchLookupFiltersMissingDisabledDeletedAndOtherTenantOrganizations() {
        assertThat(directory.findActiveOrganizations(List.of(20L, 13L, 12L, 11L, 10L, 10L, 50L, 51L, 999L)))
                .containsExactly(new TenantOrganization(10L, 0L, "同名组织"), new TenantOrganization(11L, 0L, "同名组织"));
        assertThat(directory.findActiveOrganizations(List.of())).isEmpty();
    }

    @Test
    void childrenUseStablePagesAndRequireAnActiveParentInTheSameTenant() {
        assertThat(directory.findActiveChildren(0L, null, 1)).extracting(TenantOrganization::orgId).containsExactly(10L);
        assertThat(directory.findActiveChildren(0L, 10L, 1)).extracting(TenantOrganization::orgId).containsExactly(11L);
        assertThat(directory.findActiveChildren(0L, 11L, 1)).isEmpty();
        assertThat(directory.findActiveChildren(10L, null, 1)).extracting(TenantOrganization::orgId).containsExactly(30L);
        assertThat(directory.findActiveChildren(10L, 30L, 1)).extracting(TenantOrganization::orgId).containsExactly(31L);
        assertThat(directory.findActiveChildren(10L, 31L, 1)).isEmpty();
        for (Long parentId : List.of(20L, 12L, 13L, 999L)) {
            assertThat(directory.findActiveChildren(parentId, null, 200)).isEmpty();
        }
    }

    @Test
    void renamedAndRevokedOrganizationsAreNotHiddenByQueryCaching() {
        assertThat(directory.findActiveOrganizations(List.of(10L))).hasSize(1);
        master.update("UPDATE sys_org SET org_name='新名称' WHERE id=10");
        assertThat(directory.findActiveOrganizations(List.of(10L)).get(0).orgName()).isEqualTo("新名称");
        master.update("UPDATE sys_org SET org_status=0 WHERE id=10");
        assertThat(directory.findActiveOrganizations(List.of(10L))).isEmpty();
        assertThat(directory.findActiveChildren(10L, null, 200)).isEmpty();
        master.update("UPDATE sys_tenant SET tenant_status=0 WHERE id=2");
        assertThat(directory.findActiveOrganizations(List.of(11L))).isEmpty();
        assertThat(directory.findActiveChildren(0L, null, 200)).isEmpty();
        master.update("UPDATE sys_tenant SET tenant_status=1,del_flag=2 WHERE id=2");
        assertThat(directory.findActiveOrganizations(List.of(11L))).isEmpty();
    }

    @Test
    void transactionProxySuspendsBusinessTransactionAndReadsCommittedMasterData() {
        DynamicDataSourceContextHolder.push("business-db");
        JdbcTemplate routed = new JdbcTemplate(routing);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            routed.update("UPDATE sys_org SET org_name='业务未提交' WHERE id=10");
            assertThat(directory.findActiveOrganizations(List.of(10L)).get(0).orgName()).isEqualTo("同名组织");
            assertThat(DynamicDataSourceContextHolder.peek()).isEqualTo("business-db");
            assertThat(routed.queryForObject("SELECT org_name FROM sys_org WHERE id=10", String.class)).isEqualTo("业务未提交");
            status.setRollbackOnly();
        });
        assertThat(business.queryForObject("SELECT org_name FROM sys_org WHERE id=10", String.class)).isEqualTo("业务库同ID");
        assertThat(master.queryForObject("SELECT org_name FROM sys_org WHERE id=10", String.class)).isEqualTo("同名组织");
    }
}
