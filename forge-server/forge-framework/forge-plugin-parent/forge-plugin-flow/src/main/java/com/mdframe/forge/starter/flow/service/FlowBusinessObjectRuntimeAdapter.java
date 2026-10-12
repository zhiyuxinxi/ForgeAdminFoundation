package com.mdframe.forge.starter.flow.service;

import com.mdframe.forge.starter.flow.entity.FlowEntry;
import com.mdframe.forge.starter.flow.entity.FlowEntryFieldMapping;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 业务对象落表扩展点。
 *
 * <p>Flow 插件不直接依赖低代码 generator 插件；需要 BUSINESS_OBJECT/HYBRID 时，
 * 由业务侧提供实现，把表单数据映射为业务对象记录。</p>
 */
public interface FlowBusinessObjectRuntimeAdapter {

    BusinessRecordCreateResult createBusinessRecord(FlowEntry entry,
                                                    List<FlowEntryFieldMapping> mappings,
                                                    Map<String, Object> formData);

    default void afterProcessStarted(FlowEntry entry,
                                     BusinessRecordCreateResult record,
                                     String processInstanceId,
                                     Map<String, Object> variables) {
        // Optional hook for business modules to create object-flow links.
    }

    @Data
    class BusinessRecordCreateResult {
        private String objectCode;

        private Long recordId;

        private String businessKey;

        private Map<String, Object> variables;
    }
}
