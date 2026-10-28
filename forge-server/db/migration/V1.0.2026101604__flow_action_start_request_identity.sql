-- Existing reservations retain NULL and their legacy remote-start semantics.
-- New START/SUBMIT reservations persist their identity before invoking Flow.
SET @forge_flow_start_request_ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'ai_capability_flow_action_log'
             AND column_name = 'flow_start_request_id'),
    'SELECT 1',
    'ALTER TABLE ai_capability_flow_action_log ADD COLUMN flow_start_request_id VARCHAR(64) NULL COMMENT ''Immutable workflow start request identity'''
);
PREPARE forge_flow_start_request_stmt FROM @forge_flow_start_request_ddl;
EXECUTE forge_flow_start_request_stmt;
DEALLOCATE PREPARE forge_flow_start_request_stmt;
