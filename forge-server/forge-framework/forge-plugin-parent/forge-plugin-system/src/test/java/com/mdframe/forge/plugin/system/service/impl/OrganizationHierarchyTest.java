package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mdframe.forge.plugin.system.dto.SysOrgDTO;
import com.mdframe.forge.plugin.system.entity.SysOrg;
import com.mdframe.forge.plugin.system.entity.SysTenant;
import com.mdframe.forge.plugin.system.mapper.SysOrgMapper;
import com.mdframe.forge.plugin.system.manager.OrganizationHierarchy;
import com.mdframe.forge.plugin.system.mapper.SysTenantMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgMapper;
import com.mdframe.forge.plugin.system.service.IUserLoadService;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class OrganizationHierarchyTest {
    private final SysOrgMapper orgs = mock(SysOrgMapper.class);
    private final SysTenantMapper tenants = mock(SysTenantMapper.class);
    private final IDataScopeService scopes = mock(IDataScopeService.class);
    private final OrganizationHierarchy hierarchy = new OrganizationHierarchy(orgs);
    private final SysOrgServiceImpl service = new SysOrgServiceImpl(orgs, tenants,
            mock(SysUserOrgMapper.class), mock(IUserLoadService.class), scopes);
    private final TransactionTemplate transaction =
            new TransactionTemplate(new UserSessionRevocationTest.TestTransactions());

    @BeforeAll
    static void metadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "org-hierarchy"), SysTenant.class);
    }

    @Test
    void rejectsMissingForeignSelfAndDescendantParents() {
        assertThatThrownBy(() -> hierarchy.ancestors(10L, 20L, 2L)).isInstanceOf(IllegalArgumentException.class);
        when(orgs.selectOrgForUpdate(10L, 2L)).thenReturn(org(10L, 0L, "0"));
        assertThatThrownBy(() -> hierarchy.ancestors(10L, 10L, 2L)).isInstanceOf(IllegalArgumentException.class);
        when(orgs.selectOrgForUpdate(20L, 2L)).thenReturn(org(20L, 10L, "0,10"));
        assertThatThrownBy(() -> hierarchy.ancestors(10L, 20L, 2L)).isInstanceOf(IllegalArgumentException.class);
        verify(orgs, never()).updateDescendantAncestors(anyLong(), anyLong(), anyString());
    }

    @Test
    void movingOrganizationIgnoresClientAncestorsAndRefreshesOnlyAfterCommit() {
        SysOrg current = org(10L, 1L, "0,1");
        setup(current);
        when(orgs.selectOrgForUpdate(20L, 2L)).thenReturn(org(20L, 3L, "0,3"));
        SysOrgDTO request = request(20L);
        request.setAncestors("0,999");
        try (MockedStatic<SessionHelper> session = identity()) {
            transaction.executeWithoutResult(status -> {
                assertThat(service.updateOrg(request)).isTrue();
                verifyNoInteractions(scopes);
            });
        }
        ArgumentCaptor<SysOrg> update = ArgumentCaptor.forClass(SysOrg.class);
        verify(orgs).updateById(update.capture());
        assertThat(update.getValue().getAncestors()).isEqualTo("0,3,20");
        verify(orgs).lockOrganizationTenant(2L);
        verify(orgs).updateDescendantAncestors(2L, 10L, "0,3,20");
        verify(scopes).refreshDataScopeCache();
    }

    @Test
    void updateWithoutParentPreservesParentAndRollbackDoesNotRefreshScope() {
        setup(org(10L, 20L, "0,20"));
        when(orgs.selectOrgForUpdate(20L, 2L)).thenReturn(org(20L, 0L, "0"));
        try (MockedStatic<SessionHelper> session = identity()) {
            transaction.executeWithoutResult(status -> {
                assertThat(service.updateOrg(request(null))).isTrue();
                status.setRollbackOnly();
            });
        }
        ArgumentCaptor<SysOrg> update = ArgumentCaptor.forClass(SysOrg.class);
        verify(orgs).updateById(update.capture());
        assertThat(update.getValue().getParentId()).isEqualTo(20L);
        assertThat(update.getValue().getAncestors()).isEqualTo("0,20");
        verify(orgs, never()).updateDescendantAncestors(anyLong(), anyLong(), anyString());
        verifyNoInteractions(scopes);
    }

    @Test
    void rootMoveProducesRootChainAndTenantCannotBeChanged() {
        assertThat(hierarchy.ancestors(10L, 0L, 2L)).isEqualTo("0");
        setup(org(10L, 0L, "0"));
        SysOrgDTO request = request(0L);
        request.setTenantId(3L);
        try (MockedStatic<SessionHelper> session = identity()) {
            assertThatThrownBy(() -> service.updateOrg(request)).isInstanceOf(IllegalArgumentException.class);
        }
        verify(orgs, never()).updateById(any(SysOrg.class));
    }

    private void setup(SysOrg current) {
        when(orgs.selectById(10L)).thenReturn(current);
        when(orgs.selectOrgForUpdate(10L, 2L)).thenReturn(current);
        when(orgs.lockOrganizationTenant(2L)).thenReturn(2L);
        when(tenants.selectCount(any())).thenReturn(1L);
        when(orgs.updateById(any(SysOrg.class))).thenReturn(1);
    }

    private SysOrgDTO request(Long parentId) {
        SysOrgDTO request = new SysOrgDTO();
        request.setId(10L);
        request.setParentId(parentId);
        return request;
    }

    private SysOrg org(Long id, Long parentId, String ancestors) {
        SysOrg org = new SysOrg();
        org.setId(id);
        org.setTenantId(2L);
        org.setParentId(parentId);
        org.setAncestors(ancestors);
        return org;
    }

    private MockedStatic<SessionHelper> identity() {
        LoginUser user = new LoginUser();
        user.setUserId(1L);
        user.setTenantId(2L);
        user.setUserType(0);
        MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class);
        session.when(SessionHelper::getLoginUser).thenReturn(user);
        return session;
    }
}
