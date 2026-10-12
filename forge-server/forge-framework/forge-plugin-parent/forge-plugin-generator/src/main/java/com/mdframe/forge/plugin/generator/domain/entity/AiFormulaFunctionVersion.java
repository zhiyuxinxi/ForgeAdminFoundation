package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 公式函数版本。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_formula_function_version")
public class AiFormulaFunctionVersion extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String functionCode;

    private String version;

    /** 首期仅允许 JAVA_BEAN。 */
    private String implementationType;

    private String beanName;

    private String methodName;

    private String argumentSchema;

    private String returnType;

    private String example;

    private String releaseNote;

    /** ENABLED/DISABLED/DEPRECATED */
    private String status;

    private String remark;
}
