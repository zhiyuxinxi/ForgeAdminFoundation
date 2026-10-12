package com.mdframe.forge.plugin.generator.dto.formula;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FormulaValidateResponse {
    private boolean valid;
    private String errorMessage;
    private Integer errorLine;
    private Integer errorColumn;
    private List<String> variables;
    private List<String> dependencyWarnings;
}