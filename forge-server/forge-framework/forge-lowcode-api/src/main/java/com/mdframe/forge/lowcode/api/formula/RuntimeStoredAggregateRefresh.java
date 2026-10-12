package com.mdframe.forge.lowcode.api.formula;

import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import java.util.Map;

/** Refreshes the existing stored aggregates after a runtime record change. */
public interface RuntimeStoredAggregateRefresh {
    void refreshRuntimeAfterChildInsert(LowcodeRuntimeConfig config, Map<String, Object> record);

    void refreshRuntimeAfterChildUpdate(LowcodeRuntimeConfig config,
                                       Map<String, Object> beforeRecord,
                                       Map<String, Object> afterRecord);

    void refreshRuntimeAfterChildDelete(LowcodeRuntimeConfig config, Map<String, Object> beforeRecord);

    void refreshRuntimeRecord(LowcodeRuntimeConfig config, Object recordId);
}
