package com.mdframe.forge.plugin.generator.dto.formula;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class FormulaDependencyResponse {
    private boolean valid;
    private boolean hasCycle;
    private List<String> topologicalOrder;
    private Map<String, Integer> depthMap;
    private List<String> cyclePath;
    private List<String> errors;
    private List<String> warnings;
}