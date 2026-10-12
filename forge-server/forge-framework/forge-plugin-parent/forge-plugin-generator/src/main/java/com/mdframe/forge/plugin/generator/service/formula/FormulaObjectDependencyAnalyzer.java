package com.mdframe.forge.plugin.generator.service.formula;

import com.mdframe.forge.plugin.generator.domain.formula.CrossObjectConfig;
import com.mdframe.forge.plugin.generator.domain.formula.CrossObjectRecomputeMode;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaConfig;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaMode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Analyzes cross-object formula dependencies as an object-level graph.
 */
@Component
public class FormulaObjectDependencyAnalyzer {

    public ObjectDependencyAnalysisResult analyze(Collection<ObjectContext> contexts) {
        Map<String, ObjectContext> contextByObject = indexContexts(contexts);
        List<ObjectDependencyEdge> edges = new ArrayList<>();
        List<ObjectDependencyError> errors = new ArrayList<>();

        for (ObjectContext context : contextByObject.values()) {
            collectContextEdges(context, contextByObject, edges, errors);
        }

        List<String> cyclePath = findCycle(buildGraph(edges));
        if (!cyclePath.isEmpty()) {
            errors.add(new ObjectDependencyError(
                    cyclePath.get(0),
                    "OBJECT_GRAPH",
                    "CROSS_OBJECT_CYCLE",
                    "Cross-object dependency cycle: " + String.join(" -> ", cyclePath)));
        }

        return new ObjectDependencyAnalysisResult(edges, errors, cyclePath);
    }

    private Map<String, ObjectContext> indexContexts(Collection<ObjectContext> contexts) {
        Map<String, ObjectContext> result = new LinkedHashMap<>();
        if (contexts == null) {
            return result;
        }
        for (ObjectContext context : contexts) {
            if (context == null || StringUtils.isBlank(context.getObjectCode())) {
                continue;
            }
            result.put(context.getObjectCode(), context);
        }
        return result;
    }

    private void collectContextEdges(ObjectContext context,
                                     Map<String, ObjectContext> contextByObject,
                                     List<ObjectDependencyEdge> edges,
                                     List<ObjectDependencyError> errors) {
        for (Map.Entry<String, FormulaConfig> entry : context.getFormulaMap().entrySet()) {
            String fieldCode = entry.getKey();
            FormulaConfig formula = entry.getValue();
            if (formula == null || !formula.hasCrossObject()) {
                continue;
            }
            CrossObjectConfig crossObject = formula.getCrossObject();
            ObjectRelation relation = findRelation(context, crossObject);
            if (relation == null) {
                errors.add(error(context.getObjectCode(), fieldCode,
                        "Relation not found for cross-object formula: " + crossObject.getRelationCode()));
                continue;
            }
            validateRelation(context, relation, crossObject, fieldCode, contextByObject, errors);
            edges.add(new ObjectDependencyEdge(
                    context.getObjectCode(),
                    crossObject.getTargetObjectCode(),
                    fieldCode,
                    crossObject.getRelationCode(),
                    relation.getSourceField(),
                    relation.getTargetField(),
                    crossObject.getReturnField(),
                    formula.getMode(),
                    crossObject.getRecomputeMode(),
                    buildDependencyTrace(context.getObjectCode(), fieldCode, crossObject)));
        }
    }

    private ObjectRelation findRelation(ObjectContext context, CrossObjectConfig crossObject) {
        for (ObjectRelation relation : context.getRelations()) {
            if (relation == null) {
                continue;
            }
            if (!sameText(context.getObjectCode(), relation.getSourceObjectCode())) {
                continue;
            }
            if (!sameText(crossObject.getTargetObjectCode(), relation.getTargetObjectCode())) {
                continue;
            }
            if (matchesRelationCode(crossObject, relation) && matchesPathAlias(crossObject, relation)) {
                return relation;
            }
        }
        return null;
    }

    private void validateRelation(ObjectContext source,
                                  ObjectRelation relation,
                                  CrossObjectConfig crossObject,
                                  String fieldCode,
                                  Map<String, ObjectContext> contextByObject,
                                  List<ObjectDependencyError> errors) {
        if (StringUtils.isBlank(relation.getSourceField())) {
            errors.add(error(source.getObjectCode(), fieldCode,
                    "Relation source field is blank: " + relation.getRelationCode()));
        } else if (!source.hasField(relation.getSourceField())) {
            errors.add(error(source.getObjectCode(), fieldCode,
                    "Relation source field does not exist: " + relation.getSourceField()));
        }

        ObjectContext target = contextByObject.get(crossObject.getTargetObjectCode());
        if (target == null) {
            errors.add(error(source.getObjectCode(), fieldCode,
                    "Target object schema not found: " + crossObject.getTargetObjectCode()));
            return;
        }

        if (StringUtils.isBlank(relation.getTargetField())) {
            errors.add(error(source.getObjectCode(), fieldCode,
                    "Relation target field is blank: " + relation.getRelationCode()));
        } else if (!target.hasField(relation.getTargetField())) {
            errors.add(error(source.getObjectCode(), fieldCode,
                    "Relation target field does not exist: " + relation.getTargetField()));
        }

        if (!target.hasField(crossObject.getReturnField())) {
            errors.add(error(source.getObjectCode(), fieldCode,
                    "Cross-object return field does not exist on target object: " + crossObject.getReturnField()));
        }
    }

