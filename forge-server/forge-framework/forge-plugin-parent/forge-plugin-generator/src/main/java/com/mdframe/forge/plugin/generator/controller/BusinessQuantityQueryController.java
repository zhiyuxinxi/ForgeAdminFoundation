package com.mdframe.forge.plugin.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessQuantityQueryDTO;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessQuantityQueryService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessQuantityBalanceVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessQuantityLedgerVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessQuantityLockVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通用数量台账只读查询接口。
 */
@RestController
@RequestMapping("/ai/business/quantity/query")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class BusinessQuantityQueryController {

    private final BusinessQuantityQueryService quantityQueryService;

    @PostMapping("/balance")
    @SaCheckPermission("ai:businessQuantity:view")
    public RespInfo<Page<BusinessQuantityBalanceVO>> balance(@RequestBody(required = false) BusinessQuantityQueryDTO query) {
        return RespInfo.success(quantityQueryService.selectBalancePage(query));
    }

    @PostMapping("/ledger")
    @SaCheckPermission("ai:businessQuantity:view")
    public RespInfo<Page<BusinessQuantityLedgerVO>> ledger(@RequestBody(required = false) BusinessQuantityQueryDTO query) {
        return RespInfo.success(quantityQueryService.selectLedgerPage(query));
    }

    @PostMapping("/lock")
    @SaCheckPermission("ai:businessQuantity:view")
    public RespInfo<Page<BusinessQuantityLockVO>> lock(@RequestBody(required = false) BusinessQuantityQueryDTO query) {
        return RespInfo.success(quantityQueryService.selectLockPage(query));
    }
}
