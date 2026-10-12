package com.mdframe.forge.plugin.system.datasource;

import lombok.Data;

/** Non-credential datasource metadata used by platform grants and runtime admission. */
@Data
public class TenantDatasourceOption {
    private Long datasourceId;
    private String datasourceCode;
    private String datasourceName;
    private String usageScope;
    private Integer isEnabled;
    private Integer readonly;
    private Integer allowRuntimeWrite;
    private boolean granted;

    public boolean isSelectable() {
        return Integer.valueOf(1).equals(isEnabled)
                && Integer.valueOf(0).equals(readonly)
                && Integer.valueOf(1).equals(allowRuntimeWrite)
                && ("TENANT_BUSINESS".equals(usageScope) || "BOTH".equals(usageScope))
                && datasourceCode != null && !datasourceCode.isBlank()
                && !"master".equals(datasourceCode);
    }
}
