package com.mdframe.forge.plugin.generator.service.formula;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Result of resolving a LOOKUP formula.
 */
public class LookupResolveResult {

    private final boolean success;
    private final boolean matched;
    private final Object value;
    private final String errorMessage;
    private final Map<String, Object> metadata;

    private LookupResolveResult(boolean success,
                                boolean matched,
                                Object value,
                                String errorMessage,
                                Map<String, Object> metadata) {
        this.success = success;
        this.matched = matched;
        this.value = value;
        this.errorMessage = errorMessage;
        this.metadata = metadata == null
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }

    public static LookupResolveResult matched(Object value, Map<String, Object> metadata) {
        return new LookupResolveResult(true, true, value, null, metadata);
    }

    public static LookupResolveResult notFound(Object value, Map<String, Object> metadata) {
        return new LookupResolveResult(true, false, value, null, metadata);
    }

    public static LookupResolveResult failure(String errorMessage, Map<String, Object> metadata) {
        return new LookupResolveResult(false, false, null, errorMessage, metadata);
    }

    public boolean isSuccess() { return success; }
    public boolean isMatched() { return matched; }
    public Object getValue() { return value; }
    public String getErrorMessage() { return errorMessage; }
    public Map<String, Object> getMetadata() { return metadata; }
}
