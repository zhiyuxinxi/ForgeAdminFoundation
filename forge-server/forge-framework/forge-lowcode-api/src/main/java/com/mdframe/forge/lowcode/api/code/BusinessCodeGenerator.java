package com.mdframe.forge.lowcode.api.code;

import java.util.Map;

/** Generates one business code; rule editing and persistence are not runtime contracts. */
public interface BusinessCodeGenerator {
    /** Tenant and system variables come from the trusted server context, not this map. */
    String generate(String ruleCode, Map<String, Object> context);
}
