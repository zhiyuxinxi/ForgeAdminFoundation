package com.mdframe.forge.plugin.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessRecordSelectorQueryDTO;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessRecordSelectorService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessRecordSelectorResultVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通用业务记录选择器接口。
 */
@RestController
@RequestMapping("/ai/business/selector")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class BusinessRecordSelectorController {

    private final BusinessRecordSelectorService selectorService;

    @PostMapping("/query")
    @SaCheckPermission("ai:businessObject:list")
    public RespInfo<BusinessRecordSelectorResultVO> query(@RequestBody BusinessRecordSelectorQueryDTO dto,
                                                          PageQuery pageQuery) {
        return RespInfo.success(selectorService.query(dto, pageQuery));
    }
}
