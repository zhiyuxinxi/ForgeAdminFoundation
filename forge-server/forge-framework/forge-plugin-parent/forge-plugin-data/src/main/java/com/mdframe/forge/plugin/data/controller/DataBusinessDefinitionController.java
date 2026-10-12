package com.mdframe.forge.plugin.data.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.plugin.data.dto.DataBusinessDefinitionSaveDTO;
import com.mdframe.forge.plugin.data.entity.DataBusinessDefinition;
import com.mdframe.forge.plugin.data.service.DataBusinessDefinitionService;
import com.mdframe.forge.plugin.data.vo.DataBusinessAiContextVO;
import com.mdframe.forge.plugin.data.vo.DataBusinessDefinitionDetailVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/data/business")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class DataBusinessDefinitionController {

    private final DataBusinessDefinitionService businessDefinitionService;

    @GetMapping("/page")
    public RespInfo<IPage<DataBusinessDefinition>> page(
        @RequestParam(required = false) String businessName,
        @RequestParam(required = false) Integer status,
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize) {
        return RespInfo.success(businessDefinitionService.page(businessName, status, pageNum, pageSize));
    }

    @GetMapping("/list")
    public RespInfo<List<DataBusinessDefinition>> list() {
        return RespInfo.success(businessDefinitionService.listEnabled());
    }

    @GetMapping("/{id}")
    public RespInfo<DataBusinessDefinitionDetailVO> getById(@PathVariable Long id) {
        return RespInfo.success(businessDefinitionService.getDetail(id));
    }

    @GetMapping("/{id}/ai-context")
    public RespInfo<DataBusinessAiContextVO> aiContext(@PathVariable Long id) {
        return RespInfo.success(businessDefinitionService.getAiContext(id));
    }

    @PostMapping
    @OperationLog(module = "数据资产", desc = "新增业务定义：{{#dto.businessName}}")
    public RespInfo<DataBusinessDefinition> add(@Validated @RequestBody DataBusinessDefinitionSaveDTO dto) {
        return RespInfo.success(businessDefinitionService.saveBusiness(dto));
    }

    @PutMapping
    @OperationLog(module = "数据资产", desc = "修改业务定义：{{#dto.businessName}}")
    public RespInfo<DataBusinessDefinition> edit(@Validated @RequestBody DataBusinessDefinitionSaveDTO dto) {
        return RespInfo.success(businessDefinitionService.updateBusiness(dto));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "数据资产", desc = "删除业务定义")
    public RespInfo<Void> remove(@PathVariable Long id) {
        businessDefinitionService.deleteBusiness(id);
        return RespInfo.success();
    }
}
