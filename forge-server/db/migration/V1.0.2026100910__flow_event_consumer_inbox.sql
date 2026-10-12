-- Each callback marker commits with that callback's local business transaction.
CREATE TABLE IF NOT EXISTS sys_flow_event_inbox (
    tenant_id BIGINT NOT NULL,
    consumer_id VARCHAR(100) NOT NULL,
    event_id VARCHAR(64) NOT NULL,
    callback_id VARCHAR(64) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    status VARCHAR(24) NOT NULL,
    completed_at DATETIME(3) NULL,
    PRIMARY KEY (tenant_id, consumer_id, event_id, callback_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
