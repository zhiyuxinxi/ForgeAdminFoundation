package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用记录选择器查询请求。
 */
@Data
public class BusinessRecordSelectorQueryDTO {

    private String suiteCode;

    private String objectCode;

    private String businessObjectCode;

    private String targetObjectCode;

    private String targetEntityCode;

    private String candidateObjectCode;

    private String referenceObjectCode;

    private String refObjectCode;

    private String sourceObjectCode;

    private String targetCode;

    private String keyword;

    private List<String> keywordFields = new ArrayList<>();

    private Map<String, Object> searchParams = new LinkedHashMap<>();

    private List<String> displayFields = new ArrayList<>();

    private List<FieldMappingDTO> fieldMappings = new ArrayList<>();

    private String orderByColumn;

    private String isAsc;

    @Data
    public static class FieldMappingDTO {

        private String sourceField;

        private String targetField;
    }
}
