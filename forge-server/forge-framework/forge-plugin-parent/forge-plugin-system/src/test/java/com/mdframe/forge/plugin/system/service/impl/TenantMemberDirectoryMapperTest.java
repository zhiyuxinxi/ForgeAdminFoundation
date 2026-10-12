package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.mdframe.forge.plugin.system.mapper.TenantMemberDirectoryMapper;
import com.mdframe.forge.starter.core.identity.TenantMember;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.datascope.config.DataScopeProperties;
import com.mdframe.forge.starter.datascope.handler.DataScopeInterceptor;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import com.mdframe.forge.starter.tenant.config.TenantProperties;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import com.mdframe.forge.starter.tenant.handler.DefaultTenantLineHandler;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** Exercises production XML with tenant and strict data-scope interceptors, not mock SQL. */
class TenantMemberDirectoryMapperTest {
    private SqlSession session;
    private MockedStatic<SessionHelper> login;
    private JdbcTemplate jdbc;
    private SystemTenantMemberDirectory directory;

    @BeforeEach
    void prepare() throws IOException {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:tenant_members_" + UUID.randomUUID() + ";MODE=MySQL");
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setEnvironment(new Environment("test", new JdbcTransactionFactory(), source));
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
        Path xml = Path.of("src/main/resources/mapper/TenantMemberDirectoryMapper.xml");
        try (var input = Files.newInputStream(xml)) {
            new XMLMapperBuilder(input, configuration, xml.toString(), configuration.getSqlFragments()).parse();
        }
        session = new MybatisSqlSessionFactoryBuilder().build(configuration).openSession(true);
        jdbc = new JdbcTemplate(new SingleConnectionDataSource(session.getConnection(), true));
        directory = new SystemTenantMemberDirectory(session.getMapper(TenantMemberDirectoryMapper.class));
        TenantContextHolder.setTenantId(2L);
        login = SystemTenantMemberDirectoryTest.login();
        jdbc.execute("CREATE TABLE sys_user(id BIGINT PRIMARY KEY,tenant_id BIGINT,username VARCHAR(40),real_name VARCHAR(80),user_status INT,del_flag BIGINT)");
        jdbc.execute("CREATE TABLE sys_user_tenant(user_id BIGINT,tenant_id BIGINT,status INT)");
        jdbc.execute("CREATE TABLE sys_tenant(id BIGINT PRIMARY KEY,tenant_status INT,del_flag BIGINT)");
        jdbc.execute("CREATE TABLE sys_org(id BIGINT PRIMARY KEY,tenant_id BIGINT,org_status INT,del_flag BIGINT)");
        jdbc.execute("CREATE TABLE sys_user_org(user_id BIGINT,tenant_id BIGINT,org_id BIGINT)");
        jdbc.execute("INSERT INTO sys_tenant VALUES(1,1,0),(2,1,0),(3,1,0)");
        jdbc.execute("INSERT INTO sys_org VALUES(10,2,1,0),(11,2,0,0),(12,2,1,12),(20,3,1,0)");
        jdbc.execute("INSERT INTO sys_user VALUES(101,1,'alice','同名',1,0),(102,1,'bob','同名',1,0),"
                + "(103,2,'inactiveMember','成员停用',1,0),(104,2,'inactiveAccount','账号停用',0,0),"
                + "(105,2,'deleted','删除',1,105),(106,2,'foreign','外租户',1,0),"
                + "(107,2,'wrongOrg','组织跨租户',1,0),(108,1,'fallback','  ',1,0)");
        jdbc.execute("INSERT INTO sys_user_tenant VALUES(101,2,1),(102,2,1),(103,2,0),(104,2,1),"
                + "(105,2,1),(106,3,1),(107,2,1),(108,2,1)");
        jdbc.execute("INSERT INTO sys_user_org VALUES(101,2,10),(102,2,10),(103,2,10),(104,2,10),"
                + "(105,2,10),(106,3,10),(107,2,20),(108,2,11),(108,2,12)");
    }

    @AfterEach
    void close() {
        if (login != null) {
            login.close();
        }
        if (session != null) {
            session.close();
        }
        TenantContextHolder.clear();
    }

    @Test
    void recognizesMembershipRatherThanDefaultTenantAndFiltersDisabledOrForeignIdentities() {
        List<Long> requested = List.of(101L,102L,103L,104L,105L,106L,107L,108L,999L);
        assertThat(directory.findActiveMembers(requested, null)).extracting(TenantMember::userId)
                .containsExactly(101L,102L,107L,108L);
        assertThat(directory.findActiveMembers(requested, 10L)).extracting(TenantMember::userId)
                .containsExactly(101L,102L);
        assertThat(directory.findActiveMembers(List.of(108L), null).get(0).displayName()).isEqualTo("fallback");
        assertThat(directory.findActiveMembers(requested, 20L)).isEmpty();
        assertThat(directory.findActiveMembers(requested, 11L)).isEmpty();
        assertThat(directory.findActiveMembers(requested, 12L)).isEmpty();
    }

    @Test
    void sameNamesRemainDistinctAndKeysetPagesAreBoundedToTheRequestedOrganization() {
        List<TenantMember> first = directory.findActiveMembersByOrg(10L, "同名", null, 1);
        assertThat(first).extracting(TenantMember::userId).containsExactly(101L);
        assertThat(directory.findActiveMembersByOrg(10L, "同名", first.get(0).userId(), 1))
                .extracting(TenantMember::userId).containsExactly(102L);
        assertThat(directory.findActiveMembersByOrg(10L, "同名", 102L, 1)).isEmpty();
        assertThat(directory.findActiveMembersByOrg(20L, null, null, 20)).isEmpty();
    }

    @Test
    void currentMembershipOrgAndTenantRevocationAreNotHiddenByTheSessionQueryCache() {
        assertThat(directory.findActiveMembers(List.of(101L), 10L)).hasSize(1);
        jdbc.update("UPDATE sys_user SET real_name='新名字' WHERE id=101");
        assertThat(directory.findActiveMembers(List.of(101L), 10L).get(0))
                .isEqualTo(new TenantMember(101L,"alice","新名字"));
        jdbc.update("UPDATE sys_user_tenant SET status=0 WHERE user_id=101 AND tenant_id=2");
        assertThat(directory.findActiveMembers(List.of(101L), 10L)).isEmpty();
        jdbc.update("UPDATE sys_org SET org_status=0 WHERE id=10");
        assertThat(directory.findActiveMembers(List.of(102L), 10L)).isEmpty();
        assertThat(directory.findActiveMembers(List.of(102L), null)).hasSize(1);
        jdbc.update("UPDATE sys_tenant SET tenant_status=0 WHERE id=2");
        assertThat(directory.findActiveMembers(List.of(102L), null)).isEmpty();
    }
}
