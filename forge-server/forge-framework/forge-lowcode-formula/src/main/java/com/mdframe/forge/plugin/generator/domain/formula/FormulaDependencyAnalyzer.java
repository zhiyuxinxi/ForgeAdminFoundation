package com.mdframe.forge.plugin.generator.domain.formula;

import com.mdframe.forge.starter.core.i18n.UiMessages;

import java.util.*;
import org.springframework.stereotype.Component;

/**
 * Formula dependency analyzer.
 * <p>
 * Performs DAG analysis on a set of formula configurations:
 * <ul>
 *   <li>Build prerequisite->dependent adjacency for topological sort</li>
 *   <li>Kahn topological sort to detect cyclic dependencies</li>
 *   <li>Compute nesting depth for each formula</li>
 *   <li>Validate maximum nesting depth</li>
 * </ul>
 * <p>
 * Does not depend on Aviator, database, Spring, or any external framework.
 * Pure domain logic.
 */
@Component
public class FormulaDependencyAnalyzer {
    /** Default maximum nesting depth. */

    private final ExpressionParser parser = new ExpressionParser();
    public static final int DEFAULT_MAX_DEPTH = 3;

    private final int maxDepth;

    public FormulaDependencyAnalyzer() {
        this(DEFAULT_MAX_DEPTH);
    }

    public FormulaDependencyAnalyzer(int maxDepth) {
        if (maxDepth < 1) {
            throw new IllegalArgumentException("maxDepth must be >= 1, got: " + maxDepth);
        }
        this.maxDepth = maxDepth;
    }

    /**
     * Perform complete dependency analysis on formula configurations.
     *
     * @param formulaMap field name to formula config mapping
     * @return analysis result with topological order, depth map, and cycle information
     */
    public DependencyAnalysisResult analyze(Map<String, FormulaConfig> formulaMap) {
        Objects.requireNonNull(formulaMap, "formulaMap must not be null");

        DependencyAnalysisResult.Builder result = DependencyAnalysisResult.builder();

        if (formulaMap.isEmpty()) {
            return result.valid(true).topologicalOrder(Collections.emptyList()).build();
        }

        Set<String> formulaFields = formulaMap.keySet();

        // 1. Build prerequisite->dependent adjacency: B->A means B must be computed before A
        Map<String, Set<String>> prereqAdjacency = buildPrereqAdjacency(formulaMap);

        // 2. Compute indegree: how many prerequisites each node has
        Map<String, Integer> indegree = computeIndegree(formulaFields, prereqAdjacency);

        // 3. Kahn topological sort + cycle detection
        List<String> topologicalOrder = new ArrayList<>();
        List<List<String>> cycles = kahnSort(prereqAdjacency, indegree, formulaFields, topologicalOrder);

        // 4. Compute depth using dependsOn from formula configs (correct direction)
        Map<String, Integer> depthMap = computeDepth(formulaMap, topologicalOrder);

        // 5. Validate depth
        List<String> depthErrors = validateDepth(depthMap, formulaFields);

        boolean hasCycles = !cycles.isEmpty();
        boolean hasDepthErrors = !depthErrors.isEmpty();
        boolean valid = !hasCycles && !hasDepthErrors;

        result.valid(valid)
            .topologicalOrder(Collections.unmodifiableList(topologicalOrder))
            .depthMap(Collections.unmodifiableMap(depthMap));

        if (hasCycles) {
            for (List<String> cycle : cycles) {
                result.addCycle(Collections.unmodifiableList(cycle));
                result.addError(UiMessages.translate("检测到循环依赖: ") + String.join(" -> ", cycle) + " -> " + cycle.get(0));
            }
        }

        if (hasDepthErrors) {
            for (String err : depthErrors) {
                result.addError(err);
            }
        }

        return result.build();
    }

