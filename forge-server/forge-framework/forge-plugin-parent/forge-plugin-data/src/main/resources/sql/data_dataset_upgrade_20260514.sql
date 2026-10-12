-- 数据集发布态、分类树升级脚本
-- 适用于已初始化数据资产表的环境；新库请直接执行 data_tables.sql。

CREATE TABLE IF NOT EXISTS `ai_report_data_dataset_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `parent_id` bigint DEFAULT NULL COMMENT '父分类ID',
  `level` int NOT NULL DEFAULT 1 COMMENT '层级',
  `ancestors` varchar(500) NOT NULL DEFAULT '0/' COMMENT '祖先路径',
  `category_code` varchar(64) NOT NULL COMMENT '分类编码',
  `category_name` varchar(100) NOT NULL COMMENT '分类名称',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_dataset_category_code_tenant` (`tenant_id`, `category_code`),
  KEY `idx_data_dataset_category_parent` (`tenant_id`, `parent_id`),
  KEY `idx_data_dataset_category_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台数据集分类';

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_report_data_dataset'
    AND column_name = 'category_id'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE ai_report_data_dataset ADD COLUMN category_id BIGINT DEFAULT NULL COMMENT ''分类ID'' AFTER connection_id',
  'SELECT ''ai_report_data_dataset.category_id exists'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_report_data_dataset'
    AND column_name = 'publish_status'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE ai_report_data_dataset ADD COLUMN publish_status TINYINT NOT NULL DEFAULT 0 COMMENT ''发布状态：0未发布 1已发布 2已下架'' AFTER status',
  'SELECT ''ai_report_data_dataset.publish_status exists'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE ai_report_data_dataset
SET publish_status = CASE
    WHEN status = 1 THEN 1
    ELSE 2
END
WHERE publish_status IS NULL OR publish_status = 0;
