# 参与 ForgeAdminFoundation

感谢你帮助改进这个基于 Forge Admin 的下游发行版。开发前请阅读[项目说明](README.md)、[上游与改造说明](docs/upstream-and-changes.md)以及[当前公开版本状态](docs/release-status.md)。

## 反馈问题

本发行版问题请提交到 [ForgeAdminFoundation Issues](https://github.com/zhiyuxinxi/ForgeAdminFoundation/issues)。请提供：

- 公开提交号或标签，以及发生问题的模块。
- 操作系统、Java/Node/pnpm 或数据库等相关版本。
- 最小复现步骤、预期结果和实际结果。
- 已脱敏的日志或截图，以及必要配置项名称。

不要公开密码、令牌、私人地址、客户资料或完整业务数据库。涉及安全问题时，先通过 GitHub 上可用的维护者联系/安全报告渠道沟通，避免在公开 Issue 中附上可利用细节或真实凭据。

若问题属于 [ForgeAdminHarness-open-source](https://github.com/zhiyuxinxi/ForgeAdminHarness-open-source)，请在该仓库反馈。上游原版的问题应先与[原项目](https://github.com/yaomindong1996/forge-admin)版本对照，避免让原作者为本修改版特有行为负责。

## 提交 Pull Request

1. Fork 本仓库并建立自己的功能或修复分支。
2. 先复现要解决的问题，保持修改范围明确。
3. 对实际影响的模块进行必要验证，并同步相应文档。
4. 向本仓库当前公开主分支提出 PR，说明问题、实现、验证结果和未验证事项。

不要假设存在 `dev` 分支、可访问的内部服务器或公共部署账号。PR 是否合并由本仓库维护者审阅决定。

## 修改约定

- 尊重既有模块职责、命名和公共组件；前端参考[文档导航](forge-admin-ui/docs/README.md)，后端参考[架构](docs/architecture.md)及直接相关模块说明。
- 新增界面与提示时，保持中文/英文显示资源和业务原值的区分。
- 权限修改同时考虑前端入口、服务端接口、数据范围与租户，不能通过隐藏按钮替代后端授权。
- 数据库升级使用新的、可审阅的迁移，避免改写已执行的历史脚本。
- 保留锁文件，避免把依赖批量升级混入无关修复。
- 不提交本地配置、密钥、构建产物、客户数据或维护环境记录。

构建、测试、运行验收请分别报告；文档修改只需执行与文档直接相关的核对。没有执行的检查如实写明。

## 来源与许可

保留 [LICENSE](LICENSE)、[NOTICE](NOTICE)和源文件中的作者、版权及许可声明。报表工程保留 [GoView MIT 许可证](forge-report-ui/LICENSE)。引入第三方代码、素材或文档时标明来源和适用许可，不将上游能力重新描述为自己的原创成果。

本贡献指南适用于 zhiyuxinxi 维护的下游仓库；上游的协作政策、品牌或商业约定应以其自己的说明为准。
