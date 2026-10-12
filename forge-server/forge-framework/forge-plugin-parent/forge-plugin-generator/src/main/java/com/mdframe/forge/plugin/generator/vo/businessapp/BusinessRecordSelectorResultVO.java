package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用记录选择器查询结果。
 */
@Data
public class BusinessRecordSelectorResultVO {

    private String suiteCode;

    private String objectCode;

    private String objectName;

    private String configKey;

    private Long total;

    private Long current;

    private Long size;

    private List<SelectorColumnVO> columns = new ArrayList<>();

    private List<Map<String, Object>> records = new ArrayList<>();

    private Map<String, String> fieldMappings = new LinkedHashMap<>();

    @Data
    public static class SelectorColumnVO {

        private String field;

        private String label;

        private String type;

        private Integer width;
    }
}
