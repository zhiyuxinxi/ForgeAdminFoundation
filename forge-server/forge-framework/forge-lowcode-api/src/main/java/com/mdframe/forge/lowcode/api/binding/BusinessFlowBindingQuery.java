package com.mdframe.forge.lowcode.api.binding;

import java.util.Optional;

/** Reads an object's flow configuration without exposing designer persistence. */
public interface BusinessFlowBindingQuery {
    /**
     * The caller supplies its already-authorized server tenant, never a request override.
     * Preserves FLOW precedence and uses APPROVAL only when no FLOW row exists.
     * This configuration read does not authorize starting or acting on a process.
     */
    Optional<BusinessFlowBinding> findForObject(Long tenantId, String objectCode);
}
