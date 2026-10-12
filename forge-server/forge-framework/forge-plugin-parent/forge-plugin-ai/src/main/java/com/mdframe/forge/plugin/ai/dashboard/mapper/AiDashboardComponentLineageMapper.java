package com.mdframe.forge.plugin.ai.dashboard.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.ai.dashboard.domain.AiDashboardComponentLineage;
import com.mdframe.forge.plugin.ai.dashboard.vo.AiDashboardDatasetImpactVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiDashboardComponentLineageMapper extends BaseMapper<AiDashboardComponentLineage> {

    int deleteByRecordId(@Param("tenantId") Long tenantId,
                         @Param("userId") Long userId,
                         @Param("recordId") Long recordId);

    List<AiDashboardDatasetImpactVO> selectDatasetImpact(@Param("tenantId") Long tenantId,
                                                         @Param("datasetId") Long datasetId,
                                                         @Param("limit") Integer limit);
}
