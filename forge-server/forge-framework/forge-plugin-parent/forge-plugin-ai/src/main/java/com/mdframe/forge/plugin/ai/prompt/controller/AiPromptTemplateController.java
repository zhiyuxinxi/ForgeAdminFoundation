package com.mdframe.forge.plugin.ai.prompt.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.ai.prompt.domain.AiPromptTemplate;
import com.mdframe.forge.plugin.ai.prompt.dto.AiPromptTemplateQuery;
import com.mdframe.forge.plugin.ai.prompt.service.AiPromptTemplateService;
import com.mdframe.forge.plugin.ai.prompt.vo.AiPromptTemplateVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai/prompt-template")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class AiPromptTemplateController {

    private final AiPromptTemplateService promptTemplateService;

    @GetMapping("/page")
    public RespInfo<Page<AiPromptTemplateVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            AiPromptTemplateQuery query) {
        return RespInfo.success(promptTemplateService.pageForAdmin(pageNum, pageSize, query));
    }

    @GetMapping("/list")
    public RespInfo<List<AiPromptTemplateVO>> list(AiPromptTemplateQuery query) {
        return RespInfo.success(promptTemplateService.listEnabled(query));
    }

    @GetMapping("/{id}")
    public RespInfo<AiPromptTemplate> getById(@PathVariable Long id) {
        return RespInfo.success(promptTemplateService.getDetail(id));
    }

    @GetMapping("/{id}/preview")
    public RespInfo<AiPromptTemplate> preview(@PathVariable Long id) {
        return RespInfo.success(promptTemplateService.getDetail(id));
    }

    @PostMapping
    public RespInfo<Void> create(@RequestBody AiPromptTemplate template) {
        promptTemplateService.createTemplate(template);
        return RespInfo.success();
    }

    @PutMapping
    public RespInfo<Void> update(@RequestBody AiPromptTemplate template) {
        promptTemplateService.updateTemplate(template);
        return RespInfo.success();
    }

    @DeleteMapping("/{id}")
    public RespInfo<Void> delete(@PathVariable Long id) {
        promptTemplateService.deleteTemplate(id);
        return RespInfo.success();
    }

    @PostMapping("/{id}/use")
    public RespInfo<AiPromptTemplate> use(@PathVariable Long id) {
        return RespInfo.success(promptTemplateService.useTemplate(id));
    }

    @PostMapping("/{id}/test")
    public RespInfo<AiPromptTemplate> test(@PathVariable Long id) {
        return RespInfo.success(promptTemplateService.testTemplate(id));
    }

    @PostMapping("/{id}/download")
    public RespInfo<AiPromptTemplate> download(@PathVariable Long id) {
        return RespInfo.success(promptTemplateService.downloadTemplate(id));
    }
}
