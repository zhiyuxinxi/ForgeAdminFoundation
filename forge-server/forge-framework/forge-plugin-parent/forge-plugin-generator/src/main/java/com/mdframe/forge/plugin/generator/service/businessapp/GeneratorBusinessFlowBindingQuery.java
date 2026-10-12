package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.lowcode.api.binding.BusinessFlowBinding;
import com.mdframe.forge.lowcode.api.binding.BusinessFlowBindingQuery;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Runtime projection backed by the existing tenant-scoped designer query. */
@Component
@RequiredArgsConstructor
public class GeneratorBusinessFlowBindingQuery implements BusinessFlowBindingQuery {
    private final BusinessBindingMapper bindings;

    @Override
    public Optional<BusinessFlowBinding> findForObject(Long tenantId, String objectCode) {
        if (tenantId == null || tenantId <= 0 || objectCode == null || objectCode.isBlank()) {
            throw new IllegalArgumentException("An authorized tenant and object code are required");
        }
        AiBusinessBinding binding = bindings.selectBindingByTypeAndCode(tenantId, "OBJECT", objectCode, "FLOW");
        if (binding == null) {
            binding = bindings.selectBindingByTypeAndCode(tenantId, "OBJECT", objectCode, "APPROVAL");
        }
        return Optional.ofNullable(binding)
                .map(value -> new BusinessFlowBinding(value.getId(), value.getBindingConfig()));
    }
}
