-- 报表素材独立表迁移脚本
-- 说明：
-- 1. 文件本体和通用文件元数据仍保留在 sys_file_metadata。
-- 2. ai_report_material 维护报表素材与 file_id 的关系、素材分类、公共/私有和素材所有者。
-- 3. 文件名、大小、MIME 等文件本体信息继续从 sys_file_metadata 读取。

CREATE TABLE IF NOT EXISTS `ai_report_material`
(
    `id`                bigint       NOT NULL COMMENT '主键ID',
    `tenant_id`         bigint       NOT NULL DEFAULT '1' COMMENT '租户ID',
    `file_id`           varchar(64)  NOT NULL COMMENT '通用文件ID',
    `material_category` varchar(50)  NOT NULL DEFAULT 'background' COMMENT '素材分类(background/panel/icon/illustration)',
    `is_private`        tinyint(1)   NOT NULL DEFAULT '1' COMMENT '是否私有(1私有 0公共)',
    `status`            tinyint(1)   NOT NULL DEFAULT '1' COMMENT '状态(1正常 0删除)',
    `create_by`         bigint                DEFAULT NULL COMMENT '创建者',
    `create_time`       datetime              DEFAULT NULL COMMENT '创建时间',
    `create_dept`       bigint                DEFAULT NULL COMMENT '创建部门',
    `update_by`         bigint                DEFAULT NULL COMMENT '更新者',
    `update_time`       datetime              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_report_material_file` (`tenant_id`, `file_id`),
    KEY `idx_report_material_category` (`tenant_id`, `material_category`, `status`),
    KEY `idx_report_material_scope` (`tenant_id`, `is_private`, `create_by`, `status`),
    KEY `idx_report_material_create_by` (`create_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表素材表';

SET @report_material_missing_is_private = (
    SELECT IF(COUNT(1) = 0, 1, 0)
    FROM `INFORMATION_SCHEMA`.`COLUMNS`
    WHERE `TABLE_SCHEMA` = DATABASE()
      AND `TABLE_NAME` = 'ai_report_material'
      AND `COLUMN_NAME` = 'is_private'
);

SET @report_material_add_is_private_sql = IF(
    @report_material_missing_is_private = 1,
    'ALTER TABLE `ai_report_material` ADD COLUMN `is_private` tinyint(1) NOT NULL DEFAULT 1 COMMENT ''是否私有(1私有 0公共)'' AFTER `material_category`',
    'SELECT 1'
);

PREPARE report_material_add_is_private_stmt FROM @report_material_add_is_private_sql;
EXECUTE report_material_add_is_private_stmt;
DEALLOCATE PREPARE report_material_add_is_private_stmt;

SET @report_material_add_scope_index_sql = (
    SELECT IF(
        COUNT(1) = 0,
        'CREATE INDEX `idx_report_material_scope` ON `ai_report_material` (`tenant_id`, `is_private`, `create_by`, `status`)',
        'SELECT 1'
    )
    FROM `INFORMATION_SCHEMA`.`STATISTICS`
    WHERE `TABLE_SCHEMA` = DATABASE()
      AND `TABLE_NAME` = 'ai_report_material'
      AND `INDEX_NAME` = 'idx_report_material_scope'
);

PREPARE report_material_add_scope_index_stmt FROM @report_material_add_scope_index_sql;
EXECUTE report_material_add_scope_index_stmt;
DEALLOCATE PREPARE report_material_add_scope_index_stmt;

-- 对齐 file_id 与通用文件表的字符集和排序规则，避免 MySQL 8 默认 utf8mb4_0900_ai_ci
-- 与历史报表表 utf8mb4_general_ci 混用时 JOIN 报 Illegal mix of collations。
SET @report_material_file_id_charset = COALESCE((
    SELECT `CHARACTER_SET_NAME`
    FROM `INFORMATION_SCHEMA`.`COLUMNS`
    WHERE `TABLE_SCHEMA` = DATABASE()
      AND `TABLE_NAME` = 'sys_file_metadata'
      AND `COLUMN_NAME` = 'file_id'
    LIMIT 1
), 'utf8mb4');

SET @report_material_file_id_collation = COALESCE((
    SELECT `COLLATION_NAME`
    FROM `INFORMATION_SCHEMA`.`COLUMNS`
    WHERE `TABLE_SCHEMA` = DATABASE()
      AND `TABLE_NAME` = 'sys_file_metadata'
      AND `COLUMN_NAME` = 'file_id'
    LIMIT 1
), 'utf8mb4_0900_ai_ci');

SET @report_material_file_id_sql = CONCAT(
    'ALTER TABLE `ai_report_material` MODIFY `file_id` varchar(64) CHARACTER SET ',
    @report_material_file_id_charset,
    ' COLLATE ',
    @report_material_file_id_collation,
    ' NOT NULL COMMENT ''通用文件ID'''
);

PREPARE report_material_file_id_stmt FROM @report_material_file_id_sql;
EXECUTE report_material_file_id_stmt;
DEALLOCATE PREPARE report_material_file_id_stmt;

INSERT IGNORE INTO `ai_report_material`
(
    `id`, `tenant_id`, `file_id`, `material_category`, `is_private`, `status`,
    `create_by`, `create_time`, `create_dept`, `update_by`, `update_time`
)
SELECT
    f.`id`,
    1 AS `tenant_id`,
    f.`file_id`,
    COALESCE(NULLIF(f.`business_id`, ''), 'background') AS `material_category`,
    COALESCE(f.`is_private`, 1) AS `is_private`,
    1 AS `status`,
    f.`uploader_id` AS `create_by`,
    COALESCE(f.`upload_time`, f.`create_time`, NOW()) AS `create_time`,
    NULL AS `create_dept`,
    f.`uploader_id` AS `update_by`,
    COALESCE(f.`update_time`, f.`upload_time`, NOW()) AS `update_time`
FROM `sys_file_metadata` f
WHERE f.`business_type` = 'report_material'
  AND f.`status` = 1
  AND NOT EXISTS (
      SELECT 1
      FROM `ai_report_material` m
      WHERE m.`tenant_id` = 1
        AND m.`file_id` = f.`file_id`
        AND m.`status` = 1
  );

UPDATE `ai_report_material` m
INNER JOIN `sys_file_metadata` f ON f.`file_id` = m.`file_id`
SET m.`is_private` = COALESCE(f.`is_private`, 1),
    m.`create_by` = COALESCE(m.`create_by`, f.`uploader_id`),
    m.`update_by` = COALESCE(m.`update_by`, f.`uploader_id`)
WHERE f.`business_type` = 'report_material'
  AND f.`status` = 1
  AND @report_material_missing_is_private = 1;
