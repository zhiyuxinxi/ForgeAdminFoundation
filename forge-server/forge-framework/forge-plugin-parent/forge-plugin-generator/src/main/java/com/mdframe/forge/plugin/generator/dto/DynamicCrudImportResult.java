package com.mdframe.forge.plugin.generator.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 动态 CRUD Excel 导入结果。
 */
@Data
public class DynamicCrudImportResult {

    private Boolean success = true;

    private Integer totalRows = 0;

    private Integer successRows = 0;

    private Integer failedRows = 0;

    private String summary;

    private List<DynamicCrudImportError> errors = new ArrayList<>();

    public void addError(Integer rowNum, String field, String label, Object rawValue, String message) {
        DynamicCrudImportError error = new DynamicCrudImportError();
        error.setRowNum(rowNum);
        error.setField(field);
        error.setLabel(label);
        error.setRawValue(rawValue);
        error.setMessage(message);
        errors.add(error);
    }
}
