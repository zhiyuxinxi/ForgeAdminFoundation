package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

/**
 * 低代码运行表租户隔离策略。
 */
@Data
public class LowcodeTenantStrategy {

    /** NONE-不追加租户条件，FORGE_TENANT_ID-按当前 Forge 租户过滤。 */
    private String mode;

    private String columnName;
}
