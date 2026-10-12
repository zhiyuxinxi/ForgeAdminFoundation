package com.mdframe.forge.plugin.generator.dto.formula;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 公式依赖图响应。
 */
@Data
@Builder
public class FormulaDependencyGraphResponse {

    private boolean valid;

    private boolean hasCycle;

    private List<FormulaDependencyGraphNode> nodes;

    private List<FormulaDependencyGraphEdge> edges;

    private List<String> cyclePath;

    private List<String> topologicalOrder;

    private Map<String, Integer> depthMap;

    private List<String> errors;
}
