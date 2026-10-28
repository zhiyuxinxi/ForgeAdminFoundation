# Forge Admin 设计与公共组件约束

本文件定义 `forge-admin-ui` 的界面设计原则、公共页面组件和高频交互约束。它服务于长期维护：新页面、新组件和改版都应先遵循这里的视觉与交互规则，再补充具体业务逻辑。

公开版本协作说明见根目录 [CONTRIBUTING.md](../CONTRIBUTING.md)，阅读入口见 [前端文档导航](docs/README.md)；目录、命名、接口、字典和编码细则见 [Forge Admin前端开发与组件规范.md](./Forge%20Admin%E5%89%8D%E7%AB%AF%E5%BC%80%E5%8F%91%E4%B8%8E%E7%BB%84%E4%BB%B6%E8%A7%84%E8%8C%83.md)。

## 1. 设计目标

Forge Admin 是供日常反复使用的企业管理控制台，不是营销网站。

- 信息密度保持中高：内容紧凑、可扫读，但不拥挤。
- 页面首先明确“当前管理什么对象、已选择什么对象、操作作用于哪里”。
- 用边框、间距、字重和主题色表达层级；不靠渐变、大阴影或装饰卡片堆砌效果。
- 动效只用于确认操作、悬停和加载状态，不能妨碍阅读或降低列表性能。

## 2. 颜色、文字与表面

| 角色     | 变量或参考色             | 用途                             |
| -------- | ------------------------ | -------------------------------- |
| 页面底色 | `--gray-100` / `#f6f8fb` | Layout 内容背景                  |
| 面板底色 | `--bg-primary` / `#fff`  | 工作台、表格和表单               |
| 主文字   | `--text-primary`         | 标题、核心值和当前对象           |
| 次要文字 | `--text-tertiary`        | 编码、账号、说明和时间           |
| 边框     | `--border-light`         | 面板分隔、工具栏和控件边界       |
| 主题色   | `--primary-color`        | 主操作、可点击实体、选中态和焦点 |
| 危险色   | `--error-color`          | 删除和不可逆操作                 |

- 正常交互只使用一个主题强调色；不要再引入独立的绿色、紫色或渐变体系。
- 系统页标题通常为 14–17px，表格正文 13px，辅助信息 11–12px。
- 管理页禁止使用营销式大标题、英文装饰标签、虚假指标卡和重阴影。
- 面板圆角通常为 6px，控件间距通常为 6–8px；页面根节点不再额外叠加大留白。

## 3. 页面边界与工作台

### 3.1 页面级边界

`/system/**` 已由 `SystemPageLayout` 负责满高和页面滚动边界。各 Layout 统一提供 8px 页面外边距。

- 页面和 `AiCrudPage` 内部不得自行增加页面级 `padding`、外层白卡或大圆角。
- 业务组件只处理自身内容的留白；页面边界必须由 Layout 统一维护。
- 所有可滚动的 Flex 或 Grid 子区域必须同时设置 `min-height: 0`，再由真正承载内容的子元素声明 `overflow: auto`。

### 3.2 左侧对象区 + 右侧工作区

当树、角色、组织、分类等对象决定右侧内容时，必须使用 `MasterDetailWorkspace`：

```vue
<MasterDetailWorkspace
  :collapsed="collapsed"
  :aside-width="220"
  :collapsed-aside-width="72"
>
  <template #aside>
    <OrgTreePanel />
  </template>

  <AiCrudPage ... />
</MasterDetailWorkspace>
```

- 默认使用连体工作台：一个外框、内部一条细分隔线，避免左树和右表像两张无关卡片。
- `aside` 放树、对象列表或筛选导航；默认插槽放表格、详情或页签工作区。
- 仅当两块区域确实无业务关联时，才设置 `:attached="false"`。
- 窄屏由组件自动上下堆叠；页面不要复制一套响应式列宽逻辑。

### 3.3 三栏工作台

菜单、资源等“树 + 列表 + 详情”页面遵循以下滚动边界：

