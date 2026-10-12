package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 公式函数安装状态。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_formula_function_install")
public class AiFormulaFunctionInstall extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String functionCode;

    private String installedVersion;

    /** INSTALLED/UNINSTALLED */
    private String installStatus;

    private Boolean enabled;

    /** BUILTIN/SYSTEM/TENANT/MARKET */
    private String sourceType;

    private Long installedBy;

    private LocalDateTime installedTime;

    private String remark;
}
