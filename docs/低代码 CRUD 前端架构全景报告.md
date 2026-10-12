低代码 CRUD 前端架构全景报告

       一、整体架构定位

       该系统有两条平行的 CRUD 建设路径，最终共用同一套运行时渲染器：

       路径 A：AI 生成 (useCrudGenerator)
         ↓ 生成 JSON Schema (searchSchema / columns / editSchema / apiConfig)
         ↓ 保存为 crud_config 记录
         → 运行时: AiCrudPage + Template Catalog

       路径 B：低代码可视化搭建 (LowcodeBuilder)
         ↓ Step1: LowcodeModelDesigner → modelSchema (字段 + 数据库配置)
         ↓ Step2: LowcodePageBuilder → pageSchema (zones + fieldRefs + canvas)
         ↓ Step3: LowcodePreviewPane (前端预览)
         ↓ Step4: PublishPanel (建表 + 菜单发布)
         → 运行时: 通过 crudConfigRender(configKey) 同样走 AiCrudPage / TreeCrudTemplate

       两条路径产出同一种数据格式（configKey 对应的配置记录），共用同一个运行时入口 /ai/crud-page/:configKey。

       ---
       二、目录结构与职责

       src/
       ├── views/ai/
       │   ├── crud-config.vue           # CRUD 配置管理列表（卡片视图）
       │   ├── crud-generator.vue        # AI 生成向导（3 面板 + SSE 流）
       │   ├── crud-page.vue             # 运行时页面入口（动态加载模板）
       │   ├── lowcode-apps.vue          # 低代码应用列表
       │   ├── lowcode-builder.vue       # 4 步向导主页面
       │   └── components/
       │       ├── SchemaFieldEditor.vue # JSON Schema 字段表格编辑器
       │       ├── ApiConfigEditor.vue   # API 配置编辑
       │       ├── DictConfigPanel.vue   # 字典配置
       │       ├── EncryptConfigPanel.vue
       │       ├── DesensitizeConfigPanel.vue
       │       ├── TransConfigPanel.vue
       │       └── ...
       ├── components/
       │   ├── ai-form/
       │   │   ├── AiCrudPage.vue        # 核心运行时 CRUD 页面
       │   │   ├── AiSearch.vue          # 查询栏（含 AiForm + 查询/重置按钮）
       │   │   ├── AiForm.vue            # 动态表单渲染引擎
       │   │   ├── AiFormItem.vue        # 单字段渲染（20+ 控件类型）
       │   │   ├── AiTable.vue           # 数据表格（table/card 双模式）
       │   │   ├── AiCrudPageProps.js    # AiCrudPage Props 定义
       │   │   └── schemaHelper.js       # 运行时 Schema 动态更新工具
       │   ├── lowcode-builder/
       │   │   ├── model/
       │   │   │   ├── LowcodeModelDesigner.vue    # 数据模型设计器（主容器）
       │   │   │   ├── ModelFieldTable.vue         # 字段列表表格（可拖排）
       │   │   │   ├── ModelFieldPropertyPanel.vue # 字段属性面板
       │   │   │   ├── model-schema.js             # 字段选项/默认值/工具函数
       │   │   │   └── shared/
       │   │   │       ├── DictTypeSelect.vue
       │   │   │       └── FieldTypeSelect.vue
       │   │   ├── page/
       │   │   │   ├── LowcodePageBuilder.vue          # 页面搭建主容器（2 tab）
       │   │   │   ├── StructuredListPageDesigner.vue  # 列表页配置（查询集/列表/详情）
       │   │   │   ├── FormCreateDesignerAdapter.vue   # 编辑表单设计器适配器
       │   │   │   └── page-schema.js                  # 页面 Schema 核心逻辑
       │   │   ├── preview/
       │   │   │   └── LowcodePreviewPane.vue  # 实时预览（前端模拟）
       │   │   └── publish/
       │   │       └── PublishPanel.vue        # DDL + 发布 + 版本回滚
       │   └── page-templates/
       │       ├── SimpleCrudTemplate.vue      # 标准 CRUD 模板
       │       └── TreeCrudTemplate.vue        # 左树右表模板
       ├── catalog/
       │   └── index.js                # templateKey → 异步 Vue 组件注册表
       ├── api/
       │   ├── lowcode-crud.js         # 低代码后端 API (9 个接口)
       │   └── ...
       ├── composables/
       │   └── useCrudGenerator.js     # AI 生成全部状态管理（SSE + 保存）
       └── router/
           └── index.js                # 路由配置

       ---
       三、路由结构

       手工注册的关键路由（src/router/index.js）：

       ┌──────────────────────────┬─────────────────────────┬───────────────────────────────────────────────────────────┐
       │           路径           │          组件           │                           说明                            │
       ├──────────────────────────┼─────────────────────────┼───────────────────────────────────────────────────────────┤
       │ /ai/crud-page/:configKey │ AiCrudPageDynamic       │ CRUD 运行时入口，支持 configKey/params/query 三种方式注入 │
       ├──────────────────────────┼─────────────────────────┼───────────────────────────────────────────────────────────┤
       │ /ai/lowcode-builder/:id? │ AiLowcodeBuilderDynamic │ 低代码搭建器（id 可选，新建时无 id）                      │
       └──────────────────────────┴─────────────────────────┴───────────────────────────────────────────────────────────┘

       其余路由（含 /ai/crud-config、/ai/crud-generator、/ai/lowcode-apps）通过 vue-router/auto-routes 按文件系统自动生成。

       ---
       四、数据模型层（modelSchema）

       由 /components/lowcode-builder/model/model-schema.js 定义。字段对象结构：

       {
         field: 'contractName',       // camelCase 字段名
         columnName: 'contract_name', // snake_case DB 列名
         label: '合同名称',
         dataType: 'varchar',         // varchar/int/bigint/decimal/tinyint/date/datetime/text/json
         componentType: 'input',      // input/textarea/select/radio/checkbox/switch/date/datetime/number/upload/imageUpload/fileUpload
         length: 255,
         precision: 0,
         required: false,
         searchable: false,           // 是否出现在查询集
         listVisible: true,           // 是否出现在列表列
         formVisible: true,           // 是否出现在编辑/详情表单
         queryType: 'LIKE',           // EQ/LIKE/BETWEEN/GT/LT/GTE/LTE/NE/IN
         dictType: '',                // 绑定字典类型
         sensitiveType: '',           // 脱敏类型
         encryptAlgorithm: '',        // SM4/AES
         width: null,                 // 列宽（px）
         remark: '',
       }

       模型容器：

       {
         businessName: '合同管理',
         tableName: 'contract_manage',
         tableMode: 'STANDARD',       // STANDARD/ARCHIVE
         appType: 'NORMAL',           // NORMAL/TREE
         treeConfig: { keyField, parentField, labelField, childrenField, treeTitle },
         fields: [...]
       }

       ---
       五、页面 Schema 层（pageSchema）

       由 /components/lowcode-builder/page/page-schema.js 定义。完整结构：

       {
         layoutType: 'simple-crud',  // simple-crud / tree-crud
         zones: [
           {
             zoneKey: 'search',
             componentKey: 'search-form',
             enabled: true,
             fieldRefs: ['field1', 'field2'],  // 明确排序的字段引用
             props: {
               canvas: {                        // 画布布局（内部计算，不暴露给设计器 UI）
                 width: 1040,
                 height: 300,
                 snap: 8,
                 items: [{
                   id: 'search_query_set',
                   componentKey: 'query-set',
                   fieldRefs: [...],
                   x: 32, y: 36, w: 700, h: 132, zIndex: 1,
                   style: { labelWidth: 86, radius: 6, fill: '#fff', stroke: '#cbd5e1' },
                   props: { fieldRefs: [...] }
                 }]
               }
             }
           },
           { zoneKey: 'table', ... props: { showImport, showExport, enableCustomQuery, treeConfig, canvas } },
           { zoneKey: 'edit', ... props: { formCreateRule: [...], canvas } },  // @form-create 规则
           { zoneKey: 'detail', enabled: false, ... }
         ]
       }

       字段引用解析优先级（resolveFields / resolveFieldRefsFromFormCreateRules）：
       1. zone.props.formCreateRule（仅 edit zone，@form-create Designer 输出）
       2. zone.fieldRefs（StructuredListPageDesigner 维护）
       3. zone.props.canvas.items[*].fieldRefs（画布推断）
       4. modelSchema.fields 按 flag 过滤（最终兜底）

       ---
       六、组件层次与数据流

       低代码搭建器

       lowcode-builder.vue (reactive draft: { modelSchema, pageSchema })
         ├── LowcodeModelDesigner (v-model: draft.modelSchema)
         │     ├── ModelFieldTable (vuedraggable 拖排 + inline 编辑)
         │     └── ModelFieldPropertyPanel (字段详细属性，右侧面板)
         ├── LowcodePageBuilder (v-model: draft.pageSchema, :model-schema)
         │     ├── StructuredListPageDesigner (列表页 tab)
         │     │     ├── ComponentPreviewControl (inline defineComponent, disabled 控件预览)
         │     │     └── FieldOrderEditor (inline defineComponent, vuedraggable 字段排序)
         │     └── FormCreateDesignerAdapter (编辑表单 tab)
         │           └── FcDesigner (@form-create/designer, ElementUI)
         ├── LowcodePreviewPane (:draft 只读)
         └── PublishPanel (:app-id, :draft)

       数据流：draft.modelSchema 变化时，syncPageSchemaWithModel 自动清理孤立 fieldRef、补全缺失 zone，保持 pageSchema 与 modelSchema 一致。

       运行时渲染

       crud-page.vue (resolves configKey → crudConfig)
         └── template catalog[layoutType].component (lazy import)
               ├── SimpleCrudTemplate.vue
               │     └── AiCrudPage (searchSchema, columns, editSchema, apiConfig)
               │           ├── AiSearch → AiForm → AiFormItem × N
               │           ├── AiTable (NDataTable / card 模式)
               │           └── AiForm (编辑表单，在 NModal/NDrawer 内)
               └── TreeCrudTemplate.vue
                     ├── NTree (左侧树，注入 parentId 到 publicParams)
                     └── AiCrudPage (右侧，同上)

       ---
       七、AI 生成流程（useCrudGenerator）

       SSE 流式 7 阶段生成：

       analyzing → generating-meta → generating-search → generating-columns
                → generating-edit → generating-api → generating-sql

       - [STAGE:xxx] 标记在流中切换阶段
       - thinking-phase 标记分离推理内容与输出内容
       - parseRawContentFallback() 在流中 JSON 不完整时做容错解析
       - 生成完成后由 saveConfig() 写入后端数据库
       - 支持 initWithConfigKey(ck) 从现有配置初始化，复用生成结果

       ---
       八、API 接口层

       低代码构建 API（/src/api/lowcode-crud.js）：

       ┌──────────────────────┬──────┬────────────────────────────────────────────────────────────────────────────┐
       │         函数         │ 方法 │                                    路径                                    │
       ├──────────────────────┼──────┼────────────────────────────────────────────────────────────────────────────┤
       │ lowcodeAppPage       │ GET  │ /ai/lowcode/app/page                                                       │
       ├──────────────────────┼──────┼────────────────────────────────────────────────────────────────────────────┤
       │ lowcodeAppDetail     │ GET  │ /ai/lowcode/app/{id}                                                       │
       ├──────────────────────┼──────┼────────────────────────────────────────────────────────────────────────────┤
       │ lowcodeSaveDraft     │ POST │ /ai/lowcode/app/draft                                                      │
       ├──────────────────────┼──────┼────────────────────────────────────────────────────────────────────────────┤
       │ lowcodePreview       │ POST │ /ai/lowcode/app/{id}/preview                                               │
       ├──────────────────────┼──────┼────────────────────────────────────────────────────────────────────────────┤
       │ lowcodePublish       │ POST │ /ai/lowcode/app/{id}/publish（deployMode: SKIP_DDL / ONLINE_CREATE_TABLE） │
       ├──────────────────────┼──────┼────────────────────────────────────────────────────────────────────────────┤
       │ lowcodeVersions      │ GET  │ /ai/lowcode/app/{id}/versions                                              │
       ├──────────────────────┼──────┼────────────────────────────────────────────────────────────────────────────┤
       │ lowcodeRollback      │ POST │ /ai/lowcode/app/{id}/rollback/{versionId}                                  │
       ├──────────────────────┼──────┼────────────────────────────────────────────────────────────────────────────┤
       │ lowcodeValidateModel │ POST │ /ai/lowcode/model/validate                                                 │
       ├──────────────────────┼──────┼────────────────────────────────────────────────────────────────────────────┤
       │ lowcodeDdlPreview    │ POST │ /ai/lowcode/model/ddl/preview                                              │
       └──────────────────────┴──────┴────────────────────────────────────────────────────────────────────────────┘

       ---
       九、状态管理方式

       无 Vuex/Pinia，完全基于组合式函数：

       - lowcode-builder.vue：reactive(draft) 单一状态源，通过 watch 进行三路同步（appName↔businessName↔menuName，appType↔layoutType，modelSchema→pageSchema）
       - LowcodePageBuilder.vue：ref(localSchema) + watch(props.modelValue) + watch(props.modelSchema) 实现双向外部同步
       - useCrudGenerator.js：自成体系的 composable，内聚所有 AI 生成状态
       - AiCrudPage.vue：内部 ref 维护分页/选中/loading 等运行时状态

       ---
       十、关键依赖

       ┌─────────────────────────┬───────────────────────────────────────────────┐
       │          依赖           │                     用途                      │
       ├─────────────────────────┼───────────────────────────────────────────────┤
       │ Naive UI                │ 主 UI 库（全体 NaiveUI 组件）                 │
       ├─────────────────────────┼───────────────────────────────────────────────┤
       │ @form-create/designer   │ 编辑表单拖拽设计器（ElementUI 生态）          │
       ├─────────────────────────┼───────────────────────────────────────────────┤
       │ @form-create/element-ui │ 配套 form-create 运行时                       │
       ├─────────────────────────┼───────────────────────────────────────────────┤
       │ element-plus            │ 为 FcDesigner 所需，动态挂载到 app 实例       │
       ├─────────────────────────┼───────────────────────────────────────────────┤
       │ vuedraggable            │ 字段拖排（ModelFieldTable、FieldOrderEditor） │
       ├─────────────────────────┼───────────────────────────────────────────────┤
       │ vue-router/auto-routes  │ 文件系统路由自动生成                          │
       ├─────────────────────────┼───────────────────────────────────────────────┤
       │ @vicons/ionicons5       │ 图标库                                        │
       └─────────────────────────┴───────────────────────────────────────────────┘

       混用 UI 库的隔离策略：FormCreateDesignerAdapter 通过检查 app.__lowcodeFormCreateInstalled 标记，仅在首次使用时调用 app.use(ElementPlus) 和 app.use(formCreate)，避免重复注册。

       ---
       十一、当前能力 vs 完整低代码平台的差距

       已具备

       - 数据模型设计（字段 CRUD、类型映射、安全属性）
       - 页面 Schema 与模型的自动同步机制
       - 查询集字段排序（含 vuedraggable 拖排）
       - 列表字段排序与导入/导出/自定义查询开关
       - 详情页字段排序
       - 编辑表单拖拽设计（借助 @form-create/designer）
       - 树形结构（left-tree right-table）内置支持
       - canvas 位置数据结构（x/y/w/h/zIndex）已在 page-schema 完整建模
       - DDL 预览、在线建表、版本发布、版本回滚
       - AI 辅助生成（7 阶段 SSE 流，含推理展示）
       - 运行时 20+ 控件类型、字典集成、脱敏/加密

       缺口与待建

       1. 自由画布设计器未实现：page-schema.js 已建模了完整的 canvas（items 含 x/y/w/h/zIndex），但前端没有对应的可视化拖拽画布 UI，StructuredListPageDesigner 仅做字段排序，不暴露画布。这是当前最大的功能空白。
       2. 列表操作列缺乏配置：操作列（编辑/删除/查看详情/自定义按钮）在当前设计器中固定写死，没有可配置的操作按钮 builder。
       3. 详情页缺乏启用后的配置入口：detailZone.enabled 默认为 false，StructuredListPageDesigner 提供了 enable 开关和字段排序，但没有更细粒度的详情布局控制。
       4. 查询条件类型不可设计器内调整：queryType（EQ/LIKE/BETWEEN 等）只在模型层设置，页面设计器无法覆盖。
       5. 条件联动/级联逻辑无可视化配置：schemaHelper.js 提供了 createCascadeUpdater 等运行时工具，但构建阶段没有可视化的联动规则编辑器。
       6. 模板体系单薄：catalog/index.js 目前只注册了 simple-crud 和 tree-crud 两种模板，缺少看板、统计图表、日历、表单流程等扩展模板。
       7. 发布后的菜单权限配置：PublishPanel 仅处理 DDL 和菜单创建，没有集成权限/角色绑定流程。
       8. 多步骤表单 / 标签页表单：FormCreateDesignerAdapter 支持任意 @form-create 布局，但 lowcode builder 入口没有针对多 step 表单的向导辅助。

业务模型设计布局：
┌─────────────────────────────────────────────────────────────────────┐
│  顶部导航: Logo | 面包屑 | 搜索 | 预览/保存/发布按钮                │
├──────────┬──────────────────────────────────────────────────────────┤
│          │  ┌────────────┬───────────────────┬──────────────────┐   │
│  左侧菜单 │  │ 模型基础    │   字段设计表格-放在模型基础信息下面   │  字段属性面板     │   │
│  (树形)   │  │  信息       │   (主工作区)      │  (右侧抽屉)      │   │
│  ~240px   │  │            │                   │                  │   │
│          │  ├────────────┴───────────────────┴──────────────────┤   │
│          │  │           ER 关系图 (可视化)                         │   │
│          │  ├─────────────────────────────────────────────────────┤   │
│          │  │  底部工具栏: 添加字段 | 导入 | 生成 | 保存 | 发布    │   │
└──────────┴──────────────────────────────────────────────────────────┘
