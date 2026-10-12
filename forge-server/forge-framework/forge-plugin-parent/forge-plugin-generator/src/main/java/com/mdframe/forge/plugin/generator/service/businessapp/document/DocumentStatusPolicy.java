package com.mdframe.forge.plugin.generator.service.businessapp.document;

import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessDocumentConfigDTO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessDocumentConfigVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/** Document status defaults, overrides and allowed action policy. */
@Slf4j
public final class DocumentStatusPolicy {

    private final ObjectMapper objectMapper;

    public DocumentStatusPolicy(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<BusinessDocumentConfigVO.StatusMappingRowVO> normalizeStatusRows(
            List<BusinessDocumentConfigDTO.StatusMappingRowDTO> rows,
            Map<String, String> legacyMapping) {
        Map<String, BusinessDocumentConfigVO.StatusMappingRowVO> defaults = new LinkedHashMap<>();
        for (BusinessDocumentConfigVO.StatusMappingRowVO row : defaultStatusRows()) {
            defaults.put(row.getStandardStatus(), row);
        }
        if (legacyMapping != null) {
            legacyMapping.forEach((key, value) -> {
                BusinessDocumentConfigVO.StatusMappingRowVO row = defaults.get(key);
                if (row != null && StringUtils.isNotBlank(value)) {
                    row.setStatusValue(value.trim());
                }
            });
        }
        if (rows != null) {
            for (BusinessDocumentConfigDTO.StatusMappingRowDTO input : rows) {
                applyOverride(defaults, input);
            }
        }
        for (BusinessDocumentConfigVO.StatusMappingRowVO row : defaults.values()) {
            if (StringUtils.isBlank(row.getStatusValue())) {
                throw new BusinessException(UiMessages.translate("状态映射缺少存储值: ") + row.getStandardStatus());
            }
        }
        return new ArrayList<>(defaults.values());
    }

    private void applyOverride(Map<String, BusinessDocumentConfigVO.StatusMappingRowVO> defaults,
            BusinessDocumentConfigDTO.StatusMappingRowDTO input) {
        if (input == null || StringUtils.isBlank(input.getStandardStatus())) {
            return;
        }
        String standardStatus = input.getStandardStatus().trim().toUpperCase();
        BusinessDocumentConfigVO.StatusMappingRowVO row = defaults.computeIfAbsent(
                standardStatus, key -> statusRow(key, key, key, key, "default", true, true, false));
        if (StringUtils.isNotBlank(input.getStatusValue())) {
            row.setStatusValue(input.getStatusValue().trim());
        }
        if (StringUtils.isNotBlank(input.getDisplayName())) {
            row.setDisplayName(input.getDisplayName().trim());
        }
        if (StringUtils.isNotBlank(input.getTagType())) {
            row.setTagType(input.getTagType().trim());
        }
        if (input.getAllowEdit() != null) {
            row.setAllowEdit(input.getAllowEdit());
        }
        if (input.getAllowDelete() != null) {
            row.setAllowDelete(input.getAllowDelete());
        }
        if (input.getAllowStartFlow() != null) {
            row.setAllowStartFlow(input.getAllowStartFlow());
        }
    }

    public List<BusinessDocumentConfigVO.StatusMappingRowVO> readStatusRows(Map<String, Object> options,
                                                                            Map<String, String> legacyMapping) {
        Object rawRows = options == null ? null : options.get("statusMappingRows");
        if (rawRows != null) {
            List<BusinessDocumentConfigVO.StatusMappingRowVO> rows;
            try {
                rows = objectMapper.convertValue(
                        rawRows, new TypeReference<List<BusinessDocumentConfigVO.StatusMappingRowVO>>() {});
            } catch (IllegalArgumentException conversionFailure) {
                log.warn("Invalid stored status-mapping rows; using the legacy mapping ({})",
                        conversionFailure.getClass().getSimpleName());
                return normalizeStatusRows(null, legacyMapping);
            }
            if (rows != null && !rows.isEmpty()) {
                return normalizeStatusRowsFromVO(rows, legacyMapping);
            }
        }
        return normalizeStatusRows(null, legacyMapping);
    }

    private List<BusinessDocumentConfigVO.StatusMappingRowVO> normalizeStatusRowsFromVO(
            List<BusinessDocumentConfigVO.StatusMappingRowVO> rows,
            Map<String, String> legacyMapping) {
        List<BusinessDocumentConfigDTO.StatusMappingRowDTO> dtoRows = new ArrayList<>();
        for (BusinessDocumentConfigVO.StatusMappingRowVO row : rows) {
            BusinessDocumentConfigDTO.StatusMappingRowDTO dto = new BusinessDocumentConfigDTO.StatusMappingRowDTO();
            dto.setStandardStatus(row.getStandardStatus());
            dto.setStatusValue(row.getStatusValue());
            dto.setDisplayName(row.getDisplayName());
            dto.setTagType(row.getTagType());
            dto.setAllowEdit(row.getAllowEdit());
            dto.setAllowDelete(row.getAllowDelete());
            dto.setAllowStartFlow(row.getAllowStartFlow());
            dtoRows.add(dto);
        }
        return normalizeStatusRows(dtoRows, legacyMapping);
    }

    public Map<String, String> statusMappingFromRows(List<BusinessDocumentConfigVO.StatusMappingRowVO> rows) {
        Map<String, String> result = new LinkedHashMap<>();
        for (BusinessDocumentConfigVO.StatusMappingRowVO row : rows) {
            if (StringUtils.isNotBlank(row.getStandardStatus()) && StringUtils.isNotBlank(row.getStatusValue())) {
                result.put(row.getStandardStatus(), row.getStatusValue());
            }
        }
        return normalizeStatusMapping(result);
    }

    public List<BusinessDocumentConfigVO.StatusMappingRowVO> defaultStatusRows() {
        List<BusinessDocumentConfigVO.StatusMappingRowVO> rows = new ArrayList<>();
        rows.add(statusRow("DRAFT", "草稿", "DRAFT", "草稿", "default", true, true, true));
        rows.add(statusRow("SUBMITTED", "已提交", "SUBMITTED", "已提交", "info", false, false, false));
        rows.add(statusRow("IN_PROCESS", "流程中", "IN_PROCESS", "流程中", "warning", false, false, false));
        rows.add(statusRow("APPROVED", "已通过", "APPROVED", "已通过", "success", false, false, false));
        rows.add(statusRow("REJECTED", "已驳回", "REJECTED", "已驳回", "error", true, false, true));
        rows.add(statusRow("CANCELED", "已撤回", "CANCELED", "已撤回", "default", true, false, true));
        rows.add(statusRow("CLOSED", "已关闭", "CLOSED", "已关闭", "default", false, false, false));
        return rows;
    }

    private BusinessDocumentConfigVO.StatusMappingRowVO statusRow(String standardStatus,
                                                                  String standardLabel,
                                                                  String statusValue,
                                                                  String displayName,
                                                                  String tagType,
                                                                  Boolean allowEdit,
                                                                  Boolean allowDelete,
                                                                  Boolean allowStartFlow) {
        BusinessDocumentConfigVO.StatusMappingRowVO row = new BusinessDocumentConfigVO.StatusMappingRowVO();
        row.setStandardStatus(standardStatus);
        row.setStandardLabel(standardLabel);
        row.setStatusValue(statusValue);
        row.setDisplayName(displayName);
        row.setTagType(tagType);
        row.setAllowEdit(allowEdit);
        row.setAllowDelete(allowDelete);
        row.setAllowStartFlow(allowStartFlow);
        return row;
    }

    private Map<String, String> normalizeStatusMapping(Map<String, String> input) {
        Map<String, String> result = defaultStatusMapping();
        if (input == null) {
            return result;
        }
        input.forEach((key, value) -> {
            if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
                result.put(key.trim(), value.trim());
            }
        });
        return result;
    }

    public Map<String, String> defaultStatusMapping() {
        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put("DRAFT", "DRAFT");
        mapping.put("SUBMITTED", "SUBMITTED");
        mapping.put("IN_PROCESS", "IN_PROCESS");
        mapping.put("APPROVED", "APPROVED");
        mapping.put("REJECTED", "REJECTED");
        mapping.put("CANCELED", "CANCELED");
        mapping.put("CLOSED", "CLOSED");
        return mapping;
    }

    public Map<String, String> readStringMap(String json) {
        if (StringUtils.isBlank(json)) {
            return defaultStatusMapping();
        }
        try {
            Map<String, String> value = objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
            return normalizeStatusMapping(value);
        } catch (Exception e) {
            return defaultStatusMapping();
        }
    }
}
