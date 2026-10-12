package com.mdframe.forge.plugin.generator.service.formula;

import com.mdframe.forge.starter.core.i18n.UiMessages;

import com.mdframe.forge.plugin.generator.domain.formula.AggregateConfig;
import com.mdframe.forge.plugin.generator.domain.formula.AggregateFunction;
import com.mdframe.forge.plugin.generator.domain.formula.ConditionConfig;
import com.mdframe.forge.plugin.generator.domain.formula.CrossObjectConfig;
import com.mdframe.forge.plugin.generator.domain.formula.CrossObjectRecomputeMode;
import com.mdframe.forge.plugin.generator.domain.formula.DependencyAnalysisResult;
import com.mdframe.forge.plugin.generator.domain.formula.ExpressionParser;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaConfig;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaDependencyAnalyzer;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaMode;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaType;
import com.mdframe.forge.plugin.generator.domain.formula.LookupConfig;
import com.mdframe.forge.plugin.generator.dto.formula.FormulaDependencyGraphEdge;
import com.mdframe.forge.plugin.generator.dto.formula.FormulaDependencyGraphNode;
import com.mdframe.forge.plugin.generator.dto.formula.FormulaDependencyGraphRequest;
import com.mdframe.forge.plugin.generator.dto.formula.FormulaDependencyGraphResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 公式依赖图服务。
 */
@Service
@RequiredArgsConstructor
public class FormulaDependencyGraphService {

    private static final String NODE_FIELD = "FIELD";
    private static final String NODE_FORMULA = "FORMULA";
    private static final String NODE_RELATION = "RELATION";
    private static final String EDGE_DEPENDS_ON = "DEPENDS_ON";
    private static final String EDGE_AGGREGATE = "AGGREGATE";
    private static final String EDGE_LOOKUP = "LOOKUP";
    private static final String EDGE_CROSS_OBJECT = "CROSS_OBJECT";

    private final FormulaDependencyAnalyzer dependencyAnalyzer;
    private final ExpressionParser expressionParser = new ExpressionParser();

    public FormulaDependencyGraphResponse graph(FormulaDependencyGraphRequest request) {
        try {
            if (request == null) {
                throw new IllegalArgumentException("依赖图请求不能为空");
            }
            Map<String, FormulaConfig> formulaMap = buildFormulaMap(request.getFormulas());
            DependencyAnalysisResult analysisResult = dependencyAnalyzer.analyze(formulaMap);
            GraphBuildResult graph = buildGraph(request.getObjectCode(), formulaMap, analysisResult);

            return FormulaDependencyGraphResponse.builder()
                    .valid(analysisResult.isValid())
                    .hasCycle(analysisResult.hasCycle())
                    .nodes(graph.nodes())
                    .edges(graph.edges())
                    .cyclePath(analysisResult.getCyclePath())
                    .topologicalOrder(analysisResult.getTopologicalOrder())
                    .depthMap(analysisResult.getDepthMap())
                    .errors(analysisResult.getErrors())
                    .build();
        } catch (Exception e) {
            return FormulaDependencyGraphResponse.builder()
                    .valid(false)
                    .hasCycle(false)
                    .nodes(List.of())
                    .edges(List.of())
                    .cyclePath(List.of())
                    .topologicalOrder(List.of())
                    .depthMap(Map.of())
                    .errors(List.of(e.getMessage()))
                    .build();
        }
    }

