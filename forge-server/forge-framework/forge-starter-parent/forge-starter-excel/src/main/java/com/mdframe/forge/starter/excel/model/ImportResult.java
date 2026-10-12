package com.mdframe.forge.starter.excel.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 导入结果
 */
@Data
public class ImportResult<T> {

    public static final String PUBLIC_FAILURE_MESSAGE = "导入失败，请检查文件内容或联系管理员";
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 总行数
     */
    private Integer totalRows;
    
    /**
     * 成功行数
     */
    private Integer successRows;
    
    /**
     * 失败行数
     */
    private Integer failedRows;
    
    /**
     * 成功导入的数据
     */
    private List<T> successData = new ArrayList<>();
    
    /**
     * 错误记录列表
     */
    private List<ImportErrorRecord> errors = new ArrayList<>();
    
    /**
     * 错误报告文件路径（如果有）
     */
    @JsonIgnore
    private String errorReportPath;
    
    /**
     * 汇总信息
     */
    private String summary;

    public String getSummary() { return UiMessages.translate(summary); }
    
    /**
     * 添加错误记录
     */
    public void addError(ImportErrorRecord error) {
        this.errors.add(error);
        this.failedRows = this.errors.size();
    }
    
    /**
     * 构建汇总信息
     */
    public void buildSummary() {
        if (errors.isEmpty()) {
            this.summary = UiMessages.message("excel.import.successSummary", totalRows, successRows);
        } else {
            String errorDetail = errors.stream()
                    .limit(5)
                    .map(e -> {
                        StringBuilder sb = new StringBuilder();
                        sb.append(UiMessages.message("excel.import.rowNumber", e.getRowNum()));
                        if (e.getColumnName() != null) {
                            sb.append("【").append(e.getColumnName()).append("】");
                        }
                        sb.append(e.getErrorMessage());
                        return sb.toString();
                    })
                    .collect(Collectors.joining("en".equals(UiMessages.locale().getLanguage()) ? "; " : "；"));
            if (errors.size() > 5) {
                errorDetail += UiMessages.message("excel.import.moreErrors", errors.size());
            }
            this.summary = UiMessages.message("excel.import.failureSummary",
                    totalRows, successRows, failedRows, errorDetail);
        }
    }
}
