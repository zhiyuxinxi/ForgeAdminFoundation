package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.system.entity.SysFileGroup;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 文件分组Mapper
 */
@Mapper
public interface SysFileGroupMapper extends BaseMapper<SysFileGroup> {

    /**
     * 获取分组及其文件数量
     */
    List<SysFileGroup> selectGroupWithFileCount();

    /**
     * 获取各类型文件数量统计
     */
    Map<String, Object> selectFileStatistics();
}
