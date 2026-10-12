package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.entity.SysExcelExportConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Excel导出配置Mapper
 */
@Mapper
public interface SysExcelExportConfigMapper extends BaseMapper<SysExcelExportConfig> {

    Page<SysExcelExportConfig> selectConfigPage(Page<SysExcelExportConfig> page,
                                                @Param("condition") SysExcelExportConfig condition);

    SysExcelExportConfig selectByConfigKey(@Param("configKey") String configKey);

    Long countByConfigKey(@Param("configKey") String configKey);

    Long countByConfigKeyExcludeId(@Param("configKey") String configKey, @Param("id") Long id);
}
