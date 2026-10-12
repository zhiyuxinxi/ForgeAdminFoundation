package com.mdframe.forge.plugin.generator.dto.formula;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class FormulaValidateRequest {
    @NotBlank
    private String expression;
    private List<String> dependsOn;
    private String type;
}