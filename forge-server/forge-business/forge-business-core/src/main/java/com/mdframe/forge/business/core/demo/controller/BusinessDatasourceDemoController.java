package com.mdframe.forge.business.core.demo.controller;

import com.mdframe.forge.business.core.demo.service.BusinessDatasourceDemoService;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租户业务数据源路由演示接口。
 */
@RestController
@RequestMapping("/business/datasource-demo")
@RequiredArgsConstructor
public class BusinessDatasourceDemoController {

    private final BusinessDatasourceDemoService demoService;

    @GetMapping("/current")
    public RespInfo<?> current(@RequestParam(required = false) Long tenantId) {
        if (tenantId != null) {
            return RespInfo.success(demoService.currentForTenant(tenantId));
        }
        return RespInfo.success(demoService.current());
    }

    @PostMapping("/prepare")
    public RespInfo<?> prepare(@RequestParam(required = false) Long tenantId,
                               @RequestParam(defaultValue = "tenant datasource demo") String title) {
        if (tenantId != null) {
            return RespInfo.success(demoService.prepareForTenant(tenantId, title));
        }
        return RespInfo.success(demoService.prepare(title));
    }

    @GetMapping("/list")
    public RespInfo<?> list(@RequestParam(required = false) Long tenantId,
                            @RequestParam(defaultValue = "10") Integer limit) {
        if (tenantId != null) {
            return RespInfo.success(demoService.listForTenant(tenantId, limit));
        }
        return RespInfo.success(demoService.list(limit));
    }
}
