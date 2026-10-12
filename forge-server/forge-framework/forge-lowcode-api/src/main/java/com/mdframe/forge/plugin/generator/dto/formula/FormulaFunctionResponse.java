package com.mdframe.forge.plugin.generator.dto.formula;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FormulaFunctionResponse {
    private String name;
    private String displayName;
    private String category;
    private String description;
    private String argumentSchema;
    private String returnType;
    private String sourceType;
    private String example;
}

@Data
@Builder
class FormulaFunctionListResponse {
    private List<FormulaFunctionResponse> functions;
}
