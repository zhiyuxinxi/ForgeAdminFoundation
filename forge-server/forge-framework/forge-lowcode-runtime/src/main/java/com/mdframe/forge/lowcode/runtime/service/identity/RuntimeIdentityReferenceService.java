package com.mdframe.forge.lowcode.runtime.service.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.starter.core.identity.TenantMember;
import com.mdframe.forge.starter.core.identity.TenantMemberDirectory;
import com.mdframe.forge.starter.core.identity.TenantOrganization;
import com.mdframe.forge.starter.core.identity.TenantOrganizationDirectory;
import com.mdframe.forge.starter.core.identity.TenantPost;
import com.mdframe.forge.starter.core.identity.TenantPostDirectory;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.mdframe.forge.lowcode.runtime.service.identity.RuntimeIdentityScope.failure;

/** Candidate and changed-reference validation share the same trusted field and authorization. */
@Service
public final class RuntimeIdentityReferenceService {
    private final RuntimeIdentityFields fields;
    private final RuntimeIdentityScope scopes;
    private final TenantOrganizationDirectory organizations;
    private final TenantMemberDirectory members;
    private final TenantPostDirectory posts;

    public RuntimeIdentityReferenceService(ObjectMapper json, IDataScopeService scopes,
                                           TenantOrganizationDirectory organizations, TenantMemberDirectory members, TenantPostDirectory posts) {
        this.fields = new RuntimeIdentityFields(json);
        this.scopes = new RuntimeIdentityScope(scopes);
        this.organizations = organizations;
        this.members = members;
        this.posts = posts;
    }

    public List<TenantOrganization> organizations(LowcodeRuntimeConfig config, String field, String operation,
                                                 Long parentId, Long afterOrgId, int limit) {
        fields.require(config, field);
        RuntimeIdentityScope.Scope scope = scopes.authorize(config, operation);
        checkPage(afterOrgId, limit);
        if (parentId != null && (parentId < 0 || (parentId != 0 && !scope.allowsOrg(parentId)))) {
            throw failure(403, "组织不在本业务允许范围", "The organization is outside the allowed business scope");
        }
        if (scope.allOrganizations()) {
            return organizations.findActiveChildren(parentId == null ? 0L : parentId, afterOrgId, limit);
        }
        List<TenantOrganization> allowed = loadOrganizations(scope.orgIds());
        Set<Long> ids = new HashSet<>(allowed.stream().map(TenantOrganization::orgId).toList());
        return allowed.stream().filter(org -> parentId == null ? !ids.contains(org.parentId()) : parentId.equals(org.parentId()))
                .filter(org -> afterOrgId == null || org.orgId() > afterOrgId).sorted(Comparator.comparing(TenantOrganization::orgId))
                .limit(limit).toList();
    }

    public List<TenantMember> members(LowcodeRuntimeConfig config, String field, String operation,
                                     Long directOrgId, String keyword, Long afterUserId, int limit) {
        if (!fields.require(config, field).user()) {
            throw failure(403, "该字段不是人员引用", "This field is not a member reference");
        }
        RuntimeIdentityScope.Scope scope = scopes.authorize(config, operation);
        checkPage(afterUserId, limit);
        if (keyword != null && keyword.length() > 100) {
            throw failure(400, "人员搜索词超过上限", "The member search keyword is too long");
        }
        if (!scope.allowsOrg(directOrgId)) {
            throw failure(403, "请选择允许范围内的组织", "Select an organization within the allowed scope");
        }
        if (scope.selfOnly()) {
            return members.findActiveMembers(List.of(scope.userId()), directOrgId).stream()
                    .filter(member -> afterUserId == null || member.userId() > afterUserId)
                    .filter(member -> keyword == null || keyword.isBlank() || member.username().contains(keyword.trim())
                            || member.displayName().contains(keyword.trim())).toList();
        }
        return members.findActiveMembersByOrg(directOrgId, keyword, afterUserId, limit);
    }

    public record PostScope(boolean tenantWideAllowed) { }

    public PostScope postScope(LowcodeRuntimeConfig config, String field, String operation) {
        return new PostScope(authorizePost(config, field, operation).allOrganizations());
    }

    public List<TenantPost> posts(LowcodeRuntimeConfig config, String field, String operation,
                                 Long directOrgId, boolean tenantWide, String keyword, Long afterPostId, int limit) {
        RuntimeIdentityScope.Scope scope = authorizePost(config, field, operation);
        checkPage(afterPostId, limit);
        if ((keyword != null && keyword.length() > 100) || (tenantWide && directOrgId != null)) {
            throw failure(400, "岗位查询参数无效", "Invalid post lookup parameters");
        }
        if (tenantWide) {
            if (!scope.allOrganizations()) {
                throw failure(403, "当前业务范围不允许选择租户级岗位", "The business scope does not allow tenant-wide posts");
            }
            return posts.findActiveTenantWidePosts(keyword, afterPostId, limit);
        }
        if (!scope.allowsOrg(directOrgId)) {
            throw failure(403, "请选择允许范围内的组织", "Select an organization within the allowed scope");
        }
        return posts.findActivePostsByOrg(directOrgId, keyword, afterPostId, limit);
    }

