package com.mdframe.forge.plugin.generator.service;

import com.mdframe.forge.plugin.generator.dto.DynamicCrudQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 动态 CRUD 异步导出执行器。
 */
@Service
@RequiredArgsConstructor
public class DynamicCrudAsyncExportWorker {

    private final DynamicCrudExcelService excelService;

    @Async
    public void executeAsync(Long taskId,
                             String configKey,
                             DynamicCrudQuery query,
                             DynamicCrudExcelService.ExportExecutionContext context) {
        excelService.executeAsyncExportTask(taskId, configKey, query, context);
    }
}
