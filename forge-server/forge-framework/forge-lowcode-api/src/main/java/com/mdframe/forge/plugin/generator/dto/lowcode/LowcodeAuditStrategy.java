package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

/**
 * 低代码运行表审计字段策略。
 */
@Data
public class LowcodeAuditStrategy {

    /** NONE-不自动填充，FORGE_COLUMNS-使用 Forge 标准审计字段。 */
    private String mode;

    private String createByColumn;

    private String createTimeColumn;

    private String createDeptColumn;

    private String updateByColumn;

    private String updateTimeColumn;
}
