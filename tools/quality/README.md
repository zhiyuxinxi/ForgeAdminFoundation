# 后端非 AI 检查

当前第一批接入的是源码快检 `fast`：PMD 7.17.0 + Checkstyle 10.26.1。固定版本由本目录独立POM管理，不编译整个业务仓库。字节码、依赖漏洞、架构规则及Harness消费接入仍在本次整改任务中；不把源码快检称为完整安全审查。

使用JDK17、Python3.10及Maven。首次准备工具依赖；后续保留Maven缓存和本目录target：

```powershell
mvn -B -f tools/quality/pom.xml org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath -Dmdep.outputFile=target/runtime/classpath.txt
python scripts/backend-quality.py fast --base origin/main
python scripts/backend-quality.py fast --file forge-server/path/to/ChangedService.java
```

`--base`包含相对基线的已提交、暂存、未暂存及未跟踪源文件；删除文件不扫描。`--file`可重复，文件必须位于本仓。没有输入范围时直接拒绝，不自动扫描全仓。每次输出原始JSON/XML、日志与统一result.json，包含源码摘要、规则、行号、工具退出码。

当前结构规则与解释：

| 角度 | 工具规则 / 本仓配置 | 使用边界 |
| --- | --- | --- |
| 类方法数量 | PMD `TooManyMethods.maxmethods=50` | 超过50时报告；原生规则不计至多一条语句的简单get/set/is方法，告警只给出超限结论，不包含精确总数。 |
| 方法长度 | Checkstyle `MethodLength.max=150,countEmpty=false` | 方法/构造器代码行，排除空行和注释；不是NCSS语句数。 |
| 方法/类语句规模 | PMD `NcssCount.methodReportLevel=80,classReportLevel=800` | 有效语句规模，不以删除注释或压缩排版降低指标。 |
| 认知复杂度 | PMD `CognitiveComplexity.reportLevel=20` | 分支、循环、嵌套带来的理解成本；短方法也可能超限。 |
| 职责/依赖信号 | PMD `GodClass`、`CouplingBetweenObjects.threshold=20` | 结合复杂度、外部数据、内聚与依赖类型审阅，不能单靠计数判定职责错误。 |
| 文件体积 | Checkstyle `FileLength.max=800` | 定位候选，不能用拆文件/整体平移隐藏职责混合。 |

统一报告的每条发现含`reviewKind=structure/code`，`findingCounts`分别汇总两类；PMD位置保留`endLine`与官方规则`documentationUrl`。指标数值沿用工具原始message，未自行解析Java源码。`code`分类也只是具体代码规则发现，不等同已确认安全漏洞。所有发现继续保留退出码1，执行错误仍为2，不因分类伪装通过。

本轮改变的职责出现结构问题时，先划定可独立验证的职责，再实际提取策略/转换/编排边界，保留授权、事务和失败语义并运行对应正反例；只重验修改文件及直接消费者。记录重构前后的原规则结果和保留的剩余问题。历史无关结构告警由有界整改清单跟踪，不自动阻塞局部安全补丁；需要修复的既有大类不能仅写告警就标完成。阈值是启动审阅的位置，不是合格架构的充分条件，不自动运行全仓Hook。

新增规则的真实边界验证（复用已准备工具，不构建应用）：

```powershell
python -m unittest scripts.tests.test_backend_quality.SourceQualityContractTest.test_real_scanners_enforce_method_count_and_coupling_boundaries
```

其中50方法/20依赖类型通过，51方法/21类型触发两条原生规则。其他范围外拒绝、解析错误与工具失败合同沿用既有测试，只有相关行为变更才重跑。

参数依据：[PMD 7.17.0设计规则](https://docs.pmd-code.org/pmd-doc-7.17.0/pmd_rules_java_design.html)、[Checkstyle 10.26.1 MethodLength源码及配置说明](https://github.com/checkstyle/checkstyle/blob/checkstyle-10.26.1/src/main/java/com/puppycrawl/tools/checkstyle/checks/sizes/MethodLengthCheck.java)。固定版本10.26.1已核支持`METHOD_DEF/CTOR_DEF/COMPACT_CTOR_DEF`，未从最新文档猜测旧版本能力。

退出码0为无发现或没有适用Java文件（状态分别为passed/not_applicable）；1为有发现需审阅；2为执行错误，不能解释成零问题。复杂度、类长度、方法长度是审阅信号，不证明职责错误；不要为局部安全修复改造无关旧代码。工具不能证明租户权限、流程和并发正确，必须保留对应正反例测试。

AI按具体rule/path/line定位根因，修当前授权范围后重跑受影响文件。不得为通过删除规则、加NOPMD、扩大排除或改业务测试断言。误报须保留原结果并具体解释；不要启用未校准的无条件提交Hook。

维护时在本目录POM固定升级版本，用相同的最小正反例比较结果和耗时；配置修改单独审阅，日志与失败报告保留，不全局清缓存。参考：[PMD CLI](https://docs.pmd-code.org/pmd-doc-7.17.0/pmd_userdocs_cli_reference.html)、[PMD设计规则](https://docs.pmd-code.org/pmd-doc-7.17.0/pmd_rules_java_design.html)、[Checkstyle命令行](https://checkstyle.org/cmdline.html)。