    private ObjectDependencyError error(String objectCode, String fieldCode, String message) {
        return new ObjectDependencyError(
                objectCode,
                fieldCode,
                "CROSS_OBJECT",
                "[" + objectCode + "." + fieldCode + "] " + message);
    }

    private boolean matchesRelationCode(CrossObjectConfig crossObject, ObjectRelation relation) {
        return sameText(crossObject.getRelationCode(), relation.getRelationCode())
                || sameText(crossObject.getRelationCode(), relation.getRelationId())
                || sameText(crossObject.getRelationCode(), relation.getTargetObjectCode());
    }

    private boolean matchesPathAlias(CrossObjectConfig crossObject, ObjectRelation relation) {
        String alias = crossObject.getRelationAlias();
        return sameText(alias, crossObject.getRelationCode())
                || sameText(alias, relation.getRelationCode())
                || sameText(alias, relation.getTargetObjectCode());
    }

    private Map<String, Set<String>> buildGraph(List<ObjectDependencyEdge> edges) {
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        for (ObjectDependencyEdge edge : edges) {
            graph.computeIfAbsent(edge.getSourceObjectCode(), key -> new LinkedHashSet<>())
                    .add(edge.getTargetObjectCode());
            graph.computeIfAbsent(edge.getTargetObjectCode(), key -> new LinkedHashSet<>());
        }
        return graph;
    }

    private List<String> findCycle(Map<String, Set<String>> graph) {
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        Deque<String> path = new ArrayDeque<>();
        for (String node : graph.keySet()) {
            List<String> cycle = dfs(node, graph, visited, visiting, path);
            if (!cycle.isEmpty()) {
                return cycle;
            }
        }
        return List.of();
    }

    private List<String> dfs(String node,
                             Map<String, Set<String>> graph,
                             Set<String> visited,
                             Set<String> visiting,
                             Deque<String> path) {
        if (visited.contains(node)) {
            return List.of();
        }
        if (visiting.contains(node)) {
            List<String> currentPath = new ArrayList<>(path);
            int start = currentPath.indexOf(node);
            if (start < 0) {
                return List.of(node);
            }
            List<String> cycle = new ArrayList<>(currentPath.subList(start, currentPath.size()));
            cycle.add(node);
            return cycle;
        }

        visiting.add(node);
        path.addLast(node);
        for (String next : graph.getOrDefault(node, Set.of())) {
            List<String> cycle = dfs(next, graph, visited, visiting, path);
            if (!cycle.isEmpty()) {
                return cycle;
            }
        }
        path.removeLast();
        visiting.remove(node);
        visited.add(node);
        return List.of();
    }

    private String buildDependencyTrace(String objectCode, String fieldCode, CrossObjectConfig crossObject) {
        return objectCode + "." + fieldCode
                + "->" + crossObject.getTargetObjectCode() + "." + crossObject.getReturnField()
                + "@" + crossObject.getRelationCode();
    }

    private boolean sameText(String left, String right) {
        return Objects.equals(StringUtils.trimToNull(left), StringUtils.trimToNull(right));
    }

    public static class ObjectContext {
        private final String objectCode;
        private final Set<String> fieldNames;
        private final Map<String, FormulaConfig> formulaMap;
        private final List<ObjectRelation> relations;

        public ObjectContext(String objectCode,
                             Collection<String> fieldNames,
                             Map<String, FormulaConfig> formulaMap,
                             Collection<ObjectRelation> relations) {
            this.objectCode = objectCode;
            this.fieldNames = Collections.unmodifiableSet(new LinkedHashSet<>(
                    fieldNames == null ? Set.of() : fieldNames));
            this.formulaMap = Collections.unmodifiableMap(new LinkedHashMap<>(
                    formulaMap == null ? Map.of() : formulaMap));
            this.relations = Collections.unmodifiableList(new ArrayList<>(
                    relations == null ? List.of() : relations));
        }

        public String getObjectCode() {
            return objectCode;
        }

        public Set<String> getFieldNames() {
            return fieldNames;
        }

        public Map<String, FormulaConfig> getFormulaMap() {
            return formulaMap;
        }

        public List<ObjectRelation> getRelations() {
            return relations;
        }

        boolean hasField(String fieldName) {
            if (StringUtils.isBlank(fieldName)) {
                return false;
            }
            return fieldNames.contains(fieldName)
                    || fieldNames.contains(snakeToCamel(fieldName))
                    || fieldNames.contains(camelToSnake(fieldName));
        }
    }

