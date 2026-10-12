package com.mdframe.forge.plugin.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessActionExecutionLog;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessActionExecuteDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessActionLogQueryDTO;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessActionExecutionService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessActionExecuteResultVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通用业务动作执行接口。
 */
@RestController
@RequestMapping("/ai/business/action")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class BusinessActionExecutionController {

    private final BusinessActionExecutionService actionExecutionService;

    @PostMapping("/execute")
    @SaCheckPermission("ai:businessAction:execute")
    public RespInfo<BusinessActionExecuteResultVO> execute(@RequestBody BusinessActionExecuteDTO dto) {
        return RespInfo.success(actionExecutionService.execute(dto));
    }

    @PostMapping("/preview")
    @SaCheckPermission("ai:businessAction:execute")
    public RespInfo<BusinessActionExecuteResultVO> preview(@RequestBody BusinessActionExecuteDTO dto) {
        return RespInfo.success(actionExecutionService.preview(dto));
    }

    @GetMapping("/logs")
    @SaCheckPermission("ai:businessAction:log")
    public RespInfo<Page<AiBusinessActionExecutionLog>> logs(BusinessActionLogQueryDTO query, PageQuery pageQuery) {
        return RespInfo.success(actionExecutionService.selectLogPage(query, pageQuery));
    }
}