```text
工作台（固定高度）
├─ 左侧树：自身滚动
├─ 中间列表：工具栏固定，列表区域自身滚动
└─ 右侧详情：标题与操作固定，详情正文自身滚动
```

- 禁止将内容放进 `overflow: hidden` 的面板后却没有内部滚动容器。
- 列表和详情的滚动容器应使用 `flex: 1`、`min-height: 0`、`overflow: auto` 和 `scrollbar-gutter: stable`。
- 不依赖一次性的 JavaScript `max-height` 计算维持滚动；布局变化后应仍能正确滚动。

## 4. 公共组件约束

### 4.1 `SystemPageLayout`

- 仅负责系统页面的满高与边界，不承担业务区 `padding`。
- 已在应用根节点按 `/system/**` 自动包裹；页面内不要重复套用。

### 4.2 `MasterDetailWorkspace`

- Props：`asideWidth`、`collapsedAsideWidth`、`mainWidth`、`collapsed`、`attached`。
- 插槽：`aside` 仅放主对象区域，默认插槽放从属工作区。
- 组件必须同时支持亮色、暗色和窄屏上下布局。
- 左右内容区不得向外撑破工作台；内部滚动由各自内容区负责。

### 4.3 `SystemTableCell`

`SystemTableCell` 是系统 CRUD 列表的统一单元格能力，不是通用装饰容器。

- 实体模式：传入 `title`、`subtitle`、`interactive`；用户、租户、客户端等可传 `avatar` 显示首字标识。
- 实体主标题是可识别的业务名称；辅助标题只能放账号、编码、键名或稳定标识，必须换行显示。
- 关联模式：传入 `values`，仅显示主值和可点击的 `+N`；点击后用浮层查看全部值。
- `interactive` 必须提供真实的 `activate` 动作，例如详情、编辑或进入下级管理，禁止制造无动作的蓝色链接。
- 用户优先显示真实姓名或昵称，登录名显示为 `@username`；当两者相同时不要重复显示。

普通代码列与低代码列表可通过既有列配置声明 `renderType: 'entity' | 'relations'`，统一复用 `SystemTableCell`。`titleField` 指定主值来源（缺省使用当前列），`subtitleField` 是实体辅助标识来源，`avatar` 仅控制可选首字标识；未配置辅助字段时不显示副标题。列表设计器在现有 `fieldSettings` 中维护这些配置，保存后由同一运行配置消费；它们只改变显示，不改变字段身份、权限或业务数据。普通代码实体列的 `clickAction: 'detail'` 仍调用 `AiCrudPage` 的详情权限入口；低代码实体沿已有 `clickAction: 'navigate'` 与跳转参数执行真实导航，无导航消费者时保持普通文本。

Code and low-code list columns share the `entity` / `relations` presentation in `SystemTableCell`. `titleField`, `subtitleField` and `avatar` are display metadata saved in the existing `fieldSettings`; they never change data identity or authorization. Explicit action-column width/count remain authoritative; otherwise show two common actions and size the column for those actions plus the overflow menu.

### 4.4 `AiCrudPage` 与 `AiTable`

- `AiCrudPage` 只负责搜索、工具栏、表格和表单；不负责页面级外框、间距和背景。
- 默认表格密度为 `medium`。日志、审计、纯对比列表才可使用 `small`。
- 列表/卡片切换通过 `showRenderModeSwitch` 控制；不适合卡片浏览的树表、日志页应显式关闭。
- 详情页使用明确的可读字段配置；普通详情通过 `detailPanels` 的 `descriptions` 面板展示，并设置 `hideDefaultDetailContent: true`。完整 `editSchema` 仍负责新增/编辑，禁用编辑控件和无 Schema 的只读兜底不能替代正式详情。

### 4.5 表单、详情与弹层模式

先按任务选择现成骨架，再配置字段与业务动作。同类任务使用同一配置，不为新增、编辑、详情分别编写另一套表单或 `NModal` / `NDrawer` 外壳。

