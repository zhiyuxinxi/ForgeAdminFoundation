package com.mdframe.forge.plugin.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.lowcode.runtime.service.identity.RuntimeIdentityReferenceService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.identity.TenantMember;
import com.mdframe.forge.starter.core.identity.TenantMemberDirectory;
import com.mdframe.forge.starter.core.identity.TenantPostDirectory;
import com.mdframe.forge.starter.core.identity.TenantOrganization;
import com.mdframe.forge.starter.core.identity.TenantOrganizationDirectory;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.datascope.context.DataScopeContext;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RuntimeIdentityReferenceServiceTest {
    private final TenantOrganizationDirectory organizations = mock(TenantOrganizationDirectory.class);
    private final TenantMemberDirectory members = mock(TenantMemberDirectory.class);
    private final IDataScopeService scopes = mock(IDataScopeService.class);
    private final RuntimeIdentityReferenceService service = new RuntimeIdentityReferenceService(new ObjectMapper(), scopes, organizations, members, mock(TenantPostDirectory.class));
    private final LowcodeRuntimeConfig config = new LowcodeRuntimeConfig();
    private final DataScopeContext scope = new DataScopeContext();
    private MockedStatic<SessionHelper> session;

    @BeforeEach
    void prepare() {
        config.setTenantId(2L);
        config.setObjectCode("inspection");
        config.setConfigKey("inspection_form");
        config.setModelSchema("""
                {"fields":[
                  {"field":"orgId","columnName":"org_id","dataType":"bigint","componentType":"orgTreeSelect"},
                  {"field":"ownerId","columnName":"owner_id","dataType":"bigint","componentType":"userSelect"},
                  {"field":"lockedOrg","dataType":"bigint","componentType":"orgTreeSelect","readonly":true},
                  {"field":"externalOrg","componentType":"orgTreeSelect","basicProps":{"optionSource":{"api":"get@/contractors/options"}}}
                ]}
                """);
        LoginUser user = new LoginUser();
        user.setUserId(9L);
        user.setTenantId(2L);
        session = mockStatic(SessionHelper.class);
        session.when(SessionHelper::getLoginUser).thenReturn(user);
        session.when(() -> SessionHelper.hasPermission("ai:business:inspection:add")).thenReturn(true);
        session.when(() -> SessionHelper.hasPermission("ai:business:inspection:edit")).thenReturn(true);
        scope.setUserId(9L);
        scope.setTenantId(2L);
        scope.setOrgIds(List.of(10L));
        scope.setMinDataScope(3);
        when(scopes.getCurrentUserDataScope("ai:business:inspection")).thenReturn(scope);
        when(organizations.findActiveOrganizations(anyCollection())).thenAnswer(call -> {
            Set<Long> ids = Set.copyOf(call.getArgument(0));
            return ids.contains(10L) ? List.of(new TenantOrganization(10L, 0L, "现有组织")) : List.of();
        });
    }

    @AfterEach
    void close() { session.close(); }

    @Test
    void businessPermissionAllowsOwnOrganizationWithoutSystemManagementPermission() {
        assertThat(service.organizations(config, "orgId", "add", null, null, 20))
                .extracting(TenantOrganization::orgId).containsExactly(10L);
        service.validateWrite(config, Map.of("orgId", "10"), null, "add");
        session.verify(() -> SessionHelper.hasPermission("ai:business:inspection:add"), times(2));
        session.verify(() -> SessionHelper.hasPermission("system:org:list"), never());
    }

    @Test
    void clientOperationCannotGrantAWritePermissionAndConfigTenantMustMatch() {
        session.when(() -> SessionHelper.hasPermission("ai:business:inspection:edit")).thenReturn(false);
        assertCode(403, () -> service.organizations(config, "orgId", "edit", null, null, 20));
        assertCode(403, () -> service.validateWrite(config, Map.of("orgId", "10"), Map.of("orgId", "11"), "edit"));
        assertCode(400, () -> service.organizations(config, "orgId", "list", null, null, 20));
        config.setTenantId(3L);
        assertCode(403, () -> service.organizations(config, "orgId", "add", null, null, 20));
        verifyNoInteractions(organizations, members);
    }

    @Test
    void arbitraryReadonlyAndExternalFieldsCannotEnumerateTheDirectory() {
        for (String field : List.of("invented", "lockedOrg", "externalOrg")) {
            assertCode(403, () -> service.organizations(config, field, "add", null, null, 20));
        }
        service.validateWrite(config, Map.of("externalOrg", "contractor-A"), null, "add");
        config.setEditSchema("""
                {"fields":[{"field":"orgId","type":"orgTreeSelect","options":[{"value":"contractor-A","label":"External"}]}]}
                """);
        assertCode(403, () -> service.organizations(config, "orgId", "add", null, null, 20));
        service.validateWrite(config, Map.of("orgId", "contractor-A"), null, "add");
        verifyNoInteractions(organizations, members, scopes);
    }

    @Test
    void orgScopeIsTheSameForCandidatesAndChangedWrites() {
        assertCode(403, () -> service.organizations(config, "orgId", "add", 20L, null, 20));
        assertCode(400, () -> service.validateWrite(config, Map.of("orgId", 20L), null, "add"));
        when(organizations.findActiveOrganizations(anyCollection())).thenReturn(List.of());
        assertThat(service.organizations(config, "orgId", "add", null, null, 20)).isEmpty();
        assertCode(400, () -> service.validateWrite(config, Map.of("orgId", 10L), null, "add"));
    }

    @Test
    void unchangedInactiveHistoryAndPartialPatchesDoNotRequireANewReference() {
        service.validateWrite(config, Map.of("orgId", "99"), Map.of("org_id", 99L), "edit");
        service.validateWrite(config, Map.of("title", "new title"), Map.of("org_id", 99L), "edit");
        service.validateWrite(config, Map.of("main", Map.of("ownerId", "901")), Map.of("main", Map.of("owner_id", 901L)), "edit");
        verifyNoInteractions(organizations, members, scopes);
        assertCode(400, () -> service.validateWrite(config, Map.of("orgId", 98L), Map.of("org_id", 99L), "edit"));
    }

    @Test
    void aliasInjectionAndDisplayNamesCannotReplaceAStableIdentifier() {
        assertCode(400, () -> service.validateWrite(config, Map.of("orgId", 10L, "org_id", 20L), null, "add"));
        assertCode(400, () -> service.validateWrite(config, Map.of("ownerId", "同名"), null, "add"));
        assertCode(400, () -> service.validateWrite(config, Map.of("orgId", "9223372036854775808"), null, "add"));
        verifyNoInteractions(organizations, members, scopes);
    }

    @Test
    void membersRequireAuthorizedDirectOrganizationAndPreserveCursorAndSameNames() {
        List<TenantMember> page = List.of(new TenantMember(101L, "first", "同名"), new TenantMember(102L, "second", "同名"));
        when(members.findActiveMembersByOrg(10L, "同名", 100L, 20)).thenReturn(page);
        assertThat(service.members(config, "ownerId", "add", 10L, "同名", 100L, 20)).isEqualTo(page);
        assertCode(403, () -> service.members(config, "ownerId", "add", 20L, null, null, 20));
        assertCode(403, () -> service.members(config, "ownerId", "add", null, null, null, 20));
        assertCode(403, () -> service.members(config, "orgId", "add", 10L, null, null, 20));
        assertCode(400, () -> service.members(config, "ownerId", "add", 10L, null, null, 201));
    }

    @Test
    void inactiveMembersAndIncompleteMultiSelectionsAreRejected() {
        when(members.findActiveMembers(anyCollection(), eq(10L))).thenReturn(List.of(new TenantMember(101L, "first", "同名")));
        service.validateWrite(config, Map.of("ownerId", 101L), null, "add");
        assertCode(400, () -> service.validateWrite(config, Map.of("ownerId", List.of(101L, 102L)), null, "add"));
    }

    @Test
    void allTenantScopeStillUsesBoundedRootPagesWhileRegionScopeFailsExplicitly() {
        scope.setMinDataScope(2);
        when(organizations.findActiveChildren(0L, 10L, 20)).thenReturn(List.of(new TenantOrganization(11L, 0L, "下一组织")));
        assertThat(service.organizations(config, "orgId", "add", null, 10L, 20)).extracting(TenantOrganization::orgId).containsExactly(11L);
        scope.setMinDataScope(7);
        assertCode(400, () -> service.organizations(config, "orgId", "add", null, null, 20));
    }

    @Test
    void selfScopeCannotSelectAnotherUserEvenWhenActive() {
        scope.setMinDataScope(5);
        when(members.findActiveMembers(Set.of(9L), null)).thenReturn(List.of(new TenantMember(9L, "self", "本人")));
        service.validateWrite(config, Map.of("ownerId", 9L), null, "add");
        assertCode(403, () -> service.validateWrite(config, Map.of("ownerId", 101L), null, "add"));
    }

    private void assertCode(int code, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getCode()).isEqualTo(code));
    }
}
