package com.mdframe.forge.plugin.generator.service.lowcode.runtime;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeAuditStrategy;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeLogicDeleteStrategy;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePrimaryKeyStrategy;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRuntimeDatasourceSnapshot;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeTenantStrategy;
import lombok.Data;

/**
 * 低代码运行时数据源解析结果。
 */
@Data
public class LowcodeRuntimeDataSourceContext {

    private boolean master;

    private Long datasourceId;

    private String datasourceCode;

    private String datasourceName;

    private String dbType;

    private String tableName;

    private String tableMode;

    private boolean allowWrite;

    private boolean allowDdl;

    private boolean readonly;

    private String riskLevel;

    private LowcodeRuntimeDatasourceSnapshot snapshot;

    private LowcodePrimaryKeyStrategy primaryKey;

    private LowcodeTenantStrategy tenantStrategy;

    private LowcodeAuditStrategy auditStrategy;

    private LowcodeLogicDeleteStrategy logicDeleteStrategy;

    public static LowcodeRuntimeDataSourceContext master(String tableName) {
        LowcodeRuntimeDataSourceContext context = new LowcodeRuntimeDataSourceContext();
        context.setMaster(true);
        context.setTableName(tableName);
        context.setAllowWrite(true);
        context.setAllowDdl(true);
        context.setReadonly(false);
        context.setDbType("MySQL");
        return context;
    }
}
