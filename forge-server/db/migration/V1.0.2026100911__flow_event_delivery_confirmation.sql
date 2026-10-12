-- Only new events with an explicit trusted consumer binding enter this delivery table.
-- No historical outbox rows are backfilled or replayed.
CREATE TABLE IF NOT EXISTS sys_flow_event_delivery (
    event_id VARCHAR(64) NOT NULL,
    tenant_id BIGINT NOT NULL,
    consumer_id VARCHAR(100) NOT NULL,
    status VARCHAR(24) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(3) NOT NULL,
    claimed_until DATETIME(3) NULL,
    claim_id VARCHAR(64) NULL,
    acknowledged_at DATETIME(3) NULL,
    last_error VARCHAR(100) NULL,
    PRIMARY KEY (event_id),
    KEY idx_flow_delivery_due (status, next_attempt_at, claimed_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- A resource only: service accounts and their role grants are a private deployment responsibility.
INSERT INTO sys_resource (
    tenant_id, resource_name, resource_name_en, parent_id, resource_type, sort,
    is_external, open_target, is_public, menu_status, visible, perms, api_method, api_url,
    keep_alive, always_show, create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, '确认流程事件消费', 'Acknowledge flow event consumption', 0, 4, 0,
       0, '_self', 0, 1, 0, 'flow:event:ack', 'POST', '/api/flow/events/ack',
       0, 0, 1, NOW(), 1, NOW(), 1, 'pc'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_resource
    WHERE resource_type = 4 AND perms = 'flow:event:ack' AND api_method = 'POST'
      AND api_url = '/api/flow/events/ack' AND client_code = 'pc' AND del_flag = 0
);