    public static class ObjectRelation {
        private final String relationId;
        private final String relationCode;
        private final String sourceObjectCode;
        private final String targetObjectCode;
        private final String sourceField;
        private final String targetField;

        public ObjectRelation(String relationId,
                              String relationCode,
                              String sourceObjectCode,
                              String targetObjectCode,
                              String sourceField,
                              String targetField) {
            this.relationId = relationId;
            this.relationCode = relationCode;
            this.sourceObjectCode = sourceObjectCode;
            this.targetObjectCode = targetObjectCode;
            this.sourceField = sourceField;
            this.targetField = targetField;
        }

        public String getRelationId() {
            return relationId;
        }

        public String getRelationCode() {
            return relationCode;
        }

        public String getSourceObjectCode() {
            return sourceObjectCode;
        }

        public String getTargetObjectCode() {
            return targetObjectCode;
        }

        public String getSourceField() {
            return sourceField;
        }

        public String getTargetField() {
            return targetField;
        }
    }

    public static class ObjectDependencyEdge {
        private final String sourceObjectCode;
        private final String targetObjectCode;
        private final String fieldCode;
        private final String relationCode;
        private final String sourceField;
        private final String targetField;
        private final String returnField;
        private final FormulaMode formulaMode;
        private final CrossObjectRecomputeMode recomputeMode;
        private final String dependencyTrace;

        public ObjectDependencyEdge(String sourceObjectCode,
                                    String targetObjectCode,
                                    String fieldCode,
                                    String relationCode,
                                    String sourceField,
                                    String targetField,
                                    String returnField,
                                    FormulaMode formulaMode,
                                    CrossObjectRecomputeMode recomputeMode,
                                    String dependencyTrace) {
            this.sourceObjectCode = sourceObjectCode;
            this.targetObjectCode = targetObjectCode;
            this.fieldCode = fieldCode;
            this.relationCode = relationCode;
            this.sourceField = sourceField;
            this.targetField = targetField;
            this.returnField = returnField;
            this.formulaMode = formulaMode;
            this.recomputeMode = recomputeMode;
            this.dependencyTrace = dependencyTrace;
        }

        public String getSourceObjectCode() {
            return sourceObjectCode;
        }

        public String getTargetObjectCode() {
            return targetObjectCode;
        }

        public String getFieldCode() {
            return fieldCode;
        }

        public String getRelationCode() {
            return relationCode;
        }

        public String getSourceField() {
            return sourceField;
        }

        public String getTargetField() {
            return targetField;
        }

        public String getReturnField() {
            return returnField;
        }

        public FormulaMode getFormulaMode() {
            return formulaMode;
        }

        public CrossObjectRecomputeMode getRecomputeMode() {
            return recomputeMode;
        }

        public String getDependencyTrace() {
            return dependencyTrace;
        }
    }

    public static class ObjectDependencyError {
        private final String objectCode;
        private final String fieldName;
        private final String category;
        private final String message;

        public ObjectDependencyError(String objectCode, String fieldName, String category, String message) {
            this.objectCode = objectCode;
            this.fieldName = fieldName;
            this.category = category;
            this.message = message;
        }

        public String getObjectCode() {
            return objectCode;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getCategory() {
            return category;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class ObjectDependencyAnalysisResult {
        private final boolean valid;
        private final List<ObjectDependencyEdge> edges;
        private final List<ObjectDependencyError> errors;
        private final List<String> cyclePath;

        ObjectDependencyAnalysisResult(List<ObjectDependencyEdge> edges,
                                       List<ObjectDependencyError> errors,
                                       List<String> cyclePath) {
            this.edges = Collections.unmodifiableList(new ArrayList<>(edges));
            this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
            this.cyclePath = Collections.unmodifiableList(new ArrayList<>(cyclePath));
            this.valid = this.errors.isEmpty() && this.cyclePath.isEmpty();
        }

        public boolean isValid() {
            return valid;
        }

        public List<ObjectDependencyEdge> getEdges() {
            return edges;
        }

        public List<ObjectDependencyError> getErrors() {
            return errors;
        }

        public List<String> getCyclePath() {
            return cyclePath;
        }

        public boolean hasCycle() {
            return !cyclePath.isEmpty();
        }
    }

    private static String snakeToCamel(String value) {
        if (StringUtils.isBlank(value) || !value.contains("_")) {
            return value;
        }
        StringBuilder result = new StringBuilder();
        boolean upperNext = false;
        for (char ch : value.toCharArray()) {
            if (ch == '_') {
                upperNext = true;
                continue;
            }
            result.append(upperNext ? Character.toUpperCase(ch) : ch);
            upperNext = false;
        }
        return result.toString();
    }

    private static String camelToSnake(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }
}
