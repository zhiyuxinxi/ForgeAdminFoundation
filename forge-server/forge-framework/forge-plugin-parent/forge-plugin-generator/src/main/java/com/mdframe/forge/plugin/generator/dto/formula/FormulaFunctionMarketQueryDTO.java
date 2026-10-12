package com.mdframe.forge.plugin.generator.dto.formula;

import com.mdframe.forge.starter.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 公式函数市场查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FormulaFunctionMarketQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    private String keyword;

    private String category;

    private String sourceType;

    private String status;

    private String installStatus;

    private Boolean enabled;
}
