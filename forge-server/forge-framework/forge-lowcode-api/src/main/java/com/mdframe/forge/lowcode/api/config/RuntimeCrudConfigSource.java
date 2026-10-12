package com.mdframe.forge.lowcode.api.config;

/** Loads an admitted configuration snapshot; design persistence stays in its adapter. */
public interface RuntimeCrudConfigSource {
    LowcodeRuntimeConfig load(String configKey);

    LowcodeRuntimeConfig loadAllowDraft(String configKey);
}
