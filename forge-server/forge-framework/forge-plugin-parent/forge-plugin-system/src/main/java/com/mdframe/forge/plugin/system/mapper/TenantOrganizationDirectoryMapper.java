package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.mdframe.forge.starter.core.identity.TenantOrganization;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** Internal system queries; business consumers depend on TenantOrganizationDirectory. */
@Mapper
public interface TenantOrganizationDirectoryMapper {
    // XML explicitly binds organizations and parents to trusted tenantId. sys_tenant
    // is the tenant owner table and does not have a tenant_id discriminator itself.
    @InterceptorIgnore(tenantLine = "true")
    List<TenantOrganization> selectActiveOrganizations(@Param("tenantId") Long tenantId,
                                                      @Param("orgIds") List<Long> orgIds);

    @InterceptorIgnore(tenantLine = "true")
    List<TenantOrganization> selectActiveChildren(@Param("tenantId") Long tenantId,
                                                 @Param("parentId") Long parentId,
                                                 @Param("afterOrgId") Long afterOrgId,
                                                 @Param("limit") int limit);
}
