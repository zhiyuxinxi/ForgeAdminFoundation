package com.mdframe.forge.plugin.ai.prompt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.ai.prompt.domain.AiPromptTemplate;
import com.mdframe.forge.plugin.ai.prompt.dto.AiPromptTemplateQuery;
import com.mdframe.forge.plugin.ai.prompt.vo.AiPromptTemplateVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiPromptTemplateMapper extends BaseMapper<AiPromptTemplate> {

    Page<AiPromptTemplateVO> selectAdminPage(Page<AiPromptTemplateVO> page,
                                             @Param("tenantId") Long tenantId,
                                             @Param("query") AiPromptTemplateQuery query);

    List<AiPromptTemplateVO> selectEnabledList(@Param("tenantId") Long tenantId,
                                               @Param("query") AiPromptTemplateQuery query,
                                               @Param("limit") Integer limit);

    AiPromptTemplate selectDetail(@Param("tenantId") Long tenantId,
                                  @Param("id") Long id);

    int countByCode(@Param("tenantId") Long tenantId,
                    @Param("templateCode") String templateCode,
                    @Param("excludeId") Long excludeId);

    int deleteByTenantId(@Param("tenantId") Long tenantId,
                         @Param("id") Long id);

    int incrementUseCount(@Param("tenantId") Long tenantId,
                          @Param("id") Long id);

    int incrementTestCount(@Param("tenantId") Long tenantId,
                           @Param("id") Long id);

    int incrementDownloadCount(@Param("tenantId") Long tenantId,
                               @Param("id") Long id);
}
