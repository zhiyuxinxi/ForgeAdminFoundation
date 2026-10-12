package com.mdframe.forge.plugin.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessAppDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessAppQueryDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeCodegenRequest;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessBootstrapService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessAppCodegenService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessAppService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessAppOpenInfoVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessAppVO;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeCodePreviewVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.RespInfo;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 业务应用平台访问入口接口。
 */
@Slf4j
@RestController
@RequestMapping("/ai/business/app")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class BusinessAppController {

    private final BusinessAppService appService;
    private final BusinessBootstrapService bootstrapService;
    private final BusinessAppCodegenService appCodegenService;

    @GetMapping("/page")
    @SaCheckPermission("ai:businessApp:list")
    @OperationLog(module = "访问入口", type = OperationType.QUERY, desc = "分页查询访问入口")
    public RespInfo<Page<BusinessAppVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                              BusinessAppQueryDTO query) {
        return RespInfo.success(appService.page(pageNum, pageSize, query));
    }

    @GetMapping("/list")
    @SaCheckPermission("ai:businessApp:list")
    @OperationLog(module = "访问入口", type = OperationType.QUERY, desc = "查询访问入口列表")
    public RespInfo<List<BusinessAppVO>> list(BusinessAppQueryDTO query) {
        return RespInfo.success(appService.list(query));
    }

    @PostMapping("/sync-published-apps")
    @SaCheckPermission("ai:businessApp:edit")
    @OperationLog(module = "访问入口", type = OperationType.UPDATE, desc = "同步已发布访问入口")
    public RespInfo<Void> syncPublishedApps() {
        bootstrapService.syncAppsFromPublishedCrudConfigs();
        return RespInfo.success();
    }

    @PostMapping("/sync-published-crud-configs")
    @SaCheckPermission("ai:businessApp:edit")
    @OperationLog(module = "访问入口", type = OperationType.UPDATE, desc = "兼容旧路径同步已发布访问入口")
    public RespInfo<Void> syncPublishedCrudConfigs() {
        log.warn("[BusinessAppController] 旧同步入口已废弃，请使用 /ai/business/app/sync-published-apps");
        return syncPublishedApps();
    }

    @GetMapping("/{id}")
    @SaCheckPermission("ai:businessApp:list")
    @OperationLog(module = "访问入口", type = OperationType.QUERY, desc = "查询访问入口详情")
    public RespInfo<BusinessAppVO> detail(@PathVariable Long id) {
        return RespInfo.success(appService.detail(id));
    }

    @GetMapping("/{id}/open-info")
    @SaCheckPermission("ai:businessApp:open")
    @OperationLog(module = "访问入口", type = OperationType.QUERY, desc = "查询访问入口打开信息")
    public RespInfo<BusinessAppOpenInfoVO> openInfo(@PathVariable Long id) {
        return RespInfo.success(appService.openInfo(id));
    }

    @GetMapping("/{id}/code/options")
    @SaCheckPermission("ai:businessApp:code")
    @OperationLog(module = "访问入口", type = OperationType.QUERY, desc = "查询访问入口代码包设置")
    public RespInfo<Map<String, Object>> codeOptions(@PathVariable Long id) {
        return RespInfo.success(appCodegenService.getOptions(id));
    }

    @PutMapping("/{id}/code/options")
    @SaCheckPermission("ai:businessApp:code")
    @OperationLog(module = "访问入口", type = OperationType.UPDATE, desc = "保存访问入口代码包设置")
    public RespInfo<Void> saveCodeOptions(@PathVariable Long id, @RequestBody LowcodeCodegenRequest request) {
        appCodegenService.saveOptions(id, request);
        return RespInfo.success();
    }

    @GetMapping("/{id}/code/preview")
    @SaCheckPermission("ai:businessApp:codePreview")
    @OperationLog(module = "访问入口", type = OperationType.QUERY, desc = "预览访问入口功能代码")
    public RespInfo<LowcodeCodePreviewVO> previewCode(@PathVariable Long id, LowcodeCodegenRequest request) {
        return RespInfo.success(appCodegenService.previewCode(id, request));
    }

    @GetMapping("/{id}/code/download")
    @SaCheckPermission("ai:businessApp:codeDownload")
    @OperationLog(module = "访问入口", type = OperationType.QUERY, desc = "下载访问入口功能代码")
    public void downloadCode(@PathVariable Long id,
                             LowcodeCodegenRequest request,
                             HttpServletResponse response) throws Exception {
        byte[] zipBytes = appCodegenService.downloadCode(id, request);
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + appCodegenService.resolveDownloadFilename(id) + "\"");
        response.setContentLength(zipBytes.length);
        response.getOutputStream().write(zipBytes);
        response.getOutputStream().flush();
    }

    @PostMapping
    @SaCheckPermission("ai:businessApp:add")
    @OperationLog(module = "访问入口", type = OperationType.ADD, desc = "新增访问入口")
    public RespInfo<Long> create(@RequestBody BusinessAppDTO dto) {
        return RespInfo.success(appService.create(dto));
    }

    @PutMapping
    @SaCheckPermission("ai:businessApp:edit")
    @OperationLog(module = "访问入口", type = OperationType.UPDATE, desc = "修改访问入口")
    public RespInfo<Void> update(@RequestBody BusinessAppDTO dto) {
        appService.update(dto);
        return RespInfo.success();
    }

    @PutMapping("/{id}/status")
    @SaCheckPermission("ai:businessApp:status")
    @OperationLog(module = "访问入口", type = OperationType.UPDATE, desc = "启停访问入口")
    public RespInfo<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        appService.updateStatus(id, status);
        return RespInfo.success();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("ai:businessApp:delete")
    @OperationLog(module = "访问入口", type = OperationType.DELETE, desc = "删除访问入口")
    public RespInfo<Void> delete(@PathVariable Long id) {
        appService.delete(id);
        return RespInfo.success();
    }
}
