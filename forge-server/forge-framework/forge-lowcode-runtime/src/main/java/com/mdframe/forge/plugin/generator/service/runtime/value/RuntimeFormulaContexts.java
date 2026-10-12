package com.mdframe.forge.plugin.generator.service.runtime.value;

import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaRuntimeContext;
import java.util.*;

/** Builds the existing tenant, suite, object and row formula context. */
public final class RuntimeFormulaContexts {
    private RuntimeFormulaContexts() {
    }

    public static FormulaRuntimeContext create(LowcodeRuntimeConfig config,
                                                        Map<String, Object> currentRow) {
        Long tenantId = config.getTenantId();
        String suiteCode = extractSuiteCode(config);
        String objectCode = config.getObjectCode();
        return new FormulaRuntimeContext(tenantId, suiteCode, objectCode, currentRow);
    }

    private static String extractSuiteCode(LowcodeRuntimeConfig config) {
        String configKey = config.getConfigKey();
        if (org.apache.commons.lang3.StringUtils.isBlank(configKey)) {
            return "default";
        }
        int idx = configKey.indexOf("_");
        return idx > 0 ? configKey.substring(0, idx) : configKey;
    }
}