    /**
     * Build prerequisite->dependent adjacency.
     * <p>For formula A that depends on fields [B, C], creates edges B->A and C->A.
     */
    Map<String, Set<String>> buildPrereqAdjacency(Map<String, FormulaConfig> formulaMap) {
        Map<String, Set<String>> adjacency = new LinkedHashMap<>();

        for (String fieldName : formulaMap.keySet()) {
            adjacency.putIfAbsent(fieldName, new LinkedHashSet<>());
        }

        for (Map.Entry<String, FormulaConfig> entry : formulaMap.entrySet()) {
            String fieldName = entry.getKey();
            FormulaConfig config = entry.getValue();

            if (config.getDependsOn() != null) {
                for (String dep : config.getDependsOn()) {
                    adjacency.computeIfAbsent(dep, k -> new LinkedHashSet<>()).add(fieldName);
                }
            }

            if (config.isAggregate() && config.getAggregate() != null) {
                String relationKey = "@" + config.getAggregate().getRelationCode();
                adjacency.computeIfAbsent(relationKey, k -> new LinkedHashSet<>()).add(fieldName);
            }

            if (config.isLookup() && config.getLookup() != null) {
                LookupConfig lookup = config.getLookup();
                adjacency.computeIfAbsent(lookup.getSourceField(), k -> new LinkedHashSet<>()).add(fieldName);
                String relationKey = "@" + lookup.getRelationCode();
                adjacency.computeIfAbsent(relationKey, k -> new LinkedHashSet<>()).add(fieldName);
            }

            if (config.isConditional() && config.getCondition() != null) {
                ConditionConfig cc = config.getCondition();
                if (cc.hasExpression()) {
                    var parseResult = parser.parse(cc.getExpression());
                    if (parseResult.isValid()) {
                        for (String var : parseResult.getVariables()) {
                            adjacency.computeIfAbsent(var, k -> new LinkedHashSet<>()).add(fieldName);
                        }
                    }
                }
            }
        }

        return adjacency;
    }

    /**
     * Compute indegree for each node.
     * Non-formula dependency nodes (e.g., raw field "price") are included
     * with indegree 0 so Kahn can process them and reduce their dependents.
     */
    Map<String, Integer> computeIndegree(Set<String> formulaFields,
                                          Map<String, Set<String>> prereqAdjacency) {
        Map<String, Integer> indegree = new LinkedHashMap<>();
        // Include formula nodes + dependency-only nodes (e.g., raw fields like "price")
        Set<String> allKeys = new LinkedHashSet<>(formulaFields);
        allKeys.addAll(prereqAdjacency.keySet());
        for (String node : allKeys) {
            indegree.put(node, 0);
        }
        for (Set<String> dependents : prereqAdjacency.values()) {
            for (String dependent : dependents) {
                indegree.merge(dependent, 1, Integer::sum);
            }
        }
        return indegree;
    }

