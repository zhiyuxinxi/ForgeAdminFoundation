package com.mdframe.forge.lowcode.api.datasource;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRuntimeDatasourceSnapshot;
import javax.sql.DataSource;

/** Runtime access to current datasource policy and a borrowed, externally owned pool. */
public interface RuntimeDataSourceAccess {
    LowcodeRuntimeDatasourceSnapshot resolveSnapshot(Long datasourceId, String datasourceCode,
                                                     String tableName, String tableMode);

    /** Called only when JDBC is needed. The caller must not close the returned pool. */
    DataSource dataSource(Long datasourceId);
}
