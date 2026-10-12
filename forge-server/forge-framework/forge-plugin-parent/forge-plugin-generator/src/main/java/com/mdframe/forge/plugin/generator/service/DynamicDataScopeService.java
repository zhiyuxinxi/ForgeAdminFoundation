package com.mdframe.forge.plugin.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.lowcode.runtime.service.RuntimeDataScopeService;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodePolicyService;
import com.mdframe.forge.plugin.generator.service.lowcode.RuntimeCrudConfigAdapter;
import com.mdframe.forge.starter.datascope.context.DataScopeContext;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import org.springframework.stereotype.Service;

/** Design entity compatibility for the single runtime data-scope implementation. */
@Service
public class DynamicDataScopeService extends RuntimeDataScopeService {
    public DynamicDataScopeService(IDataScopeService dataScopeService,
                                   DynamicCrudRepository repository,
                                   ObjectMapper objectMapper,
                                   LowcodePolicyService policyService) {
        super(dataScopeService, repository, objectMapper, policyService);
    }

    public DynamicCrudRepository.SqlCondition buildCondition(AiCrudConfig config, String tableName, String tableAlias) {
        return buildRuntimeCondition(RuntimeCrudConfigAdapter.fromDesign(config), tableName, tableAlias);
    }

    public DynamicCrudRepository.SqlCondition buildCondition(AiCrudConfig config, String tableName,
                                                             String tableAlias, DataScopeContext explicitContext) {
        return buildRuntimeCondition(RuntimeCrudConfigAdapter.fromDesign(config), tableName, tableAlias, explicitContext);
    }
}
