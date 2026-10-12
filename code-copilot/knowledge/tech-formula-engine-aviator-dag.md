# 低代码字段公式引擎（Aviator + DAG）

> 来源：变更 formula-capability-extension、formula-next-capabilities
> 时间：2026-06-27

## 问题描述

低代码业务对象缺统一的字段级公式体系（金额计算、主从表汇总、条件赋值、跨对象引用），需要可计算、可校验、可观测，且不侵入 CRUD 业务代码。

## 解决方案

### 表达式引擎
- 选 **Aviator**（弃 MVEL/自研），用 `AviatorAdapter` 封装编译/执行，错误统一转 `FormulaExecutionException`。
- 金额结果统一用 `long`（单位：分）。

### 配置模型
- 公式配置以 JSON 落在字段定义上：`GenTableColumn.formula_config`、`LowcodeFieldSchema.formulaConfig`。
- 统一结构 `{type, expression, mode, dependsOn, maxDepth, aggregate, lookup, crossObject, rule}`，向后兼容 V1。
- 计算模式：`STORED`（保存时算并持久化）、`VIRTUAL`（查询时实时算不存储）。抽 `AbstractFormulaRuntime` 消除两者重复。

### 发布期校验（DAG）
- 拓扑排序（Kahn）做循环依赖检测 + 嵌套深度校验（max 3）+ 依赖字段存在性校验（`FormulaDependencyAnalyzer` / `FormulaPublishValidator`）。
- 注意邻接表方向：`B→A` 表示 B 先算；`dependsOn` 方向要与之一致。

### 聚合公式
- SUM/COUNT/AVG/MAX/MIN 通过 `AiBusinessObjectRelation` 映射取从表数据（`DbAggregateDataProvider`）。
- **不要假设 `relationCode = targetObjectCode`**。
- 缺关系/缺字段必须抛 `AggregateDataException`，禁止静默返回空导致脏数据。

### 二期能力
- 可观测性：执行日志表 `ai_formula_execution_log`（含 traceId、inputSnapshot 脱敏）、调试器逐步 trace、依赖图 nodes/edges。
- 跨对象公式只支持**一跳路径**；STORED 跨对象默认走异步队列重算（幂等键 `objectCode:recordId:fieldCode:dependencyTrace`），VIRTUAL 批量预取避免 N+1。

## 相关文件

- `forge-plugin-generator`：`FormulaDependencyAnalyzer.java`、`FormulaExecutionEngine.java`、`AviatorAdapter.java`、`AggregateEngine.java`、`DbAggregateDataProvider.java`、`FormulaPublishValidator.java`、`StoredFormulaRuntime.java` / `VirtualFormulaRuntime.java`、`CrossObjectRecomputeTaskService.java`