    private GraphBuildResult buildGraph(String objectCode,
                                        Map<String, FormulaConfig> formulaMap,
                                        DependencyAnalysisResult analysisResult) {
        Map<String, FormulaDependencyGraphNode> nodes = new LinkedHashMap<>();
        Map<String, FormulaDependencyGraphEdge> edges = new LinkedHashMap<>();

        for (Map.Entry<String, FormulaConfig> entry : formulaMap.entrySet()) {
            String fieldCode = entry.getKey();
            FormulaConfig config = entry.getValue();
            putFieldNode(nodes, objectCode, fieldCode, NODE_FORMULA, config, analysisResult.getDepthMap().get(fieldCode));

            for (String dependency : fieldDependencies(config)) {
                putFieldNode(nodes, objectCode, dependency,
                        formulaMap.containsKey(dependency) ? NODE_FORMULA : NODE_FIELD,
                        formulaMap.get(dependency),
                        analysisResult.getDepthMap().get(dependency));
                putEdge(edges, dependency, fieldCode, EDGE_DEPENDS_ON, "depends on", Map.of());
            }

            if (config.isAggregate() && config.getAggregate() != null) {
                String relationNodeId = relationNodeId(config.getAggregate().getRelationCode());
                nodes.putIfAbsent(relationNodeId, FormulaDependencyGraphNode.builder()
                        .id(relationNodeId)
                        .label(config.getAggregate().getRelationCode())
                        .type(NODE_RELATION)
                        .objectCode(objectCode)
                        .metadata(Map.of("targetField", config.getAggregate().getTargetField()))
                        .build());
                putEdge(edges, relationNodeId, fieldCode, EDGE_AGGREGATE,
                        config.getAggregate().getFunctionName(),
                        Map.of("targetField", config.getAggregate().getTargetField()));
            }

            if (config.isLookup() && config.getLookup() != null) {
                LookupConfig lookup = config.getLookup();
                String relationNodeId = relationNodeId(lookup.getRelationCode());
                nodes.putIfAbsent(relationNodeId, FormulaDependencyGraphNode.builder()
                        .id(relationNodeId)
                        .label(lookup.getRelationCode())
                        .type(NODE_RELATION)
                        .objectCode(objectCode)
                        .metadata(Map.of(
                                "targetObjectCode", lookup.getTargetObjectCode(),
                                "targetField", lookup.getTargetField(),
                                "returnField", lookup.getReturnField()))
                        .build());
                putEdge(edges, relationNodeId, fieldCode, EDGE_LOOKUP,
                        lookup.getReturnField(),
                        Map.of(
                                "targetObjectCode", lookup.getTargetObjectCode(),
                                "sourceField", lookup.getSourceField(),
                                "targetField", lookup.getTargetField(),
                                "returnField", lookup.getReturnField()));
            }

            if (config.hasCrossObject()) {
                CrossObjectConfig crossObject = config.getCrossObject();
                String relationNodeId = relationNodeId(crossObject.getRelationCode());
                nodes.putIfAbsent(relationNodeId, FormulaDependencyGraphNode.builder()
                        .id(relationNodeId)
                        .label(crossObject.getRelationAlias())
                        .type(NODE_RELATION)
                        .objectCode(objectCode)
                        .metadata(Map.of(
                                "path", crossObject.getPath(),
                                "targetObjectCode", crossObject.getTargetObjectCode(),
                                "returnField", crossObject.getReturnField()))
                        .build());
                putEdge(edges, relationNodeId, fieldCode, EDGE_CROSS_OBJECT,
                        crossObject.getPath(),
                        Map.of(
                                "relationCode", crossObject.getRelationCode(),
                                "targetObjectCode", crossObject.getTargetObjectCode(),
                                "returnField", crossObject.getReturnField(),
                                "recomputeMode", crossObject.getRecomputeMode().name()));
            }
        }

        return new GraphBuildResult(new ArrayList<>(nodes.values()), new ArrayList<>(edges.values()));
    }

    private void putFieldNode(Map<String, FormulaDependencyGraphNode> nodes,
                              String objectCode,
                              String fieldCode,
                              String nodeType,
                              FormulaConfig config,
                              Integer depth) {
        if (fieldCode == null || fieldCode.isBlank()) {
            return;
        }
        FormulaDependencyGraphNode existing = nodes.get(fieldCode);
        if (existing != null && NODE_FORMULA.equals(existing.getType())) {
            return;
        }
        nodes.put(fieldCode, FormulaDependencyGraphNode.builder()
                .id(fieldCode)
                .label(fieldCode)
                .type(nodeType)
                .objectCode(objectCode)
                .fieldCode(fieldCode)
                .formulaType(config == null ? null : config.getType().name())
                .depth(depth)
                .metadata(config == null ? Map.of() : Map.of("formulaMode", config.getMode().name()))
                .build());
    }

    private void putEdge(Map<String, FormulaDependencyGraphEdge> edges,
                         String source,
                         String target,
                         String type,
                         String label,
                         Map<String, Object> metadata) {
        String id = source + "->" + target + ":" + type;
        edges.putIfAbsent(id, FormulaDependencyGraphEdge.builder()
                .id(id)
                .source(source)
                .target(target)
                .type(type)
                .label(label)
                .metadata(metadata)
                .build());
    }

    private Set<String> fieldDependencies(FormulaConfig config) {
        Set<String> dependencies = new LinkedHashSet<>();
        if (config.getDependsOn() != null) {
            dependencies.addAll(config.getDependsOn());
        }
        if (config.isCalc()) {
            dependencies.addAll(parseVariables(config.getExpression()));
        }
        if (config.isConditional() && config.getCondition() != null) {
            dependencies.addAll(parseVariables(config.getCondition().getExpression()));
        }
        if (config.isLookup() && config.getLookup() != null) {
            dependencies.add(config.getLookup().getSourceField());
        }
        dependencies.removeIf(value -> value == null || value.isBlank());
        return dependencies;
    }

