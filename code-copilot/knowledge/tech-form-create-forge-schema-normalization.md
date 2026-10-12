# form-create ⟷ Forge Schema 双向转换归一化

> 来源：变更 lowcode-business-object-designer、form-first-business-object-designer（及一系列设计器变更）
> 时间：2026-06-27

## 问题描述

低代码表单设计器基于 fcDesigner（form-create）。fcDesigner 生成的布局组件会带临时 ref（`ref_xxx`），若把这些临时 id 当作字段编码/组件 id/布局标题来源，会污染保存后的 Forge Schema，导致字段资产与表单配置不一致、布局标题错乱。

## 解决方案

### 临时 ref 归一化
- `FormDesignerSchema` 识别 `ref_...` 临时引用，布局标题归一化时剥离 fcDesigner 生成的临时 ref。
- 旧 schema 里已保存的布局临时 id 替换为稳定的 `cmp_<componentKey>_<index>`。
- `formCreateToForge` 不把 `ref_...` 当作布局标题、组件 id 或字段编码来源。
- `forgeToFormCreate` 回写布局 rule 时，不给非字段组件写 `name`，避免 fcDesigner 把布局 id 当展示前缀。

### 表单配置优先 + 字典直配
- 保存表单时，字段资产的 `fieldType / dataType / componentType / queryType / dictType / reference*` 以当前表单组件为准同步（“表单配置优先”）。
- 后端保存草稿时再次按 `FormDesignerSchema` 归一化字段，防止绕过前端时字段资产与表单配置不一致。
- `select / radio / checkbox` 组件支持直接绑定系统字典类型（选项来自 `/system/dict/type/list`）；转 Forge Schema 时保留原生组件类型，不再把带 `dictType` 的普通选择器强制改成 `dictSelect`。

## 相关文件

- 前端：`FormDesignerSchema`、`formCreateToForge` / `forgeToFormCreate` 转换器（`forge-admin-ui` 表单设计器）
- 后端：草稿保存归一化逻辑（按 FormDesignerSchema）
