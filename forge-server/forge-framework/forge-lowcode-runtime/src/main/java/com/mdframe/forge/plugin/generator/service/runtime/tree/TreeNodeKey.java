package com.mdframe.forge.plugin.generator.service.runtime.tree;

import org.apache.commons.lang3.StringUtils;

/** Identity normalization shared by tree retrieval and projection. */
final class TreeNodeKey {
    private TreeNodeKey() {
    }

    static boolean isRoot(String parentKey) {
        return StringUtils.isBlank(parentKey) || "0".equals(parentKey) || "null".equalsIgnoreCase(parentKey);
    }

    static String normalize(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return StringUtils.isBlank(text) ? null : text;
    }
}
