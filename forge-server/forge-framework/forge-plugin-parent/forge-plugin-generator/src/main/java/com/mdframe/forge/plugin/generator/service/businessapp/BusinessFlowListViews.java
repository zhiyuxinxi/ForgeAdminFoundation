package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.flow.client.spi.FlowBusinessListDisplayItem;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessDocumentConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessFlowInstanceLink;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.mapper.BusinessFlowInstanceLinkMapper;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectVO;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowRuntimeMetadata.BusinessRuntimeContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowRuntimeMetadata.parseBusinessKeyObjectCode;
import static com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowRuntimeMetadata.parseBusinessKeyRecordId;
import static com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowRuntimeMetadata.readJsonObject;
import static com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowRuntimeMetadata.resolveTenantId;

/** Batches task-list enrichment by canonical object, retaining authorized lowcode/provider reads. */
@RequiredArgsConstructor
final class BusinessFlowListViews {
    private final BusinessFlowRuntimeMetadata metadata;
    private final BusinessFlowInstanceLinkMapper flowInstanceLinkMapper;
    private final DynamicCrudService dynamicCrudService;
    private final BusinessCodeFormProviderRegistry codeFormProviderRegistry;

    void enrichBusinessListDisplay(List<FlowBusinessListDisplayItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        Long tenantId = resolveTenantId();
        Map<String, AiBusinessFlowInstanceLink> linkByBusinessKey = loadLinksByBusinessKey(tenantId, items);
        Map<String, BusinessRuntimeContext> contextCache = new HashMap<>();
        Map<String, BusinessListGroup> grouped = new LinkedHashMap<>();
        for (FlowBusinessListDisplayItem item : items) {
            if (item == null) {
                continue;
            }
            AiBusinessFlowInstanceLink link = linkByBusinessKey.get(StringUtils.trimToEmpty(item.getBusinessKey()));
            BusinessListRuntime runtime = resolveListRuntime(item, link);
            if (runtime == null) {
                item.setProcessDefinitionName(StringUtils.firstNonBlank(
                        item.getProcessDefinitionName(), item.getProcessName(), item.getProcessDefKey()));
                continue;
            }
            String objectCode = runtime.objectCode();
            BusinessRuntimeContext context = contextCache.computeIfAbsent(objectCode,
                    code -> metadata.resolveBusinessRuntimeContext(tenantId, code));
            String canonicalObjectCode = StringUtils.firstNonBlank(context.objectCode(), objectCode);
            grouped.computeIfAbsent(canonicalObjectCode,
                            key -> new BusinessListGroup(context, new ArrayList<>()))
                    .runtimes()
                    .add(new BusinessListRuntime(item, canonicalObjectCode, runtime.recordId(), runtime.businessKey()));
        }
        grouped.forEach((objectCode, group) -> enrichBusinessListGroup(tenantId, group.context(), group.runtimes()));
    }

    /** Link identity wins over item fields; tolerant key parsing is the final fallback. */
    private BusinessListRuntime resolveListRuntime(FlowBusinessListDisplayItem item, AiBusinessFlowInstanceLink link) {
        String objectCode = StringUtils.firstNonBlank(
                link == null ? null : link.getObjectCode(),
                StringUtils.trimToNull(item.getObjectCode()),
                parseBusinessKeyObjectCode(item.getBusinessKey()));
        Long recordId = link == null || link.getRecordId() == null
                ? item.getRecordId()
                : link.getRecordId();
        if (recordId == null) {
            recordId = parseBusinessKeyRecordId(item.getBusinessKey());
        }
        String businessKey = StringUtils.firstNonBlank(
                link == null ? null : link.getBusinessKey(),
                StringUtils.trimToNull(item.getBusinessKey()),
                objectCode != null && recordId != null ? objectCode + ":" + recordId : null);
        if (StringUtils.isBlank(objectCode) || recordId == null) {
            return null;
        }
        return new BusinessListRuntime(item, objectCode, recordId, businessKey);
    }

    private Map<String, AiBusinessFlowInstanceLink> loadLinksByBusinessKey(Long tenantId,
                                                                           List<FlowBusinessListDisplayItem> items) {
        Set<String> businessKeys = new LinkedHashSet<>();
        for (FlowBusinessListDisplayItem item : items) {
            String businessKey = item == null ? null : StringUtils.trimToNull(item.getBusinessKey());
            if (businessKey != null) {
                businessKeys.add(businessKey);
            }
        }
        if (businessKeys.isEmpty()) {
            return Map.of();
        }
        List<AiBusinessFlowInstanceLink> links = flowInstanceLinkMapper.selectLatestByBusinessKeys(tenantId, businessKeys);
        Map<String, AiBusinessFlowInstanceLink> result = new LinkedHashMap<>();
        if (links != null) {
            for (AiBusinessFlowInstanceLink link : links) {
                if (link != null && StringUtils.isNotBlank(link.getBusinessKey())) {
                    result.put(link.getBusinessKey(), link);
                }
            }
        }
        return result;
    }

