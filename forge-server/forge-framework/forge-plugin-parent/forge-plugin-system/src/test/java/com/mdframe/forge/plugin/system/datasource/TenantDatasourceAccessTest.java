package com.mdframe.forge.plugin.system.datasource;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.mdframe.forge.plugin.system.dto.SysTenantDTO;
import com.mdframe.forge.plugin.system.entity.SysTenant;
import com.mdframe.forge.plugin.system.mapper.SysTenantMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserPostMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserTenantMapper;
import com.mdframe.forge.plugin.system.mapper.TenantDatasourceGrantMapper;
import com.mdframe.forge.plugin.system.service.ISysConfigService;
import com.mdframe.forge.plugin.system.service.IUserLoadService;
import com.mdframe.forge.plugin.system.service.impl.SysTenantServiceImpl;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import com.mdframe.forge.starter.tenant.datasource.BusinessDataSourceProperties;
import com.mdframe.forge.starter.tenant.datasource.TenantBusinessDataSourceInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.ObjectProvider;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantDatasourceAccessTest {
    private final SysTenantMapper tenants = mock(SysTenantMapper.class);
    private final TenantDatasourceGrantMapper grants = mock(TenantDatasourceGrantMapper.class);
    private final TenantDatasourceAccessService access = new TenantDatasourceAccessService(grants, tenants);
    private final TenantDatasourceOption option = new TenantDatasourceOption();
    private final SysTenant tenant = new SysTenant();
    private SysTenantServiceImpl service;
    private SysTenantBusinessDataSourceResolver resolver;

    @BeforeEach
    void prepare() {
        option.setDatasourceId(3L);
        option.setDatasourceCode("business-a");
        option.setDatasourceName("Business A");
        option.setUsageScope("TENANT_BUSINESS");
        option.setIsEnabled(1);
        option.setReadonly(0);
        option.setAllowRuntimeWrite(1);
        option.setGranted(true);
        tenant.setId(7L);
        tenant.setDefaultBusinessDatasourceId(3L);
        tenant.setDefaultBusinessDatasourceCode("business-a");
        when(tenants.selectById(7L)).thenReturn(tenant);
        when(grants.selectDatasource(7L, 3L)).thenAnswer(call -> {
            assertThat(DynamicDataSourceContextHolder.peek()).isEqualTo("master");
            assertThat(TenantContextHolder.isIgnore()).isTrue();
            return option;
        });
        when(tenants.updateById(any(SysTenant.class))).thenReturn(1);
        service = new SysTenantServiceImpl(tenants, mock(SysUserMapper.class), mock(SysUserTenantMapper.class),
                mock(SysUserRoleMapper.class), mock(SysUserOrgRoleMapper.class), mock(SysUserOrgMapper.class),
                mock(SysUserPostMapper.class), mock(IUserLoadService.class), access);
        BusinessDataSourceProperties properties = new BusinessDataSourceProperties();
        properties.setEnabled(true);
        properties.setTenantRoutingEnabledDefault(true);
        ObjectProvider<DynamicRoutingDataSource> provider = mock(ObjectProvider.class);
        DynamicRoutingDataSource routing = mock(DynamicRoutingDataSource.class);
        when(provider.getIfAvailable()).thenReturn(routing);
        when(routing.getDataSources()).thenReturn(Map.of("business-a", mock(DataSource.class)));
        resolver = new SysTenantBusinessDataSourceResolver(properties, tenants, mock(ISysConfigService.class),
                provider, access);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ungranted", "disabled", "readonly", "no-write", "unknown-write", "wrong-purpose", "code-mismatch", "id-mismatch"})
    void rejectsSameInvalidSelectionInActualSaveAndResolver(String kind) {
        switch (kind) {
            case "ungranted" -> option.setGranted(false);
            case "disabled" -> option.setIsEnabled(0);
            case "readonly" -> option.setReadonly(1);
            case "no-write" -> option.setAllowRuntimeWrite(0);
            case "unknown-write" -> option.setAllowRuntimeWrite(null);
            case "wrong-purpose" -> option.setUsageScope("LOWCODE_RUNTIME");
            case "code-mismatch" -> tenant.setDefaultBusinessDatasourceCode("other-key");
            case "id-mismatch" -> tenant.setDefaultBusinessDatasourceId(99L);
            default -> throw new IllegalArgumentException(kind);
        }
        try (MockedStatic<SessionHelper> ignored = login(0)) {
            assertThatThrownBy(() -> service.updateTenant(request())).isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> resolver.resolve(7L)).isInstanceOf(BusinessException.class);
        }
        verify(tenants, never()).updateById(any(SysTenant.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"TENANT_BUSINESS", "BOTH"})
    void savesAndRoutesOnlyAuthorizedWritableMetadataAndRestoresContext(String purpose) {
        option.setUsageScope(purpose);
        DynamicDataSourceContextHolder.push("outer");
        try (MockedStatic<SessionHelper> ignored = login(0)) {
            assertThat(service.updateTenant(request())).isTrue();
            TenantBusinessDataSourceInfo info = resolver.resolve(7L);
            assertThat(info.getDatasourceId()).isEqualTo(3L);
            assertThat(info.getDsKey()).isEqualTo("business-a");
            try (var scope = resolver.use(info)) {
                assertThat(DynamicDataSourceContextHolder.peek()).isEqualTo("business-a");
            }
            assertThat(DynamicDataSourceContextHolder.peek()).isEqualTo("outer");
            assertThat(TenantContextHolder.isIgnore()).isFalse();
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
    }

    @Test
    void revocationAlsoRejectsPreviouslyResolvedInfoBeforeItCanBeReused() {
        TenantBusinessDataSourceInfo info = resolver.resolve(7L);
        option.setGranted(false);
        assertThatThrownBy(() -> resolver.use(info)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> resolver.resolve(7L)).isInstanceOf(BusinessException.class);
        assertThat(DynamicDataSourceContextHolder.peek()).isNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void onlySuperAdministratorCanListOrChangeGrants(int userType) {
        try (MockedStatic<SessionHelper> ignored = login(userType)) {
            assertThatThrownBy(() -> access.options(7L, false)).isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> access.setGrant(7L, 3L, true)).isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> access.setGrant(7L, 3L, false)).isInstanceOf(BusinessException.class);
        }
        verify(grants, never()).grant(7L, 3L, 100L);
        verify(grants, never()).revoke(7L, 3L);
    }

    @Test
    void administratorCanGrantAndRevokeButCannotGrantReadOnlyMetadata() {
        when(grants.selectOptions(7L)).thenReturn(List.of(option));
        try (MockedStatic<SessionHelper> ignored = login(0)) {
            assertThat(access.options(7L, true)).containsExactly(option);
            access.setGrant(7L, 3L, true);
            option.setReadonly(1);
            assertThatThrownBy(() -> access.setGrant(7L, 3L, true)).isInstanceOf(BusinessException.class);
            assertThat(access.options(7L, true)).isEmpty();
            access.setGrant(7L, 3L, false);
        }
        verify(grants).grant(7L, 3L, 100L);
        verify(grants).revoke(7L, 3L);
    }

    @Test
    void creationCannotSelfAuthorizeAndExplicitClearIsSeparateFromOmission() {
        try (MockedStatic<SessionHelper> ignored = login(0)) {
            assertThatThrownBy(() -> service.insertTenant(request())).isInstanceOf(BusinessException.class);
            SysTenantDTO omitted = new SysTenantDTO();
            omitted.setId(7L);
            service.updateTenant(omitted);
            verify(tenants, never()).clearBusinessDatasource(7L);
            omitted.setDefaultBusinessDatasourceCode("");
            service.updateTenant(omitted);
            verify(tenants).clearBusinessDatasource(7L);
        }
        verify(tenants, never()).insert(any(SysTenant.class));
    }

    private SysTenantDTO request() {
        SysTenantDTO dto = new SysTenantDTO();
        dto.setId(7L);
        dto.setDefaultBusinessDatasourceId(tenant.getDefaultBusinessDatasourceId());
        dto.setDefaultBusinessDatasourceCode(tenant.getDefaultBusinessDatasourceCode());
        return dto;
    }

    private MockedStatic<SessionHelper> login(int type) {
        LoginUser caller = new LoginUser();
        caller.setUserId(100L);
        caller.setUserType(type);
        caller.setTenantId(7L);
        MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class);
        session.when(SessionHelper::getLoginUser).thenReturn(caller);
        return session;
    }
}
