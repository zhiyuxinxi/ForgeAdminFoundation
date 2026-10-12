package com.mdframe.forge.flow.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.flow.dto.FlowFillBatchQueryDTO;
import com.mdframe.forge.starter.flow.entity.FlowFillBatch;
import com.mdframe.forge.starter.flow.entity.FlowFillBatchItem;
import com.mdframe.forge.starter.flow.service.FlowFillBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 组织填报批次接口。
 */
@RestController
@RequestMapping("/api/flow/fill-batch")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@IgnoreTenant
public class FlowFillBatchController {

    private final FlowFillBatchService flowFillBatchService;

    @GetMapping("/page")
    public RespInfo<IPage<FlowFillBatch>> page(FlowFillBatchQueryDTO query,
                                               @RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "10") Integer pageSize) {
        return RespInfo.success(flowFillBatchService.pageBatches(query, pageNum, pageSize));
    }

    @GetMapping("/{id}/items")
    public RespInfo<List<FlowFillBatchItem>> items(@PathVariable Long id) {
        return RespInfo.success(flowFillBatchService.listItems(id));
    }

    @PostMapping
    public RespInfo<Void> create(@RequestBody FlowFillBatch batch) {
        flowFillBatchService.saveBatchConfig(batch);
        return RespInfo.success("保存成功", null);
    }

    @PutMapping
    public RespInfo<Void> update(@RequestBody FlowFillBatch batch) {
        flowFillBatchService.saveBatchConfig(batch);
        return RespInfo.success("保存成功", null);
    }

    @PostMapping("/{id}/publish")
    public RespInfo<Void> publish(@PathVariable Long id) {
        flowFillBatchService.publishBatch(id);
        return RespInfo.success("发布成功", null);
    }

    @DeleteMapping("/{id}")
    public RespInfo<Void> delete(@PathVariable Long id) {
        flowFillBatchService.deleteBatch(id);
        return RespInfo.success("删除成功", null);
    }
}
