package com.mdframe.forge.plugin.generator.dto.formula;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公式执行日志响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormulaExecutionLogResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long tenantId;

    private String traceId;

    private String objectCode;

    private String recordId;

    private String fieldCode;

    private String formulaType;

    private String formulaMode;

    private String expression;

    private String inputSnapshot;

    private String outputValue;

    private Boolean success;

    private String errorMessage;

    private Long elapsedMs;

    private Long createBy;

    private LocalDateTime createTime;

    private Long createDept;

    private Long updateBy;

    private LocalDateTime updateTime;
}
