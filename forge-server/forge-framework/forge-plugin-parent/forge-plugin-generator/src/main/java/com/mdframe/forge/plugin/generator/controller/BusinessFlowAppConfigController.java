package com.mdframe.forge.plugin.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowAppConfigDTO;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowAppConfigService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessFlowAppConfigVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 业务流程应用统一配置接口。
 */
@Slf4j
@RestController
@RequestMapping("/ai/business/flow-app")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class BusinessFlowAppConfigController {

    private final BusinessFlowAppConfigService configService;

    @GetMapping("/config/{objectCode}")
    @SaCheckPermission("ai:businessFlow:config")
    @OperationLog(module = "业务流程应用", type = OperationType.QUERY, desc = "查询业务流程应用配置")
    public RespInfo<BusinessFlowAppConfigVO> getConfig(@PathVariable String objectCode) {
        return RespInfo.success(configService.getConfig(objectCode));
    }

    @PutMapping("/config/{objectCode}")
    @SaCheckPermission("ai:businessFlow:config")
    @OperationLog(module = "业务流程应用", type = OperationType.UPDATE, desc = "保存业务流程应用配置")
    public RespInfo<BusinessFlowAppConfigVO> saveConfig(@PathVariable String objectCode,
                                                       @RequestBody BusinessFlowAppConfigDTO dto) {
        return RespInfo.success(configService.saveConfig(objectCode, dto));
    }
}
