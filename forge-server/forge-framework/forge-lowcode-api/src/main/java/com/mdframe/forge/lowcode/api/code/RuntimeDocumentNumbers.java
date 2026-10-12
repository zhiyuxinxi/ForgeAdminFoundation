package com.mdframe.forge.lowcode.api.code;

import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import java.util.Map;

/** Resolves one trusted configuration snapshot, then allocates from that same snapshot. */
public interface RuntimeDocumentNumbers {
    RuntimeDocumentNumber resolve(LowcodeRuntimeConfig config);

    String generate(RuntimeDocumentNumber number, Map<String, Object> recordData);
}
