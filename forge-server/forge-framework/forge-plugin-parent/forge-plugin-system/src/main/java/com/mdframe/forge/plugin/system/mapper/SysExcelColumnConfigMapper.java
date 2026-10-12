package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.system.entity.SysExcelColumnConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Excel列配置Mapper
 */
@Mapper
public interface SysExcelColumnConfigMapper extends BaseMapper<SysExcelColumnConfig> {

    List<SysExcelColumnConfig> selectByConfigKey(@Param("configKey") String configKey);

    int deleteByConfigKey(@Param("configKey") String configKey);
}
