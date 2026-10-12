package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 业务流程应用统一配置保存参数。
 */
@Data
public class BusinessFlowAppConfigDTO {

    private BusinessDocumentConfigDTO documentConfig;

    private BusinessFlowBindingDTO flowBinding;

    private Map<String, Object> options = new LinkedHashMap<>();
}
