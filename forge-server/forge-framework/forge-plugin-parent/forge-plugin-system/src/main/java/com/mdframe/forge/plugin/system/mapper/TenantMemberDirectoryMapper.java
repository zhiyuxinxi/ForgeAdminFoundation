package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.mdframe.forge.starter.core.identity.TenantMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** Internal system queries; public consumers use TenantMemberDirectory. */
@Mapper
public interface TenantMemberDirectoryMapper {
    // XML scopes every membership/org by trusted tenantId. Filtering sys_user.tenant_id
    // would incorrectly treat the account's default tenant as its membership.
    @InterceptorIgnore(tenantLine = "true")
    List<TenantMember> selectActiveMembers(@Param("tenantId") Long tenantId,
                                          @Param("userIds") List<Long> userIds,
                                          @Param("orgId") Long orgId);

    @InterceptorIgnore(tenantLine = "true")
    List<TenantMember> selectActiveMembersByOrg(@Param("tenantId") Long tenantId,
                                               @Param("orgId") Long orgId,
                                               @Param("keyword") String keyword,
                                               @Param("afterUserId") Long afterUserId,
                                               @Param("limit") int limit);
}
