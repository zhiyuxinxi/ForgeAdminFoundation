package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiLowcodeDomain;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeDomainWorkspaceVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiLowcodeDomainMapper extends BaseMapper<AiLowcodeDomain> {

    Page<AiLowcodeDomain> selectDomainPage(Page<AiLowcodeDomain> page,
                                           @Param("tenantId") Long tenantId,
                                           @Param("keyword") String keyword,
                                           @Param("status") String status,
                                           @Param("parentId") Long parentId);

    List<AiLowcodeDomain> selectDomainList(@Param("tenantId") Long tenantId,
                                           @Param("keyword") String keyword,
                                           @Param("status") String status);

    AiLowcodeDomain selectDomainById(@Param("tenantId") Long tenantId,
                                      @Param("id") Long id);

    AiLowcodeDomain selectByCode(@Param("tenantId") Long tenantId,
                                 @Param("domainCode") String domainCode);

    Long countByCode(@Param("tenantId") Long tenantId,
                     @Param("domainCode") String domainCode,
                     @Param("excludeId") Long excludeId);

    Long countByNameInParent(@Param("tenantId") Long tenantId,
                             @Param("parentId") Long parentId,
                             @Param("domainName") String domainName,
                             @Param("excludeId") Long excludeId);

    Long countChildren(@Param("tenantId") Long tenantId,
                       @Param("parentId") Long parentId);

    Long countAppsByDomainId(@Param("tenantId") Long tenantId,
                             @Param("domainId") Long domainId);

    LowcodeDomainWorkspaceVO selectWorkspaceSummary(@Param("tenantId") Long tenantId,
                                                    @Param("domainId") Long domainId);

    List<LowcodeDomainWorkspaceVO.ObjectOverviewVO> selectObjectOverviews(@Param("tenantId") Long tenantId,
                                                                          @Param("domainId") Long domainId);

    List<LowcodeDomainWorkspaceVO.RecentVersionVO> selectRecentVersions(@Param("tenantId") Long tenantId,
                                                                        @Param("domainId") Long domainId);
}
