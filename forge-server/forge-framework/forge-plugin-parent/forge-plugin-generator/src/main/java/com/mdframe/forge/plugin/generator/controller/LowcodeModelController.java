package com.mdframe.forge.plugin.generator.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeDataModelDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeDataModelStatusDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeDdlPreviewDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelImportRequest;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeDataModelService;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeDdlService;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeModelImportService;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeSchemaValidator;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeDataModelVO;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeDdlPreviewVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 低代码数据模型接口。
 */
@Slf4j
@RestController
@RequestMapping("/ai/lowcode/model")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class LowcodeModelController {

    private final LowcodeDataModelService modelService;
    private final LowcodeSchemaValidator schemaValidator;
    private final LowcodeDdlService ddlService;
    private final LowcodeModelImportService importService;

    @GetMapping("/page")
    @OperationLog(module = "低代码模型", type = OperationType.QUERY, desc = "分页查询低代码数据模型")
    public RespInfo<Page<LowcodeDataModelVO>> page(PageQuery pageQuery,
                                                   @RequestParam(required = false) Long domainId,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) Boolean masterData) {
        return RespInfo.success(modelService.page(pageQuery, domainId, keyword, status, masterData));
    }

    @GetMapping("/list")
    @OperationLog(module = "低代码模型", type = OperationType.QUERY, desc = "查询低代码数据模型列表")
    public RespInfo<List<LowcodeDataModelVO>> list(@RequestParam(required = false) Long domainId,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String status) {
        return RespInfo.success(modelService.list(domainId, keyword, status));
    }

    @GetMapping("/{id}")
    @OperationLog(module = "低代码模型", type = OperationType.QUERY, desc = "查询低代码数据模型详情")
    public RespInfo<LowcodeDataModelVO> detail(@PathVariable Long id) {
        return RespInfo.success(modelService.getDetail(id));
    }

    @PostMapping
    @OperationLog(module = "低代码模型", type = OperationType.ADD, desc = "新增低代码数据模型")
    public RespInfo<Long> create(@RequestBody LowcodeDataModelDTO dto) {
        return RespInfo.success(modelService.saveModel(dto));
    }

    @PutMapping
    @OperationLog(module = "低代码模型", type = OperationType.UPDATE, desc = "修改低代码数据模型")
    public RespInfo<Long> update(@RequestBody LowcodeDataModelDTO dto) {
        return RespInfo.success(modelService.saveModel(dto));
    }

    @PutMapping("/{id}/status")
    @OperationLog(module = "低代码模型", type = OperationType.UPDATE, desc = "启停低代码数据模型")
    public RespInfo<Void> updateStatus(@PathVariable Long id, @RequestBody LowcodeDataModelStatusDTO dto) {
        modelService.updateStatus(id, dto == null ? null : dto.getStatus());
        return RespInfo.success();
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "低代码模型", type = OperationType.DELETE, desc = "删除低代码数据模型")
    public RespInfo<Void> delete(@PathVariable Long id) {
        modelService.delete(id);
        return RespInfo.success();
    }

    @PostMapping("/validate")
    @OperationLog(module = "低代码模型", type = OperationType.QUERY, desc = "校验低代码数据模型")
    public RespInfo<Void> validate(@RequestBody LowcodeModelSchema modelSchema) {
        schemaValidator.validateModel(modelSchema);
        return RespInfo.success();
    }

    @PostMapping("/ddl/preview")
    @OperationLog(module = "低代码模型", type = OperationType.QUERY, desc = "预览低代码建表DDL")
    public RespInfo<LowcodeDdlPreviewVO> previewDdl(@RequestBody LowcodeDdlPreviewDTO dto) {
        return RespInfo.success(ddlService.previewCreateTable(dto.getModelSchema()));
    }

    @PostMapping("/preview-db-table")
    @OperationLog(module = "低代码模型", type = OperationType.QUERY, desc = "预览数据源表模型")
    public RespInfo<LowcodeModelSchema> previewDbTable(@RequestBody LowcodeModelImportRequest request) {
        return RespInfo.success(importService.previewDbTableModel(request));
    }

    @PostMapping("/import-db-table")
    @OperationLog(module = "低代码模型", type = OperationType.ADD, desc = "从数据源表导入低代码模型")
    public RespInfo<Long> importDbTable(@RequestBody LowcodeModelImportRequest request) {
        return RespInfo.success(importService.importDbTableModel(request));
    }
}
