package com.mdframe.forge.plugin.generator.service.runtime.read;

import com.mdframe.forge.plugin.generator.service.runtime.value.RuntimeFormulaContexts;
import com.mdframe.forge.plugin.generator.service.runtime.value.RuntimeStructuredValues;
import com.mdframe.forge.plugin.generator.service.runtime.value.RuntimeMoneyValues;
import com.mdframe.forge.plugin.generator.service.runtime.value.RuntimeFieldMetadata;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaRuntimeContext;
import com.mdframe.forge.lowcode.formula.runtime.port.VirtualFormulaCalculation;
import lombok.RequiredArgsConstructor;
import java.util.*;

/** Owns the fixed read-stage ordering; it never retrieves or authorizes records. */
@RequiredArgsConstructor
public final class RuntimeReadPipeline {
    private final RuntimeFieldMetadata metadata;
    private final RuntimeFieldAliases aliases;
    private final RuntimeMoneyValues money;
    private final RuntimeStructuredValues structured;
    private final RuntimePrivacyProjection privacy;
    private final VirtualFormulaCalculation virtualFormulaRuntime;
    private final RuntimeTranslationConfig translationConfig;
    private final RuntimeTranslationProjection translation;

    public void apply(List<Map<String, Object>> rows, LowcodeRuntimeConfig config) {
        aliases.aliases(rows, config);
        privacy.decrypt(rows, config.getEncryptConfig());
        money.money(rows, config);
        structured.structured(rows, config);
        applyVirtualFormulas(config, rows);
        translation.apply(rows, translationConfig.resolve(config));
        privacy.mask(rows, config.getDesensitizeConfig());
    }

    private void applyVirtualFormulas(LowcodeRuntimeConfig config, List<Map<String, Object>> records) {
        if (records == null || records.isEmpty()) return;
        LowcodeModelSchema modelSchema = metadata.model(config);
        if (modelSchema == null) return;
        for (Map<String, Object> record : records) {
            FormulaRuntimeContext ctx = RuntimeFormulaContexts.create(config, record);
            virtualFormulaRuntime.calculate(List.of(record), modelSchema, ctx);
        }
    }
}
