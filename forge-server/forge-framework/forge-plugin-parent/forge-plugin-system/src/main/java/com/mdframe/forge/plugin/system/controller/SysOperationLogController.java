package com.mdframe.forge.plugin.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.SysOperationLogQuery;
import com.mdframe.forge.plugin.system.entity.SysOperationLog;
import com.mdframe.forge.plugin.system.service.ISysOperationLogService;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 操作日志查询接口
 */
@RestController
@RequestMapping("/system/operationLog")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class SysOperationLogController {

    private final ISysOperationLogService operationLogService;

    /**
     * 分页查询操作日志
     */
    @GetMapping("/page")
    @OperationLog(module = "页面操作审计", type = OperationType.QUERY, desc = "分页查询页面操作审计日志")
    public RespInfo<Page<SysOperationLog>> page(
            PageQuery pageQuery,
            SysOperationLogQuery query) {
        return RespInfo.success(operationLogService.page(pageQuery, query));
    }

    /**
     * 查询操作日志详情
     */
    @GetMapping("/{id}")
    @OperationLog(module = "页面操作审计", type = OperationType.QUERY, desc = "查询页面操作审计详情")
    public RespInfo<SysOperationLog> detail(@PathVariable Long id) {
        return RespInfo.success(operationLogService.detail(id));
    }
}
