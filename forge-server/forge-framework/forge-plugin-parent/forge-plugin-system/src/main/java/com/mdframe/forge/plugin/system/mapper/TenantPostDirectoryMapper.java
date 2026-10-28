package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.mdframe.forge.starter.core.identity.TenantPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** Internal explicit tenant/post/org predicates; public consumers use TenantPostDirectory. */
@Mapper
public interface TenantPostDirectoryMapper {
    @InterceptorIgnore(tenantLine = "true")
    List<TenantPost> selectActivePosts(@Param("tenantId") Long tenantId,
                                      @Param("postIds") List<Long> postIds,
                                      @Param("orgId") Long orgId);

    @InterceptorIgnore(tenantLine = "true")
    List<TenantPost> selectActivePostsByOrg(@Param("tenantId") Long tenantId,
                                           @Param("orgId") Long orgId,
                                           @Param("keyword") String keyword,
                                           @Param("afterPostId") Long afterPostId,
                                           @Param("limit") int limit);

    @InterceptorIgnore(tenantLine = "true")
    List<TenantPost> selectActiveTenantWidePosts(@Param("tenantId") Long tenantId,
                                               @Param("keyword") String keyword,
                                               @Param("afterPostId") Long afterPostId,
                                               @Param("limit") int limit);
}
