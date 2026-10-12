package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 公式函数注册表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_formula_function")
public class AiFormulaFunction extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String functionCode;

    private String displayName;

    private String category;

    private String description;

    /** BUILTIN/SYSTEM/TENANT/MARKET */
    private String sourceType;

    /** 参数 JSON Schema。 */
    private String argumentSchema;

    private String returnType;

    private String example;

    /** ENABLED/DISABLED */
    private String status;

    private String currentVersion;

    private String latestVersion;

    private Boolean builtin;

    private Integer sortOrder;

    private String remark;
}
