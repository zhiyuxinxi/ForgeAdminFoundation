package com.mdframe.forge.plugin.generator.service.businessapp.document;

import com.mdframe.forge.lowcode.api.code.RuntimeDocumentNumber;
import com.mdframe.forge.lowcode.api.code.RuntimeDocumentNumbers;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessDocumentConfig;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessDocumentConfigService;
import org.apache.commons.lang3.StringUtils;
import java.util.Map;

/** Owns design lookup and projects only the selected document-number values. */
public final class RuntimeDocumentNumbersAdapter implements RuntimeDocumentNumbers {
    private final BusinessDocumentConfigService documents;

    public RuntimeDocumentNumbersAdapter(BusinessDocumentConfigService documents) {
        this.documents = documents;
    }

    @Override
    public RuntimeDocumentNumber resolve(LowcodeRuntimeConfig config) {
        AiBusinessDocumentConfig selected = documents.selectEnabledByConfigKey(
                config.getTenantId(), config.getConfigKey());
        if (selected == null && StringUtils.isNotBlank(config.getObjectCode())) {
            selected = documents.selectEnabledByObjectCode(config.getTenantId(), config.getObjectCode());
        }
        if (selected == null) {
            return null;
        }
        String field = documents.resolveRuntimeDocumentNoField(selected, config);
        return new RuntimeDocumentNumber(selected.getTenantId(), selected.getSuiteCode(), selected.getObjectCode(),
                selected.getDocumentEnabled(), selected.getDocumentNoRule(), field);
    }

    @Override
    public String generate(RuntimeDocumentNumber number, Map<String, Object> recordData) {
        return documents.generateRuntimeDocumentNo(number, recordData);
    }
}
