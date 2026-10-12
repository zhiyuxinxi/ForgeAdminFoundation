package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 公式执行日志。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_formula_execution_log")
public class AiFormulaExecutionLog extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String traceId;

    private String objectCode;

    private String recordId;

    private String fieldCode;

    private String formulaType;

    private String formulaMode;

    private String expression;

    /** 输入快照 JSON，写入前必须脱敏。 */
    private String inputSnapshot;

    /** 输出值摘要，写入前必须脱敏。 */
    private String outputValue;

    private Boolean success;

    private String errorMessage;

    private Long elapsedMs;
}