    private void enrichBusinessListGroup(Long tenantId, BusinessRuntimeContext context, List<BusinessListRuntime> runtimes) {
        String objectCode = context == null ? null : context.objectCode();
        if (StringUtils.isBlank(objectCode) || runtimes == null || runtimes.isEmpty()) {
            return;
        }
        BusinessObjectVO object = metadata.queryBusinessObject(tenantId, objectCode);
        AiCrudConfig runtimeConfig = context.runtimeConfig();
        AiBusinessDocumentConfig documentConfig = context.documentConfig();
        String configKey = context.configKey();
        AiBusinessBinding binding = metadata.selectMainFlowBindingForConfig(tenantId, objectCode);
        JSONObject bindingConfig = binding == null ? new JSONObject() : readJsonObject(binding.getBindingConfig());
        String objectName = StringUtils.firstNonBlank(
                object == null ? null : object.getObjectName(),
                documentConfig == null ? null : documentConfig.getDocumentName(),
                runtimeConfig == null ? null : runtimeConfig.getObjectName(),
                objectCode);

        if (StringUtils.isNotBlank(configKey)) {
            enrichLowcodeBusinessListGroup(objectCode, objectName, configKey, object, bindingConfig, runtimes);
            return;
        }
        enrichCodeBusinessListGroup(objectCode, objectName, runtimes);
    }

    private void enrichLowcodeBusinessListGroup(String objectCode,
                                                String objectName,
                                                String configKey,
                                                BusinessObjectVO object,
                                                JSONObject bindingConfig,
                                                List<BusinessListRuntime> runtimes) {
        List<Long> recordIds = runtimes.stream()
                .map(BusinessListRuntime::recordId)
                .distinct()
                .toList();
        Map<Object, Map<String, Object>> records = dynamicCrudService.selectByIds(configKey, recordIds);
        for (BusinessListRuntime runtime : runtimes) {
            FlowBusinessListDisplayItem item = runtime.item();
            Map<String, Object> recordData = findBatchRecord(records, runtime.recordId());
            item.setObjectCode(objectCode);
            item.setRecordId(runtime.recordId());
            item.setBusinessObjectName(objectName);
            item.setBusinessSummary(StringUtils.firstNonBlank(
                    BusinessFlowDisplayText.resolveBusinessSummary(object, bindingConfig, recordData),
                    item.getBusinessSummary()));
            item.setProcessDefinitionName(StringUtils.firstNonBlank(
                    item.getProcessDefinitionName(),
                    item.getProcessName(),
                    bindingConfig.getString("flowModelName"),
                    item.getProcessDefKey()));
        }
    }

    private Map<String, Object> findBatchRecord(Map<Object, Map<String, Object>> records, Long recordId) {
        if (records == null || records.isEmpty() || recordId == null) {
            return Map.of();
        }
        Map<String, Object> record = records.get(recordId);
        if (record != null) {
            return record;
        }
        record = records.get(String.valueOf(recordId));
        return record == null ? Map.of() : record;
    }

    private void enrichCodeBusinessListGroup(String objectCode,
                                             String fallbackObjectName,
                                             List<BusinessListRuntime> runtimes) {
        List<Map<String, Object>> assets = codeFormProviderRegistry.listAssets(objectCode, true);
        if (assets.isEmpty()) {
            applyBusinessListFallback(objectCode, fallbackObjectName, runtimes);
            return;
        }
        Map<String, Object> asset = assets.get(0);
        String providerKey = StringUtils.trimToNull(textValue(asset.get("providerKey")));
        String objectName = StringUtils.firstNonBlank(
                StringUtils.trimToNull(textValue(asset.get("objectName"))),
                StringUtils.trimToNull(textValue(asset.get("businessName"))),
                StringUtils.trimToNull(textValue(asset.get("appName"))),
                fallbackObjectName,
                objectCode);
        Map<Long, String> summaries = providerKey == null
                ? Map.of()
                : codeFormProviderRegistry.find(providerKey)
                        .map(provider -> provider.buildSummaries(objectCode, collectRecordIds(runtimes)))
                        .orElse(Map.of());
        if (summaries == null) {
            summaries = Map.of();
        }
        for (BusinessListRuntime runtime : runtimes) {
            FlowBusinessListDisplayItem item = runtime.item();
            item.setObjectCode(objectCode);
            item.setRecordId(runtime.recordId());
            item.setBusinessObjectName(objectName);
            item.setBusinessSummary(StringUtils.firstNonBlank(summaries.get(runtime.recordId()), item.getBusinessSummary()));
            item.setProcessDefinitionName(StringUtils.firstNonBlank(
                    item.getProcessDefinitionName(), item.getProcessName(), item.getProcessDefKey()));
        }
    }

    private Collection<Long> collectRecordIds(List<BusinessListRuntime> runtimes) {
        List<Long> ids = new ArrayList<>();
        for (BusinessListRuntime runtime : runtimes) {
            if (runtime.recordId() != null && !ids.contains(runtime.recordId())) {
                ids.add(runtime.recordId());
            }
        }
        return ids;
    }

    private void applyBusinessListFallback(String objectCode,
                                           String objectName,
                                           List<BusinessListRuntime> runtimes) {
        for (BusinessListRuntime runtime : runtimes) {
            FlowBusinessListDisplayItem item = runtime.item();
            item.setObjectCode(objectCode);
            item.setRecordId(runtime.recordId());
            item.setBusinessObjectName(StringUtils.firstNonBlank(objectName, item.getBusinessObjectName(), objectCode));
            item.setProcessDefinitionName(StringUtils.firstNonBlank(
                    item.getProcessDefinitionName(), item.getProcessName(), item.getProcessDefKey()));
        }
    }

    private String textValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return "null".equalsIgnoreCase(text) ? null : text;
    }

    private record BusinessListGroup(BusinessRuntimeContext context,
                                     List<BusinessListRuntime> runtimes) {
    }

    private record BusinessListRuntime(FlowBusinessListDisplayItem item,
                                       String objectCode,
                                       Long recordId,
                                       String businessKey) {
    }
}
