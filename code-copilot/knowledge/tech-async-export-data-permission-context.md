# 通用异步导出与数据权限上下文保持

> 来源：变更 common-crud-async-export
> 时间：2026-06-27

## 问题描述

通用 CRUD 导出一次性查全量、直接写 HTTP 响应，大数据量超时且 OOM。改为异步任务后，后台线程脱离了 HTTP 会话和登录态，必须保证导出不绕过租户/数据权限边界，否则会退化成全量查询导致越权。

## 解决方案

- **智能阈值分流**：总数 `<= sys.export.async.threshold`（默认 5000）走同步文件流；超过则建持久任务返回 `async=true / taskId`。异步执行按 `sys.export.batch.size`（默认 1000）分页 + EasyExcel writer 分批写入，禁止把完整数据集或文件放内存。
- **数据权限上下文捕获/恢复**（核心）：提交任务时捕获提交人的租户、用户、数据权限上下文（`DynamicDataScopeService`），后台 worker 执行时恢复。否则后台线程丢登录态后，`FOLLOW_SYSTEM` 等数据权限会退化为全量查询。
- **任务持久化与隔离**：任务表 `ai_crud_export_task` 只存 `file_id` 等元数据，文件先上传文件中心，前端复用 `/api/file/download/{fileId}` 下载。任务查询按 `tenant_id + 当前用户 + configKey` 三重隔离。
- **文件中心流式上传**：`FileManager` 需补充“已知大小的 InputStream 流式上传”入口（原只暴露 `MultipartFile`）。
- **资源与失败处理**：失败任务进 `FAILED`；临时文件在 finally 删除；writer / 流显式关闭防句柄泄漏。

## 相关文件

- `forge-plugin-generator`：`DynamicCrudAsyncExportWorker.java`、`DynamicCrudExcelService.java`、`DynamicCrudService.java`、`DynamicDataScopeService.java`、`AiCrudExportTaskMapper.xml`
- `forge-starter-file`：`FileManager.java`、`FileStorage.java`
- 迁移脚本：`forge/db/migration/V1.0.23__add_common_crud_async_export.sql`
