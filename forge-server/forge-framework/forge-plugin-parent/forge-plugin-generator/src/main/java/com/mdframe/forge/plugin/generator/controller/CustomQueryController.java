package com.mdframe.forge.plugin.generator.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.dto.CustomQueryExecuteDTO;
import com.mdframe.forge.plugin.generator.dto.CustomQuerySchemeDTO;
import com.mdframe.forge.plugin.generator.dto.CustomQuerySchemeVO;
import com.mdframe.forge.plugin.generator.service.CustomQueryService;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 自定义查询控制器。
 */
@Slf4j
@ApiEncrypt
@ApiDecrypt
@RestController
@RequestMapping("/ai/custom-query/{configKey}")
@RequiredArgsConstructor
public class CustomQueryController {

    private final CustomQueryService customQueryService;

    @PostMapping("/execute")
    public RespInfo<Page<Map<String, Object>>> execute(@PathVariable String configKey,
                                                       @RequestBody(required = false) CustomQueryExecuteDTO request) {
        return RespInfo.success(customQueryService.execute(configKey, request));
    }

    @GetMapping("/scheme/list")
    public RespInfo<List<CustomQuerySchemeVO>> listSchemes(@PathVariable String configKey) {
        return RespInfo.success(customQueryService.listSchemes(configKey));
    }

    @GetMapping("/scheme/{id}")
    public RespInfo<CustomQuerySchemeVO> getScheme(@PathVariable String configKey,
                                                   @PathVariable Long id) {
        return RespInfo.success(customQueryService.getScheme(configKey, id));
    }

    @PostMapping("/scheme")
    public RespInfo<Long> createScheme(@PathVariable String configKey,
                                       @RequestBody CustomQuerySchemeDTO dto) {
        return RespInfo.success(customQueryService.createScheme(configKey, dto));
    }

    @PutMapping("/scheme")
    public RespInfo<Void> updateScheme(@PathVariable String configKey,
                                       @RequestBody CustomQuerySchemeDTO dto) {
        customQueryService.updateScheme(configKey, dto);
        return RespInfo.success();
    }

    @DeleteMapping("/scheme/{id}")
    public RespInfo<Void> deleteScheme(@PathVariable String configKey,
                                       @PathVariable Long id) {
        customQueryService.deleteScheme(configKey, id);
        return RespInfo.success();
    }
}
