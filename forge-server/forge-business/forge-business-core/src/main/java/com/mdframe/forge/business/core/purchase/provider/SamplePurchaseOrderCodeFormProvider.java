package com.mdframe.forge.business.core.purchase.provider;

import com.mdframe.forge.business.core.purchase.dto.SamplePurchaseOrderTaskSaveDTO;
import com.mdframe.forge.business.core.purchase.service.SamplePurchaseOrderService;
import com.mdframe.forge.business.core.purchase.support.SamplePurchaseOrderFlowDefinition;
import com.mdframe.forge.business.core.purchase.vo.SamplePurchaseOrderVO;
import com.mdframe.forge.lowcode.api.form.BusinessCodeFormProvider;
import com.mdframe.forge.lowcode.api.form.BusinessTaskFormContextQueryDTO;
import com.mdframe.forge.lowcode.api.form.BusinessTaskFormContextVO;
import com.mdframe.forge.lowcode.api.form.BusinessTaskFormSaveDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 采购单审批测试代码表单 Provider。
 */
@Component
@RequiredArgsConstructor
public class SamplePurchaseOrderCodeFormProvider implements BusinessCodeFormProvider {

    private final SamplePurchaseOrderService purchaseOrderService;

    @Override
    public String providerKey() {
        return SamplePurchaseOrderFlowDefinition.PROVIDER_KEY;
    }

    @Override
    public String providerName() {
        return SamplePurchaseOrderFlowDefinition.PROVIDER_NAME;
    }

    @Override
    public String buildSummary(Map<String, Object> recordData) {
        return SamplePurchaseOrderFlowDefinition.buildSummary(recordData);
    }

    @Override
    public Map<Long, String> buildSummaries(String objectCode, Collection<Long> recordIds) {
        if (!SamplePurchaseOrderFlowDefinition.supportsObject(objectCode)) {
            return Map.of();
        }
        List<SamplePurchaseOrderVO> details = purchaseOrderService.detailsByIds(recordIds);
        if (details == null || details.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> result = new LinkedHashMap<>();
        for (SamplePurchaseOrderVO detail : details) {
            if (detail != null && detail.getId() != null) {
                result.put(detail.getId(), SamplePurchaseOrderFlowDefinition.buildSummary(detail));
            }
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> formAssets(String objectCode) {
        return SamplePurchaseOrderFlowDefinition.formAssets(objectCode);
    }

    @Override
    public BusinessTaskFormContextVO buildContext(BusinessTaskFormContextQueryDTO query,
                                                  Map<String, Object> formRef,
                                                  List<Map<String, Object>> fieldPermissions) {
        Long recordId = SamplePurchaseOrderFlowDefinition.resolveRecordId(
                query == null ? null : query.getRecordId(),
                query == null ? null : query.getBusinessKey());
        SamplePurchaseOrderVO detail = recordId == null ? null : purchaseOrderService.detail(recordId);
        BusinessTaskFormContextVO vo = SamplePurchaseOrderFlowDefinition.buildTaskFormContext(
                query,
                formRef,
                fieldPermissions,
                recordId,
                SamplePurchaseOrderFlowDefinition.buildSummary(detail),
                detail != null);
        if (detail != null) {
            vo.setRecordData(SamplePurchaseOrderFlowDefinition.recordData(detail));
        } else {
            vo.getWarnings().add("未找到采购单业务记录");
        }
        return vo;
    }

    @Override
    public BusinessTaskFormContextVO saveContext(BusinessTaskFormSaveDTO dto,
                                                 Map<String, Object> formRef,
                                                 List<Map<String, Object>> fieldPermissions) {
        SamplePurchaseOrderTaskSaveDTO saveDTO = SamplePurchaseOrderFlowDefinition.toTaskSaveDTO(dto);
        SamplePurchaseOrderVO detail = purchaseOrderService.saveTaskFields(saveDTO);

        BusinessTaskFormContextQueryDTO query = new BusinessTaskFormContextQueryDTO();
        query.setTaskId(dto.getTaskId());
        query.setBusinessKey(dto.getBusinessKey());
        query.setProcessInstanceId(dto.getProcessInstanceId());
        query.setProcessDefKey(dto.getProcessDefKey());
        query.setTaskDefKey(dto.getTaskDefKey());
        query.setObjectCode(dto.getObjectCode());
        query.setRecordId(detail.getId());
        query.setFormKey(dto.getFormKey());
        return buildContext(query, formRef, fieldPermissions);
    }
}
