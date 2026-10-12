package com.mdframe.forge.flow.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.flow.dto.FlowEntryDTO;
import com.mdframe.forge.starter.flow.dto.FlowEntryQueryDTO;
import com.mdframe.forge.starter.flow.entity.FlowEntry;
import com.mdframe.forge.starter.flow.service.FlowEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 流程入口管理接口。
 */
@RestController
@RequestMapping("/api/flow/entry")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@IgnoreTenant
public class FlowEntryController {

    private final FlowEntryService flowEntryService;

    @GetMapping("/page")
    public RespInfo<IPage<FlowEntry>> page(FlowEntryQueryDTO query,
                                           @RequestParam(defaultValue = "1") Integer pageNum,
                                           @RequestParam(defaultValue = "10") Integer pageSize) {
        return RespInfo.success(flowEntryService.pageEntries(query, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public RespInfo<FlowEntry> detail(@PathVariable Long id) {
        return RespInfo.success(flowEntryService.getEntryDetail(id));
    }

    @PostMapping
    public RespInfo<Void> create(@RequestBody FlowEntryDTO dto) {
        flowEntryService.saveEntry(dto);
        return RespInfo.success("保存成功", null);
    }

    @PutMapping
    public RespInfo<Void> update(@RequestBody FlowEntryDTO dto) {
        flowEntryService.saveEntry(dto);
        return RespInfo.success("保存成功", null);
    }

    @DeleteMapping("/{id}")
    public RespInfo<Void> delete(@PathVariable Long id) {
        flowEntryService.deleteEntry(id);
        return RespInfo.success("删除成功", null);
    }
}
