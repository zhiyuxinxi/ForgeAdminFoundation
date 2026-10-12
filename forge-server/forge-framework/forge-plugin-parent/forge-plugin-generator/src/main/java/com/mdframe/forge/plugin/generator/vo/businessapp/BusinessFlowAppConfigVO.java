package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 业务流程应用统一配置视图。
 */
@Data
public class BusinessFlowAppConfigVO {

    private Long objectId;

    private String suiteCode;

    private String objectCode;

    private String objectName;

    private String configKey;

    private BusinessDocumentConfigVO documentConfig;

    private BusinessFlowBindingVO flowBinding;

    private Map<String, Object> formAssets = new LinkedHashMap<>();

    private Map<String, Object> summary = new LinkedHashMap<>();

    private Map<String, Object> options = new LinkedHashMap<>();
}
