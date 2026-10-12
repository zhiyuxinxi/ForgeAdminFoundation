package com.mdframe.forge.starter.excel.model;

import lombok.Data;
import com.mdframe.forge.starter.core.i18n.UiMessages;

/**
 * 导入错误记录
 */
@Data
public class ImportErrorRecord {
    
    /**
     * 行号（从 1 开始，0 表示表头）
     */
    private Integer rowNum;
    
    /**
     * 列名
     */
    private String columnName;
    
    /**
     * 原始值
     */
    private String rawValue;
    
    /**
     * 错误类型
     */
    private String errorType;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 建议修正值
     */
    private String suggestion;

    public String getColumnName() { return UiMessages.label(columnName, null); }
    public String getErrorType() { return UiMessages.translate(errorType); }
    public String getErrorMessage() { return UiMessages.label(errorMessage, null); }
    public String getSuggestion() { return UiMessages.translate(suggestion); }
}
