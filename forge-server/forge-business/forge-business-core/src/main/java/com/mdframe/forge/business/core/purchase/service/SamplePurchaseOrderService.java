package com.mdframe.forge.business.core.purchase.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.business.core.purchase.dto.SamplePurchaseOrderDTO;
import com.mdframe.forge.business.core.purchase.dto.SamplePurchaseOrderQuery;
import com.mdframe.forge.business.core.purchase.dto.SamplePurchaseOrderSubmitDTO;
import com.mdframe.forge.business.core.purchase.dto.SamplePurchaseOrderTaskSaveDTO;
import com.mdframe.forge.business.core.purchase.support.SamplePurchaseOrderFlowDefinition;
import com.mdframe.forge.business.core.purchase.vo.SamplePurchaseOrderFlowInitVO;
import com.mdframe.forge.business.core.purchase.vo.SamplePurchaseOrderVO;
import com.mdframe.forge.starter.core.domain.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 采购单审批测试服务。
 */
public interface SamplePurchaseOrderService {

    String MODEL_KEY = SamplePurchaseOrderFlowDefinition.MODEL_KEY;

    String BUSINESS_TYPE = SamplePurchaseOrderFlowDefinition.BUSINESS_TYPE;

    String STATUS_DRAFT = SamplePurchaseOrderFlowDefinition.STATUS_DRAFT;

    String STATUS_IN_PROCESS = SamplePurchaseOrderFlowDefinition.STATUS_IN_PROCESS;

    String STATUS_NEED_MODIFY = SamplePurchaseOrderFlowDefinition.STATUS_NEED_MODIFY;

    String STATUS_APPROVED = SamplePurchaseOrderFlowDefinition.STATUS_APPROVED;

    String STATUS_REJECTED = SamplePurchaseOrderFlowDefinition.STATUS_REJECTED;

    String STATUS_CANCELED = SamplePurchaseOrderFlowDefinition.STATUS_CANCELED;

    IPage<SamplePurchaseOrderVO> page(PageQuery pageQuery, SamplePurchaseOrderQuery query);

    SamplePurchaseOrderVO detail(Long id);

    List<SamplePurchaseOrderVO> detailsByIds(Collection<Long> ids);

    SamplePurchaseOrderVO detailByBusinessKey(String businessKey);

    Long create(SamplePurchaseOrderDTO dto);

    void update(SamplePurchaseOrderDTO dto);

    void delete(Long id);

    String submit(Long id, SamplePurchaseOrderSubmitDTO dto);

    SamplePurchaseOrderVO saveTaskFields(SamplePurchaseOrderTaskSaveDTO dto);

    SamplePurchaseOrderFlowInitVO ensureFlowModel();
}
