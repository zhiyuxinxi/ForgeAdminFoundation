package com.mdframe.forge.plugin.generator.controller;

import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.lowcode.runtime.service.identity.RuntimeIdentityReferenceService;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.plugin.generator.service.lowcode.RuntimeCrudConfigAdapter;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.identity.TenantMember;
import com.mdframe.forge.starter.core.identity.TenantOrganization;
import com.mdframe.forge.starter.core.identity.TenantPost;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Published business-field lookups; the service owns action and selectable-scope checks. */
@RestController
@RequestMapping("/ai/crud/{configKey}/references/{field}")
@RequiredArgsConstructor
@ApiEncrypt
public class IdentityReferenceController {
    private final DynamicCrudService crud;
    private final RuntimeIdentityReferenceService references;

    @GetMapping("/organizations")
    public RespInfo<List<TenantOrganization>> organizations(@PathVariable String configKey, @PathVariable String field,
            @RequestParam String operation, @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) Long afterOrgId, @RequestParam(defaultValue = "200") int limit) {
        return RespInfo.success(references.organizations(config(configKey), field, operation, parentId, afterOrgId, limit));
    }

    @GetMapping("/members")
    public RespInfo<List<TenantMember>> members(@PathVariable String configKey, @PathVariable String field,
            @RequestParam String operation, @RequestParam Long directOrgId, @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long afterUserId, @RequestParam(defaultValue = "20") int limit) {
        return RespInfo.success(references.members(config(configKey), field, operation, directOrgId, keyword, afterUserId, limit));
    }

    @GetMapping("/post-scope")
    public RespInfo<RuntimeIdentityReferenceService.PostScope> postScope(@PathVariable String configKey,
            @PathVariable String field, @RequestParam String operation) {
        return RespInfo.success(references.postScope(config(configKey), field, operation));
    }

    @GetMapping("/posts")
    public RespInfo<List<TenantPost>> posts(@PathVariable String configKey, @PathVariable String field,
            @RequestParam String operation, @RequestParam(required = false) Long directOrgId,
            @RequestParam(defaultValue = "false") boolean tenantWide, @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long afterPostId, @RequestParam(defaultValue = "20") int limit) {
        return RespInfo.success(references.posts(config(configKey), field, operation,
                directOrgId, tenantWide, keyword, afterPostId, limit));
    }

    private LowcodeRuntimeConfig config(String configKey) {
        return RuntimeCrudConfigAdapter.fromDesign(crud.getRuntimeConfig(configKey));
    }
}
