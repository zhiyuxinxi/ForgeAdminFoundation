# PC 前端开发文档导航

首先阅读[模块 README](../README.md)和[项目上手指南](../../docs/getting-started.md)，再按实际开发内容选择专题。

| 主题 | 文档 | 适用内容 |
| --- | --- | --- |
| 页面与公共组件设计 | [DESIGN](../DESIGN.md) | 布局、工作区、表格、详情与交互约束 |
| 开发与组件规范 | [Forge Admin 前端开发与组件规范](../Forge%20Admin前端开发与组件规范.md) | 命名、目录、接口、公共组件用法 |
| 工具函数 | [UTILITIES_GUIDE](UTILITIES_GUIDE.md) | 前端通用工具和辅助方法 |
| 主题 | [THEME_CONFIG](THEME_CONFIG.md) | 主题与样式配置 |
| 字典组件 | [DICT_USAGE_GUIDE](../src/components/DICT_USAGE_GUIDE.md) | 当前组件使用方法 |
| 字典配置背景 | [DICT_MANAGEMENT_SETUP](../DICT_MANAGEMENT_SETUP.md)、[DICT_MIGRATION](../DICT_MIGRATION.md) | 历史接入与迁移背景；执行前对照当前数据库和源码 |
| 文件访问 | [FILE_URL_GUIDE](../FILE_URL_GUIDE.md) | 附件地址及访问处理 |
| 标签页 | [TAB_UTILS_GUIDE](../src/utils/TAB_UTILS_GUIDE.md) | 标签页工具接口 |
| 流程设计器 | [BPMN 组件](../src/components/bpmn/README.md) | 流程设计与组件结构 |
| 表单设计器 | [Forge Form Designer](../src/views/app-center/components/designer/forge-form-designer/README.md) | 表单设计器集成 |
| 低代码整体结构 | [低代码 CRUD 前端架构全景报告](../../docs/低代码%20CRUD%20前端架构全景报告.md) | 前端设计和运行组件关系 |

现有专题文档保留其模块设计背景，不能将其中的示例路径、历史迁移或操作步骤直接当作所有版本通用的部署流程。本轮重点更新公开入门与索引，未对全部技术专题逐段进行运行验证。

修改公共组件时，复用当前组件和国际化机制，并按实际影响进行验证。协作方式见[贡献指南](../../CONTRIBUTING.md)。
