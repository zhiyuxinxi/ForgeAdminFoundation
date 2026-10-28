package com.mdframe.forge.plugin.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.lowcode.runtime.service.identity.RuntimeIdentityReferenceService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.identity.TenantMemberDirectory;
import com.mdframe.forge.starter.core.identity.TenantOrganizationDirectory;
import com.mdframe.forge.starter.core.identity.TenantPost;
import com.mdframe.forge.starter.core.identity.TenantPostDirectory;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RuntimePostReferenceServiceTest {
    private final TenantPostDirectory posts = mock(TenantPostDirectory.class);
    private final IDataScopeService scopes = mock(IDataScopeService.class);
    private final RuntimeIdentityReferenceService service = new RuntimeIdentityReferenceService(new ObjectMapper(), scopes,
            mock(TenantOrganizationDirectory.class), mock(TenantMemberDirectory.class), posts);
    private final LowcodeRuntimeConfig config = new LowcodeRuntimeConfig();
    private final DataScopeContext scope = new DataScopeContext();
    private MockedStatic<SessionHelper> session;

    @BeforeEach
    void prepare() {
        config.setTenantId(2L);
        config.setObjectCode("inspection");
        config.setModelSchema("""
                {"fields":[
                  {"field":"postId","columnName":"post_id","dataType":"bigint","componentType":"postSelect"},
                  {"field":"ownerId","dataType":"bigint","componentType":"userSelect"},
                  {"field":"locked","dataType":"bigint","componentType":"postSelect","readonly":true},
                  {"field":"external","componentType":"postSelect","basicProps":{"referenceSource":"EXTERNAL"}}
                ]}
                """);
        LoginUser caller = new LoginUser();
        caller.setUserId(9L);
        caller.setTenantId(2L);
        session = mockStatic(SessionHelper.class);
        session.when(SessionHelper::getLoginUser).thenReturn(caller);
        session.when(() -> SessionHelper.hasPermission("ai:business:inspection:add")).thenReturn(true);
        session.when(() -> SessionHelper.hasPermission("ai:business:inspection:edit")).thenReturn(true);
        scope.setUserId(9L);
        scope.setTenantId(2L);
        scope.setOrgIds(List.of(10L));
        scope.setMinDataScope(3);
        when(scopes.getCurrentUserDataScope("ai:business:inspection")).thenReturn(scope);
    }

    @AfterEach
    void close() { session.close(); }

    @Test
    void candidateAndWriteUseTheSameAuthorizedDirectOrganizationAndStablePostId() {
        TenantPost post = new TenantPost(101L, 10L, "A", "同名岗位");
        when(posts.findActivePostsByOrg(10L, "A", 100L, 20)).thenReturn(List.of(post));
        when(posts.findActivePosts(Set.of(101L), null)).thenReturn(List.of(post));
        assertThat(service.postScope(config, "postId", "add").tenantWideAllowed()).isFalse();
        assertThat(service.posts(config, "postId", "add", 10L, false, "A", 100L, 20)).containsExactly(post);
        service.validateWrite(config, Map.of("post_id", "101"), null, "add");
        assertCode(403, () -> service.posts(config, "postId", "add", 20L, false, null, null, 20));
        assertCode(400, () -> service.validateWrite(config, Map.of("postId", "岗位名称"), null, "add"));
    }

    @Test
    void tenantWidePostsRequireAllTenantScopeAndExplicitCandidateMode() {
        TenantPost global = new TenantPost(103L, null, "GLOBAL", "租户级岗位");
        when(posts.findActiveTenantWidePosts(null, null, 20)).thenReturn(List.of(global));
        when(posts.findActivePosts(Set.of(103L), null)).thenReturn(List.of(global));
        assertCode(403, () -> service.posts(config, "postId", "add", null, true, null, null, 20));
        assertCode(400, () -> service.validateWrite(config, Map.of("postId", "103"), null, "add"));
        for (int roleScope : new int[]{1, 2, 6}) {
            scope.setMinDataScope(roleScope);
            assertThat(service.postScope(config, "postId", "add").tenantWideAllowed()).isTrue();
            assertThat(service.posts(config, "postId", "add", null, true, null, null, 20)).containsExactly(global);
            service.validateWrite(config, Map.of("postId", "103"), null, "add");
        }
        assertCode(400, () -> service.posts(config, "postId", "add", 10L, true, null, null, 20));
        assertCode(403, () -> service.posts(config, "postId", "add", null, false, null, null, 20));
    }

    @Test
    void missingDisabledForeignAndOutOfScopePostsFailWhileUnchangedHistorySurvives() {
        when(posts.findActivePosts(Set.of(101L, 999L), null)).thenReturn(List.of(new TenantPost(101L, 10L, "A", "岗位")));
        assertCode(400, () -> service.validateWrite(config, Map.of("postId", List.of(101, 999)), null, "add"));
        when(posts.findActivePosts(Set.of(201L), null)).thenReturn(List.of(new TenantPost(201L, 20L, "B", "外部门")));
        assertCode(400, () -> service.validateWrite(config, Map.of("postId", 201), null, "add"));
        service.validateWrite(config, Map.of("postId", "999"), Map.of("postId", 999L), "edit");
        service.validateWrite(config, Map.of("title", "仅改标题"), Map.of("postId", 999L), "edit");
        assertCode(400, () -> service.validateWrite(config, Map.of("postId", 101, "post_id", 201), null, "add"));
    }

    @Test
    void selfScopeNeverInfersPostHoldingAndCannotEnumerateOrChangePosts() {
        scope.setMinDataScope(5);
        assertCode(403, () -> service.postScope(config, "postId", "add"));
        assertCode(403, () -> service.posts(config, "postId", "add", 10L, false, null, null, 20));
        assertCode(403, () -> service.validateWrite(config, Map.of("postId", 101), null, "add"));
        service.validateWrite(config, Map.of("postId", 101), Map.of("postId", "101"), "edit");
        verifyNoInteractions(posts);
    }

    @Test
    void fieldPermissionTenantAndPagingCannotBeSuppliedByTheClient() {
        for (String field : List.of("ownerId", "locked", "external", "invented")) {
            assertCode(403, () -> service.postScope(config, field, "add"));
        }
        assertCode(400, () -> service.posts(config, "postId", "add", 10L, false, null, null, 201));
        assertCode(400, () -> service.posts(config, "postId", "add", 10L, false, "x".repeat(101), null, 20));
        session.when(() -> SessionHelper.hasPermission("ai:business:inspection:add")).thenReturn(false);
        assertCode(403, () -> service.postScope(config, "postId", "add"));
        session.when(() -> SessionHelper.hasPermission("ai:business:inspection:add")).thenReturn(true);
        config.setTenantId(3L);
        assertCode(403, () -> service.postScope(config, "postId", "add"));
        verifyNoInteractions(posts);
    }

    private void assertCode(int code, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getCode()).isEqualTo(code));
    }
}
