package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.system.entity.SysResource;
import com.mdframe.forge.plugin.system.entity.SysRoleResource;
import com.mdframe.forge.plugin.system.mapper.SysResourceMapper;
import com.mdframe.forge.starter.tenant.config.TenantProperties;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class RoleResourceQueryBehaviorTest {
    @Test
    void namedClientQueriesMatchFormerWrappersForNullEmptyHiddenAndForeignRows() throws IOException {
        try (RoleResourceQueryFixture f = new RoleResourceQueryFixture()) {
            for (String client : new String[]{null, "", "pc", "h5", "unknown", "' OR 1=1 --"}) {
                List<Long> before = f.resources.selectList(clientQuery(client)).stream().map(SysResource::getId).toList();
                assertThat(f.resources.selectRoleAssignableResources(client)).extracting(SysResource::getId)
                        .containsExactlyElementsOf(before);
                assertThat(f.resources.selectRoleAssignableResourceIds(client)).containsExactlyElementsOf(before);
            }
            assertThat(f.resources.selectRoleAssignableResourceIds("pc")).containsExactly(1L, 2L, 3L, 4L, 9L, 11L);
            assertThat(f.resources.selectRoleAssignableResourceIds(null)).containsExactly(1L, 2L);
            // The production default shares the resource catalog, not role bindings, across tenants.
            f.tenant.setIgnoreTables(new TenantProperties().getIgnoreTables());
            List<Long> globalBefore = f.resources.selectList(clientQuery("pc")).stream().map(SysResource::getId).toList();
            assertThat(f.resources.selectRoleAssignableResourceIds("pc")).containsExactlyElementsOf(globalBefore)
                    .contains(7L, 8L).doesNotContain(5L);
        }
    }

    @Test
    void ancestorLookupKeepsLogicDeleteTenantAndExplicitIgnoreSemantics() throws IOException {
        try (RoleResourceQueryFixture f = new RoleResourceQueryFixture()) {
            RoleResourceCatalog catalog = new RoleResourceCatalog(f.resources);
            assertThat(catalog.withAncestors(Set.of(3L))).containsOnlyKeys(1L, 2L, 3L);
            assertThat(catalog.withAncestors(Set.of(4L))).containsOnlyKeys(4L);
            assertThat(catalog.withAncestors(Set.of(11L))).containsOnlyKeys(11L);
            assertThat(catalog.withAncestors(Set.of())).isEmpty();
            TenantContextHolder.executeIgnore(() -> {
                assertThat(catalog.withAncestors(Set.of(11L))).containsOnlyKeys(7L, 11L);
                assertThat(f.resources.selectRoleAssignableResourceIds("pc")).contains(7L, 8L).doesNotContain(5L);
            });
            assertThat(f.resources.selectRoleAssignableResourceIds("pc")).doesNotContain(7L, 8L);
        }
    }

    @Test
    void bindingProjectionKeepsOriginalOrderDuplicatesAndBothTenantConstraints() throws IOException {
        try (RoleResourceQueryFixture f = new RoleResourceQueryFixture()) {
            List<Long> before = f.bindings.selectList(new LambdaQueryWrapper<SysRoleResource>()
                    .eq(SysRoleResource::getRoleId, 7L).eq(SysRoleResource::getTenantId, 20L)
                    .select(SysRoleResource::getResourceId)).stream().map(SysRoleResource::getResourceId).toList();
            assertThat(before).containsExactly(9L, 1L, 9L);
            assertThat(f.bindings.selectRoleResourceIdsForProjection(20L, 7L)).containsExactlyElementsOf(before);
            assertThat(f.bindings.selectRoleResourceIdsForProjection(21L, 7L)).isEmpty();
            assertThat(f.bindings.selectRoleResourceIdsForProjection(null, 7L)).isEmpty();
            TenantContextHolder.executeIgnore(() -> {
                assertThat(f.bindings.selectRoleResourceIdsForProjection(20L, 7L)).containsExactlyElementsOf(before);
                assertThat(f.bindings.selectRoleResourceIdsForProjection(21L, 7L)).containsExactly(6L);
            });
        }
    }

    @Test
    void namedQueriesRetainOldDataScopeWhileExplicitNewPolicyTakesPrecedence() throws IOException {
        try (RoleResourceQueryFixture f = new RoleResourceQueryFixture()) {
            f.restrictToCreator("selectList", 10L);
            List<Long> before = f.resources.selectList(clientQuery("pc")).stream().map(SysResource::getId).toList();
            assertThat(before).containsExactly(1L, 2L, 3L, 4L, 11L);
            assertThat(f.resources.selectRoleAssignableResourceIds("pc")).containsExactlyElementsOf(before);
            assertThat(f.resources.selectRoleAssignableResources("pc")).extracting(SysResource::getId)
                    .containsExactlyElementsOf(before);
            when(f.scopes.getDataScopeConfig(SysResourceMapper.class.getName() + ".selectRoleAssignableResourceIds"))
                    .thenReturn(f.policy(false));
            assertThat(f.resources.selectRoleAssignableResourceIds("pc")).contains(9L);
            assertThat(f.resources.selectRoleAssignableResources("pc")).extracting(SysResource::getId).doesNotContain(9L);
        }
    }

    @Test
    void missingTenantOrScopeAndDatabaseFailuresAreNotTurnedIntoEmptySuccess() throws IOException {
        try (RoleResourceQueryFixture f = new RoleResourceQueryFixture()) {
            when(f.scopes.getDataScopeConfig(SysResourceMapper.class.getName() + ".selectList")).thenReturn(null);
            assertThatThrownBy(() -> f.resources.selectRoleAssignableResourceIds("pc"))
                    .hasMessageContaining("未配置");
            when(f.scopes.getDataScopeConfig(SysResourceMapper.class.getName() + ".selectList")).thenReturn(f.policy(false));
            TenantContextHolder.clear();
            assertThatThrownBy(() -> f.resources.selectRoleAssignableResources("pc"))
                    .hasMessageContaining("租户");
            TenantContextHolder.setTenantId(20L);
            f.jdbc.execute("DROP TABLE sys_resource");
            assertThatThrownBy(() -> new RoleResourceCatalog(f.resources).replacement(Set.of(), "pc"))
                    .hasMessageContaining("SYS_RESOURCE");
        }
    }

    private LambdaQueryWrapper<SysResource> clientQuery(String client) {
        return new LambdaQueryWrapper<SysResource>().and(scope -> scope.eq(SysResource::getClientCode, client)
                .or().isNull(SysResource::getClientCode).or().eq(SysResource::getClientCode, ""));
    }
}
