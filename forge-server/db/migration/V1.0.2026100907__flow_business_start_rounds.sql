-- B07: retain every flow round; legacy records become round 1.
-- Names checked against the public source and both customer directories by the delivery owner.
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_business' AND COLUMN_NAME = 'round_no');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE sys_flow_business ADD COLUMN round_no INT NOT NULL DEFAULT 1', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_business' AND COLUMN_NAME = 'start_request_id');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE sys_flow_business ADD COLUMN start_request_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_business' AND COLUMN_NAME = 'previous_process_instance_id');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE sys_flow_business ADD COLUMN previous_process_instance_id VARCHAR(64) NULL', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_business' AND COLUMN_NAME = 'start_request_digest');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE sys_flow_business ADD COLUMN start_request_digest CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Install replacement constraints before removing the old single-round constraint.
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_business' AND INDEX_NAME = 'uk_flow_business_round');
SET @sql = IF(@index_exists = 0, 'ALTER TABLE sys_flow_business ADD UNIQUE INDEX uk_flow_business_round (tenant_id, business_key, round_no)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_business' AND INDEX_NAME = 'uk_flow_business_start_request');
SET @sql = IF(@index_exists = 0, 'ALTER TABLE sys_flow_business ADD UNIQUE INDEX uk_flow_business_start_request (tenant_id, business_key, start_request_id)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_business' AND INDEX_NAME = 'uk_flow_business_tenant_key');
SET @sql = IF(@index_exists > 0, 'ALTER TABLE sys_flow_business DROP INDEX uk_flow_business_tenant_key', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_business' AND INDEX_NAME = 'uk_business_key');
SET @sql = IF(@index_exists > 0, 'ALTER TABLE sys_flow_business DROP INDEX uk_business_key', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
