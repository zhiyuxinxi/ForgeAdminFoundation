package com.mdframe.forge.plugin.generator.dto.formula;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 公式依赖图节点。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormulaDependencyGraphNode {

    private String id;

    private String label;

    /** FIELD / FORMULA / OBJECT / FUNCTION / RELATION */
    private String type;

    private String objectCode;

    private String fieldCode;

    private String formulaType;

    private Integer depth;

    private Map<String, Object> metadata;
}
