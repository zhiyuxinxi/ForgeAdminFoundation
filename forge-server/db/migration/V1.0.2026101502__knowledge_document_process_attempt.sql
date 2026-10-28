-- Null means no processing request was recorded. Never infer confirmation for legacy pending rows.
SET @forge_document_attempt_ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'ai_knowledge_document'
             AND column_name = 'process_attempt'),
    'SELECT 1',
    'ALTER TABLE ai_knowledge_document ADD COLUMN process_attempt VARCHAR(36) NULL COMMENT ''Processing request identity; null means not requested'''
);
PREPARE forge_document_attempt_stmt FROM @forge_document_attempt_ddl;
EXECUTE forge_document_attempt_stmt;
DEALLOCATE PREPARE forge_document_attempt_stmt;