    private List<String> parseVariables(String expression) {
        if (expression == null || expression.isBlank()) {
            return Collections.emptyList();
        }
        var parseResult = expressionParser.parse(expression);
        if (!parseResult.isValid()) {
            return Collections.emptyList();
        }
        return parseResult.getVariables();
    }

    private Map<String, FormulaConfig> buildFormulaMap(List<FormulaDependencyGraphRequest.FormulaFieldConfig> fields) {
        if (fields == null) {
            throw new IllegalArgumentException("公式列表不能为空");
        }
        Map<String, FormulaConfig> result = new LinkedHashMap<>();
        for (FormulaDependencyGraphRequest.FormulaFieldConfig field : fields) {
            String fieldCode = trimToNull(field == null ? null : field.resolvedFieldCode());
            if (fieldCode == null) {
                throw new IllegalArgumentException("公式字段编码不能为空");
            }
            if (result.containsKey(fieldCode)) {
                throw new IllegalArgumentException(UiMessages.translate("公式字段编码重复: ") + fieldCode);
            }
            result.put(fieldCode, buildFormulaConfig(field));
        }
        return result;
    }

    private FormulaConfig buildFormulaConfig(FormulaDependencyGraphRequest.FormulaFieldConfig field) {
        FormulaType type = parseEnum(FormulaType.class, field.getType(), FormulaType.CALC);
        FormulaMode mode = parseEnum(FormulaMode.class, field.getMode(), FormulaMode.STORED);
        FormulaConfig.Builder builder = FormulaConfig.builder()
                .type(type)
                .mode(mode)
                .expression(field.getExpression())
                .dependsOn(field.getDependsOn() == null ? List.of() : field.getDependsOn())
                .crossObject(buildCrossObjectConfig(field.getCrossObject()));

        if (type == FormulaType.AGGREGATE) {
            builder.aggregate(buildAggregateConfig(field.getAggregate()));
        }
        if (type == FormulaType.CONDITIONAL) {
            builder.condition(buildConditionConfig(field));
        }
        if (type == FormulaType.LOOKUP) {
            builder.lookup(buildLookupConfig(field.getLookup()));
        }
        return builder.build();
    }

    private AggregateConfig buildAggregateConfig(FormulaDependencyGraphRequest.AggregateGraph aggregate) {
        if (aggregate == null) {
            throw new IllegalArgumentException("AGGREGATE 公式缺少聚合配置");
        }
        AggregateFunction function = parseEnum(AggregateFunction.class, aggregate.getFunction(), AggregateFunction.SUM);
        return new AggregateConfig(function, aggregate.getRelationCode(), aggregate.getTargetField(), aggregate.getFilter());
    }

    private ConditionConfig buildConditionConfig(FormulaDependencyGraphRequest.FormulaFieldConfig field) {
        FormulaDependencyGraphRequest.ConditionGraph condition = field.getCondition();
        String expression = condition == null ? field.getExpression() : condition.getExpression();
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("CONDITIONAL 公式缺少条件表达式");
        }
        Object trueValue = condition == null ? Boolean.TRUE : condition.getTrueValue();
        Object falseValue = condition == null ? Boolean.FALSE : condition.getFalseValue();
        return new ConditionConfig(expression, trueValue, falseValue);
    }

    private LookupConfig buildLookupConfig(FormulaDependencyGraphRequest.LookupGraph lookup) {
        if (lookup == null) {
            throw new IllegalArgumentException("LOOKUP 公式缺少查询配置");
        }
        return new LookupConfig(
                lookup.getRelationCode(),
                lookup.getTargetObjectCode(),
                lookup.getSourceField(),
                lookup.getTargetField(),
                lookup.getReturnField(),
                lookup.getNotFoundValue());
    }

    private CrossObjectConfig buildCrossObjectConfig(FormulaDependencyGraphRequest.CrossObjectGraph crossObject) {
        if (crossObject == null) {
            return null;
        }
        return new CrossObjectConfig(
                crossObject.getPath(),
                crossObject.getRelationCode(),
                crossObject.getTargetObjectCode(),
                crossObject.getReturnField(),
                parseEnum(CrossObjectRecomputeMode.class, crossObject.getRecomputeMode(), CrossObjectRecomputeMode.ASYNC));
    }

    private String relationNodeId(String relationCode) {
        return "relation:" + relationCode;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String rawValue, E fallback) {
        String value = trimToNull(rawValue);
        if (value == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(enumType, value.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException(UiMessages.translate("不支持的") + enumType.getSimpleName() + ": " + rawValue);
        }
    }

    private record GraphBuildResult(List<FormulaDependencyGraphNode> nodes,
                                    List<FormulaDependencyGraphEdge> edges) {
    }
}
