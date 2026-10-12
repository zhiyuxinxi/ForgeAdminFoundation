package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudExportTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiCrudExportTaskMapper extends BaseMapper<AiCrudExportTask> {

    Page<AiCrudExportTask> selectTaskPage(Page<AiCrudExportTask> page,
                                          @Param("tenantId") Long tenantId,
                                          @Param("createBy") Long createBy,
                                          @Param("configKey") String configKey);

    AiCrudExportTask selectTaskById(@Param("tenantId") Long tenantId,
                                    @Param("createBy") Long createBy,
                                    @Param("id") Long id);

    String selectConfigValue(@Param("tenantId") Long tenantId,
                             @Param("configKey") String configKey);
}
