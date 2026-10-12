package com.mdframe.forge.starter.flow.dto;

import lombok.Data;

/**
 * 流程表单字段目录项。
 */
@Data
public class FormFieldCatalogItemDTO {

    private String field;

    private String label;

    private String componentType;

    private String dataType;

    private Boolean required;

    private String optionSource;

    private String source;
}
