-- API resources are endpoint records: one business permission can protect several methods/paths.
-- Menus/buttons keep their permission identity; client isolation and primary-key tombstones remain.
-- Reuse an already installed compatible resource key without rewriting its origin or any seed data.
SET @resource_identity_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_resource'
      AND COLUMN_NAME = 'resource_unique_key'
);
SET @sql = IF(
    @resource_identity_exists = 0,
    'ALTER TABLE sys_resource ADD COLUMN resource_unique_key VARCHAR(400) GENERATED ALWAYS AS (CASE WHEN resource_type = 4 THEN CONCAT(COALESCE(perms, ''''), ''|'', UPPER(COALESCE(api_method, '''')), ''|'', COALESCE(api_url, '''')) ELSE perms END) STORED COMMENT ''资源有效唯一键：API按权限+方法+路径，其余按权限''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @resource_index_columns = (
    SELECT COALESCE(GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ','), '')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_resource'
      AND INDEX_NAME = 'uk_tenant_resource_active'
);
SET @resource_index_is_unique = (
    SELECT COALESCE(MIN(NON_UNIQUE) = 0, 0)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_resource'
      AND INDEX_NAME = 'uk_tenant_resource_active'
);
SET @sql = IF(
    @resource_index_columns = 'tenant_id,resource_type,resource_unique_key,client_code,del_flag'
        AND @resource_index_is_unique = 1,
    'SELECT 1',
    CONCAT(
        'ALTER TABLE sys_resource ',
        IF(@resource_index_columns = '', '', 'DROP INDEX uk_tenant_resource_active, '),
        'ADD UNIQUE INDEX uk_tenant_resource_active (tenant_id, resource_type, resource_unique_key, client_code, del_flag)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
