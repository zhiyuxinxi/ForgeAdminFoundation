package com.mdframe.forge.plugin.generator.dto.formula;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 公式依赖图边。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormulaDependencyGraphEdge {

    private String id;

    private String source;

    private String target;

    /** DEPENDS_ON / LOOKUP / CROSS_OBJECT / AGGREGATE / FUNCTION_CALL */
    private String type;

    private String label;

    private Map<String, Object> metadata;
}
