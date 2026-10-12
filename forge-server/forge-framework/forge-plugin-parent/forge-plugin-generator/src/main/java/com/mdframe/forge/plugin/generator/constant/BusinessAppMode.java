package com.mdframe.forge.plugin.generator.constant;

import org.apache.commons.lang3.StringUtils;

import java.util.Set;

/**
 * 业务访问入口使用模式。
 */
public final class BusinessAppMode {

    public static final String DYNAMIC_RENDER = "DYNAMIC_RENDER";
    public static final String CODE_DOWNLOAD = "CODE_DOWNLOAD";

    private static final Set<String> MODES = Set.of(DYNAMIC_RENDER, CODE_DOWNLOAD);

    private BusinessAppMode() {
    }

    public static String normalize(Object value) {
        String mode = value == null ? null : StringUtils.trimToNull(String.valueOf(value));
        mode = StringUtils.defaultIfBlank(mode, DYNAMIC_RENDER).toUpperCase();
        return MODES.contains(mode) ? mode : DYNAMIC_RENDER;
    }

    public static boolean isCodeDownload(Object value) {
        return CODE_DOWNLOAD.equals(normalize(value));
    }
}
