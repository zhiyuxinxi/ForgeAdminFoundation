package com.mdframe.forge.plugin.generator.dto.formula;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FormulaPreviewResponse {
    private boolean success;
    private Object result;
    private String errorMessage;
    private long elapsedMs;
}