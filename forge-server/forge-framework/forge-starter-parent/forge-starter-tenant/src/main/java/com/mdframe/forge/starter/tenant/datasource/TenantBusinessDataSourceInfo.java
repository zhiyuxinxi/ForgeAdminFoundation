package com.mdframe.forge.starter.tenant.datasource;

import org.apache.commons.lang3.StringUtils;

/**
 * 当前租户业务数据源解析结果。
 */
public class TenantBusinessDataSourceInfo {

    private static final TenantBusinessDataSourceInfo MASTER = builder().master(true).build();

    private final boolean master;

    private final String dsKey;

    private final Long tenantId;

    private final Long datasourceId;

    private final String datasourceCode;

    private final String datasourceName;

    private final String dbType;

    private final boolean readonly;

    private final boolean allowWrite;

    private final String riskLevel;

    private TenantBusinessDataSourceInfo(Builder builder) {
        this.master = builder.master;
        this.dsKey = builder.dsKey;
        this.tenantId = builder.tenantId;
        this.datasourceId = builder.datasourceId;
        this.datasourceCode = builder.datasourceCode;
        this.datasourceName = builder.datasourceName;
        this.dbType = builder.dbType;
        this.readonly = builder.readonly;
        this.allowWrite = builder.allowWrite;
        this.riskLevel = builder.riskLevel;
    }

    public static TenantBusinessDataSourceInfo master() {
        return MASTER;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean shouldRoute() {
        return !master && StringUtils.isNotBlank(dsKey);
    }

    public boolean isMaster() {
        return master;
    }

    public String getDsKey() {
        return dsKey;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getDatasourceId() {
        return datasourceId;
    }

    public String getDatasourceCode() {
        return datasourceCode;
    }

    public String getDatasourceName() {
        return datasourceName;
    }

    public String getDbType() {
        return dbType;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public boolean isAllowWrite() {
        return allowWrite;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public static class Builder {

        private boolean master;

        private String dsKey;

        private Long tenantId;

        private Long datasourceId;

        private String datasourceCode;

        private String datasourceName;

        private String dbType;

        private boolean readonly;

        private boolean allowWrite;

        private String riskLevel;

        public Builder master(boolean master) {
            this.master = master;
            return this;
        }

        public Builder dsKey(String dsKey) {
            this.dsKey = dsKey;
            return this;
        }

        public Builder tenantId(Long tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder datasourceId(Long datasourceId) {
            this.datasourceId = datasourceId;
            return this;
        }

        public Builder datasourceCode(String datasourceCode) {
            this.datasourceCode = datasourceCode;
            return this;
        }

        public Builder datasourceName(String datasourceName) {
            this.datasourceName = datasourceName;
            return this;
        }

        public Builder dbType(String dbType) {
            this.dbType = dbType;
            return this;
        }

        public Builder readonly(boolean readonly) {
            this.readonly = readonly;
            return this;
        }

        public Builder allowWrite(boolean allowWrite) {
            this.allowWrite = allowWrite;
            return this;
        }

        public Builder riskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
            return this;
        }

        public TenantBusinessDataSourceInfo build() {
            return new TenantBusinessDataSourceInfo(this);
        }
    }
}
