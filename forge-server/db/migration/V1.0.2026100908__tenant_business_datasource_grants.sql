-- Explicit platform grants only: existing default selections are not authorization.
CREATE TABLE IF NOT EXISTS sys_tenant_datasource_grant (
    tenant_id BIGINT NOT NULL,
    datasource_id BIGINT NOT NULL,
    granted_by BIGINT NOT NULL,
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id, datasource_id)
);
