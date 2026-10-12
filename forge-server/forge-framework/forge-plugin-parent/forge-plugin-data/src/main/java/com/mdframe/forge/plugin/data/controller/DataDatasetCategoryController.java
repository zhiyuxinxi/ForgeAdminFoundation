package com.mdframe.forge.plugin.data.controller;

import com.mdframe.forge.plugin.data.dto.DataDatasetCategorySaveDTO;
import com.mdframe.forge.plugin.data.entity.DataDatasetCategory;
import com.mdframe.forge.plugin.data.service.DataDatasetCategoryService;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/data/dataset/category")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class DataDatasetCategoryController {

    private final DataDatasetCategoryService categoryService;

    @GetMapping("/tree")
    public RespInfo<List<DataDatasetCategory>> tree() {
        return RespInfo.success(categoryService.listTree());
    }

    @PostMapping
    @OperationLog(module = "数据资产", desc = "新增数据集分类：{{#dto.categoryName}}")
    public RespInfo<DataDatasetCategory> add(@Validated @RequestBody DataDatasetCategorySaveDTO dto) {
        return RespInfo.success(categoryService.saveCategory(dto));
    }

    @PutMapping
    @OperationLog(module = "数据资产", desc = "修改数据集分类：{{#dto.categoryName}}")
    public RespInfo<DataDatasetCategory> edit(@Validated @RequestBody DataDatasetCategorySaveDTO dto) {
        return RespInfo.success(categoryService.updateCategory(dto));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "数据资产", desc = "删除数据集分类")
    public RespInfo<Void> remove(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return RespInfo.success();
    }
}
