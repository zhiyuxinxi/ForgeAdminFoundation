package com.mdframe.forge.plugin.system.controller;

import com.mdframe.forge.plugin.system.datasource.TenantDatasourceAccessService;
import com.mdframe.forge.plugin.system.datasource.TenantDatasourceOption;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Every operation has the service's explicit super-administrator check. */
@RestController
@RequestMapping("/system/tenant/{tenantId}")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@ApiPermissionIgnore
public class TenantDatasourceGrantController {
    private final TenantDatasourceAccessService access;

    @GetMapping("/datasources")
    public RespInfo<List<TenantDatasourceOption>> options(@PathVariable Long tenantId) {
        return RespInfo.success(access.options(tenantId, true));
    }

    @GetMapping("/datasource-grants")
    public RespInfo<List<TenantDatasourceOption>> grants(@PathVariable Long tenantId) {
        return RespInfo.success(access.options(tenantId, false));
    }

    @PostMapping("/datasource-grants/{datasourceId}")
    public RespInfo<Void> setGrant(@PathVariable Long tenantId, @PathVariable Long datasourceId,
                                   @RequestParam boolean granted) {
        access.setGrant(tenantId, datasourceId, granted);
        return RespInfo.success();
    }
}
