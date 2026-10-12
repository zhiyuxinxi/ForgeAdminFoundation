package com.mdframe.forge.business.core.purchase.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.business.core.purchase.dto.SamplePurchaseOrderDTO;
import com.mdframe.forge.business.core.purchase.dto.SamplePurchaseOrderQuery;
import com.mdframe.forge.business.core.purchase.dto.SamplePurchaseOrderSubmitDTO;
import com.mdframe.forge.business.core.purchase.dto.SamplePurchaseOrderTaskSaveDTO;
import com.mdframe.forge.business.core.purchase.service.SamplePurchaseOrderService;
import com.mdframe.forge.business.core.purchase.vo.SamplePurchaseOrderFlowInitVO;
import com.mdframe.forge.business.core.purchase.vo.SamplePurchaseOrderVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 采购单审批测试接口。
 */
@RestController
@RequestMapping("/business/sample-purchase-order")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class SamplePurchaseOrderController {

    private final SamplePurchaseOrderService purchaseOrderService;

    @GetMapping("/page")
    @OperationLog(module = "采购单审批测试", type = OperationType.QUERY, desc = "分页查询采购单")
    public RespInfo<IPage<SamplePurchaseOrderVO>> page(PageQuery pageQuery, SamplePurchaseOrderQuery query) {
        return RespInfo.success(purchaseOrderService.page(pageQuery, query));
    }

    @PostMapping("/getById")
    @OperationLog(module = "采购单审批测试", type = OperationType.QUERY, desc = "查询采购单详情")
    public RespInfo<SamplePurchaseOrderVO> detail(@RequestParam(required = false) Long id,
                                                  @RequestParam(required = false) String businessKey) {
        if (StringUtils.hasText(businessKey)) {
            return RespInfo.success(purchaseOrderService.detailByBusinessKey(businessKey));
        }
        return RespInfo.success(purchaseOrderService.detail(id));
    }

    @PostMapping("/add")
    @OperationLog(module = "采购单审批测试", type = OperationType.ADD, desc = "新增采购单")
    public RespInfo<Long> add(@RequestBody SamplePurchaseOrderDTO dto) {
        return RespInfo.success(purchaseOrderService.create(dto));
    }

    @PostMapping("/edit")
    @OperationLog(module = "采购单审批测试", type = OperationType.UPDATE, desc = "修改采购单")
    public RespInfo<Void> edit(@RequestBody SamplePurchaseOrderDTO dto) {
        purchaseOrderService.update(dto);
        return RespInfo.success();
    }

    @PostMapping("/remove/{id}")
    @OperationLog(module = "采购单审批测试", type = OperationType.DELETE, desc = "删除采购单")
    public RespInfo<Void> remove(@PathVariable Long id) {
        purchaseOrderService.delete(id);
        return RespInfo.success();
    }

    @PostMapping("/submit/{id}")
    @OperationLog(module = "采购单审批测试", type = OperationType.UPDATE, desc = "提交采购单审批")
    public RespInfo<String> submit(@PathVariable Long id, @RequestBody SamplePurchaseOrderSubmitDTO dto) {
        return RespInfo.success(purchaseOrderService.submit(id, dto));
    }

    @PostMapping("/task/save")
    @OperationLog(module = "采购单审批测试", type = OperationType.UPDATE, desc = "保存采购单待办节点字段")
    public RespInfo<SamplePurchaseOrderVO> saveTaskFields(@RequestBody SamplePurchaseOrderTaskSaveDTO dto) {
        return RespInfo.success(purchaseOrderService.saveTaskFields(dto));
    }

    @PostMapping("/init-flow")
    @OperationLog(module = "采购单审批测试", type = OperationType.UPDATE, desc = "初始化采购单测试流程")
    public RespInfo<SamplePurchaseOrderFlowInitVO> initFlow() {
        return RespInfo.success(purchaseOrderService.ensureFlowModel());
    }
}
