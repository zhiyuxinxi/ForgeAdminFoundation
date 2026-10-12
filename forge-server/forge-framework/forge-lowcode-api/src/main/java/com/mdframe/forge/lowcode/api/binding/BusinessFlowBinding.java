package com.mdframe.forge.lowcode.api.binding;

/** Read-only runtime projection; no designer entity, mapper, or mutation methods escape. */
public record BusinessFlowBinding(Long bindingId, String bindingConfig) {
}
