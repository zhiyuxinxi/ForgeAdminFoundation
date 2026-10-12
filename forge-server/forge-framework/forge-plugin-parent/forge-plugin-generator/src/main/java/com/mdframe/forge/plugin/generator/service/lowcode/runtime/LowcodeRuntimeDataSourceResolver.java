package com.mdframe.forge.plugin.generator.service.lowcode.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.domain.entity.GenDatasource;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRuntimeDatasourceSnapshot;
import com.mdframe.forge.plugin.generator.mapper.GenDatasourceMapper;
import com.mdframe.forge.plugin.generator.service.lowcode.GeneratorRuntimeDataSourceAccess;
import com.mdframe.forge.plugin.generator.service.lowcode.RuntimeCrudConfigAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Design compatibility facade; runtime resolution and datasource ownership are separate. */
@Service
public class LowcodeRuntimeDataSourceResolver {
    private final GeneratorRuntimeDataSourceAccess datasourceAccess;
    private final RuntimeDataSourceResolution resolution;

    @Autowired
    public LowcodeRuntimeDataSourceResolver(GeneratorRuntimeDataSourceAccess datasourceAccess,
                                          ObjectMapper objectMapper) {
        this.datasourceAccess = datasourceAccess;
        this.resolution = new RuntimeDataSourceResolution(objectMapper, datasourceAccess);
    }

    /** Retains direct legacy construction for design-only consumers. */
    public LowcodeRuntimeDataSourceResolver(ObjectMapper objectMapper, GenDatasourceMapper datasourceMapper) {
        this(new GeneratorRuntimeDataSourceAccess(datasourceMapper, null), objectMapper);
    }

    public LowcodeRuntimeDataSourceContext resolve(LowcodeModelSchema modelSchema) {
        return resolution.resolve(modelSchema);
    }

    public LowcodeRuntimeDataSourceContext resolveRuntime(LowcodeRuntimeConfig config) {
        return resolution.resolveRuntime(config);
    }

    public LowcodeRuntimeDataSourceContext resolve(AiCrudConfig config) {
        return resolveRuntime(RuntimeCrudConfigAdapter.fromDesign(config));
    }

    public LowcodeRuntimeDatasourceSnapshot buildSnapshot(GenDatasource datasource, String tableName, String tableMode) {
        return datasourceAccess.buildSnapshot(datasource, tableName, tableMode);
    }

    public GenDatasource getDatasourceById(Long datasourceId) {
        return datasourceAccess.getDatasourceById(datasourceId);
    }

    public void evictDatasource(Long datasourceId, String datasourceCode) {
        datasourceAccess.evictDatasource(datasourceId, datasourceCode);
    }

    public void clearDatasourceCache() {
        datasourceAccess.clearDatasourceCache();
    }
    public RuntimeDataSourceResolution getResolution() {
        return resolution;
    }
}
