CREATE TABLE IF NOT EXISTS `ai_report_data_business_definition` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `business_code` varchar(64) NOT NULL COMMENT '业务编码',
  `business_name` varchar(100) NOT NULL COMMENT '业务名称',
  `business_desc` text NOT NULL COMMENT '业务定义描述',
  `analysis_goal` text DEFAULT NULL COMMENT '分析目标',
  `metric_definition` text DEFAULT NULL COMMENT '指标口径',
  `dimension_definition` text DEFAULT NULL COMMENT '分析维度',
  `usage_guide` text DEFAULT NULL COMMENT 'AI使用建议',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_business_code_tenant` (`tenant_id`, `business_code`),
  KEY `idx_data_business_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台业务定义';

CREATE TABLE IF NOT EXISTS `ai_report_data_business_dataset` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `business_id` bigint NOT NULL COMMENT '业务定义ID',
  `dataset_id` bigint NOT NULL COMMENT '数据集ID',
  `is_primary` tinyint NOT NULL DEFAULT 0 COMMENT '是否主数据集：1是 0否',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `usage_remark` varchar(500) DEFAULT NULL COMMENT '数据集用途说明',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_business_dataset` (`tenant_id`, `business_id`, `dataset_id`),
  KEY `idx_data_business_dataset_business` (`tenant_id`, `business_id`),
  KEY `idx_data_business_dataset_dataset` (`tenant_id`, `dataset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台业务定义数据集绑定';
