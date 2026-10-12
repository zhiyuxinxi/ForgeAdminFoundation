package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessTaskFormContextVO;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Applies configured public code-form metadata before the existing task permission projection. */
final class BusinessCodeTaskFields {
    private final TaskFormProjection projection;

    BusinessCodeTaskFields(TaskFormProjection projection) {
        this.projection = projection;
    }

    BusinessTaskFormContextVO apply(BusinessTaskFormContextVO context, JSONObject metadata,
                                    List<Map<String, Object>> permissions) {
        return projection.applyBusinessCodeFieldPermissions(applyMetadata(context, metadata), permissions);
    }

    private BusinessTaskFormContextVO applyMetadata(BusinessTaskFormContextVO context, JSONObject metadata) {
        if (context == null) {
            return null;
        }
        List<Map<String, Object>> configuredFields = CodeFormMetadata.normalizeCodeAppMetadataFields(metadata.get("fields"), false);
        if (configuredFields.isEmpty() || context.getFields() == null || context.getFields().isEmpty()) {
            return context;
        }
        Map<String, Map<String, Object>> configuredMap = new LinkedHashMap<>();
        for (Map<String, Object> field : configuredFields) {
            String fieldCode = StringUtils.trimToNull(textValue(field.get("field")));
            if (fieldCode != null) {
                projection.putPermissionAliases(configuredMap, fieldCode, field);
            }
        }
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> source : context.getFields()) {
            String fieldCode = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(textValue(source.get("field"))),
                    StringUtils.trimToNull(textValue(source.get("fieldCode"))));
            Map<String, Object> configured = fieldCode == null ? null : configuredMap.get(fieldCode);
            if (configured == null || !CodeFormMetadata.isPublicCodeAppFormField(configured)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>(source);
            mergeNonNull(item, configured);
            item.put("field", fieldCode);
            item.put("fieldCode", fieldCode);
            filtered.add(item);
        }
        context.setFields(filtered);
        context.setRecordData(projection.filterVisibleRecordData(context.getRecordData(), filtered));
        return context;
    }

    private void mergeNonNull(Map<String, Object> target, Map<String, Object> source) {
        if (target == null || source == null) {
            return;
        }
        source.forEach((key, value) -> {
            if (value != null) {
                target.put(key, value);
            }
        });
    }

    void applyApprovalPolicy(BusinessTaskFormContextVO vo, JSONObject source) {
        if (vo == null || source == null) {
            return;
        }
        vo.setAllowApprove(readNullableBooleanValue(source.get("allowApprove")));
        vo.setAllowDelegate(readNullableBooleanValue(source.get("allowDelegate")));
        vo.setAllowReject(readNullableBooleanValue(source.get("allowReject")));
        vo.setAllowRejectToStart(readNullableBooleanValue(source.get("allowRejectToStart")));
        vo.setAllowReturn(readNullableBooleanValue(source.get("allowReturn")));
        vo.setAllowTerminate(readNullableBooleanValue(source.get("allowTerminate")));
        vo.setRequireSignature(readNullableBooleanValue(source.get("requireSignature")));
        vo.setRequireComment(readNullableBooleanValue(source.get("requireComment")));
    }

    private Boolean readNullableBooleanValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = StringUtils.trimToEmpty(String.valueOf(value));
        if (StringUtils.isBlank(text)) {
            return null;
        }
        return "true".equalsIgnoreCase(text) || "1".equals(text) || "yes".equalsIgnoreCase(text);
    }

    private String textValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return "null".equalsIgnoreCase(text) ? null : text;
    }

}
