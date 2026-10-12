-- Public foundation display translations; no business values, permissions or existing labels are changed.

SET @bilingual_ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sys_dict_data' AND COLUMN_NAME='dict_label_en')=0, 'ALTER TABLE `sys_dict_data` ADD COLUMN `dict_label_en` VARCHAR(255) NULL COMMENT ''English display text'' AFTER `dict_label`', 'SELECT 1');
PREPARE bilingual_stmt FROM @bilingual_ddl;
EXECUTE bilingual_stmt;
DEALLOCATE PREPARE bilingual_stmt;

SET @bilingual_ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sys_resource' AND COLUMN_NAME='resource_name_en')=0, 'ALTER TABLE `sys_resource` ADD COLUMN `resource_name_en` VARCHAR(255) NULL COMMENT ''English display text'' AFTER `resource_name`', 'SELECT 1');
PREPARE bilingual_stmt FROM @bilingual_ddl;
EXECUTE bilingual_stmt;
DEALLOCATE PREPARE bilingual_stmt;

SET @bilingual_ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sys_excel_column_config' AND COLUMN_NAME='column_name_en')=0, 'ALTER TABLE `sys_excel_column_config` ADD COLUMN `column_name_en` VARCHAR(255) NULL COMMENT ''English display text'' AFTER `column_name`', 'SELECT 1');
PREPARE bilingual_stmt FROM @bilingual_ddl;
EXECUTE bilingual_stmt;
DEALLOCATE PREPARE bilingual_stmt;

SET @bilingual_ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gen_table' AND COLUMN_NAME='table_comment_en')=0, 'ALTER TABLE `gen_table` ADD COLUMN `table_comment_en` VARCHAR(255) NULL COMMENT ''English display text'' AFTER `table_comment`', 'SELECT 1');
PREPARE bilingual_stmt FROM @bilingual_ddl;
EXECUTE bilingual_stmt;
DEALLOCATE PREPARE bilingual_stmt;

SET @bilingual_ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gen_table' AND COLUMN_NAME='function_name_en')=0, 'ALTER TABLE `gen_table` ADD COLUMN `function_name_en` VARCHAR(255) NULL COMMENT ''English display text'' AFTER `function_name`', 'SELECT 1');
PREPARE bilingual_stmt FROM @bilingual_ddl;
EXECUTE bilingual_stmt;
DEALLOCATE PREPARE bilingual_stmt;

SET @bilingual_ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gen_table_column' AND COLUMN_NAME='column_comment_en')=0, 'ALTER TABLE `gen_table_column` ADD COLUMN `column_comment_en` VARCHAR(255) NULL COMMENT ''English display text'' AFTER `column_comment`', 'SELECT 1');
PREPARE bilingual_stmt FROM @bilingual_ddl;
EXECUTE bilingual_stmt;
DEALLOCATE PREPARE bilingual_stmt;

SET @bilingual_ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gen_table_column' AND COLUMN_NAME='dict_labels_i18n')=0, 'ALTER TABLE `gen_table_column` ADD COLUMN `dict_labels_i18n` JSON NULL COMMENT ''Display translations keyed by dictionary value and locale''', 'SELECT 1');
PREPARE bilingual_stmt FROM @bilingual_ddl;
EXECUTE bilingual_stmt;
DEALLOCATE PREPARE bilingual_stmt;

SET @bilingual_ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sys_excel_export_config' AND COLUMN_NAME='export_name_en')=0, 'ALTER TABLE `sys_excel_export_config` ADD COLUMN `export_name_en` VARCHAR(255) NULL COMMENT ''English display text'' AFTER `export_name`', 'SELECT 1');
PREPARE bilingual_stmt FROM @bilingual_ddl;
EXECUTE bilingual_stmt;
DEALLOCATE PREPARE bilingual_stmt;

SET @bilingual_ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sys_excel_export_config' AND COLUMN_NAME='sheet_name_en')=0, 'ALTER TABLE `sys_excel_export_config` ADD COLUMN `sheet_name_en` VARCHAR(255) NULL COMMENT ''English display text'' AFTER `sheet_name`', 'SELECT 1');
PREPARE bilingual_stmt FROM @bilingual_ddl;
EXECUTE bilingual_stmt;
DEALLOCATE PREPARE bilingual_stmt;

SET @bilingual_ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sys_excel_export_config' AND COLUMN_NAME='file_name_template_en')=0, 'ALTER TABLE `sys_excel_export_config` ADD COLUMN `file_name_template_en` VARCHAR(255) NULL COMMENT ''English display text'' AFTER `file_name_template`', 'SELECT 1');
PREPARE bilingual_stmt FROM @bilingual_ddl;
EXECUTE bilingual_stmt;
DEALLOCATE PREPARE bilingual_stmt;