| 任务 | 复用入口 | 配置与边界 |
| --- | --- | --- |
| 普通新增/编辑 | `AiCrudPage` + `editSchema` | 默认 `modalType="modal"`；字段扩展使用 `form-${slotName}`，额外动作使用 `formActions`，数据变化沿已有 hooks。 |
| 轻量单一职责编辑 | `AiCrudPage modalType="drawer"` | 默认右侧；复用同一 schema、提交链和 footer，不另写抽屉业务表单。 |
| 结构化配置、长表单 | `formOpenMode="flat"` 或 `"tabWorkspace"` | 同页工作区；正文独立滚动、操作区保持可达。多页签默认最多8个、复用同记录、显示未保存标记、保存后不关闭，关闭未保存页签会确认。 |
| 独立新增填报 | `AiCrudPage formOnly` | 只提供新增填报及成功结果页，不是通用编辑或详情开关；内容最大960px。 |
| 只读详情 | `AiCrudPage` 的详情动作 + `detailPanels` | 普通详情使用 `descriptions` 面板及 `hideDefaultDetailContent: true`，按业务分组展示可读值；子表需要显式详情面板，合法动作沿 `detailActions`。编辑用抽屉时详情仍默认弹窗；显式 flat/tabWorkspace 则留在同页工作区。 |
| 非CRUD的小型任务弹窗 | `components/ai-modal/index.vue` | 默认插槽放内容；`header-extra`、`footer`、`footer-left` 只扩展必要部分；`open(options)` 配置行为。`onOk` 必须完成真实验证和保存，返回false阻止关闭。 |
| 关联值只读浮层 | `SystemTableCell :values` | 点击 `+N` 查看全部关联值；轻量浮层不承载完整编辑表单。 |

可读详情沿用现有配置，不另写弹窗。`dataSource.type: 'row'` 使用详情动作已经加载的记录；多个面板按现有页签分组。字典字段使用 `render: { type: 'dictTag', dictType: '实际字典类型' }`；日期、关系名称等传入可读值，不能展示内部 ID 或原始 JSON。`hideDefaultDetailContent` 同时隐藏默认只读表单和默认子表，因此需要的子表也应在 `detailPanels` 中声明。它仅影响详情，不隐藏新增/编辑表单。

```js
const detailView = {
  hideDefaultDetailContent: true,
  detailPanels: [{
    key: 'basic',
    title: '基本信息',
    type: 'descriptions',
    dataSource: { type: 'row' },
    descriptions: { columns: 2, fields: [{ field: 'name', label: '名称' }] },
  }],
}
```

Readable details use the existing `detailPanels` descriptions renderer with `hideDefaultDetailContent: true`; editing keeps its complete `editSchema`. Use readable values and dictionary tags, explicitly include required child panels, and preserve the shared modal or inline workspace. The option affects details only, including detail views without workflow tabs.

**同类表单布局。** `AiCrudPage` 默认宽800px、一列、label左置且右对齐、label宽auto、控件medium、列/行间距16/8。字段少的表单保留紧凑布局；相关字段较多时显式配置 `editGridCols`、`modalWidth`，使用 schema 的分组与 `span`。用户管理的900px、两列、跨两列分组标题是一种已实现示例，不是所有表单必须使用的尺寸。详情默认宽 `min(1080px, 92vw)`。不要为了看起来不同改同类表单的 label、间距、按钮顺序或footer位置。

**正文与操作区。** 复用原版CRUD弹层及 `NDrawerContent` 的已有滚动布局，不在业务页再套一层滚动容器。通用 `ai-modal` 在自身卡片上限制视口宽高，只有直接正文滚动，标题与footer不随正文滚走；选择器等弹出内容继续沿Naive的既有浮层机制。关联浮层限制最大高度，长列表在内部滚动。不要把局部样式扩大为所有后代 `.n-card` / `.n-spin-content` 的高度规则。真实运行页已具备滚动和footer行为时应复用证据，不以组件局部源码未见约束为理由重复改造。

