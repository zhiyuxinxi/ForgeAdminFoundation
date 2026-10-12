package com.mdframe.forge.plugin.generator.dto;

import lombok.Data;

/**
 * 动态 CRUD 导出提交结果。
 */
@Data
public class DynamicCrudExportResult {

    private Boolean async;
    private Long taskId;
    private Long totalCount;
    private Integer threshold;
    private String message;

    public static DynamicCrudExportResult async(Long taskId, Long totalCount, Integer threshold) {
        DynamicCrudExportResult result = new DynamicCrudExportResult();
        result.setAsync(true);
        result.setTaskId(taskId);
        result.setTotalCount(totalCount);
        result.setThreshold(threshold);
        result.setMessage("导出数据量较大，已转为异步导出任务");
        return result;
    }

    public static DynamicCrudExportResult sync(Long totalCount, Integer threshold) {
        DynamicCrudExportResult result = new DynamicCrudExportResult();
        result.setAsync(false);
        result.setTotalCount(totalCount);
        result.setThreshold(threshold);
        result.setMessage("同步导出完成");
        return result;
    }
}
