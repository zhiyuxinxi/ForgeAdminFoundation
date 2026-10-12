package com.mdframe.forge.plugin.generator.constant;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.Set;

/**
 * 数据源运行时用途和风险等级常量。
 */
public final class GenDatasourceRuntime {

    public static final String USAGE_LOWCODE_RUNTIME = "LOWCODE_RUNTIME";

    public static final String USAGE_TENANT_BUSINESS = "TENANT_BUSINESS";

    public static final String USAGE_DEVELOPER_IMPORT = "DEVELOPER_IMPORT";

    public static final String USAGE_BOTH = "BOTH";

    public static final String RISK_LOW = "LOW";

    public static final String RISK_MEDIUM = "MEDIUM";

    public static final String RISK_HIGH = "HIGH";

    private static final Set<String> USAGE_SCOPES = Set.of(
        USAGE_LOWCODE_RUNTIME,
        USAGE_TENANT_BUSINESS,
        USAGE_DEVELOPER_IMPORT,
        USAGE_BOTH
    );

    private static final Set<String> RISK_LEVELS = Set.of(RISK_LOW, RISK_MEDIUM, RISK_HIGH);

    private GenDatasourceRuntime() {
    }

    public static String normalizeUsageScope(String usageScope) {
        String value = StringUtils.defaultIfBlank(usageScope, USAGE_BOTH).trim().toUpperCase(Locale.ROOT);
        return USAGE_SCOPES.contains(value) ? value : USAGE_BOTH;
    }

    public static String normalizeUsageScopeFilter(String usageScope) {
        String value = StringUtils.trimToNull(usageScope);
        if (value == null) {
            return null;
        }
        return value.toUpperCase(Locale.ROOT);
    }

    public static String normalizeRiskLevel(String riskLevel) {
        String value = StringUtils.defaultIfBlank(riskLevel, RISK_MEDIUM).trim().toUpperCase(Locale.ROOT);
        return RISK_LEVELS.contains(value) ? value : RISK_MEDIUM;
    }
}