**表单与按钮归属。** CRUD容器已经传 `AiForm :show-actions="false"` 并拥有取消/确定与提交loading。确需在 `ai-modal` 或其他既有容器中直接用 `AiForm` 时，显式传入与同类CRUD一致的 `labelPlacement/labelWidth/labelAlign/size/gridCols/xGap/yGap`，并设置 `showActions=false`，由外壳提供唯一footer；裸 `AiForm` 自身默认间距12/0和居中提交/重置不能直接当作CRUD默认样式。校验失败沿 `AiForm.validate()` 的首错展开、滚动、聚焦行为，不另外堆一套重复校验提示。

**字段语义。** 日期、时间、数字、字典、关联对象等按真实字段语义选择已有公共控件，并在API边界完成所需值格式转换；产品说明没有指定控件不等于可以全部使用文本框。沿用真实字典和关联数据源，不凭页面文案硬编码选项，也不改变业务原值或接口含义。

**关闭与提交。** CRUD弹窗/抽屉默认不能点击遮罩关闭，普通取消关闭并清理表单；未保存确认只在既有内联页签实现，不宣称所有容器都保存草稿。通用 `ai-modal` 自动处理异步确认/取消期间的busy：阻止重复动作、替换内容、关闭、Esc及遮罩关闭；确认失败或返回false保留内容，finally释放loading，成功才关闭。额外footer按钮也必须走既有 `handleOk/handleCancel`，不要绕过它们自行发送同一请求。

Form and overlay standards use existing `AiCrudPage` modes, its schema and action hooks. Keep the shared layout for equivalent tasks; the user page's two-column 900px form is an example, not a universal size. Preserve the working CRUD and drawer scrolling layout. The generic `ai-modal` bounds its own body to the viewport, serializes asynchronous actions, retains input on failure or veto, and blocks dismissal while pending. Standalone `AiForm` consumers must inherit the matching layout and let one container own the footer. Use existing semantic controls for dates, times, numbers, dictionaries and relations, with format conversion at API boundaries. Dirty-form confirmation is only guaranteed by the existing inline workspace tabs.

## 5. CRUD 列规范

每个单元格只表达一种信息层级，避免用多个小色块抢注意力。

| 列类型                 | 规则                                     |
| ---------------------- | ---------------------------------------- |
| 实体                   | 主标题 + 换行辅助标识；可点击时整块点击  |
| 关联关系               | 主值 + 可展开 `+N`                       |
| 状态、类型、布尔枚举   | 使用 `DictTag` 或单个语义 Tag            |
| 普通属性               | 纯文字，长文本可省略并提供完整提示       |
| 编码、键名、账号       | 优先作为实体辅助标识，不单独挤占主表列   |
| ID、性别、低频技术字段 | 放入详情或列设置，不占高频列表首屏       |
| 操作                   | 最多两个常用文字操作直显，其余收入“更多” |

- 不要在同一个单元格同时叠加图标、多个 Tag、徽章、描边和多种颜色。
- 名称与编码成对出现时优先合并为实体列，例如“字典名称 + 字典类型”“参数名称 + 参数键名”。
- 字典类型的主体列点击进入字典数据；配置、资源、用户等主体列点击进入真实详情或编辑，不要只做视觉链接。
- 多个 Tag 同时出现会破坏扫描节奏；只有状态、类型等确有语义的字段才保留 Tag。

## 6. 工具栏、操作与对象归属

- 操作必须贴近其作用对象：组织操作在组织树或组织面板，用户操作在用户行，页面级操作才放顶栏。
- 一个区域只保留一个主按钮；低频动作使用三点菜单。
- 图标按钮必须提供中文 `title` 或 `aria-label`。
- 不要把单个常用操作藏进“更多”；只有两个以上低频操作才折叠。
- 批量操作只在可批量选择的列表中显示，选中数量必须清晰可见。
- `AiCrudPage` 批删结果由公共组件处理，页面不自行复制刷新或选择修正逻辑。批量接口可返回 `data: { successCount, failureCount, failedIds }`；计数须为非负整数、总数等于本次请求数量，`failedIds` 是本次ID的无重复字符串子集且数量等于 `failureCount`。支持正常 `code: 200` 响应，以及 HTTP 200 / 业务 `code: 500` 经错误链保留的同形结果。只有明确成功才刷新当前查询，保留失败项选择；未知或不完整结果按真实错误处理，不清空选择。旧的全成功响应保持兼容。
- `/:id` 删除仍按顺序请求并由公共组件汇总实际结果；失败项保持选择，鉴权或网络故障停止后的未执行项也保留，不能视为成功。批删请求只在当前调用使用 `needTip: false`，由公共函数统一反馈；未知错误仍通过现有错误展示保留原消息与详情，不全局关闭错误提示。

