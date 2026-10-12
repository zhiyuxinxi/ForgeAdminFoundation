package com.mdframe.forge.plugin.generator.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeDomainDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeDomainStatusDTO;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeDomainService;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeDomainTreeVO;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeDomainVO;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeDomainWorkspaceVO;
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
 * 低代码业务领域接口。
 */
@Slf4j
@RestController
@RequestMapping("/ai/lowcode/domain")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class LowcodeDomainController {

    private final LowcodeDomainService domainService;

    @GetMapping("/page")
    @OperationLog(module = "低代码业务领域", type = OperationType.QUERY, desc = "分页查询业务领域")
    public RespInfo<Page<LowcodeDomainVO>> page(PageQuery pageQuery,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(required = false) Long parentId) {
        return RespInfo.success(domainService.page(pageQuery, keyword, status, parentId));
    }

    @GetMapping("/tree")
    @OperationLog(module = "低代码业务领域", type = OperationType.QUERY, desc = "查询业务领域树")
    public RespInfo<List<LowcodeDomainTreeVO>> tree(@RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String status) {
        return RespInfo.success(domainService.tree(keyword, status));
    }

    @GetMapping("/{id}")
    @OperationLog(module = "低代码业务领域", type = OperationType.QUERY, desc = "查询业务领域详情")
    public RespInfo<LowcodeDomainVO> detail(@PathVariable Long id) {
        return RespInfo.success(domainService.getDetail(id));
    }

    @PostMapping
    @OperationLog(module = "低代码业务领域", type = OperationType.ADD, desc = "新增业务领域")
    public RespInfo<Long> create(@RequestBody LowcodeDomainDTO dto) {
        return RespInfo.success(domainService.create(dto));
    }

    @PutMapping
    @OperationLog(module = "低代码业务领域", type = OperationType.UPDATE, desc = "修改业务领域")
    public RespInfo<Void> update(@RequestBody LowcodeDomainDTO dto) {
        domainService.update(dto);
        return RespInfo.success();
    }

    @PutMapping("/{id}/status")
    @OperationLog(module = "低代码业务领域", type = OperationType.UPDATE, desc = "启停业务领域")
    public RespInfo<Void> updateStatus(@PathVariable Long id, @RequestBody LowcodeDomainStatusDTO dto) {
        domainService.updateStatus(id, dto == null ? null : dto.getStatus());
        return RespInfo.success();
    }

    @GetMapping("/{id}/workspace")
    @OperationLog(module = "低代码业务领域", type = OperationType.QUERY, desc = "查询业务领域工作台")
    public RespInfo<LowcodeDomainWorkspaceVO> workspace(@PathVariable Long id) {
        return RespInfo.success(domainService.workspace(id));
    }

    @GetMapping("/{id}/defaults")
    @OperationLog(module = "低代码业务领域", type = OperationType.QUERY, desc = "查询业务领域默认规则")
    public RespInfo<LowcodeDomainVO> defaults(@PathVariable Long id) {
        return RespInfo.success(domainService.getDefaults(id));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "低代码业务领域", type = OperationType.DELETE, desc = "删除业务领域")
    public RespInfo<Void> delete(@PathVariable Long id) {
        domainService.delete(id);
        return RespInfo.success();
    }
}
