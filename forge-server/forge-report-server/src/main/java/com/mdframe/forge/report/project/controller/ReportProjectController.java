package com.mdframe.forge.report.project.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.report.project.domain.ReportProject;
import com.mdframe.forge.report.project.domain.ReportProjectVersion;
import com.mdframe.forge.report.project.service.ReportProjectService;
import com.mdframe.forge.report.project.service.ReportProjectVersionService;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Go-View 项目 Controller
 */
@RestController
@RequestMapping("/report/project")
@RequiredArgsConstructor
@ApiEncrypt
@ApiDecrypt
public class ReportProjectController {

    private final ReportProjectService projectService;
    private final ReportProjectVersionService projectVersionService;

    /**
     * 分页查询项目列表
     */
    @GetMapping("/page")
    public RespInfo<Page<ReportProject>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String projectName,
            @RequestParam(required = false) Long directoryId) {
        return RespInfo.success(projectService.pageProjects(pageNum, pageSize, projectName, directoryId));
    }

    /**
     * 查询项目详情
     */
    @GetMapping("/{id}")
    public RespInfo<ReportProject> getById(@PathVariable Long id) {
        ReportProject project = projectService.getById(id);
        return RespInfo.success(project);
    }

    /**
     * 分页查询项目历史版本
     */
    @GetMapping("/{projectId}/versions")
    public RespInfo<Page<ReportProjectVersion>> versions(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return RespInfo.success(projectVersionService.pageVersions(projectId, pageNum, pageSize));
    }

    /**
     * 查询项目版本详情
     */
    @GetMapping("/version/{versionId}")
    public RespInfo<ReportProjectVersion> getVersion(@PathVariable Long versionId) {
        return RespInfo.success(projectVersionService.getVersionDetail(versionId));
    }

    /**
     * 创建项目
     */
    @PostMapping
    public RespInfo<ReportProject> create(@RequestBody ReportProject project) {
        return RespInfo.success(projectService.createProject(project));
    }

    /**
     * 更新项目
     */
    @PutMapping
    public RespInfo<Void> update(@RequestBody ReportProject project) {
        projectService.updateProject(project);
        return RespInfo.success();
    }

    /**
     * 删除项目
     */
    @DeleteMapping("/{id}")
    public RespInfo<Void> delete(@PathVariable Long id) {
        projectService.removeById(id);
        return RespInfo.success();
    }

    /**
     * 发布项目
     */
    @PostMapping("/publish/{id}")
    public RespInfo<Void> publish(@PathVariable Long id, @RequestParam String publishUrl) {
        projectService.publishProject(id, publishUrl);
        return RespInfo.success();
    }

    /**
     * 回退项目到指定历史版本
     */
    @PostMapping("/version/{versionId}/rollback")
    public RespInfo<ReportProjectVersion> rollbackVersion(@PathVariable Long versionId) {
        return RespInfo.success(projectVersionService.rollbackVersion(versionId));
    }
}
