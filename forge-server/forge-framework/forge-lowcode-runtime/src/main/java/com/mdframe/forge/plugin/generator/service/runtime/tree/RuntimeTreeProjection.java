package com.mdframe.forge.plugin.generator.service.runtime.tree;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeTreeConfig;
import org.apache.commons.lang3.StringUtils;
import java.util.*;

/** Converts already processed rows into navigation nodes without querying or altering them. */
public final class RuntimeTreeProjection {
    public void markAncestors(List<Map<String, Object>> rows,
                                      LowcodeTreeConfig treeConfig,
                                      Set<String> ancestorKeys) {
        if (rows == null || rows.isEmpty() || ancestorKeys == null || ancestorKeys.isEmpty()) {
            return;
        }
        String keyField = treeConfig.getKeyField();
        for (Map<String, Object> row : rows) {
            String key = TreeNodeKey.normalize(row.get(keyField));
            boolean ancestor = ancestorKeys.contains(key);
            row.put("_scopeAncestor", ancestor);
            row.put("_dataScopeWritable", !ancestor);
        }
    }

    public List<Map<String, Object>> lazy(List<Map<String, Object>> rows,
                                          LowcodeTreeConfig treeConfig, Set<Object> parentsWithChildren) {
        String keyField = treeConfig.getKeyField();
        String labelField = treeConfig.getLabelField();
        String targetField = treeConfig.getTargetField();
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String key = TreeNodeKey.normalize(row.get(keyField));
            if (StringUtils.isBlank(key)) {
                continue;
            }
            Map<String, Object> node = new LinkedHashMap<>(row);
            node.putIfAbsent("key", row.get(keyField));
            node.putIfAbsent("label", resolveTreeLabel(row, labelField, keyField));
            node.putIfAbsent("targetValue", row.get(targetField));
            boolean hasChildren = parentsWithChildren.contains(row.get(keyField));
            if (hasChildren) {
                node.put("isLeaf", false);
            } else {
                node.put("isLeaf", true);
            }
            nodes.add(node);
        }
        return nodes;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> full(List<Map<String, Object>> rows, LowcodeTreeConfig treeConfig) {
        Map<String, Map<String, Object>> nodeMap = new LinkedHashMap<>();
        String keyField = treeConfig.getKeyField();
        String parentField = treeConfig.getParentField();
        String labelField = treeConfig.getLabelField();
        String targetField = treeConfig.getTargetField();
        String childrenField = treeConfig.getChildrenField();

        for (Map<String, Object> row : rows) {
            String key = TreeNodeKey.normalize(row.get(keyField));
            if (StringUtils.isBlank(key)) {
                continue;
            }
            Map<String, Object> node = new LinkedHashMap<>(row);
            node.putIfAbsent("key", row.get(keyField));
            node.putIfAbsent("label", resolveTreeLabel(row, labelField, keyField));
            node.putIfAbsent("targetValue", row.get(targetField));
            node.put(childrenField, new ArrayList<Map<String, Object>>());
            nodeMap.put(key, node);
        }

        List<Map<String, Object>> roots = new ArrayList<>();
        for (Map<String, Object> node : nodeMap.values()) {
            String key = TreeNodeKey.normalize(node.get(keyField));
            String parentKey = TreeNodeKey.normalize(node.get(parentField));
            Map<String, Object> parent = nodeMap.get(parentKey);
            if (TreeNodeKey.isRoot(parentKey) || parent == null || key.equals(parentKey)) {
                roots.add(node);
                continue;
            }
            Object children = parent.get(childrenField);
            if (children instanceof List<?> childList) {
                ((List<Map<String, Object>>) childList).add(node);
            }
        }
        normalizeFullTreeLeafState(roots, childrenField);
        return roots;
    }

    @SuppressWarnings("unchecked")
    private void normalizeFullTreeLeafState(List<Map<String, Object>> nodes, String childrenField) {
        for (Map<String, Object> node : nodes) {
            Object children = node.get(childrenField);
            if (children instanceof List<?> childList && !childList.isEmpty()) {
                node.put("isLeaf", false);
                normalizeFullTreeLeafState((List<Map<String, Object>>) childList, childrenField);
                continue;
            }
            node.put("isLeaf", true);
            node.remove(childrenField);
        }
    }

    private Object resolveTreeLabel(Map<String, Object> row, String labelField, String keyField) {
        List<String> candidates = Arrays.asList(labelField, "label", "name", "title", "treeName", "deptName", "orgName", keyField);
        for (String candidate : candidates) {
            if (StringUtils.isBlank(candidate)) {
                continue;
            }
            Object value = row.get(candidate);
            if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
                return value;
            }
        }
        return "未命名节点";
    }
}
