package com.mdframe.forge.lowcode.runtime.service.identity;

import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.datascope.context.DataScopeContext;
import com.mdframe.forge.starter.datascope.enums.DataScopeType;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Uses the existing object's action permission and data-scope owner for both read and write. */
@RequiredArgsConstructor
public final class RuntimeIdentityScope {
    private final IDataScopeService scopes;

    public record Scope(Long userId, boolean allOrganizations, boolean selfOnly, Set<Long> orgIds) {
        public boolean allowsOrg(Long id) { return id != null && id > 0 && (allOrganizations || orgIds.contains(id)); }
    }

    public Scope authorize(LowcodeRuntimeConfig config, String operation) {
        if (!Set.of("add", "edit").contains(StringUtils.defaultString(operation))) {
            throw failure(400, "请选择新增或编辑用途", "Select an add or edit operation");
        }
        LoginUser caller = SessionHelper.getLoginUser();
        if (caller == null || caller.getUserId() == null || caller.getTenantId() == null) {
            throw failure(401, "请先登录", "Authentication is required");
        }
        if (config == null || !caller.getTenantId().equals(config.getTenantId())
                || (StringUtils.isBlank(config.getObjectCode()) && StringUtils.isBlank(config.getReferencePermissionModule()))) {
            throw failure(403, "业务字段的租户或权限绑定无效", "The business field tenant or permission binding is invalid");
        }
        String module = StringUtils.defaultIfBlank(config.getReferencePermissionModule(), "ai:business:" + config.getObjectCode());
        if (!SessionHelper.hasPermission(module + ":" + operation)) {
            throw failure(403, "无权为此业务选择或更换引用", "Not allowed to select or change references for this business operation");
        }
        DataScopeContext context = scopes.getCurrentUserDataScope(module);
        if (context == null || !caller.getUserId().equals(context.getUserId()) || !caller.getTenantId().equals(context.getTenantId())) {
            throw failure(403, "未取得一致的业务数据范围", "A consistent business data scope is required");
        }
        DataScopeType type = DataScopeType.getByRoleDataScope(context.getMinDataScope(),
                context.getCustomOrgIds() != null && !context.getCustomOrgIds().isEmpty());
        List<Long> bound = context.getOrgIds() == null ? List.of() : context.getOrgIds();
        Set<Long> ids = switch (type == null ? DataScopeType.REGION : type) {
            case ALL, TENANT_ALL -> Set.of();
            case SELF, ORG -> new HashSet<>(bound);
            case ORG_AND_CHILD -> scopes.getOrgAndChildIds(bound);
            case CUSTOM -> context.getCustomOrgIds();
            default -> throw failure(400, "此组织选择暂不支持当前数据范围，请配置组织范围", "This selector requires an organization-based data scope");
        };
        return new Scope(caller.getUserId(), type == DataScopeType.ALL || type == DataScopeType.TENANT_ALL,
                type == DataScopeType.SELF, ids == null ? Set.of() : Set.copyOf(ids));
    }

    static BusinessException failure(int code, String chinese, String english) {
        return new BusinessException(code, UiMessages.label(chinese, english));
    }
}
