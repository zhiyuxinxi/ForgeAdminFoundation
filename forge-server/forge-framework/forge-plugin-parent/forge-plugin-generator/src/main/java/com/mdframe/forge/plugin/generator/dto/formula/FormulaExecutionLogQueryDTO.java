package com.mdframe.forge.plugin.generator.dto.formula;

import com.mdframe.forge.starter.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 公式执行日志查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FormulaExecutionLogQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    private String objectCode;

    private String recordId;

    private String fieldCode;

    private Boolean success;

    private String traceId;

    private LocalDateTime beginTime;

    private LocalDateTime endTime;
}