    private RuntimeIdentityScope.Scope authorizePost(LowcodeRuntimeConfig config, String field, String operation) {
        if (!fields.require(config, field).post()) {
            throw failure(403, "该字段不是岗位引用", "This field is not a post reference");
        }
        RuntimeIdentityScope.Scope scope = scopes.authorize(config, operation);
        requirePostScope(scope);
        return scope;
    }

    private void requirePostScope(RuntimeIdentityScope.Scope scope) {
        if (scope.selfOnly()) {
            throw failure(403, "本人范围不能推断可选岗位，请配置岗位组织范围", "Self scope cannot infer held posts; configure an organization scope");
        }
    }

    private void validatePosts(Set<Long> ids, RuntimeIdentityScope.Scope scope) {
        if (ids.isEmpty()) {
            return;
        }
        requirePostScope(scope);
        Set<Long> valid = new HashSet<>();
        for (TenantPost post : posts.findActivePosts(ids, null)) {
            if (post.orgId() == null ? scope.allOrganizations() : scope.allowsOrg(post.orgId())) {
                valid.add(post.postId());
            }
        }
        if (!valid.equals(ids)) {
            throw failure(400, "岗位引用不存在、已停用或超出业务范围", "A post reference is missing, inactive or outside the business scope");
        }
    }

    public void validateWrite(LowcodeRuntimeConfig config, Map<String, Object> data,
                              Map<String, Object> previous, String operation) {
        Map<?, ?> values = RuntimeIdentityFields.main(data);
        Map<?, ?> before = RuntimeIdentityFields.main(previous);
        RuntimeIdentityScope.Scope scope = null;
        for (RuntimeIdentityFields.Field field : fields.read(config)) {
            if (!RuntimeIdentityFields.contains(values, field)) {
                continue;
            }
            Set<Long> ids = ids(RuntimeIdentityFields.value(values, field));
            if (values.containsKey(field.name()) && field.column() != null && values.containsKey(field.column())
                    && !ids.equals(ids(values.get(field.column())))) {
                throw failure(400, "引用字段与数据库列别名的值不一致", "Reference field and column aliases contain different values");
            }
            if ("edit".equals(operation) && RuntimeIdentityFields.contains(before, field)
                    && ids.equals(ids(RuntimeIdentityFields.value(before, field)))) {
                continue;
            }
            if (!field.writable()) {
                throw failure(403, "只读引用字段不允许修改", "A read-only reference field cannot be changed");
            }
            if (scope == null) {
                scope = scopes.authorize(config, operation);
            }
            if (field.post()) {
                validatePosts(ids, scope);
            } else if (field.user()) {
                validateMembers(ids, scope);
            } else if (!ids.stream().allMatch(scope::allowsOrg)
                    || !new HashSet<>(loadOrganizations(ids).stream().map(TenantOrganization::orgId).toList()).equals(ids)) {
                throw failure(400, "组织引用不存在、已停用或超出业务范围", "An organization reference is missing, inactive or outside the business scope");
            }
        }
    }

    private void validateMembers(Set<Long> ids, RuntimeIdentityScope.Scope scope) {
        if (ids.isEmpty()) {
            return;
        }
        Set<Long> valid = new HashSet<>();
        if (scope.allOrganizations() || scope.selfOnly()) {
            if (scope.selfOnly() && !ids.equals(Set.of(scope.userId()))) {
                throw failure(403, "此数据范围仅允许选择本人", "This data scope only allows selecting the current user");
            }
            members.findActiveMembers(ids, null).forEach(member -> valid.add(member.userId()));
        } else {
            for (Long orgId : scope.orgIds()) {
                members.findActiveMembers(ids, orgId).forEach(member -> valid.add(member.userId()));
                if (valid.containsAll(ids)) {
                    break;
                }
            }
        }
        if (!valid.equals(ids)) {
            throw failure(400, "人员引用不存在、已停用或超出业务范围", "A member reference is missing, inactive or outside the business scope");
        }
    }

    private List<TenantOrganization> loadOrganizations(Collection<Long> ids) {
        List<Long> input = new ArrayList<>(ids);
        List<TenantOrganization> result = new ArrayList<>();
        for (int offset = 0; offset < input.size(); offset += TenantOrganizationDirectory.MAX_BATCH_SIZE) {
            result.addAll(organizations.findActiveOrganizations(input.subList(offset,
                    Math.min(offset + TenantOrganizationDirectory.MAX_BATCH_SIZE, input.size()))));
        }
        return result;
    }

    private Set<Long> ids(Object value) {
        if (value == null || "".equals(value)) {
            return Set.of();
        }
        Collection<?> input = value instanceof Collection<?> collection ? collection : List.of(value);
        if (input.size() > TenantMemberDirectory.MAX_BATCH_SIZE) {
            throw failure(400, "引用数量超过上限", "Too many identity references");
        }
        Set<Long> result = new HashSet<>();
        try {
            for (Object item : input) {
                long id = new BigDecimal(String.valueOf(item)).longValueExact();
                if (id <= 0) {
                    throw new NumberFormatException();
                }
                result.add(id);
            }
        } catch (ArithmeticException | NumberFormatException error) {
            throw failure(400, "组织、人员和岗位必须使用有效的稳定ID", "Organizations, members and posts require valid stable IDs");
        }
        return result;
    }

    private void checkPage(Long afterId, int limit) {
        if ((afterId != null && afterId <= 0) || limit < 1 || limit > 200) {
            throw failure(400, "引用查询分页参数无效", "Invalid reference lookup pagination");
        }
    }
}
