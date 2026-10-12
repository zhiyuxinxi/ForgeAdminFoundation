-- Framework delivery records only. No dispatcher is enabled by this migration.
CREATE TABLE IF NOT EXISTS sys_flow_event_outbox (
    event_id VARCHAR(64) NOT NULL,
    tenant_id BIGINT NOT NULL,
    process_instance_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    process_def_key VARCHAR(255) NOT NULL,
    destination_type VARCHAR(16) NOT NULL,
    destination VARCHAR(2048) NOT NULL,
    payload LONGTEXT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    delivery_status VARCHAR(24) NOT NULL DEFAULT 'RECORDED',
    PRIMARY KEY (event_id),
    CONSTRAINT uk_flow_terminal_event UNIQUE (tenant_id, process_instance_id, event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
