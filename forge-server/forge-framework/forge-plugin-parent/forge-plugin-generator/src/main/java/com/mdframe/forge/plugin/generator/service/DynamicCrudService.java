package com.mdframe.forge.plugin.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.lowcode.runtime.service.RuntimeCrudService;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessDocumentConfigService;
import com.mdframe.forge.plugin.generator.service.businessapp.CodeRuleService;
import com.mdframe.forge.plugin.generator.service.businessapp.document.RuntimeDocumentNumbersAdapter;
import com.mdframe.forge.plugin.generator.service.crypto.LowcodeEncryptConfigParser;
import com.mdframe.forge.plugin.generator.service.formula.StoredAggregateRefreshService;
import com.mdframe.forge.plugin.generator.service.formula.StoredFormulaRuntime;
import com.mdframe.forge.plugin.generator.service.formula.VirtualFormulaRuntime;
import com.mdframe.forge.plugin.generator.service.lowcode.RuntimeCrudConfigAdapter;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceResolver;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeStrategyFactory;
import com.mdframe.forge.starter.crypto.persistence.PersistentCryptoService;
import com.mdframe.forge.starter.trans.spi.DictValueProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Preserves the legacy Spring entry and design configuration result. */
@Service
public class DynamicCrudService extends RuntimeCrudService {
    private final RuntimeCrudConfigAdapter legacyConfigs;

    @Autowired
    public DynamicCrudService(DynamicCrudRepository repository,
                              AiCrudConfigService configService,
                              ObjectMapper objectMapper,
                              DictValueProvider dictValueProvider,
                              DesensitizeStrategyFactory desensitizeStrategyFactory,
                              PersistentCryptoService persistentCryptoService,
                              LowcodeEncryptConfigParser encryptConfigParser,
                              DynamicDataScopeService dynamicDataScopeService,
                              BusinessDocumentConfigService documentConfigService,
                              CodeRuleService codeRuleService,
                              StoredAggregateRefreshService storedAggregateRefreshService,
                              StoredFormulaRuntime storedFormulaRuntime,
                              VirtualFormulaRuntime virtualFormulaRuntime,
                              LowcodeRuntimeDataSourceResolver runtimeDataSourceResolver) {
        this(repository, new RuntimeCrudConfigAdapter(configService), objectMapper, dictValueProvider,
                desensitizeStrategyFactory, persistentCryptoService, encryptConfigParser, dynamicDataScopeService,
                documentConfigService, codeRuleService, storedAggregateRefreshService, storedFormulaRuntime,
                virtualFormulaRuntime, runtimeDataSourceResolver);
    }

    private DynamicCrudService(DynamicCrudRepository repository,
                               RuntimeCrudConfigAdapter runtimeConfigs,
                               ObjectMapper objectMapper,
                               DictValueProvider dictValueProvider,
                               DesensitizeStrategyFactory desensitizeStrategyFactory,
                               PersistentCryptoService persistentCryptoService,
                               LowcodeEncryptConfigParser encryptConfigParser,
                               DynamicDataScopeService dynamicDataScopeService,
                               BusinessDocumentConfigService documentConfigService,
                               CodeRuleService codeRuleService,
                               StoredAggregateRefreshService storedAggregateRefreshService,
                               StoredFormulaRuntime storedFormulaRuntime,
                               VirtualFormulaRuntime virtualFormulaRuntime,
                               LowcodeRuntimeDataSourceResolver runtimeDataSourceResolver) {
        super(repository, runtimeConfigs, objectMapper, dictValueProvider, desensitizeStrategyFactory,
                persistentCryptoService, encryptConfigParser, dynamicDataScopeService,
                new RuntimeDocumentNumbersAdapter(documentConfigService), codeRuleService,
                storedAggregateRefreshService, storedFormulaRuntime, virtualFormulaRuntime,
                runtimeDataSourceResolver == null ? null : runtimeDataSourceResolver.getResolution());
        this.legacyConfigs = runtimeConfigs;
    }

    public AiCrudConfig getRuntimeConfig(String configKey) {
        return legacyConfigs.loadLegacy(configKey);
    }
}
