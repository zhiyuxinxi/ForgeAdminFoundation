package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mdframe.forge.plugin.system.dto.SysRoleDTO;
import com.mdframe.forge.plugin.system.entity.*;
import com.mdframe.forge.plugin.system.mapper.*;
import com.mdframe.forge.plugin.system.service.ISysResourceService;
import com.mdframe.forge.plugin.system.service.ISysRoleService;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.datascope.mapper.SysDataScopeConfigMapper;
import com.mdframe.forge.starter.datascope.mapper.SysRoleModuleDataScopeMapper;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SysRoleCreationTransactionTest {
    @BeforeAll
    static void metadata() {
        for (Class<?> type : List.of(SysRole.class, SysRoleOrg.class, SysOrg.class, SysTenant.class)) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "create-role"), type);
        }
    }

    @Test
    void failedOrganizationBindingRollsBackInsertedRoleThroughActualServiceProxy() {
        Fixture fixture = new Fixture();
        when(fixture.orgs.selectCount(any())).thenReturn(0L);
        try (MockedStatic<SessionHelper> session = fixture.identity()) {
            assertThatThrownBy(() -> fixture.proxy.insertRole(fixture.request()))
                    .hasMessage("组织不属于当前角色租户");
        }
        assertThat(fixture.jdbc.queryForObject("SELECT COUNT(*) FROM role_write", Integer.class)).isZero();
        verifyNoInteractions(fixture.scopes);
    }

    @Test
    void createPermissionCanCreateAndBindNewRoleWithoutPretendingItIsAlreadyAssigned() {
        Fixture fixture = new Fixture();
        when(fixture.orgs.selectCount(any())).thenReturn(1L);
        try (MockedStatic<SessionHelper> session = fixture.identity()) {
            assertThat(fixture.proxy.insertRole(fixture.request())).isTrue();
        }
        assertThat(fixture.jdbc.queryForObject("SELECT COUNT(*) FROM role_write", Integer.class)).isEqualTo(1);
        verify(fixture.roleOrgs).insert(any(SysRoleOrg.class));
        verify(fixture.scopes).refreshDataScopeCache();
    }

    private static final class Fixture {
        private final SysRoleMapper roles = mock(SysRoleMapper.class);
        private final SysOrgMapper orgs = mock(SysOrgMapper.class);
        private final SysRoleOrgMapper roleOrgs = mock(SysRoleOrgMapper.class);
        private final IDataScopeService scopes = mock(IDataScopeService.class);
        private final JdbcTemplate jdbc;
        private final ISysRoleService proxy;

        Fixture() {
            JdbcDataSource datasource = new JdbcDataSource();
            datasource.setURL("jdbc:h2:mem:role" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
            jdbc = new JdbcTemplate(datasource);
            jdbc.execute("CREATE TABLE role_write(id BIGINT PRIMARY KEY)");
            SysTenantMapper tenants = mock(SysTenantMapper.class);
            when(tenants.selectCount(any())).thenReturn(1L);
            when(roles.insert(any(SysRole.class))).thenAnswer(invocation -> {
                SysRole role = invocation.getArgument(0);
                role.setId(71L);
                return jdbc.update("INSERT INTO role_write(id) VALUES (?)", role.getId());
            });
            SysRoleServiceImpl service = new SysRoleServiceImpl(roles, roleOrgs, mock(SysRoleResourceMapper.class),
                    tenants, mock(SysUserRoleMapper.class), mock(SysUserOrgRoleMapper.class), orgs,
                    mock(SysUserMapper.class), mock(SysUserTenantMapper.class), mock(SysResourceMapper.class),
                    mock(SysDataScopeConfigMapper.class), mock(SysRoleModuleDataScopeMapper.class), scopes,
                    mock(ISysResourceService.class));
            ProxyFactory factory = new ProxyFactory(service);
            factory.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(datasource),
                    new AnnotationTransactionAttributeSource()));
            proxy = (ISysRoleService) factory.getProxy();
        }

        SysRoleDTO request() {
            SysRoleDTO request = new SysRoleDTO();
            request.setDataScope(5);
            request.setOrgIds(List.of(21L));
            return request;
        }

        MockedStatic<SessionHelper> identity() {
            LoginUser user = new LoginUser();
            user.setUserId(1L);
            user.setTenantId(2L);
            user.setUserType(2);
            user.setRoleIds(List.of());
            user.setPermissions(Set.of("system:role:add"));
            MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class);
            session.when(SessionHelper::getLoginUser).thenReturn(user);
            return session;
        }
    }
}
