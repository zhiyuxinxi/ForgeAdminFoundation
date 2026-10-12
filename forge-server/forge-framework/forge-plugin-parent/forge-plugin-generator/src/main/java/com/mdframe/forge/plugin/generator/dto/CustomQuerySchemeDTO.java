package com.mdframe.forge.plugin.generator.dto;

import lombok.Data;

import java.util.List;

/**
 * 自定义查询方案保存请求。
 */
@Data
public class CustomQuerySchemeDTO {

    private Long id;

    private String schemeName;

    private List<CustomQueryConditionDTO> conditions;

    private List<String> fields;

    private String orderByColumn;

    private String isAsc;

    private String renderMode;

    private Integer isDefault;

    private String remark;
}
