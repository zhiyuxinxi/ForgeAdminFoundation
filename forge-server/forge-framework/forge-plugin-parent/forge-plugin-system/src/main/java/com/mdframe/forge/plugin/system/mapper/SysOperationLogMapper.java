package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.SysOperationLogQuery;
import com.mdframe.forge.plugin.system.entity.SysOperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 操作日志Mapper
 */
@Mapper
public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {

    /**
     * 分页查询操作日志。
     */
    Page<SysOperationLog> selectOperationLogPage(Page<SysOperationLog> page, @Param("query") SysOperationLogQuery query);

    /**
     * 查询操作日志详情。
     */
    SysOperationLog selectOperationLogDetail(@Param("id") Long id);

    /**
     * 查询操作日志导出列表。
     */
    List<SysOperationLog> selectOperationLogExportList(@Param("query") SysOperationLogQuery query);
}
