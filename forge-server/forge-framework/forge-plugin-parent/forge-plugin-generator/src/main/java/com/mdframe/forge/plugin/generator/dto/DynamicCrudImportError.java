package com.mdframe.forge.plugin.generator.dto;

import lombok.Data;

/**
 * 动态 CRUD Excel 导入错误明细。
 */
@Data
public class DynamicCrudImportError {

    private Integer rowNum;

    private String field;

    private String label;

    private Object rawValue;

    private String message;
}