    /**
     * Kahn topological sort with cycle detection.
     * Only formula fields (not raw dependency nodes) are included in outOrder.
     */
    List<List<String>> kahnSort(Map<String, Set<String>> prereqAdjacency,
                                 Map<String, Integer> indegree,
                                 Set<String> formulaFields,
                                 List<String> outOrder) {
        Map<String, Integer> inDegreeCopy = new LinkedHashMap<>(indegree);
        List<List<String>> cycles = new ArrayList<>();

        Queue<String> queue = new ArrayDeque<>();
        for (Map.Entry<String, Integer> entry : inDegreeCopy.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        int formulaCount = 0;
        while (!queue.isEmpty()) {
            String node = queue.poll();
            // Only add formula fields to the output order
            if (formulaFields.contains(node)) {
                outOrder.add(node);
                formulaCount++;
            }

            Set<String> dependents = prereqAdjacency.getOrDefault(node, Collections.emptySet());
            for (String dependent : dependents) {
                int newIndegree = inDegreeCopy.merge(dependent, -1, Integer::sum);
                if (newIndegree == 0) {
                    queue.add(dependent);
                }
            }
        }

        // Not all formula fields processed, cycle exists.
        if (formulaCount < formulaFields.size()) {
            Set<String> processed = new HashSet<>(outOrder);
            Set<String> remaining = new LinkedHashSet<>(formulaFields);
            remaining.removeAll(processed);

            Map<String, Set<String>> dependsOnAdj = buildDependsOnAdjacency(prereqAdjacency, formulaFields);

            for (String start : remaining) {
                List<String> cycle = findCycleFrom(start, dependsOnAdj, new HashSet<>(), new ArrayList<>());
                if (cycle != null && !cycle.isEmpty()) {
                    cycles.add(cycle);
                    break;
                }
            }
        }

        return cycles;
    }

    private Map<String, Set<String>> buildDependsOnAdjacency(Map<String, Set<String>> prereqAdjacency,
                                                              Set<String> formulaFields) {
        Map<String, Set<String>> dependsOn = new LinkedHashMap<>();
        for (String field : formulaFields) {
            dependsOn.put(field, new LinkedHashSet<>());
        }
        for (Map.Entry<String, Set<String>> entry : prereqAdjacency.entrySet()) {
            String prereq = entry.getKey();
            for (String dependent : entry.getValue()) {
                if (formulaFields.contains(dependent)) {
                    dependsOn.get(dependent).add(prereq);
                }
            }
        }
        return dependsOn;
    }

    private List<String> findCycleFrom(String node, Map<String, Set<String>> dependsOnAdj,
                                        Set<String> visited, List<String> path) {
        if (!visited.add(node)) {
            int idx = path.indexOf(node);
            if (idx >= 0) {
                return new ArrayList<>(path.subList(idx, path.size()));
            }
            return null;
        }
        path.add(node);
        Set<String> deps = dependsOnAdj.getOrDefault(node, Collections.emptySet());
        for (String dep : deps) {
            List<String> cycle = findCycleFrom(dep, dependsOnAdj, visited, path);
            if (cycle != null) return cycle;
        }
        path.remove(path.size() - 1);
        return null;
    }


    /**
     * Validate that all dependsOn references point to existing fields.
     * <p>
     * A dependency is valid if it exists in {@code validFieldNames} (the current object's schema fields)
     * or in {@code formulaMap} (the set of formula-configured fields).
     * <p>
     * Fix for UAT-05-GAP: missing dependency fields must block publish.
     *
     * @param formulaMap      the formula configurations to validate
     * @param validFieldNames the complete set of field names in the object schema
     * @return list of error messages (empty if all valid)
     */
    public static List<String> validateDependencyFields(Map<String, FormulaConfig> formulaMap,
                                                         Set<String> validFieldNames) {
        List<String> errors = new ArrayList<>();
        Set<String> formulaFields = formulaMap.keySet();
        Set<String> allValid = new HashSet<>(validFieldNames);
        allValid.addAll(formulaFields); // formula fields are always valid targets

        for (Map.Entry<String, FormulaConfig> entry : formulaMap.entrySet()) {
            String fieldName = entry.getKey();
            FormulaConfig config = entry.getValue();
            List<String> dependsOn = config.getDependsOn();
            Set<String> dependencies = new LinkedHashSet<>();
            if (dependsOn != null) {
                dependencies.addAll(dependsOn);
            }
            if (config.isLookup() && config.getLookup() != null) {
                dependencies.add(config.getLookup().getSourceField());
            }
            if (dependencies.isEmpty()) continue;

            for (String dep : dependencies) {
                if (!allValid.contains(dep)) {
                    errors.add("Field [" + fieldName + "] references missing field [" + dep + "]");
                }
            }
        }
        return errors;
    }
    Map<String, Integer> computeDepth(Map<String, FormulaConfig> formulaMap,
                                       List<String> topologicalOrder) {
        Map<String, Integer> depthMap = new LinkedHashMap<>();
        for (String node : topologicalOrder) {
            FormulaConfig config = formulaMap.get(node);
            if (config == null) continue;
            int maxDepDepth = 0;
            if (config.getDependsOn() != null) {
                for (String dep : config.getDependsOn()) {
                    maxDepDepth = Math.max(maxDepDepth, depthMap.getOrDefault(dep, 0));
                }
            }
            if (config.isLookup() && config.getLookup() != null) {
                String dep = config.getLookup().getSourceField();
                maxDepDepth = Math.max(maxDepDepth, depthMap.getOrDefault(dep, 0));
            }
            depthMap.put(node, maxDepDepth + 1);
        }
        return depthMap;
    }

    List<String> validateDepth(Map<String, Integer> depthMap, Set<String> fields) {
        List<String> errors = new ArrayList<>();
        for (String field : fields) {
            Integer depth = depthMap.get(field);
            if (depth != null && depth > maxDepth) {
                errors.add(UiMessages.translate("字段 [") + field + UiMessages.translate("] 公式嵌套深度 ") + depth
                    + UiMessages.translate(" 超过最大限制 ") + maxDepth);
            }
        }
        return errors;
    }
}
