package com.mdframe.forge.plugin.generator.controller;

import com.mdframe.forge.starter.core.i18n.UiMessages;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.GenTable;
import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import com.mdframe.forge.plugin.generator.dto.*;
import com.mdframe.forge.plugin.generator.service.AiRecommendService;
import com.mdframe.forge.plugin.generator.service.AiSchemaService;
import com.mdframe.forge.plugin.generator.service.IGenTableService;
import com.mdframe.forge.plugin.generator.service.GeneratorDdlService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 代码生成器Controller
 */
@RestController
@RequestMapping("/generator")
@RequiredArgsConstructor
@Slf4j
@ApiDecrypt
@ApiEncrypt
public class GenController {

    private final IGenTableService genTableService;
    private final AiRecommendService aiRecommendService;
    private final AiSchemaService aiSchemaService;
    private final GeneratorDdlService generatorDdlService;

    /**
     * 查询数据库表列表
     */
    @GetMapping("/db/list")
    @OperationLog(module = "代码生成", type = OperationType.QUERY, desc = "查询数据库表列表")
    public RespInfo<List<GenTable>> dbList() {
        List<GenTable> list = genTableService.selectDbTableList();
        return RespInfo.success(list);
    }

    /**
     * 分页查询已导入的表列表
     */
    @GetMapping("/list")
    @OperationLog(module = "代码生成", type = OperationType.QUERY, desc = "分页查询生成表列表")
    public RespInfo<Page<GenTable>> list(PageQuery pageQuery, String tableName, String tableComment) {
        return RespInfo.success(genTableService.selectGenTablePage(pageQuery, tableName, tableComment));
    }

    /**
     * 根据表ID查询表配置详情
     */
    @GetMapping("/{tableId}")
    public RespInfo<GenTable> getInfo(@PathVariable Long tableId) {
        GenTable genTable = genTableService.getById(tableId);
        return RespInfo.success(genTable);
    }

    /**
     * 导入表结构（默认数据源）
     */
    @PostMapping("/importTable")
    @OperationLog(module = "代码生成", type = OperationType.ADD, desc = "导入表结构")
    public RespInfo<Void> importTable(@RequestBody List<String> tableNames) {
        genTableService.importGenTable(tableNames);
        return RespInfo.success();
    }

    /**
     * 导入表结构（指定数据源）
     */
    @PostMapping("/importTable/{datasourceId}")
    @OperationLog(module = "代码生成", type = OperationType.ADD, desc = "导入表结构")
    public RespInfo<Void> importTableFromDatasource(
            @PathVariable Long datasourceId,
            @RequestBody List<String> tableNames) {
        genTableService.importGenTable(datasourceId, tableNames);
        return RespInfo.success();
    }

    /**
     * 修改表配置
     */
    @PostMapping("/edit")
    @OperationLog(module = "代码生成", type = OperationType.UPDATE, desc = "修改表配置")
    public RespInfo<Void> edit(@RequestBody GenTable genTable) {
        genTableService.updateGenTable(genTable);
        return RespInfo.success();
    }

    /**
     * 删除表配置
     */
    @PostMapping("/remove")
    @OperationLog(module = "代码生成", type = OperationType.DELETE, desc = "删除表配置")
    public RespInfo<Void> remove(@RequestBody Long[] tableIds) {
        genTableService.deleteGenTableByIds(tableIds);
        return RespInfo.success();
    }

    /**
     * 预览代码
     */
    @GetMapping("/preview/{tableName}")
    public RespInfo<Map<String, String>> preview(@PathVariable String tableName) {
        Map<String, String> dataMap = genTableService.previewCode(tableName);
        return RespInfo.success(dataMap);
    }

    /**
     * 生成代码（下载方式）
     */
    @GetMapping("/download/{tableName}")
    public ResponseEntity<byte[]> download(@PathVariable String tableName) throws IOException {
        byte[] data = genTableService.generatorCode(tableName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + tableName + ".zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(data.length)
                .body(data);
    }

    /**
     * 批量生成代码（下载方式）
     */
    @PostMapping("/batchDownload")
    public ResponseEntity<byte[]> batchDownload(@RequestBody String[] tableNames) throws IOException {
        byte[] data = genTableService.batchGeneratorCode(tableNames);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=code.zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(data.length)
                .body(data);
    }

    /**
     * 执行建表 SQL（主要用于 AI 生成器直接建表）
     */
    @PostMapping("/executeSql")
    @SaCheckPermission(GeneratorDdlService.PERMISSION)
    @OperationLog(module = "代码生成", type = OperationType.ADD, desc = "执行建表SQL")
    public RespInfo<Void> executeSql(@RequestBody Map<String, String> body) {
        generatorDdlService.createTable(body == null ? null : body.get("sql"));
        return RespInfo.success();
    }

    /**
     * AI 字段推荐
     */
    @PostMapping("/ai/recommendColumns")
    public RespInfo<List<GenTableColumn>> recommendColumns(@RequestBody RecommendColumnsRequest request) {
        GenTable table = genTableService.getById(request.getTableId());
        if (table == null) {
            return RespInfo.error("表配置不存在");
        }
        List<GenTableColumn> result = aiRecommendService.recommendColumns(
                request.getTableId(), request.getColumns(), table);
        return RespInfo.success(result);
    }

    /**
     * 自然语言到 Schema 推断
     */
    @PostMapping("/ai/nlToSchema")
    public RespInfo<NlToSchemaResult> nlToSchema(@RequestBody NlToSchemaRequest request) {
        NlToSchemaResult result = aiSchemaService.nlToSchema(request.getDescription());
        return RespInfo.success(result);
    }

    /**
     * Schema 追问优化（多轮）
     */
    @PostMapping("/ai/nlToSchemaRefine")
    public RespInfo<NlToSchemaResult> nlToSchemaRefine(@RequestBody NlToSchemaRefineRequest request) {
        NlToSchemaResult result = aiSchemaService.nlToSchemaRefine(
                request.getCurrentSchema(), request.getMessage(), request.getSessionId());
        return RespInfo.success(result);
    }

    /**
     * 从 AI Schema 导入为 GenTable 配置
     */
    @PostMapping("/ai/importSchema")
    @OperationLog(module = "代码生成", type = OperationType.ADD, desc = "AI Schema导入")
    public RespInfo<Void> importSchema(@RequestBody NlToSchemaResult schema) {
        aiSchemaService.importSchema(schema);
        return RespInfo.success();
    }
}
