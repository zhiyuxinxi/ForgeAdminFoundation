-- Preserve legacy associations; only new explicit starts bind a request and its caller-input digest.
SET @forge_flow_link_ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
           AND table_name = 'ai_business_flow_instance_link' AND column_name = 'start_request_id'),
    'SELECT 1',
    'ALTER TABLE ai_business_flow_instance_link ADD COLUMN start_request_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL'
);
PREPARE forge_flow_link_stmt FROM @forge_flow_link_ddl;
EXECUTE forge_flow_link_stmt;
DEALLOCATE PREPARE forge_flow_link_stmt;

SET @forge_flow_link_ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
           AND table_name = 'ai_business_flow_instance_link' AND column_name = 'start_request_digest'),
    'SELECT 1',
    'ALTER TABLE ai_business_flow_instance_link ADD COLUMN start_request_digest CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL'
);
PREPARE forge_flow_link_stmt FROM @forge_flow_link_ddl;
EXECUTE forge_flow_link_stmt;
DEALLOCATE PREPARE forge_flow_link_stmt;

SET @forge_flow_link_ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE()
           AND table_name = 'ai_business_flow_instance_link' AND index_name = 'uk_business_flow_start_request'),
    'SELECT 1',
    'ALTER TABLE ai_business_flow_instance_link ADD UNIQUE INDEX uk_business_flow_start_request (tenant_id, business_key, start_request_id)'
);
PREPARE forge_flow_link_stmt FROM @forge_flow_link_ddl;
EXECUTE forge_flow_link_stmt;
DEALLOCATE PREPARE forge_flow_link_stmt;