Batch deletion is owned by `AiCrudPage`. The supported result is `data: { successCount, failureCount, failedIds }`, with non-negative integer counts matching the requested set and unique string failed IDs from that set. Both successful responses and HTTP 200 / business code 500 errors can carry this result. Refresh the current query only for confirmed successes and retain failed or unattempted selections. Unknown or malformed results remain errors; existing all-success responses and row deletion remain compatible. Single-ID requests are sequential and keep their actual outcomes. Error-tip suppression applies only to the batch request; the shared handler retains ordinary error messages and details.


## 7. 树、菜单与性能

- 树节点保持紧凑、稳定；选中态使用轻主题底色和细边框，禁止粗左边条和重阴影。
- 左树首次进入默认展开当前层级，不应无条件全量展开深层树。
- 树 + 列表页面的中间列表默认展示当前节点的直属子项；筛选时才递归检索当前范围。
- 不要将同一批树数据同时全展开渲染到树和扁平渲染到列表。
- 数据量增长后，树应改为按 `parentId` 懒加载，列表应使用服务端分页；前端假分页不能减少接口传输、构树和 DOM 创建成本。
- Loading 应局部显示在树、列表或详情区域，禁止普通刷新时遮住整张工作台。

## 8. 加载、空态、错误与动效

- 普通 CRUD 请求使用区域级 Loading；导入、导出、上传和下载等真正阻塞的操作才使用全局 Loading。
- 空态必须贴近对应区域，并区分“暂无数据”和“没有匹配结果”。
- 错误提示要说明失败的具体动作，例如“加载资源树失败”。
- 动效只允许使用 `color`、`background`、`border`、`opacity`、`transform`，时长 120–180ms。
- 禁止动画 `width`、`height`、`top`、`left`；禁止系统 CRUD 页持续动画。

## 9. 系统页面约束

### `/system/user`、`/system/post`、`/system/tenant`

- 使用统一紧凑 CRUD 工作面。
- 用户列采用首字标识、真实姓名/昵称和 `@username` 两行信息。
- 组织、岗位、租户使用主值 + `+N`，不能平铺多个标签。

### `/system/org`

- 组织树决定右侧用户、岗位工作区；新增用户放在用户列表工具栏，不额外制造上下文大标题。

### `/system/role`

- 左侧角色对象区优先展示角色名称、键、状态和当前标记；右侧管理当前角色的用户或权限。
- 角色本身的编辑、授权和删除操作属于角色对象，不放到成员列表工具栏。

### `/system/menu`

- 客户端切换保持紧凑，不增加占首屏的大说明或指标卡。
- 左树、中间资源列表、右侧详情必须各自可滚动。
- 首屏只渲染顶级资源和当前层级，避免全树展开与全量扁平列表同时创建。

## 10. 禁止项与交付检查

禁止：

- 紫蓝霓虹渐变、按钮发光、重阴影和超大圆角。
- 管理页中的营销式 Hero、大段功能文案和假指标卡。
- 没有真实行为的可点击蓝色文本。
- 外层隐藏溢出、内部没有滚动区的工作台。
- 为了“丰富”而给每一列增加图标、Tag 或色块。
- 使用 Emoji 作为系统图标，或使用虚构数据伪造真实业务状态。

完成系统页前确认：

1. 主对象和当前选择是否一眼可见。
2. 操作是否放在正确对象附近。
3. 表格列是否符合实体、关联、状态、普通属性的分层规则。
4. 树、列表、详情是否都有正确的独立滚动边界。
5. 亮色、暗色和窄屏是否不会溢出。
6. 已执行 ESLint、`git diff --check` 和必要的前端构建。
