-- Only newly recorded attempts receive a deadline. Legacy pending is not implicit confirmation.
SET @forge_document_deadline_ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'ai_knowledge_document'
             AND column_name = 'process_deadline'),
    'SELECT 1',
    'ALTER TABLE ai_knowledge_document ADD COLUMN process_deadline DATETIME NULL COMMENT ''Current queued/running attempt deadline'''
);
PREPARE forge_document_deadline_stmt FROM @forge_document_deadline_ddl;
EXECUTE forge_document_deadline_stmt;
DEALLOCATE PREPARE forge_document_deadline_stmt;

SET @forge_document_deadline_index = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics
           WHERE table_schema = DATABASE() AND table_name = 'ai_knowledge_document'
             AND index_name = 'idx_document_process_deadline'),
    'SELECT 1',
    'CREATE INDEX idx_document_process_deadline ON ai_knowledge_document (del_flag, process_status, process_deadline)'
);
PREPARE forge_document_deadline_index_stmt FROM @forge_document_deadline_index;
EXECUTE forge_document_deadline_index_stmt;
DEALLOCATE PREPARE forge_document_deadline_index_stmt;
