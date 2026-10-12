package com.mdframe.forge.plugin.ai.dashboard.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.ai.dashboard.domain.AiDashboardGenerateRecord;
import com.mdframe.forge.plugin.ai.dashboard.dto.AiDashboardGenerateRecordQuery;
import com.mdframe.forge.plugin.ai.dashboard.vo.AiDashboardGenerateRecordAuditVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiDashboardGenerateRecordMapper extends BaseMapper<AiDashboardGenerateRecord> {

    Page<AiDashboardGenerateRecordAuditVO> selectAdminPage(Page<AiDashboardGenerateRecordAuditVO> page,
                                                           @Param("tenantId") Long tenantId,
                                                           @Param("query") AiDashboardGenerateRecordQuery query);

    AiDashboardGenerateRecordAuditVO selectAdminDetail(@Param("tenantId") Long tenantId,
                                                       @Param("id") Long id);

    List<AiDashboardGenerateRecord> selectRecent(@Param("tenantId") Long tenantId,
                                                 @Param("userId") Long userId,
                                                 @Param("businessDefinitionId") Long businessDefinitionId,
                                                 @Param("projectId") Long projectId,
                                                 @Param("limit") Integer limit);

    int deleteOwn(@Param("tenantId") Long tenantId,
                  @Param("userId") Long userId,
                  @Param("id") Long id);
}
