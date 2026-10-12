package com.mdframe.forge.report.material.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.report.material.dto.ReportMaterialCreateDTO;
import com.mdframe.forge.report.material.service.ReportMaterialService;
import com.mdframe.forge.report.material.vo.ReportMaterialVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 报表素材管理。
 */
@RestController
@RequestMapping("/report/material")
@RequiredArgsConstructor
@ApiEncrypt
@ApiDecrypt
public class ReportMaterialController {

    private final ReportMaterialService materialService;

    /**
     * 分页查询素材。
     */
    @GetMapping("/page")
    public RespInfo<Page<ReportMaterialVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "24") Integer pageSize,
            @RequestParam(required = false) String originalName,
            @RequestParam(required = false) String businessId,
            @RequestParam(required = false) Boolean isPrivate,
            @RequestParam(required = false) String mimeType) {
        return RespInfo.success(materialService.pageMaterials(pageNum, pageSize, originalName, businessId, isPrivate, mimeType));
    }

    /**
     * 通用文件上传完成后，将 fileId 记录为报表素材。
     */
    @PostMapping
    public RespInfo<ReportMaterialVO> create(@RequestBody ReportMaterialCreateDTO dto) {
        return RespInfo.success(materialService.createMaterial(dto));
    }

    /**
     * 删除素材。
     */
    @DeleteMapping("/{fileId}")
    public RespInfo<Void> delete(@PathVariable String fileId) {
        materialService.deleteMaterial(fileId);
        return RespInfo.success();
    }

    /**
     * 重命名素材。
     */
    @PutMapping("/rename")
    public RespInfo<Void> rename(@RequestParam String fileId, @RequestParam String originalName) {
        materialService.renameMaterial(fileId, originalName);
        return RespInfo.success();
    }
}
