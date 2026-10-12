-- Public export: historical user/business/session/credential rows removed.
-- forge_admin_new.ai_agent definition

CREATE TABLE `ai_agent` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `agent_name` varchar(100) NOT NULL COMMENT 'Agent名称',
  `agent_code` varchar(50) NOT NULL COMMENT 'Agent编码（唯一）',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `system_prompt` longtext NOT NULL COMMENT '系统提示词模板',
  `provider_id` bigint DEFAULT NULL COMMENT '关联供应商ID',
  `model_name` varchar(100) DEFAULT NULL COMMENT '使用的模型',
  `model_selection_mode` varchar(16) NOT NULL DEFAULT 'PINNED' COMMENT '模型选择模式（PINNED/POLICY）',
  `route_policy_id` bigint DEFAULT NULL COMMENT '模型路由策略ID',
  `temperature` decimal(3,2) DEFAULT '0.70' COMMENT '温度参数（0-1）',
  `max_tokens` int DEFAULT '4000' COMMENT '最大Token数',
  `extra_config` json DEFAULT NULL COMMENT '扩展配置',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_dept` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_code_active` (`agent_code`,`del_flag`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2079737984897568771 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI Agent配置表';


-- forge_admin_new.ai_business_action_execution_log definition

CREATE TABLE `ai_business_action_execution_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `suite_code` varchar(64) DEFAULT NULL COMMENT '业务套件编码',
  `object_code` varchar(64) NOT NULL COMMENT '业务对象编码',
  `record_id` varchar(128) NOT NULL DEFAULT '' COMMENT '业务记录ID',
  `action_code` varchar(64) NOT NULL COMMENT '动作编码',
  `action_name` varchar(128) DEFAULT NULL COMMENT '动作名称',
  `execute_status` varchar(32) NOT NULL COMMENT '执行状态：RUNNING/SUCCESS/FAILED/TODO',
  `request_digest` varchar(1024) DEFAULT NULL COMMENT '请求摘要',
  `step_result` json DEFAULT NULL COMMENT '步骤执行结果JSON',
  `result_message` varchar(500) DEFAULT NULL COMMENT '执行结果提示',
  `error_message` varchar(2000) DEFAULT NULL COMMENT '错误信息',
  `correlation_id` varchar(64) DEFAULT NULL COMMENT '链路ID',
  `idempotency_key` varchar(128) DEFAULT NULL COMMENT '幂等键',
  `duration_ms` bigint DEFAULT NULL COMMENT '执行耗时毫秒',
  `capability_request_id` varchar(64) DEFAULT NULL COMMENT 'AI能力调用请求ID',
  `client_id` bigint DEFAULT NULL COMMENT 'AI能力机器客户端ID',
  `service_user_id` bigint DEFAULT NULL COMMENT 'AI能力绑定服务账号ID',
  `actor_type` varchar(16) DEFAULT NULL COMMENT 'AI能力实际主体类型（USER/SERVICE）',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_action_idem` (`tenant_id`,`object_code`,`record_id`,`action_code`,`idempotency_key`),
  KEY `idx_ai_business_action_record` (`tenant_id`,`object_code`,`record_id`,`create_time`),
  KEY `idx_ai_business_action_code` (`tenant_id`,`object_code`,`action_code`,`execute_status`,`create_time`),
  KEY `idx_ai_business_action_corr` (`tenant_id`,`correlation_id`),
  KEY `idx_ai_business_action_capability_request` (`tenant_id`,`capability_request_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-通用动作执行日志';


-- forge_admin_new.ai_business_app definition

CREATE TABLE `ai_business_app` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `app_code` varchar(64) NOT NULL COMMENT '应用入口编码',
  `app_name` varchar(128) NOT NULL COMMENT '应用入口名称',
  `app_type` varchar(32) NOT NULL COMMENT '应用类型：BUSINESS/EMBEDDED/MOBILE/INTEGRATION',
  `application_id` bigint DEFAULT NULL COMMENT '归属业务应用聚合ID',
  `suite_code` varchar(48) NOT NULL COMMENT '所属业务套件编码',
  `object_code` varchar(48) DEFAULT NULL COMMENT '关联业务对象编码',
  `entry_mode` varchar(32) NOT NULL COMMENT '入口模式：RUNTIME/ROUTE/IFRAME/EXTERNAL/H5/API',
  `entry_url` varchar(512) DEFAULT NULL COMMENT '入口地址',
  `config_key` varchar(128) DEFAULT NULL COMMENT '关联低代码运行配置键',
  `icon` varchar(128) DEFAULT NULL COMMENT '应用图标',
  `description` varchar(500) DEFAULT NULL COMMENT '业务说明',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `options` json DEFAULT NULL COMMENT '扩展配置',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_app_code_active` (`tenant_id`,`app_code`,`del_flag`),
  KEY `idx_ai_business_app_suite` (`tenant_id`,`suite_code`,`app_type`,`status`,`sort_order`),
  KEY `idx_ai_business_app_object` (`tenant_id`,`suite_code`,`object_code`,`status`),
  KEY `idx_ai_business_app_config_key` (`tenant_id`,`config_key`),
  KEY `idx_ai_business_app_application` (`tenant_id`,`application_id`,`status`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-应用入口表';


-- forge_admin_new.ai_business_application definition

CREATE TABLE `ai_business_application` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `application_code` varchar(128) NOT NULL COMMENT '业务应用编码',
  `application_name` varchar(128) NOT NULL COMMENT '业务应用名称',
  `suite_code` varchar(128) NOT NULL COMMENT '所属业务域编码',
  `icon` varchar(255) DEFAULT NULL COMMENT '应用图标',
  `description` varchar(500) DEFAULT NULL COMMENT '应用说明',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `design_status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '设计状态：DRAFT/READY/PUBLISHED/CHANGED',
  `last_publish_version` int DEFAULT NULL COMMENT '最近发布版本号',
  `last_publish_time` datetime DEFAULT NULL COMMENT '最近发布时间',
  `options` json DEFAULT NULL COMMENT '扩展配置，不保存敏感密钥',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_application_code_active` (`tenant_id`,`application_code`,`del_flag`),
  KEY `idx_ai_business_application_suite` (`tenant_id`,`suite_code`,`status`,`design_status`,`del_flag`),
  KEY `idx_ai_business_application_update` (`tenant_id`,`update_time`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='低代码业务应用聚合';


-- forge_admin_new.ai_business_application_object definition

CREATE TABLE `ai_business_application_object` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `application_id` bigint NOT NULL COMMENT '业务应用ID',
  `object_id` bigint NOT NULL COMMENT '业务对象ID',
  `object_role` varchar(32) NOT NULL COMMENT '对象角色：PRIMARY/DETAIL/REFERENCE/SHARED',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '应用内排序',
  `options` json DEFAULT NULL COMMENT '应用内对象配置',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_application_object_active` (`tenant_id`,`application_id`,`object_id`,`del_flag`),
  KEY `idx_ai_business_application_object_app` (`tenant_id`,`application_id`,`object_role`,`sort_order`,`del_flag`),
  KEY `idx_ai_business_application_object_object` (`tenant_id`,`object_id`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务应用与业务对象关联';


-- forge_admin_new.ai_business_application_publish_run definition

CREATE TABLE `ai_business_application_publish_run` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `application_id` bigint NOT NULL COMMENT '业务应用ID',
  `idempotency_key` varchar(128) NOT NULL COMMENT '应用内发布幂等键',
  `operation_type` varchar(16) NOT NULL DEFAULT 'PUBLISH' COMMENT 'PUBLISH/ROLLBACK',
  `target_version_no` int NOT NULL COMMENT '本次发布目标版本号',
  `source_version_no` int DEFAULT NULL COMMENT '回滚来源版本号',
  `run_status` varchar(24) NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED/RUNNING/PARTIAL/FAILED/SUCCESS',
  `current_step` varchar(32) DEFAULT NULL COMMENT '当前或失败步骤',
  `snapshot_json` longtext NOT NULL COMMENT '候选发布快照，不保存密钥',
  `snapshot_hash` char(64) NOT NULL COMMENT '候选快照SHA-256摘要',
  `selection_json` json NOT NULL COMMENT '本次选择及自动补齐的依赖资产',
  `step_results_json` json NOT NULL COMMENT '可恢复步骤结果，不保存原始异常',
  `result_version_id` bigint DEFAULT NULL COMMENT '成功生成的不可变应用版本ID',
  `error_code` varchar(64) DEFAULT NULL COMMENT '脱敏错误码',
  `error_summary` varchar(500) DEFAULT NULL COMMENT '脱敏错误摘要',
  `attempt_count` int NOT NULL DEFAULT '1' COMMENT '发布或恢复尝试次数',
  `started_by` bigint DEFAULT NULL COMMENT '可信发起人ID',
  `started_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次发起时间',
  `finished_time` datetime DEFAULT NULL COMMENT '完成或最近失败时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_publish_run_key_active` (`tenant_id`,`application_id`,`idempotency_key`,`del_flag`),
  UNIQUE KEY `uk_ai_business_publish_run_version_active` (`tenant_id`,`application_id`,`target_version_no`,`del_flag`),
  KEY `idx_ai_business_publish_run_status` (`tenant_id`,`application_id`,`run_status`,`update_time`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='低代码业务应用协调发布运行单';


-- forge_admin_new.ai_business_application_version definition

CREATE TABLE `ai_business_application_version` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `application_id` bigint NOT NULL COMMENT '业务应用ID',
  `version_no` int NOT NULL COMMENT '应用内单调版本号',
  `snapshot_json` longtext NOT NULL COMMENT '不可变应用发布快照，不保存密钥',
  `snapshot_hash` char(64) NOT NULL COMMENT '快照SHA-256摘要',
  `publish_status` varchar(24) NOT NULL COMMENT 'PUBLISHED/ROLLBACK',
  `publish_summary` varchar(1000) DEFAULT NULL COMMENT '脱敏发布摘要',
  `source_version_no` int DEFAULT NULL COMMENT '回滚来源版本号',
  `published_by` bigint DEFAULT NULL COMMENT '可信发布人ID',
  `published_time` datetime NOT NULL COMMENT '发布时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_application_version_active` (`tenant_id`,`application_id`,`version_no`,`del_flag`),
  KEY `idx_ai_business_application_version_hash` (`tenant_id`,`application_id`,`snapshot_hash`,`del_flag`),
  KEY `idx_ai_business_application_version_time` (`tenant_id`,`application_id`,`published_time`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='低代码业务应用不可变发布版本';


-- forge_admin_new.ai_business_binding definition

CREATE TABLE `ai_business_binding` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `target_type` varchar(32) NOT NULL COMMENT '挂接目标类型：SUITE/OBJECT/APP',
  `target_id` bigint DEFAULT NULL COMMENT '挂接目标ID',
  `target_code` varchar(128) NOT NULL COMMENT '挂接目标编码',
  `binding_type` varchar(32) NOT NULL COMMENT '能力类型：FLOW/APPROVAL/REPORT/PERMISSION/MESSAGE/TRIGGER/IMPORT/EXPORT/MOBILE/INTEGRATION',
  `binding_key` varchar(128) NOT NULL COMMENT '能力业务键',
  `binding_name` varchar(128) NOT NULL COMMENT '挂接名称',
  `binding_config` json DEFAULT NULL COMMENT '挂接配置',
  `description` varchar(500) DEFAULT NULL COMMENT '挂接说明',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_binding_target` (`tenant_id`,`target_type`,`target_code`,`binding_type`,`binding_key`),
  KEY `idx_ai_business_binding_target_id` (`tenant_id`,`target_type`,`target_id`,`binding_type`),
  KEY `idx_ai_business_binding_type` (`tenant_id`,`binding_type`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-能力挂接表';


-- forge_admin_new.ai_business_document_config definition

CREATE TABLE `ai_business_document_config` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `object_id` bigint DEFAULT NULL COMMENT '业务对象ID',
  `suite_code` varchar(48) NOT NULL COMMENT '业务套件编码',
  `object_code` varchar(48) NOT NULL COMMENT '业务对象编码',
  `config_key` varchar(128) DEFAULT NULL COMMENT '动态CRUD运行配置键',
  `document_name` varchar(128) NOT NULL COMMENT '单据名称',
  `document_no_rule` varchar(256) DEFAULT NULL COMMENT '单据编号规则',
  `document_enabled` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否启用单据模式',
  `status_field` varchar(64) DEFAULT NULL COMMENT '单据状态字段',
  `starter_field` varchar(64) DEFAULT NULL COMMENT '发起人字段',
  `owner_field` varchar(64) DEFAULT NULL COMMENT '负责人字段',
  `default_flow_key` varchar(128) DEFAULT NULL COMMENT '默认流程模型Key',
  `status_mapping` json DEFAULT NULL COMMENT '单据状态映射JSON',
  `options` json DEFAULT NULL COMMENT '扩展配置JSON',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_document_object_code` (`tenant_id`,`object_code`),
  UNIQUE KEY `uk_ai_business_document_object_id` (`tenant_id`,`object_id`),
  KEY `idx_ai_business_document_suite` (`tenant_id`,`suite_code`),
  KEY `idx_ai_business_document_config` (`tenant_id`,`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-业务单据配置表';


-- forge_admin_new.ai_business_extension definition

CREATE TABLE `ai_business_extension` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `application_id` bigint NOT NULL COMMENT '所属业务应用ID',
  `object_id` bigint DEFAULT NULL COMMENT '可选业务对象ID',
  `entry_id` bigint DEFAULT NULL COMMENT '可选访问入口ID',
  `extension_code` varchar(64) NOT NULL COMMENT '扩展稳定编码',
  `extension_name` varchar(128) NOT NULL COMMENT '扩展名称',
  `extension_type` varchar(32) NOT NULL COMMENT 'VISUAL_RULE/CLIENT_JS/SCOPED_CSS/SERVER_BINDING',
  `hook_code` varchar(32) NOT NULL COMMENT '标准扩展钩子编码',
  `scope_type` varchar(32) NOT NULL DEFAULT 'APPLICATION' COMMENT 'APPLICATION/OBJECT/ENTRY/PAGE/COMPONENT',
  `scope_key` varchar(255) DEFAULT NULL COMMENT '页面或组件作用域键',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '同钩子执行顺序',
  `failure_policy` varchar(16) NOT NULL DEFAULT 'BLOCK' COMMENT 'BLOCK/WARN/IGNORE',
  `risk_level` varchar(16) NOT NULL DEFAULT 'MEDIUM' COMMENT 'LOW/MEDIUM/HIGH',
  `status` varchar(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/TESTED/ENABLED/DISABLED',
  `draft_version` int NOT NULL DEFAULT '1' COMMENT '当前草稿版本',
  `enabled_version` int DEFAULT NULL COMMENT '当前运行版本',
  `lock_user_id` bigint DEFAULT NULL COMMENT '编辑锁持有人',
  `lock_username` varchar(128) DEFAULT NULL COMMENT '编辑锁持有人名称',
  `lock_token_hash` varchar(64) DEFAULT NULL COMMENT '编辑锁令牌SHA-256摘要',
  `lock_time` datetime DEFAULT NULL COMMENT '编辑锁获得或续期时间',
  `lock_expire_time` datetime DEFAULT NULL COMMENT '编辑锁过期时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_extension_code_active` (`tenant_id`,`application_id`,`extension_code`,`del_flag`),
  KEY `idx_ai_business_extension_hook` (`tenant_id`,`application_id`,`hook_code`,`status`,`sort_order`,`del_flag`),
  KEY `idx_ai_business_extension_object` (`tenant_id`,`object_id`,`status`,`del_flag`),
  KEY `idx_ai_business_extension_entry` (`tenant_id`,`entry_id`,`status`,`del_flag`),
  KEY `idx_ai_business_extension_lock` (`tenant_id`,`lock_user_id`,`lock_expire_time`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='低代码受治理业务扩展';


-- forge_admin_new.ai_business_extension_execution_log definition

CREATE TABLE `ai_business_extension_execution_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `extension_id` bigint NOT NULL COMMENT '业务扩展ID',
  `extension_code` varchar(64) NOT NULL COMMENT '扩展编码快照',
  `version_no` int NOT NULL COMMENT '执行版本号',
  `application_id` bigint NOT NULL COMMENT '业务应用ID',
  `object_id` bigint DEFAULT NULL COMMENT '业务对象ID',
  `entry_id` bigint DEFAULT NULL COMMENT '访问入口ID',
  `hook_code` varchar(32) NOT NULL COMMENT '执行钩子',
  `result_status` varchar(16) NOT NULL COMMENT 'SUCCESS/FAILED',
  `duration_ms` bigint NOT NULL DEFAULT '0' COMMENT '执行耗时毫秒',
  `error_code` varchar(64) DEFAULT NULL COMMENT '脱敏错误编码',
  `error_summary` varchar(500) DEFAULT NULL COMMENT '脱敏错误摘要',
  `actor_user_id` bigint DEFAULT NULL COMMENT '可信操作者ID',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志：0正常 1删除',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_business_extension_audit_extension` (`tenant_id`,`extension_id`,`create_time`,`del_flag`),
  KEY `idx_ai_business_extension_audit_app` (`tenant_id`,`application_id`,`hook_code`,`create_time`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='低代码业务扩展脱敏执行审计';


-- forge_admin_new.ai_business_extension_version definition

CREATE TABLE `ai_business_extension_version` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `extension_id` bigint NOT NULL COMMENT '业务扩展ID',
  `version_no` int NOT NULL COMMENT '扩展内单调版本号',
  `content` longtext NOT NULL COMMENT '扩展原始内容',
  `processed_content` longtext COMMENT '服务端复核的作用域或编译结果',
  `config_json` json NOT NULL COMMENT '非敏感结构化配置',
  `content_hash` char(64) NOT NULL COMMENT '内容SHA-256摘要',
  `validation_passed` tinyint NOT NULL DEFAULT '0' COMMENT '校验是否通过',
  `validation_summary` varchar(1000) DEFAULT NULL COMMENT '校验摘要，不含原始输入',
  `test_passed` tinyint NOT NULL DEFAULT '0' COMMENT '受限测试是否通过',
  `test_summary` varchar(1000) DEFAULT NULL COMMENT '测试摘要，不含原始输入',
  `change_summary` varchar(500) DEFAULT NULL COMMENT '版本变更说明',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_extension_version_active` (`tenant_id`,`extension_id`,`version_no`,`del_flag`),
  KEY `idx_ai_business_extension_version_hash` (`tenant_id`,`extension_id`,`content_hash`,`del_flag`),
  KEY `idx_ai_business_extension_version_created` (`tenant_id`,`extension_id`,`create_time`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='低代码业务扩展不可变内容版本';


-- forge_admin_new.ai_business_field_template definition

CREATE TABLE `ai_business_field_template` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `template_code` varchar(64) NOT NULL COMMENT '字段模板编码',
  `template_name` varchar(128) NOT NULL COMMENT '字段模板名称',
  `suite_code` varchar(48) NOT NULL DEFAULT 'COMMON' COMMENT '适用业务套件编码，COMMON为通用',
  `field_type` varchar(32) NOT NULL COMMENT '业务字段类型',
  `field_schema` json NOT NULL COMMENT '字段Schema',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `description` varchar(500) DEFAULT NULL COMMENT '模板说明',
  `options` json DEFAULT NULL COMMENT '扩展配置',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_field_template_code` (`tenant_id`,`template_code`),
  KEY `idx_ai_business_field_template_code` (`tenant_id`,`template_code`),
  KEY `idx_ai_business_field_template_suite` (`tenant_id`,`suite_code`,`status`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-业务字段模板表';


-- forge_admin_new.ai_business_flow_instance_link definition

CREATE TABLE `ai_business_flow_instance_link` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `object_code` varchar(48) NOT NULL COMMENT '业务对象编码',
  `record_id` bigint NOT NULL COMMENT '业务记录ID',
  `business_key` varchar(160) NOT NULL COMMENT '流程业务Key，格式 objectCode:recordId',
  `flow_model_key` varchar(128) NOT NULL COMMENT '流程模型Key',
  `process_instance_id` varchar(128) NOT NULL COMMENT '流程实例ID',
  `flow_status` varchar(32) NOT NULL DEFAULT 'STARTED' COMMENT '流程状态：STARTED/RUNNING/APPROVED/REJECTED/CANCELED/ENDED',
  `start_user_id` bigint DEFAULT NULL COMMENT '流程发起人ID',
  `start_time` datetime DEFAULT NULL COMMENT '流程发起时间',
  `end_time` datetime DEFAULT NULL COMMENT '流程结束时间',
  `result` varchar(32) DEFAULT NULL COMMENT '流程结果',
  `variables_snapshot` json DEFAULT NULL COMMENT '流程变量快照',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_flow_process` (`tenant_id`,`process_instance_id`),
  UNIQUE KEY `uk_ai_business_flow_business_process` (`tenant_id`,`business_key`,`process_instance_id`),
  KEY `idx_ai_business_flow_business_key` (`tenant_id`,`business_key`,`flow_status`),
  KEY `idx_ai_business_flow_record` (`tenant_id`,`object_code`,`record_id`),
  KEY `idx_ai_business_flow_business_latest` (`tenant_id`,`business_key`,`start_time`,`create_time`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-单据流程实例关联表';


-- forge_admin_new.ai_business_message_channel definition

CREATE TABLE `ai_business_message_channel` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `channel_code` varchar(64) NOT NULL COMMENT '通道编码',
  `channel_name` varchar(128) NOT NULL COMMENT '通道名称',
  `channel_type` varchar(32) NOT NULL COMMENT '通道类型：WECHAT_WORK/FEISHU/DINGTALK/WEBHOOK/INTERNAL',
  `channel_config_ref` varchar(256) DEFAULT NULL COMMENT '通道配置安全引用（不存明文密钥）',
  `webhook_url` varchar(512) DEFAULT NULL COMMENT 'Webhook地址（仅WEBHOOK类型）',
  `description` varchar(500) DEFAULT NULL COMMENT '通道说明',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `options` json DEFAULT NULL COMMENT '扩展配置',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_message_channel_code` (`tenant_id`,`channel_code`),
  KEY `idx_ai_message_channel_type` (`tenant_id`,`channel_type`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-消息推送通道表';


-- forge_admin_new.ai_business_object definition

CREATE TABLE `ai_business_object` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `suite_code` varchar(48) NOT NULL COMMENT '所属业务套件编码',
  `object_code` varchar(48) NOT NULL COMMENT '业务对象编码',
  `object_name` varchar(128) NOT NULL COMMENT '业务对象名称',
  `object_type` varchar(32) NOT NULL COMMENT '对象类型：MASTER/DETAIL/LOOKUP/TRANSACTION',
  `model_id` bigint DEFAULT NULL COMMENT '关联低代码模型ID',
  `model_code` varchar(48) DEFAULT NULL COMMENT '关联低代码模型编码',
  `display_field` varchar(64) DEFAULT NULL COMMENT '默认展示字段',
  `icon` varchar(128) DEFAULT NULL COMMENT '对象图标',
  `description` varchar(500) DEFAULT NULL COMMENT '业务说明',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `options` json DEFAULT NULL COMMENT '扩展配置',
  `design_status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '设计状态：DRAFT草稿 DESIGNING设计中 READY待发布 PUBLISHED已发布 CHANGED有未发布变更',
  `config_key` varchar(128) DEFAULT NULL COMMENT '关联低代码运行配置键',
  `last_publish_time` datetime DEFAULT NULL COMMENT '最近发布时间',
  `last_publish_version` int DEFAULT NULL COMMENT '最近发布版本号',
  `designer_options` json DEFAULT NULL COMMENT '业务对象设计器配置',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_object_code_active` (`tenant_id`,`suite_code`,`object_code`,`del_flag`),
  KEY `idx_ai_business_object_suite` (`tenant_id`,`suite_code`,`status`,`sort_order`),
  KEY `idx_ai_business_object_model` (`tenant_id`,`model_id`),
  KEY `idx_ai_business_object_update` (`tenant_id`,`update_time`),
  KEY `idx_ai_business_object_config_key` (`tenant_id`,`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-业务对象表';


-- forge_admin_new.ai_business_object_design_version definition

CREATE TABLE `ai_business_object_design_version` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `object_id` bigint NOT NULL COMMENT '业务对象ID',
  `suite_code` varchar(48) NOT NULL COMMENT '业务套件编码',
  `object_code` varchar(48) NOT NULL COMMENT '业务对象编码',
  `config_id` bigint DEFAULT NULL COMMENT '关联低代码运行配置ID',
  `config_key` varchar(128) DEFAULT NULL COMMENT '关联低代码运行配置键',
  `crud_config_version_id` bigint DEFAULT NULL COMMENT '关联AI CRUD发布版本ID',
  `version_no` int NOT NULL COMMENT '对象设计版本号',
  `version_type` varchar(32) NOT NULL DEFAULT 'draft' COMMENT '版本类型：draft/publish/rollback',
  `model_snapshot` json DEFAULT NULL COMMENT '模型Schema快照',
  `page_snapshot` json DEFAULT NULL COMMENT '页面Schema快照',
  `relation_snapshot` json DEFAULT NULL COMMENT '关系配置快照',
  `designer_options_snapshot` json DEFAULT NULL COMMENT '表单优先设计器扩展快照',
  `publish_status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '发布状态：DRAFT/PUBLISHED/FAILED',
  `publish_version` int DEFAULT NULL COMMENT '发布版本号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_design_version_no` (`tenant_id`,`object_id`,`version_no`),
  KEY `idx_ai_business_design_version_object` (`tenant_id`,`object_id`,`version_no`),
  KEY `idx_ai_business_design_version_crud` (`tenant_id`,`crud_config_version_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-业务对象设计版本表';


-- forge_admin_new.ai_business_object_relation definition

CREATE TABLE `ai_business_object_relation` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `suite_code` varchar(48) NOT NULL COMMENT '所属业务套件编码',
  `source_object_code` varchar(48) NOT NULL COMMENT '来源业务对象编码',
  `target_object_code` varchar(48) NOT NULL COMMENT '目标业务对象编码',
  `relation_type` varchar(32) NOT NULL COMMENT '关系类型：REFERENCE/DETAIL/CHILD_LIST/MANY_TO_MANY',
  `relation_name` varchar(128) NOT NULL COMMENT '关系名称',
  `source_field_code` varchar(64) DEFAULT NULL COMMENT '来源对象字段编码',
  `target_field_code` varchar(64) DEFAULT NULL COMMENT '目标对象字段编码',
  `relation_config` json DEFAULT NULL COMMENT '关系配置',
  `description` varchar(500) DEFAULT NULL COMMENT '关系说明',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_relation_scope` (`tenant_id`,`suite_code`,`source_object_code`,`target_object_code`,`relation_type`,`relation_name`),
  KEY `idx_ai_business_relation_source` (`tenant_id`,`suite_code`,`source_object_code`,`status`),
  KEY `idx_ai_business_relation_target` (`tenant_id`,`suite_code`,`target_object_code`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-业务对象关系表';


-- forge_admin_new.ai_business_quantity_balance definition

CREATE TABLE `ai_business_quantity_balance` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `account_code` varchar(128) NOT NULL COMMENT '数量账户编码',
  `item_code` varchar(128) NOT NULL COMMENT '数量项编码',
  `dimension_key` varchar(256) NOT NULL DEFAULT '' COMMENT '数量维度键',
  `quantity` bigint NOT NULL DEFAULT '0' COMMENT '当前数量',
  `locked_quantity` bigint NOT NULL DEFAULT '0' COMMENT '锁定数量',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用，0停用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_qty_balance` (`tenant_id`,`account_code`,`item_code`,`dimension_key`),
  KEY `idx_ai_business_qty_balance_item` (`tenant_id`,`item_code`,`dimension_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-通用数量余额';


-- forge_admin_new.ai_business_quantity_ledger definition

CREATE TABLE `ai_business_quantity_ledger` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `operation_type` varchar(32) NOT NULL COMMENT '操作类型：INBOUND/LOCK/RELEASE/COMMIT/TRANSFER',
  `account_code` varchar(128) NOT NULL COMMENT '数量账户编码',
  `item_code` varchar(128) NOT NULL COMMENT '数量项编码',
  `dimension_key` varchar(256) NOT NULL DEFAULT '' COMMENT '数量维度键',
  `quantity_delta` bigint NOT NULL DEFAULT '0' COMMENT '数量变动值',
  `balance_quantity` bigint NOT NULL DEFAULT '0' COMMENT '操作后当前数量',
  `locked_quantity` bigint NOT NULL DEFAULT '0' COMMENT '操作后锁定数量',
  `target_account_code` varchar(128) DEFAULT NULL COMMENT '目标数量账户编码',
  `target_item_code` varchar(128) DEFAULT NULL COMMENT '目标数量项编码',
  `target_dimension_key` varchar(256) DEFAULT NULL COMMENT '目标数量维度键',
  `source_object_code` varchar(64) DEFAULT NULL COMMENT '来源业务对象编码',
  `source_record_id` varchar(128) DEFAULT NULL COMMENT '来源业务记录ID',
  `source_detail_id` varchar(128) DEFAULT NULL COMMENT '来源明细ID',
  `lock_id` bigint DEFAULT NULL COMMENT '关联锁定ID',
  `correlation_id` varchar(64) DEFAULT NULL COMMENT '链路ID',
  `idempotency_key` varchar(128) DEFAULT NULL COMMENT '幂等键',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `extra_data` json DEFAULT NULL COMMENT '扩展数据',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_qty_ledger_idem` (`tenant_id`,`idempotency_key`),
  KEY `idx_ai_business_qty_ledger_item` (`tenant_id`,`account_code`,`item_code`,`dimension_key`,`create_time`),
  KEY `idx_ai_business_qty_ledger_source` (`tenant_id`,`source_object_code`,`source_record_id`),
  KEY `idx_ai_business_qty_ledger_corr` (`tenant_id`,`correlation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-通用数量流水';


-- forge_admin_new.ai_business_quantity_lock definition

CREATE TABLE `ai_business_quantity_lock` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `lock_code` varchar(128) DEFAULT NULL COMMENT '锁定编码',
  `account_code` varchar(128) NOT NULL COMMENT '数量账户编码',
  `item_code` varchar(128) NOT NULL COMMENT '数量项编码',
  `dimension_key` varchar(256) NOT NULL DEFAULT '' COMMENT '数量维度键',
  `lock_quantity` bigint NOT NULL DEFAULT '0' COMMENT '锁定数量',
  `released_quantity` bigint NOT NULL DEFAULT '0' COMMENT '已释放数量',
  `committed_quantity` bigint NOT NULL DEFAULT '0' COMMENT '已扣减数量',
  `lock_status` varchar(32) NOT NULL DEFAULT 'LOCKED' COMMENT '锁定状态：LOCKED/RELEASED/COMMITTED',
  `source_object_code` varchar(64) DEFAULT NULL COMMENT '来源业务对象编码',
  `source_record_id` varchar(128) DEFAULT NULL COMMENT '来源业务记录ID',
  `source_detail_id` varchar(128) DEFAULT NULL COMMENT '来源明细ID',
  `correlation_id` varchar(64) DEFAULT NULL COMMENT '链路ID',
  `idempotency_key` varchar(128) DEFAULT NULL COMMENT '幂等键',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_qty_lock_idem` (`tenant_id`,`idempotency_key`),
  KEY `idx_ai_business_qty_lock_item` (`tenant_id`,`account_code`,`item_code`,`dimension_key`,`lock_status`),
  KEY `idx_ai_business_qty_lock_source` (`tenant_id`,`source_object_code`,`source_record_id`,`source_detail_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-通用数量锁定';


-- forge_admin_new.ai_business_suite definition

CREATE TABLE `ai_business_suite` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `parent_id` bigint DEFAULT NULL COMMENT '上级业务域ID',
  `suite_code` varchar(48) NOT NULL COMMENT '业务套件编码',
  `suite_name` varchar(128) NOT NULL COMMENT '业务套件名称',
  `icon` varchar(128) DEFAULT NULL COMMENT '套件图标',
  `description` varchar(500) DEFAULT NULL COMMENT '业务说明',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `options` json DEFAULT NULL COMMENT '扩展配置',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_suite_code_active` (`tenant_id`,`suite_code`,`del_flag`),
  KEY `idx_ai_business_suite_status` (`tenant_id`,`status`,`sort_order`),
  KEY `idx_ai_business_suite_update` (`tenant_id`,`update_time`),
  KEY `idx_ai_business_suite_parent` (`tenant_id`,`parent_id`,`sort_order`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-业务套件表';


-- forge_admin_new.ai_business_trigger definition

CREATE TABLE `ai_business_trigger` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `suite_code` varchar(48) NOT NULL COMMENT '所属业务套件编码',
  `object_code` varchar(48) NOT NULL COMMENT '所属业务对象编码',
  `trigger_name` varchar(128) NOT NULL COMMENT '触发器名称',
  `trigger_desc` varchar(500) DEFAULT NULL COMMENT '触发器说明',
  `trigger_type` varchar(32) NOT NULL DEFAULT 'EVENT' COMMENT '触发器类型：EVENT事件触发/SCHEDULE定时触发/MANUAL手动触发',
  `scenario_type` varchar(64) DEFAULT NULL COMMENT '业务场景模板类型',
  `blocking_mode` varchar(32) NOT NULL DEFAULT 'ASYNC' COMMENT '阻断模式：ASYNC异步/SYNC_BLOCK同步阻断',
  `developer_mode` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否开发者高级模式',
  `event_type` varchar(64) DEFAULT NULL COMMENT '事件类型：RECORD_CREATED/RECORD_UPDATED/RECORD_DELETED/STATUS_CHANGED/FIELD_CHANGED',
  `event_condition` json DEFAULT NULL COMMENT '事件条件表达式JSON',
  `action_type` varchar(32) NOT NULL COMMENT '动作类型：START_FLOW/SEND_MESSAGE/CREATE_RECORD/UPDATE_FIELD/WEBHOOK',
  `action_config` json NOT NULL COMMENT '动作配置JSON',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `execute_count` bigint NOT NULL DEFAULT '0' COMMENT '累计执行次数',
  `last_execute_time` datetime DEFAULT NULL COMMENT '最近执行时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
  PRIMARY KEY (`id`),
  KEY `idx_ai_business_trigger_object` (`tenant_id`,`suite_code`,`object_code`,`status`),
  KEY `idx_ai_business_trigger_event` (`tenant_id`,`object_code`,`event_type`,`status`),
  KEY `idx_ai_business_trigger_type` (`tenant_id`,`trigger_type`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-触发器规则表';


-- forge_admin_new.ai_business_trigger_log definition

CREATE TABLE `ai_business_trigger_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `trigger_id` bigint NOT NULL COMMENT '触发器ID',
  `trigger_name` varchar(128) DEFAULT NULL COMMENT '触发器名称',
  `suite_code` varchar(48) NOT NULL COMMENT '业务套件编码',
  `object_code` varchar(48) NOT NULL COMMENT '业务对象编码',
  `record_id` varchar(64) DEFAULT NULL COMMENT '触发记录ID',
  `event_type` varchar(64) NOT NULL COMMENT '触发事件类型',
  `event_data` json DEFAULT NULL COMMENT '事件数据快照',
  `action_type` varchar(32) NOT NULL COMMENT '执行动作类型',
  `action_result` json DEFAULT NULL COMMENT '执行结果',
  `execute_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '执行状态：PENDING/SUCCESS/FAILED/SKIPPED',
  `error_message` varchar(2000) DEFAULT NULL COMMENT '错误信息',
  `todo_code` varchar(64) DEFAULT NULL COMMENT 'TODO状态编码',
  `correlation_id` varchar(128) DEFAULT NULL COMMENT '关联流程/消息/外部动作ID',
  `execute_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
  `duration_ms` bigint DEFAULT NULL COMMENT '执行耗时(毫秒)',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '重试次数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_trigger_log_trigger` (`tenant_id`,`trigger_id`,`execute_status`),
  KEY `idx_ai_trigger_log_object` (`tenant_id`,`object_code`,`execute_time`),
  KEY `idx_ai_trigger_log_record` (`tenant_id`,`object_code`,`record_id`),
  KEY `idx_ai_trigger_log_time` (`tenant_id`,`execute_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-触发器执行日志表';


-- forge_admin_new.ai_capability definition

CREATE TABLE `ai_capability` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `capability_code` varchar(128) NOT NULL,
  `protocol_tool_name` varchar(128) NOT NULL,
  `capability_name` varchar(128) NOT NULL,
  `description` varchar(1000) NOT NULL,
  `source_type` varchar(32) NOT NULL,
  `source_key` varchar(128) NOT NULL,
  `source_version` varchar(64) NOT NULL,
  `current_version` varchar(32) NOT NULL,
  `schema_checksum` char(64) NOT NULL,
  `behavior` varchar(24) NOT NULL,
  `risk_level` varchar(16) NOT NULL,
  `visibility` varchar(16) NOT NULL DEFAULT 'PRIVATE',
  `publish_status` varchar(16) NOT NULL DEFAULT 'PUBLISHED',
  `enabled` tinyint NOT NULL DEFAULT '1',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_capability_code_active` (`tenant_id`,`capability_code`,`del_flag`),
  UNIQUE KEY `uk_ai_capability_tool_active` (`tenant_id`,`protocol_tool_name`,`del_flag`),
  KEY `idx_ai_capability_source` (`tenant_id`,`source_type`,`source_key`,`del_flag`),
  KEY `idx_ai_capability_publish` (`tenant_id`,`publish_status`,`enabled`,`del_flag`,`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI中枢能力目录';


-- forge_admin_new.ai_capability_access_token definition

CREATE TABLE `ai_capability_access_token` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `token_key_id` varchar(22) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `token_prefix` varchar(32) NOT NULL,
  `token_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `client_id` bigint NOT NULL,
  `credential_version` int NOT NULL,
  `actor_type` varchar(16) NOT NULL,
  `actor_user_id` bigint NOT NULL,
  `service_user_id` bigint NOT NULL,
  `active_org_id` bigint NOT NULL,
  `audience` varchar(512) NOT NULL,
  `scopes` varchar(2000) NOT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'ACTIVE',
  `issued_at` datetime NOT NULL,
  `expires_at` datetime NOT NULL,
  `last_used_at` datetime DEFAULT NULL,
  `revoked_at` datetime DEFAULT NULL,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_capability_token_key_active` (`token_key_id`,`del_flag`),
  KEY `idx_ai_capability_token_client` (`tenant_id`,`client_id`,`status`,`expires_at`,`del_flag`),
  KEY `idx_ai_capability_token_actor` (`tenant_id`,`actor_user_id`,`status`,`expires_at`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI中枢MCP短期访问令牌安全元数据';


-- forge_admin_new.ai_capability_approval definition

CREATE TABLE `ai_capability_approval` (
  `id` bigint NOT NULL COMMENT '审批请求ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `request_id` varchar(64) NOT NULL COMMENT '原始Capability请求ID',
  `client_id` bigint NOT NULL COMMENT '机器客户端ID',
  `credential_version` int NOT NULL COMMENT '提交时客户端凭据版本',
  `capability_id` bigint NOT NULL COMMENT '能力ID',
  `capability_code` varchar(128) NOT NULL COMMENT '能力编码',
  `capability_version` varchar(32) NOT NULL COMMENT '能力版本',
  `actor_user_id` bigint NOT NULL COMMENT '实际操作用户ID',
  `service_user_id` bigint NOT NULL COMMENT '客户端服务账号ID',
  `active_org_id` bigint NOT NULL COMMENT '当前组织ID',
  `idempotency_key` varchar(128) NOT NULL COMMENT '原始幂等键',
  `request_digest` varchar(71) NOT NULL COMMENT '规范请求SHA-256摘要',
  `business_state_digest` varchar(71) DEFAULT NULL COMMENT '提交时业务状态摘要',
  `key_id` varchar(64) NOT NULL COMMENT 'KEK版本ID',
  `wrapped_dek` varchar(512) NOT NULL COMMENT '包装后的每记录DEK',
  `payload_iv` varchar(64) NOT NULL COMMENT 'AES-GCM IV',
  `payload_ciphertext` longtext NOT NULL COMMENT '请求密文',
  `payload_auth_tag` varchar(64) NOT NULL COMMENT 'AES-GCM认证标签',
  `flow_model_key` varchar(128) NOT NULL COMMENT '审批流程模型Key',
  `process_instance_id` varchar(128) DEFAULT NULL COMMENT 'Flowable实例ID',
  `execute_status` varchar(32) NOT NULL COMMENT '审批执行状态',
  `result_code` varchar(64) NOT NULL COMMENT '稳定结果码',
  `result_snapshot` varchar(2000) DEFAULT NULL COMMENT '安全结果摘要',
  `error_code` varchar(64) DEFAULT NULL COMMENT '稳定错误码',
  `expires_at` datetime NOT NULL COMMENT '审批过期时间',
  `completed_at` datetime DEFAULT NULL COMMENT '终态时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_capability_approval_idempotency` (`tenant_id`,`client_id`,`capability_id`,`idempotency_key`,`del_flag`),
  UNIQUE KEY `uk_capability_approval_request` (`tenant_id`,`request_id`,`del_flag`),
  KEY `idx_capability_approval_actor` (`tenant_id`,`actor_user_id`,`create_time`),
  KEY `idx_capability_approval_process` (`tenant_id`,`process_instance_id`),
  KEY `idx_capability_approval_status` (`tenant_id`,`execute_status`,`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI能力高风险动作人工审批';


-- forge_admin_new.ai_capability_client definition

CREATE TABLE `ai_capability_client` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `client_code` varchar(64) NOT NULL,
  `client_name` varchar(128) NOT NULL,
  `key_id` varchar(22) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `key_prefix` varchar(32) NOT NULL,
  `key_hash` char(64) NOT NULL,
  `credential_version` int NOT NULL DEFAULT '1',
  `service_user_id` bigint NOT NULL,
  `active_org_id` bigint NOT NULL,
  `oauth_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否允许OAuth换取短期令牌',
  `oauth_client_type` varchar(16) NOT NULL DEFAULT 'CONFIDENTIAL' COMMENT 'OAuth客户端类型（PUBLIC/CONFIDENTIAL）',
  `status` varchar(16) NOT NULL DEFAULT 'ENABLED',
  `expires_at` datetime DEFAULT NULL,
  `last_used_at` datetime DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_capability_client_code_active` (`tenant_id`,`client_code`,`del_flag`),
  UNIQUE KEY `uk_ai_capability_client_key_active` (`key_id`,`del_flag`),
  KEY `idx_ai_capability_client_status` (`tenant_id`,`status`,`expires_at`,`del_flag`),
  KEY `idx_ai_capability_client_user_org` (`tenant_id`,`service_user_id`,`active_org_id`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI中枢机器客户端';


-- forge_admin_new.ai_capability_flow_action_log definition

CREATE TABLE `ai_capability_flow_action_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `request_id` varchar(64) NOT NULL COMMENT 'Capability请求ID',
  `client_id` bigint NOT NULL COMMENT '机器客户端ID',
  `capability_id` bigint NOT NULL COMMENT '能力ID',
  `capability_code` varchar(128) NOT NULL COMMENT '能力编码',
  `capability_version` varchar(32) NOT NULL COMMENT '能力版本',
  `operation` varchar(16) NOT NULL COMMENT '流程操作START/APPROVE/REJECT',
  `object_code` varchar(128) NOT NULL COMMENT '业务对象编码',
  `record_id` varchar(128) NOT NULL COMMENT '业务记录ID字符串',
  `task_ref` varchar(16) DEFAULT NULL COMMENT '任务ID安全摘要',
  `idempotency_key` varchar(128) NOT NULL COMMENT '调用方幂等键',
  `request_digest` varchar(71) NOT NULL COMMENT '规范请求SHA-256摘要',
  `actor_type` varchar(16) NOT NULL COMMENT '实际主体类型',
  `actor_user_id` bigint NOT NULL COMMENT '实际操作用户ID',
  `service_user_id` bigint NOT NULL COMMENT '客户端绑定服务账号ID',
  `active_org_id` bigint NOT NULL COMMENT '当前组织ID',
  `execute_status` varchar(32) NOT NULL COMMENT 'RUNNING/SUCCESS/FAILED',
  `result_code` varchar(64) NOT NULL COMMENT '稳定结果码',
  `result_snapshot` varchar(2000) DEFAULT NULL COMMENT '安全结果摘要JSON',
  `error_code` varchar(64) DEFAULT NULL COMMENT '稳定错误码',
  `duration_ms` bigint NOT NULL DEFAULT '0' COMMENT '耗时毫秒',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cap_flow_action_idempotency` (`tenant_id`,`client_id`,`capability_id`,`operation`,`idempotency_key`,`del_flag`),
  UNIQUE KEY `uk_cap_flow_action_request` (`tenant_id`,`request_id`,`del_flag`),
  KEY `idx_cap_flow_action_actor` (`tenant_id`,`actor_user_id`,`create_time`),
  KEY `idx_cap_flow_action_record` (`tenant_id`,`object_code`,`record_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI能力受控流程动作执行日志';


-- forge_admin_new.ai_capability_grant definition

CREATE TABLE `ai_capability_grant` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `client_id` bigint NOT NULL,
  `capability_id` bigint NOT NULL,
  `version_strategy` varchar(16) NOT NULL DEFAULT 'PINNED',
  `fixed_version` varchar(32) DEFAULT NULL,
  `field_policy` json DEFAULT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'ENABLED',
  `expires_at` datetime DEFAULT NULL,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_capability_grant_active` (`tenant_id`,`client_id`,`capability_id`,`del_flag`),
  KEY `idx_ai_capability_grant_client` (`tenant_id`,`client_id`,`status`,`expires_at`,`del_flag`),
  KEY `idx_ai_capability_grant_capability` (`tenant_id`,`capability_id`,`status`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI中枢客户端能力授权';


-- forge_admin_new.ai_capability_invocation_log definition

CREATE TABLE `ai_capability_invocation_log` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `request_id` varchar(64) NOT NULL,
  `client_id` bigint DEFAULT NULL,
  `client_code` varchar(64) DEFAULT NULL,
  `capability_id` bigint DEFAULT NULL,
  `capability_code` varchar(128) NOT NULL,
  `capability_version` varchar(32) DEFAULT NULL,
  `actor_type` varchar(16) DEFAULT NULL,
  `actor_user_id` bigint DEFAULT NULL,
  `service_user_id` bigint DEFAULT NULL,
  `active_org_id` bigint DEFAULT NULL,
  `result_status` varchar(24) NOT NULL,
  `result_code` varchar(64) NOT NULL,
  `error_code` varchar(64) DEFAULT NULL,
  `schema_path` varchar(256) DEFAULT NULL,
  `trace_id` varchar(64) DEFAULT NULL,
  `duration_ms` bigint NOT NULL DEFAULT '0',
  `del_flag` tinyint NOT NULL DEFAULT '0',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_capability_invocation_request` (`tenant_id`,`request_id`),
  KEY `idx_ai_capability_invocation_client` (`tenant_id`,`client_id`,`create_time`,`del_flag`),
  KEY `idx_ai_capability_invocation_actor` (`tenant_id`,`actor_user_id`,`create_time`,`del_flag`),
  KEY `idx_ai_capability_invocation_capability` (`tenant_id`,`capability_id`,`create_time`,`del_flag`),
  KEY `idx_ai_capability_invocation_result` (`tenant_id`,`result_code`,`create_time`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI中枢能力安全调用日志';


-- forge_admin_new.ai_capability_oauth_redirect_uri definition

CREATE TABLE `ai_capability_oauth_redirect_uri` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `client_id` bigint NOT NULL,
  `redirect_uri` varchar(2048) NOT NULL,
  `redirect_uri_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'ENABLED',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_capability_redirect_active` (`tenant_id`,`client_id`,`redirect_uri_hash`,`del_flag`),
  KEY `idx_ai_capability_redirect_client` (`tenant_id`,`client_id`,`status`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI中枢OAuth客户端精确回调地址';


-- forge_admin_new.ai_capability_policy definition

CREATE TABLE `ai_capability_policy` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `capability_id` bigint NOT NULL COMMENT '能力ID',
  `capability_version` varchar(32) NOT NULL COMMENT '不可变能力版本',
  `risk_level` varchar(16) NOT NULL COMMENT '风险等级，当前仅HIGH',
  `approval_flow_model_key` varchar(128) NOT NULL COMMENT '专用审批流程模型Key',
  `approval_candidate_group` varchar(64) NOT NULL COMMENT '服务端审批候选组',
  `expire_seconds` int NOT NULL DEFAULT '86400' COMMENT '审批有效秒数',
  `status` varchar(16) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_capability_policy_version` (`tenant_id`,`capability_id`,`capability_version`,`del_flag`),
  KEY `idx_capability_policy_status` (`tenant_id`,`risk_level`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI能力风险与人工审批策略';


-- forge_admin_new.ai_capability_version definition

CREATE TABLE `ai_capability_version` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `capability_id` bigint NOT NULL,
  `version` varchar(32) NOT NULL,
  `input_schema` json NOT NULL,
  `output_schema` json NOT NULL,
  `source_type` varchar(32) NOT NULL,
  `source_key` varchar(128) NOT NULL,
  `source_version` varchar(64) NOT NULL,
  `behavior` varchar(24) NOT NULL,
  `risk_level` varchar(16) NOT NULL,
  `visibility` varchar(16) NOT NULL,
  `policy_snapshot` json DEFAULT NULL,
  `schema_checksum` char(64) NOT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'PUBLISHED',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_capability_version_active` (`tenant_id`,`capability_id`,`version`,`del_flag`),
  KEY `idx_ai_capability_version_checksum` (`tenant_id`,`capability_id`,`schema_checksum`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI中枢能力不可变版本';


-- forge_admin_new.ai_chat_record definition

CREATE TABLE `ai_chat_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `agent_code` varchar(50) DEFAULT NULL COMMENT 'Agent编码',
  `session_id` varchar(64) NOT NULL COMMENT '会话ID',
  `role` varchar(20) NOT NULL COMMENT '角色（user/assistant/system）',
  `content` longtext NOT NULL COMMENT '消息内容',
  `token_usage` int DEFAULT NULL COMMENT 'Token消耗',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2079974031044694019 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI对话记录表';


-- forge_admin_new.ai_chat_session definition

CREATE TABLE `ai_chat_session` (
  `id` varchar(64) NOT NULL COMMENT '会话ID（UUID，由前端或服务端生成）',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `agent_code` varchar(50) DEFAULT NULL COMMENT '关联的 Agent 编码',
  `session_name` varchar(200) DEFAULT NULL COMMENT '会话标题（首条消息截取）',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0正常 1已删除）',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '最后更新时间',
  `metadata` varchar(500) DEFAULT NULL COMMENT 'metadata数据JSON',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`,`status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI会话表';


-- forge_admin_new.ai_code_rule definition

CREATE TABLE `ai_code_rule` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `rule_code` varchar(64) NOT NULL COMMENT '规则编码',
  `rule_name` varchar(128) NOT NULL COMMENT '规则名称',
  `scene` varchar(64) NOT NULL DEFAULT 'COMMON' COMMENT '适用场景：COMMON通用/DOCUMENT单据/MATERIAL物料等',
  `category` varchar(64) NOT NULL DEFAULT 'COMMON' COMMENT '编码分类',
  `source_object_id` bigint DEFAULT NULL COMMENT '业务字段来源对象ID',
  `source_object_code` varchar(64) DEFAULT NULL COMMENT '业务字段来源对象编码',
  `template` varchar(256) NOT NULL COMMENT '编码模板',
  `reset_policy` varchar(32) NOT NULL DEFAULT 'AUTO' COMMENT '流水重置策略：AUTO/NONE/YEAR/MONTH/DAY/HOUR/MINUTE/SECOND',
  `seq_length` int NOT NULL DEFAULT '4' COMMENT '默认流水号长度',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：1启用 0停用',
  `builtin` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否内置规则',
  `in_code_list` tinyint NOT NULL DEFAULT '1' COMMENT '是否可在业务配置中选择',
  `version_no` int NOT NULL DEFAULT '1' COMMENT '乐观锁版本号',
  `legacy_compat_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否兼容旧编码计数器水位',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `options` json DEFAULT NULL COMMENT '扩展配置JSON',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_code_rule_code_active` (`tenant_id`,`rule_code`,`del_flag`),
  KEY `idx_ai_code_rule_scene` (`tenant_id`,`scene`,`status`),
  KEY `idx_ai_code_rule_update` (`tenant_id`,`update_time`),
  KEY `idx_ai_code_rule_category` (`tenant_id`,`category`,`status`,`del_flag`),
  KEY `idx_ai_code_rule_source_object` (`tenant_id`,`source_object_code`,`status`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-通用编码规则';


-- forge_admin_new.ai_code_rule_segment definition

CREATE TABLE `ai_code_rule_segment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分段ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `rule_id` bigint NOT NULL COMMENT '编码规则ID',
  `segment_key` varchar(32) NOT NULL COMMENT '稳定分段键',
  `segment_order` int NOT NULL COMMENT '分段顺序，从1开始',
  `segment_type` varchar(16) NOT NULL COMMENT 'DATE/FIXED/SEQ/VARIABLE/SYS_VAR',
  `segment_value` varchar(128) DEFAULT NULL COMMENT '格式、固定值或变量名',
  `variable_source` varchar(16) NOT NULL DEFAULT 'CUSTOM' COMMENT 'VARIABLE来源：CUSTOM/LOWCODE',
  `segment_length` int DEFAULT NULL COMMENT '声明长度或最大长度',
  `pad_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否补位',
  `pad_char` varchar(4) DEFAULT NULL COMMENT '补位字符',
  `pad_direction` varchar(8) NOT NULL DEFAULT 'LEFT' COMMENT 'LEFT/RIGHT',
  `group_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否参与流水分组',
  `include_in_code` tinyint NOT NULL DEFAULT '1' COMMENT '是否输出到最终编码',
  `radix_type` varchar(32) DEFAULT NULL COMMENT 'SEQ进制类型',
  `reset_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否按周期切换计数器',
  `reset_policy` varchar(16) NOT NULL DEFAULT 'NONE' COMMENT 'NONE/YEAR/MONTH/DAY/HOUR',
  `start_value` bigint NOT NULL DEFAULT '1' COMMENT '新计数器起始值',
  `exclude_ambiguous` tinyint NOT NULL DEFAULT '0' COMMENT '是否移除I/O/Z等易混淆字符',
  `excluded_characters` varchar(16) DEFAULT NULL COMMENT '具体排除的易混淆字符，逗号分隔：I/O/Z',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_code_rule_segment_key_active` (`tenant_id`,`rule_id`,`segment_key`,`del_flag`),
  KEY `idx_ai_code_rule_segment_order` (`tenant_id`,`rule_id`,`del_flag`,`segment_order`,`id`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='编码规则结构化分段';


-- forge_admin_new.ai_context_config definition

CREATE TABLE `ai_context_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint DEFAULT '1' COMMENT '租户ID',
  `agent_code` varchar(64) NOT NULL COMMENT '关联Agent编码',
  `config_name` varchar(128) NOT NULL COMMENT '配置名称',
  `config_content` text NOT NULL COMMENT '上下文内容',
  `config_type` varchar(32) NOT NULL DEFAULT 'SPEC' COMMENT '类型：SPEC/SAMPLE/RULE',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
  PRIMARY KEY (`id`),
  KEY `idx_agent_code` (`agent_code`)
) ENGINE=InnoDB AUTO_INCREMENT=2055282589253537798 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI上下文配置表';


-- forge_admin_new.ai_crud_config definition

CREATE TABLE `ai_crud_config` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint DEFAULT '1' COMMENT '租户ID',
  `config_key` varchar(64) NOT NULL COMMENT '配置唯一标识（小写字母+数字+下划线）',
  `table_name` varchar(128) NOT NULL COMMENT '关联的数据库表名',
  `table_comment` varchar(255) DEFAULT '' COMMENT '表描述/功能名称',
  `app_name` varchar(128) DEFAULT NULL COMMENT '低代码应用名称',
  `search_schema` json DEFAULT NULL COMMENT '搜索表单Schema JSON',
  `columns_schema` json DEFAULT NULL COMMENT '表格列Schema JSON',
  `edit_schema` json DEFAULT NULL COMMENT '编辑表单Schema JSON',
  `api_config` json DEFAULT NULL COMMENT 'API配置JSON（method@url格式）',
  `options` json DEFAULT NULL COMMENT '其他AiCrudPage配置（modalType/modalWidth/gridCols等）',
  `mode` varchar(16) NOT NULL DEFAULT 'CONFIG' COMMENT '模式：CONFIG-配置驱动 / CODEGEN-代码生成',
  `build_mode` varchar(16) NOT NULL DEFAULT 'AI' COMMENT '构建模式：AI智能生成 LOWCODE可视化低代码 CODEGEN代码生成',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0启用 1停用）',
  `publish_status` varchar(16) NOT NULL DEFAULT 'DRAFT' COMMENT '发布状态：DRAFT草稿 PUBLISHED已发布 STOPPED已停用',
  `menu_name` varchar(128) DEFAULT '' COMMENT '菜单名称',
  `menu_parent_id` bigint DEFAULT NULL COMMENT '菜单父级ID',
  `menu_sort` int DEFAULT '0' COMMENT '菜单排序',
  `menu_resource_id` bigint DEFAULT NULL COMMENT '关联的sys_resource记录ID',
  `dict_config` json DEFAULT NULL COMMENT '字典数据配置',
  `desensitize_config` json DEFAULT NULL COMMENT '字段脱敏配置',
  `encrypt_config` json DEFAULT NULL COMMENT '接口加解密配置',
  `trans_config` json DEFAULT NULL COMMENT '字典翻译配置',
  `layout_type` varchar(64) DEFAULT 'simple-crud' COMMENT '页面模板类型（对应 ai_page_template.template_key）',
  `model_schema` json DEFAULT NULL COMMENT '可视化数据模型协议',
  `page_schema` json DEFAULT NULL COMMENT '可视化页面搭建协议',
  `draft_version` int NOT NULL DEFAULT '0' COMMENT '草稿版本号',
  `published_version` int NOT NULL DEFAULT '0' COMMENT '已发布版本号',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `publish_by` bigint DEFAULT NULL COMMENT '发布人',
  `domain_id` bigint DEFAULT NULL COMMENT '业务领域ID',
  `domain_code` varchar(48) DEFAULT NULL COMMENT '业务领域编码',
  `object_code` varchar(48) DEFAULT NULL COMMENT '业务对象编码',
  `object_name` varchar(128) DEFAULT NULL COMMENT '业务对象名称',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `runtime_datasource_id` bigint DEFAULT NULL COMMENT '运行数据源ID',
  `runtime_datasource_code` varchar(64) DEFAULT NULL COMMENT '运行数据源编码',
  `runtime_datasource_snapshot` json DEFAULT NULL COMMENT '运行数据源快照，不含密码',
  `runtime_table_name` varchar(128) DEFAULT NULL COMMENT '运行表名',
  `primary_key_field` varchar(64) NOT NULL DEFAULT 'id' COMMENT '主键字段名',
  `primary_key_column` varchar(64) NOT NULL DEFAULT 'id' COMMENT '主键列名',
  `primary_key_type` varchar(32) NOT NULL DEFAULT 'bigint' COMMENT '主键类型',
  `tenant_strategy` json DEFAULT NULL COMMENT '租户隔离策略',
  `audit_strategy` json DEFAULT NULL COMMENT '审计字段策略',
  `logic_delete_strategy` json DEFAULT NULL COMMENT '逻辑删除策略',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key_active` (`config_key`,`del_flag`),
  KEY `idx_table_name` (`table_name`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_ai_crud_publish_status` (`tenant_id`,`publish_status`,`update_time`),
  KEY `idx_ai_crud_domain` (`tenant_id`,`domain_id`,`publish_status`,`update_time`),
  KEY `idx_ai_crud_domain_object` (`tenant_id`,`domain_id`,`object_code`),
  KEY `idx_ai_crud_runtime_ds` (`tenant_id`,`runtime_datasource_id`,`publish_status`),
  KEY `idx_ai_crud_lowcode_page` (`tenant_id`,`mode`,`build_mode`,`update_time`,`id`),
  KEY `idx_ai_crud_runtime_object_lookup` (`tenant_id`,`object_code`,`mode`,`build_mode`,`publish_status`,`publish_time`),
  KEY `idx_ai_crud_runtime_config_lookup` (`tenant_id`,`config_key`,`mode`,`build_mode`,`publish_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='CRUD配置驱动表';


-- forge_admin_new.ai_crud_config_version definition

CREATE TABLE `ai_crud_config_version` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `config_id` bigint NOT NULL COMMENT 'CRUD配置ID',
  `config_key` varchar(64) NOT NULL COMMENT 'CRUD配置键',
  `domain_id` bigint DEFAULT NULL COMMENT '业务领域ID',
  `domain_code` varchar(48) DEFAULT NULL COMMENT '业务领域编码',
  `object_code` varchar(48) DEFAULT NULL COMMENT '业务对象编码',
  `object_name` varchar(128) DEFAULT NULL COMMENT '业务对象名称',
  `version_no` int NOT NULL COMMENT '版本号',
  `version_type` varchar(20) NOT NULL DEFAULT 'publish' COMMENT '版本类型：publish发布 rollback回滚',
  `model_schema` json DEFAULT NULL COMMENT '可视化数据模型协议',
  `page_schema` json DEFAULT NULL COMMENT '可视化页面搭建协议',
  `search_schema` json DEFAULT NULL COMMENT '搜索表单Schema JSON',
  `columns_schema` json DEFAULT NULL COMMENT '表格列Schema JSON',
  `edit_schema` json DEFAULT NULL COMMENT '编辑表单Schema JSON',
  `api_config` json DEFAULT NULL COMMENT 'API配置JSON',
  `options` json DEFAULT NULL COMMENT 'AiCrudPage运行时配置',
  `publish_snapshot` longtext COMMENT '发布快照JSON',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `runtime_datasource_id` bigint DEFAULT NULL COMMENT '运行数据源ID',
  `runtime_datasource_code` varchar(64) DEFAULT NULL COMMENT '运行数据源编码',
  `runtime_datasource_snapshot` json DEFAULT NULL COMMENT '运行数据源快照，不含密码',
  `runtime_table_name` varchar(128) DEFAULT NULL COMMENT '运行表名',
  `primary_key_field` varchar(64) NOT NULL DEFAULT 'id' COMMENT '主键字段名',
  `primary_key_column` varchar(64) NOT NULL DEFAULT 'id' COMMENT '主键列名',
  `primary_key_type` varchar(32) NOT NULL DEFAULT 'bigint' COMMENT '主键类型',
  `tenant_strategy` json DEFAULT NULL COMMENT '租户隔离策略',
  `audit_strategy` json DEFAULT NULL COMMENT '审计字段策略',
  `logic_delete_strategy` json DEFAULT NULL COMMENT '逻辑删除策略',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_crud_version_no` (`tenant_id`,`config_id`,`version_no`),
  KEY `idx_ai_crud_version_config` (`tenant_id`,`config_id`,`create_time`),
  KEY `idx_ai_crud_version_key` (`tenant_id`,`config_key`,`version_no`),
  KEY `idx_ai_crud_version_domain` (`tenant_id`,`domain_id`,`version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI CRUD低代码发布版本表';


-- forge_admin_new.ai_crud_export_task definition

CREATE TABLE `ai_crud_export_task` (
  `id` bigint NOT NULL COMMENT '任务ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `config_key` varchar(64) NOT NULL COMMENT 'CRUD配置键',
  `export_name` varchar(128) DEFAULT NULL COMMENT '导出名称',
  `file_name` varchar(255) DEFAULT NULL COMMENT '导出文件名',
  `file_id` varchar(64) DEFAULT NULL COMMENT '文件中心ID',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小(字节)',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态:PENDING/RUNNING/SUCCESS/FAILED',
  `total_count` bigint NOT NULL DEFAULT '0' COMMENT '总数据量',
  `exported_count` bigint NOT NULL DEFAULT '0' COMMENT '已导出数据量',
  `progress` int NOT NULL DEFAULT '0' COMMENT '导出进度百分比',
  `query_params` json DEFAULT NULL COMMENT '导出查询参数快照',
  `error_message` varchar(1000) DEFAULT NULL COMMENT '失败原因',
  `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
  `expire_time` datetime DEFAULT NULL COMMENT '文件过期时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建组织ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_crud_export_task_user` (`tenant_id`,`create_by`,`config_key`,`create_time`),
  KEY `idx_ai_crud_export_task_status` (`tenant_id`,`status`,`expire_time`),
  KEY `idx_ai_crud_export_task_file` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通用CRUD异步导出任务表';


-- forge_admin_new.ai_custom_query_scheme definition

CREATE TABLE `ai_custom_query_scheme` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `config_key` varchar(64) NOT NULL COMMENT 'CRUD配置键',
  `scheme_name` varchar(128) NOT NULL COMMENT '查询方案名称',
  `conditions_json` json DEFAULT NULL COMMENT '查询条件JSON',
  `columns_json` json DEFAULT NULL COMMENT '展示字段JSON',
  `sort_json` json DEFAULT NULL COMMENT '排序配置JSON',
  `display_json` json DEFAULT NULL COMMENT '展示方式配置JSON',
  `is_default` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否默认方案（1是 0否）',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_config_user` (`tenant_id`,`config_key`,`create_by`),
  KEY `idx_tenant_config_default` (`tenant_id`,`config_key`,`create_by`,`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI自定义查询方案表';


-- forge_admin_new.ai_dashboard_component_lineage definition

CREATE TABLE `ai_dashboard_component_lineage` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `record_id` bigint NOT NULL COMMENT '生成记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `session_id` varchar(80) DEFAULT NULL COMMENT 'AI会话ID',
  `project_id` bigint DEFAULT NULL COMMENT '大屏项目ID',
  `project_name` varchar(128) DEFAULT NULL COMMENT '大屏项目名称',
  `business_definition_id` bigint DEFAULT NULL COMMENT '业务定义ID',
  `business_name` varchar(128) DEFAULT NULL COMMENT '业务定义名称',
  `component_index` int DEFAULT NULL COMMENT '组件序号',
  `component_key` varchar(100) DEFAULT NULL COMMENT '组件Key',
  `component_title` varchar(200) DEFAULT NULL COMMENT '组件标题',
  `dataset_id` bigint NOT NULL COMMENT '数据集ID',
  `dataset_name` varchar(128) DEFAULT NULL COMMENT '数据集名称',
  `field_names` varchar(2000) DEFAULT NULL COMMENT '使用字段，逗号分隔',
  `binding_status` varchar(32) DEFAULT NULL COMMENT '绑定状态',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_dashboard_lineage_dataset` (`tenant_id`,`dataset_id`,`create_time`),
  KEY `idx_ai_dashboard_lineage_record` (`tenant_id`,`record_id`),
  KEY `idx_ai_dashboard_lineage_project` (`tenant_id`,`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI大屏组件数据资产血缘';


-- forge_admin_new.ai_dashboard_generate_record definition

CREATE TABLE `ai_dashboard_generate_record` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `session_id` varchar(80) DEFAULT NULL COMMENT 'AI会话ID',
  `project_id` bigint DEFAULT NULL COMMENT '大屏项目ID',
  `project_name` varchar(128) DEFAULT NULL COMMENT '大屏项目名称',
  `business_definition_id` bigint DEFAULT NULL COMMENT '业务定义ID',
  `business_name` varchar(128) DEFAULT NULL COMMENT '业务定义名称',
  `provider_id` bigint DEFAULT NULL COMMENT 'AI供应商ID',
  `provider_name` varchar(128) DEFAULT NULL COMMENT 'AI供应商名称',
  `model_name` varchar(128) DEFAULT NULL COMMENT '模型名称',
  `style` varchar(20) DEFAULT NULL COMMENT '生成风格',
  `canvas_width` int DEFAULT NULL COMMENT '画布宽度',
  `canvas_height` int DEFAULT NULL COMMENT '画布高度',
  `prompt` text COMMENT '用户提示词',
  `request_json` longtext COMMENT '生成请求JSON',
  `generated_title` varchar(200) DEFAULT NULL COMMENT '生成大屏标题',
  `response_json` longtext COMMENT '生成结果JSON',
  `validation_summary_json` longtext COMMENT '前端校验摘要JSON',
  `status` varchar(32) NOT NULL DEFAULT 'success' COMMENT '状态：success/failed/parse_failed/stopped',
  `component_count` int NOT NULL DEFAULT '0' COMMENT '组件数量',
  `bound_count` int NOT NULL DEFAULT '0' COMMENT '动态绑定数量',
  `static_count` int NOT NULL DEFAULT '0' COMMENT '静态组件数量',
  `static_fallback_count` int NOT NULL DEFAULT '0' COMMENT '静态降级数量',
  `repaired_count` int NOT NULL DEFAULT '0' COMMENT '自动修复数量',
  `elapsed_ms` bigint DEFAULT NULL COMMENT '生成耗时毫秒',
  `error_message` varchar(1000) DEFAULT NULL COMMENT '错误原因',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_dashboard_record_user_time` (`tenant_id`,`user_id`,`create_time`),
  KEY `idx_ai_dashboard_record_business` (`tenant_id`,`business_definition_id`,`create_time`),
  KEY `idx_ai_dashboard_record_project` (`tenant_id`,`project_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI大屏生成记录';


-- forge_admin_new.ai_formula_execution_log definition

CREATE TABLE `ai_formula_execution_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `trace_id` varchar(64) DEFAULT NULL COMMENT '公式执行链路ID',
  `object_code` varchar(64) NOT NULL COMMENT '业务对象编码',
  `record_id` varchar(64) DEFAULT NULL COMMENT '业务记录ID',
  `field_code` varchar(64) NOT NULL COMMENT '公式字段编码',
  `formula_type` varchar(32) DEFAULT NULL COMMENT '公式类型：CALC/AGGREGATE/CONDITIONAL/LOOKUP',
  `formula_mode` varchar(32) DEFAULT NULL COMMENT '计算模式：STORED/VIRTUAL',
  `expression` text COMMENT '表达式或配置摘要',
  `input_snapshot` json DEFAULT NULL COMMENT '输入快照，脱敏后存储',
  `output_value` text COMMENT '输出值摘要，脱敏后存储',
  `success` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否执行成功：1成功 0失败',
  `error_message` text COMMENT '错误信息',
  `elapsed_ms` bigint DEFAULT NULL COMMENT '执行耗时(毫秒)',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_formula_log_trace` (`tenant_id`,`trace_id`),
  KEY `idx_ai_formula_log_record` (`tenant_id`,`object_code`,`record_id`,`create_time`),
  KEY `idx_ai_formula_log_field` (`tenant_id`,`field_code`,`create_time`),
  KEY `idx_ai_formula_log_success` (`tenant_id`,`success`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-公式执行日志表';


-- forge_admin_new.ai_formula_function definition

CREATE TABLE `ai_formula_function` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `function_code` varchar(64) NOT NULL COMMENT '函数编码',
  `display_name` varchar(128) NOT NULL COMMENT '展示名称',
  `category` varchar(64) NOT NULL COMMENT '函数分类',
  `description` varchar(500) DEFAULT NULL COMMENT '函数说明',
  `source_type` varchar(32) NOT NULL DEFAULT 'BUILTIN' COMMENT '来源类型：BUILTIN/SYSTEM/TENANT/MARKET',
  `argument_schema` json DEFAULT NULL COMMENT '参数Schema',
  `return_type` varchar(32) DEFAULT NULL COMMENT '返回类型',
  `example` text COMMENT '示例',
  `status` varchar(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
  `current_version` varchar(32) DEFAULT NULL COMMENT '当前版本',
  `latest_version` varchar(32) DEFAULT NULL COMMENT '最新版本',
  `builtin` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否内置函数',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_formula_function_code` (`tenant_id`,`function_code`),
  KEY `idx_ai_formula_function_query` (`tenant_id`,`category`,`source_type`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-公式函数注册表';


-- forge_admin_new.ai_formula_function_install definition

CREATE TABLE `ai_formula_function_install` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `function_code` varchar(64) NOT NULL COMMENT '函数编码',
  `installed_version` varchar(32) NOT NULL COMMENT '已安装版本',
  `install_status` varchar(32) NOT NULL DEFAULT 'INSTALLED' COMMENT '安装状态：INSTALLED/UNINSTALLED',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `source_type` varchar(32) NOT NULL DEFAULT 'BUILTIN' COMMENT '来源类型：BUILTIN/SYSTEM/TENANT/MARKET',
  `installed_by` bigint DEFAULT NULL COMMENT '安装人',
  `installed_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '安装时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_formula_function_install` (`tenant_id`,`function_code`),
  KEY `idx_ai_formula_function_install_status` (`tenant_id`,`install_status`,`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-公式函数安装表';


-- forge_admin_new.ai_formula_function_version definition

CREATE TABLE `ai_formula_function_version` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `function_code` varchar(64) NOT NULL COMMENT '函数编码',
  `version` varchar(32) NOT NULL COMMENT '函数版本',
  `implementation_type` varchar(32) NOT NULL DEFAULT 'JAVA_BEAN' COMMENT '实现类型',
  `bean_name` varchar(128) DEFAULT NULL COMMENT 'Spring Bean名称',
  `method_name` varchar(128) DEFAULT NULL COMMENT '方法名称',
  `argument_schema` json DEFAULT NULL COMMENT '参数Schema',
  `return_type` varchar(32) DEFAULT NULL COMMENT '返回类型',
  `example` text COMMENT '示例',
  `release_note` varchar(500) DEFAULT NULL COMMENT '版本说明',
  `status` varchar(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED/DEPRECATED',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_formula_function_version` (`tenant_id`,`function_code`,`version`),
  KEY `idx_ai_formula_function_version_code` (`tenant_id`,`function_code`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-公式函数版本表';


-- forge_admin_new.ai_lowcode_domain definition

CREATE TABLE `ai_lowcode_domain` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父级领域ID，0表示根节点',
  `domain_code` varchar(48) NOT NULL COMMENT '领域编码',
  `domain_name` varchar(128) NOT NULL COMMENT '领域名称',
  `domain_desc` varchar(500) DEFAULT NULL COMMENT '领域说明',
  `icon` varchar(128) DEFAULT NULL COMMENT '领域图标',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` varchar(16) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED启用 DISABLED停用',
  `menu_parent_id` bigint DEFAULT NULL COMMENT '默认菜单父级ID',
  `table_prefix` varchar(64) NOT NULL DEFAULT 'biz_' COMMENT '默认表名前缀',
  `config_key_prefix` varchar(64) NOT NULL DEFAULT 'biz_' COMMENT '默认配置键前缀',
  `default_app_type` varchar(32) NOT NULL DEFAULT 'SINGLE' COMMENT '默认应用类型',
  `default_layout_type` varchar(64) NOT NULL DEFAULT 'simple-crud' COMMENT '默认页面模板',
  `default_table_mode` varchar(32) NOT NULL DEFAULT 'CREATE' COMMENT '默认建表模式',
  `domain_schema` json DEFAULT NULL COMMENT '领域扩展协议',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_lowcode_domain_code_active` (`tenant_id`,`domain_code`,`del_flag`),
  UNIQUE KEY `uk_ai_lowcode_domain_name_active` (`tenant_id`,`parent_id`,`domain_name`,`del_flag`),
  KEY `idx_ai_lowcode_domain_parent` (`tenant_id`,`parent_id`,`sort`),
  KEY `idx_ai_lowcode_domain_status` (`tenant_id`,`status`,`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI低代码业务领域表';


-- forge_admin_new.ai_lowcode_model definition

CREATE TABLE `ai_lowcode_model` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `domain_id` bigint NOT NULL COMMENT '业务领域ID',
  `domain_code` varchar(48) NOT NULL COMMENT '业务领域编码',
  `model_code` varchar(48) NOT NULL COMMENT '模型编码',
  `model_name` varchar(128) NOT NULL COMMENT '模型名称',
  `model_desc` varchar(512) DEFAULT NULL COMMENT '模型描述',
  `status` varchar(16) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED启用 DISABLED停用',
  `tenant_enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用多租户',
  `master_data` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否主数据模型',
  `model_schema` longtext COMMENT '模型结构协议JSON',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `runtime_datasource_id` bigint DEFAULT NULL COMMENT '模型运行数据源ID',
  `runtime_datasource_code` varchar(64) DEFAULT NULL COMMENT '模型运行数据源编码',
  `runtime_table_name` varchar(128) DEFAULT NULL COMMENT '模型运行表名',
  `table_mode` varchar(16) DEFAULT NULL COMMENT '表模式：CREATE/EXISTING',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_lowcode_model_code_active` (`tenant_id`,`domain_id`,`model_code`,`del_flag`),
  KEY `idx_ai_lowcode_model_domain` (`tenant_id`,`domain_id`,`status`),
  KEY `idx_ai_lowcode_model_master` (`tenant_id`,`master_data`,`status`),
  KEY `idx_lowcode_model_runtime_ds` (`tenant_id`,`runtime_datasource_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI低代码数据模型表';


-- forge_admin_new.ai_model definition

CREATE TABLE `ai_model` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `provider_id` bigint NOT NULL COMMENT '供应商ID',
  `model_type` varchar(32) NOT NULL COMMENT '模型类型(chat/embedding/image/audio)',
  `model_id` varchar(128) NOT NULL COMMENT '模型标识(如gpt-4o)',
  `model_name` varchar(128) NOT NULL COMMENT '模型显示名称',
  `description` varchar(512) DEFAULT NULL COMMENT '描述',
  `max_tokens` int DEFAULT NULL COMMENT '最大Token数',
  `context_window` int DEFAULT NULL COMMENT '上下文窗口Token数',
  `input_price_per_million_cent` bigint DEFAULT NULL COMMENT '输入价格（分/百万Token）',
  `output_price_per_million_cent` bigint DEFAULT NULL COMMENT '输出价格（分/百万Token）',
  `icon` varchar(512) DEFAULT NULL COMMENT '模型图标(文件ID或URL)',
  `is_default` char(1) DEFAULT '0' COMMENT '是否默认模型(0否1是)',
  `status` char(1) DEFAULT '0' COMMENT '状态(0正常1停用)',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户编号',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_model_provider_model_active` (`provider_id`,`model_id`,`del_flag`),
  KEY `idx_ai_model_provider_id` (`provider_id`),
  KEY `idx_ai_model_type` (`model_type`)
) ENGINE=InnoDB AUTO_INCREMENT=2075737166192865283 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI模型表';


-- forge_admin_new.ai_model_capability definition

CREATE TABLE `ai_model_capability` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `model_id` bigint NOT NULL,
  `capability_code` varchar(64) NOT NULL,
  `config_json` json DEFAULT NULL,
  `status` char(1) NOT NULL DEFAULT '0',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_model_capability_active` (`tenant_id`,`model_id`,`capability_code`,`del_flag`),
  KEY `idx_ai_model_capability_model` (`tenant_id`,`model_id`,`status`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI模型能力关系';


-- forge_admin_new.ai_model_invocation_log definition

CREATE TABLE `ai_model_invocation_log` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `request_id` varchar(64) NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `agent_code` varchar(64) NOT NULL,
  `session_id` varchar(80) DEFAULT NULL,
  `phase` varchar(24) NOT NULL,
  `dispatched` tinyint NOT NULL DEFAULT '0',
  `route_source` varchar(32) DEFAULT NULL,
  `route_reason` varchar(64) DEFAULT NULL,
  `route_policy_id` bigint DEFAULT NULL,
  `provider_id` bigint DEFAULT NULL,
  `model_id` bigint DEFAULT NULL,
  `provider_model_id` varchar(128) DEFAULT NULL,
  `adapter_code` varchar(32) DEFAULT NULL,
  `outcome` varchar(24) NOT NULL,
  `error_category` varchar(32) DEFAULT NULL,
  `http_status` int DEFAULT NULL,
  `error_code` varchar(64) DEFAULT NULL,
  `latency_ms` bigint NOT NULL DEFAULT '0',
  `prompt_tokens` bigint DEFAULT NULL,
  `completion_tokens` bigint DEFAULT NULL,
  `total_tokens` bigint DEFAULT NULL,
  `usage_available` tinyint NOT NULL DEFAULT '0',
  `cost_available` tinyint NOT NULL DEFAULT '0',
  `input_price_per_million_cent` bigint DEFAULT NULL,
  `output_price_per_million_cent` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_invocation_request` (`tenant_id`,`request_id`),
  KEY `idx_ai_invocation_tenant_time` (`tenant_id`,`create_time`),
  KEY `idx_ai_invocation_model_time` (`tenant_id`,`model_id`,`create_time`),
  KEY `idx_ai_invocation_agent_time` (`tenant_id`,`agent_code`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI模型调用治理日志';


-- forge_admin_new.ai_model_route_policy definition

CREATE TABLE `ai_model_route_policy` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `policy_code` varchar(64) NOT NULL,
  `policy_name` varchar(128) NOT NULL,
  `required_capabilities` json DEFAULT NULL,
  `status` char(1) NOT NULL DEFAULT '0',
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_route_policy_code_active` (`tenant_id`,`policy_code`,`del_flag`),
  KEY `idx_ai_route_policy_status` (`tenant_id`,`status`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI模型路由策略';


-- forge_admin_new.ai_model_route_target definition

CREATE TABLE `ai_model_route_target` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `policy_id` bigint NOT NULL,
  `model_id` bigint NOT NULL,
  `priority` int NOT NULL DEFAULT '100',
  `status` char(1) NOT NULL DEFAULT '0',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_route_target_active` (`tenant_id`,`policy_id`,`model_id`,`del_flag`),
  KEY `idx_ai_route_target_order` (`tenant_id`,`policy_id`,`status`,`del_flag`,`priority`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI模型路由显式候选';


-- forge_admin_new.ai_page_template definition

CREATE TABLE `ai_page_template` (
  `id` bigint NOT NULL COMMENT '主键',
  `template_key` varchar(64) NOT NULL COMMENT '模板唯一标识（如 simple-crud / tree-crud）',
  `template_name` varchar(128) NOT NULL COMMENT '模板显示名称',
  `description` varchar(512) DEFAULT '' COMMENT '模板描述',
  `icon` varchar(64) DEFAULT '' COMMENT '图标（mdi: 前缀）',
  `system_prompt` text COMMENT '该模板专属的 AI system prompt 补充',
  `schema_hint` json DEFAULT NULL COMMENT '告诉 AI 该模板可用的字段约束（JSON）',
  `default_config` json DEFAULT NULL COMMENT '默认配置值（JSON，如 modalType/searchGridCols 等）',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用（1启用 0停用）',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `is_builtin` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否内置（1内置不可删除）',
  `codegen_type` varchar(16) NOT NULL DEFAULT 'TEMPLATE' COMMENT '代码生成策略：TEMPLATE-Velocity模板 / AI-大模型生成',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_key_active` (`template_key`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI页面模板表（组件市场）';


-- forge_admin_new.ai_prompt_template definition

CREATE TABLE `ai_prompt_template` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `template_name` varchar(128) NOT NULL COMMENT '模板名称',
  `template_code` varchar(80) DEFAULT NULL COMMENT '模板编码',
  `usage_scene` varchar(64) NOT NULL DEFAULT 'dashboard_generate' COMMENT '使用场景：dashboard_generate大屏生成等',
  `business_category` varchar(64) DEFAULT NULL COMMENT '业务分类',
  `domain_category` varchar(64) DEFAULT NULL COMMENT '领域分类',
  `template_tags` varchar(255) DEFAULT NULL COMMENT '模板标签，逗号分隔',
  `description` varchar(500) DEFAULT NULL COMMENT '模板说明',
  `prompt_content` longtext NOT NULL COMMENT '提示词内容',
  `example_input` text COMMENT '示例输入',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态：0启用 1停用',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `is_recommended` char(1) NOT NULL DEFAULT '0' COMMENT '是否推荐：0否 1是',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `use_count` int NOT NULL DEFAULT '0' COMMENT '使用次数',
  `test_count` int NOT NULL DEFAULT '0' COMMENT '测试次数',
  `download_count` int NOT NULL DEFAULT '0' COMMENT '下载次数',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_prompt_template_code_active` (`tenant_id`,`template_code`,`del_flag`),
  KEY `idx_ai_prompt_template_scene` (`tenant_id`,`usage_scene`,`status`,`sort_order`),
  KEY `idx_ai_prompt_template_category` (`tenant_id`,`business_category`,`domain_category`),
  KEY `idx_ai_prompt_template_recommend` (`tenant_id`,`is_recommended`,`use_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI提示词模板库';


-- forge_admin_new.ai_provider definition

CREATE TABLE `ai_provider` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `provider_name` varchar(50) NOT NULL COMMENT '供应商名称（如 阿里百炼、OpenAI）',
  `provider_type` varchar(30) NOT NULL COMMENT '类型（openai/azure/dashscope/ollama）',
  `adapter_code` varchar(32) NOT NULL DEFAULT 'openai_compatible' COMMENT '连接适配器（openai_compatible/dashscope_native）',
  `logo` varchar(512) DEFAULT NULL COMMENT '供应商Logo(文件ID或URL)',
  `api_key` varchar(500) NOT NULL COMMENT 'API Key',
  `base_url` varchar(500) DEFAULT NULL COMMENT 'API Base URL',
  `models` json DEFAULT NULL COMMENT '可用模型列表 [{"name":"qwen-plus","enabled":true}]',
  `is_default` char(1) NOT NULL DEFAULT '0' COMMENT '是否默认供应商（0否 1是）',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
  `default_model` varchar(100) DEFAULT NULL COMMENT '默认模型名称',
  `create_dept` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI供应商配置表';


-- forge_admin_new.ai_report_data_business_dataset definition

CREATE TABLE `ai_report_data_business_dataset` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `business_id` bigint NOT NULL COMMENT '业务定义ID',
  `dataset_id` bigint NOT NULL COMMENT '数据集ID',
  `is_primary` tinyint NOT NULL DEFAULT '0' COMMENT '是否主数据集：1是 0否',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `usage_remark` varchar(500) DEFAULT NULL COMMENT '数据集用途说明',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_business_dataset` (`tenant_id`,`business_id`,`dataset_id`),
  KEY `idx_data_business_dataset_business` (`tenant_id`,`business_id`),
  KEY `idx_data_business_dataset_dataset` (`tenant_id`,`dataset_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台业务定义数据集绑定';


-- forge_admin_new.ai_report_data_business_definition definition

CREATE TABLE `ai_report_data_business_definition` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `business_code` varchar(64) NOT NULL COMMENT '业务编码',
  `business_name` varchar(100) NOT NULL COMMENT '业务名称',
  `business_desc` text NOT NULL COMMENT '业务定义描述',
  `analysis_goal` text COMMENT '分析目标',
  `metric_definition` text COMMENT '指标口径',
  `dimension_definition` text COMMENT '分析维度',
  `usage_guide` text COMMENT 'AI使用建议',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_business_code_tenant_active` (`tenant_id`,`business_code`,`del_flag`),
  KEY `idx_data_business_status` (`tenant_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台业务定义';


-- forge_admin_new.ai_report_data_connection definition

CREATE TABLE `ai_report_data_connection` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `connection_code` varchar(64) NOT NULL COMMENT '连接编码',
  `connection_name` varchar(100) NOT NULL COMMENT '连接名称',
  `db_type` varchar(32) NOT NULL COMMENT '数据库类型：MYSQL/POSTGRESQL/ORACLE/SQLSERVER',
  `driver_class_name` varchar(200) NOT NULL COMMENT '驱动类名',
  `jdbc_url` varchar(1000) NOT NULL COMMENT 'JDBC连接地址',
  `username` varchar(128) NOT NULL COMMENT '用户名',
  `password_cipher` varchar(500) DEFAULT NULL COMMENT '密码密文',
  `schema_name` varchar(128) DEFAULT NULL COMMENT '模式名/数据库名',
  `test_sql` varchar(200) NOT NULL DEFAULT 'SELECT 1' COMMENT '测试SQL',
  `pool_config_json` json DEFAULT NULL COMMENT '连接池配置JSON',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_connection_code_tenant_active` (`tenant_id`,`connection_code`,`del_flag`),
  KEY `idx_data_connection_status` (`tenant_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台数据连接';


-- forge_admin_new.ai_report_data_dataset definition

CREATE TABLE `ai_report_data_dataset` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `dataset_code` varchar(64) NOT NULL COMMENT '数据集编码',
  `dataset_name` varchar(100) NOT NULL COMMENT '数据集名称',
  `connection_id` bigint NOT NULL COMMENT '数据连接ID',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `dataset_type` varchar(20) NOT NULL COMMENT '数据集类型：TABLE/SQL',
  `table_name` varchar(200) DEFAULT NULL COMMENT '单表模式表名',
  `sql_text` longtext COMMENT 'SQL模式查询SQL',
  `param_schema_json` json DEFAULT NULL COMMENT '参数定义JSON',
  `default_order_json` json DEFAULT NULL COMMENT '默认排序JSON',
  `max_rows` int NOT NULL DEFAULT '1000' COMMENT '最大返回行数',
  `timeout_seconds` int NOT NULL DEFAULT '15' COMMENT '查询超时时间秒',
  `cache_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否启用缓存：1是 0否',
  `cache_ttl_seconds` int DEFAULT NULL COMMENT '缓存秒数',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `publish_status` tinyint NOT NULL DEFAULT '0' COMMENT '发布状态：0未发布 1已发布 2已下架',
  `access_mode` varchar(20) NOT NULL DEFAULT 'PUBLIC' COMMENT '访问模式：PUBLIC公开 PRIVATE私有',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_dataset_code_tenant_active` (`tenant_id`,`dataset_code`,`del_flag`),
  KEY `idx_data_dataset_connection` (`tenant_id`,`connection_id`),
  KEY `idx_data_dataset_status` (`tenant_id`,`status`),
  KEY `idx_data_dataset_access_mode` (`tenant_id`,`access_mode`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台数据集';


-- forge_admin_new.ai_report_data_dataset_acl definition

CREATE TABLE `ai_report_data_dataset_acl` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `dataset_id` bigint NOT NULL COMMENT '数据集ID',
  `subject_type` varchar(20) NOT NULL COMMENT '授权主体类型：USER/ROLE/ORG',
  `subject_id` bigint NOT NULL COMMENT '授权主体ID',
  `access_level` varchar(20) NOT NULL DEFAULT 'QUERY' COMMENT '访问级别：VIEW/QUERY/MANAGE',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_dataset_acl_subject` (`tenant_id`,`dataset_id`,`subject_type`,`subject_id`,`access_level`),
  KEY `idx_data_dataset_acl_dataset` (`tenant_id`,`dataset_id`),
  KEY `idx_data_dataset_acl_subject` (`tenant_id`,`subject_type`,`subject_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台数据集授权';


-- forge_admin_new.ai_report_data_dataset_category definition

CREATE TABLE `ai_report_data_dataset_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `parent_id` bigint DEFAULT NULL COMMENT '父分类ID',
  `level` int NOT NULL DEFAULT '1' COMMENT '层级',
  `ancestors` varchar(500) NOT NULL DEFAULT '0/' COMMENT '祖先路径',
  `category_code` varchar(64) NOT NULL COMMENT '分类编码',
  `category_name` varchar(100) NOT NULL COMMENT '分类名称',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_dataset_category_code_tenant_active` (`tenant_id`,`category_code`,`del_flag`),
  KEY `idx_data_dataset_category_parent` (`tenant_id`,`parent_id`),
  KEY `idx_data_dataset_category_status` (`tenant_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台数据集分类';


-- forge_admin_new.ai_report_data_dataset_field definition

CREATE TABLE `ai_report_data_dataset_field` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `dataset_id` bigint NOT NULL COMMENT '数据集ID',
  `field_name` varchar(128) NOT NULL COMMENT '字段名/SQL别名',
  `field_label` varchar(128) DEFAULT NULL COMMENT '字段显示名',
  `source_column` varchar(128) DEFAULT NULL COMMENT '来源列名',
  `db_type` varchar(100) DEFAULT NULL COMMENT '数据库字段类型',
  `data_type` varchar(32) NOT NULL DEFAULT 'STRING' COMMENT '标准类型：STRING/NUMBER/DATE/DATETIME/BOOLEAN',
  `field_role` varchar(20) NOT NULL DEFAULT 'DIMENSION' COMMENT '字段角色：DIMENSION/MEASURE',
  `default_agg` varchar(20) DEFAULT NULL COMMENT '默认聚合：SUM/COUNT/AVG/MAX/MIN',
  `query_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否可筛选',
  `display_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否可展示',
  `sensitive_level` varchar(20) NOT NULL DEFAULT 'NONE' COMMENT '敏感级别：NONE/MASK/HIDDEN',
  `mask_rule` varchar(100) DEFAULT NULL COMMENT '脱敏规则',
  `dict_type` varchar(100) DEFAULT NULL COMMENT '字典类型',
  `date_format` varchar(64) DEFAULT NULL COMMENT '日期展示格式',
  `data_unit` varchar(32) DEFAULT NULL COMMENT '数据计量单位',
  `dimension_id` bigint DEFAULT NULL COMMENT '绑定维度ID',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_dataset_field` (`tenant_id`,`dataset_id`,`field_name`),
  KEY `idx_data_dataset_field_dataset` (`tenant_id`,`dataset_id`),
  KEY `idx_data_dataset_field_dimension` (`tenant_id`,`dimension_id`)
) ENGINE=InnoDB AUTO_INCREMENT=125 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台数据集字段';


-- forge_admin_new.ai_report_data_dataset_row_scope definition

CREATE TABLE `ai_report_data_dataset_row_scope` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `dataset_id` bigint NOT NULL COMMENT '数据集ID',
  `enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否启用：1是 0否',
  `scope_mode` varchar(32) NOT NULL DEFAULT 'SYSTEM_DATA_SCOPE' COMMENT '行权限模式：SYSTEM_DATA_SCOPE跟随系统数据权限',
  `tenant_column` varchar(128) DEFAULT NULL COMMENT '租户字段名/表达式',
  `org_column` varchar(128) DEFAULT NULL COMMENT '组织字段名/表达式',
  `user_column` varchar(128) DEFAULT NULL COMMENT '用户字段名/表达式',
  `region_column` varchar(128) DEFAULT NULL COMMENT '行政区划字段名/表达式',
  `region_strategy` varchar(32) NOT NULL DEFAULT 'SELF_AND_DESCENDANTS' COMMENT '行政区划策略：SELF/SELF_AND_CHILDREN/SELF_AND_DESCENDANTS',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_dataset_row_scope` (`tenant_id`,`dataset_id`),
  KEY `idx_data_dataset_row_scope_enabled` (`tenant_id`,`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台数据集行权限配置';


-- forge_admin_new.ai_report_data_dimension definition

CREATE TABLE `ai_report_data_dimension` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `dimension_code` varchar(64) NOT NULL COMMENT '维度编码',
  `dimension_name` varchar(100) NOT NULL COMMENT '维度名称',
  `source_type` varchar(20) NOT NULL DEFAULT 'MANUAL' COMMENT '来源类型：MANUAL/SQL',
  `connection_id` bigint DEFAULT NULL COMMENT 'SQL来源数据连接ID',
  `sql_text` longtext COMMENT 'SQL来源查询语句',
  `value_column` varchar(128) DEFAULT NULL COMMENT '值字段列名',
  `label_column` varchar(128) DEFAULT NULL COMMENT '显示字段列名',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `last_sync_time` datetime DEFAULT NULL COMMENT '最近同步时间',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_dimension_code_tenant_active` (`tenant_id`,`dimension_code`,`del_flag`),
  KEY `idx_data_dimension_source` (`tenant_id`,`source_type`),
  KEY `idx_data_dimension_status` (`tenant_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台维度';


-- forge_admin_new.ai_report_data_dimension_item definition

CREATE TABLE `ai_report_data_dimension_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `dimension_id` bigint NOT NULL COMMENT '维度ID',
  `item_value` varchar(255) NOT NULL COMMENT '维度值',
  `item_label` varchar(255) NOT NULL COMMENT '维度显示值',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `extra_json` json DEFAULT NULL COMMENT '扩展信息JSON',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_dimension_item_value` (`tenant_id`,`dimension_id`,`item_value`),
  KEY `idx_data_dimension_item_dimension` (`tenant_id`,`dimension_id`),
  KEY `idx_data_dimension_item_status` (`tenant_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=77 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台维度值';


-- forge_admin_new.ai_report_directory definition

CREATE TABLE `ai_report_directory` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父目录ID',
  `ancestors` varchar(500) NOT NULL DEFAULT '0' COMMENT '祖级链',
  `directory_name` varchar(100) NOT NULL COMMENT '目录名称',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_parent` (`tenant_id`,`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报表目录表';


-- forge_admin_new.ai_report_material definition

CREATE TABLE `ai_report_material` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `file_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通用文件ID',
  `material_category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'background' COMMENT '素材分类(background/panel/icon/illustration)',
  `is_private` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否私有(1私有 0公共)',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态(1正常 0删除)',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_material_file` (`tenant_id`,`file_id`),
  KEY `idx_report_material_category` (`tenant_id`,`material_category`,`status`),
  KEY `idx_report_material_create_by` (`create_by`),
  KEY `idx_report_material_scope` (`tenant_id`,`is_private`,`create_by`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报表素材表';


-- forge_admin_new.ai_report_project definition

CREATE TABLE `ai_report_project` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `directory_id` bigint DEFAULT NULL COMMENT '所属目录ID',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `project_name` varchar(100) NOT NULL COMMENT '项目名称',
  `index_img` varchar(500) DEFAULT NULL COMMENT '封面图URL',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `canvas_width` int NOT NULL DEFAULT '1920' COMMENT '画布宽度',
  `canvas_height` int NOT NULL DEFAULT '1080' COMMENT '画布高度',
  `background_color` varchar(20) DEFAULT '#1e1e2e' COMMENT '背景颜色',
  `component_data` longtext COMMENT '组件列表JSON',
  `publish_status` char(1) NOT NULL DEFAULT '0' COMMENT '发布状态（0未发布 1已发布）',
  `publish_url` varchar(500) DEFAULT NULL COMMENT '发布地址',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_create_by` (`create_by`)
) ENGINE=InnoDB AUTO_INCREMENT=2079815686363049986 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='go-view项目表';


-- forge_admin_new.ai_report_project_version definition

CREATE TABLE `ai_report_project_version` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `project_id` bigint NOT NULL COMMENT '大屏项目ID',
  `project_name` varchar(100) NOT NULL COMMENT '大屏项目名称',
  `version_no` int NOT NULL COMMENT '版本号',
  `version_name` varchar(32) NOT NULL COMMENT '版本名称',
  `operation_type` varchar(20) NOT NULL DEFAULT 'publish' COMMENT '操作类型：publish发布 rollback回退',
  `source_version_id` bigint DEFAULT NULL COMMENT '回退来源版本ID',
  `publisher_id` bigint DEFAULT NULL COMMENT '发布人ID',
  `publisher_name` varchar(100) DEFAULT NULL COMMENT '发布人名称',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `publish_url` varchar(500) DEFAULT NULL COMMENT '发布地址',
  `index_img` varchar(500) DEFAULT NULL COMMENT '封面图文件ID',
  `canvas_width` int DEFAULT NULL COMMENT '画布宽度',
  `canvas_height` int DEFAULT NULL COMMENT '画布高度',
  `background_color` varchar(20) DEFAULT NULL COMMENT '背景颜色',
  `component_data` longtext COMMENT '组件配置JSON',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_project_version_no` (`tenant_id`,`project_id`,`version_no`),
  KEY `idx_report_project_version_project` (`tenant_id`,`project_id`,`create_time`),
  KEY `idx_report_project_version_publisher` (`tenant_id`,`publisher_id`,`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='大屏项目发布版本表';


-- forge_admin_new.ai_report_template definition

CREATE TABLE `ai_report_template` (
  `id` bigint NOT NULL COMMENT '模板ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `source_project_id` bigint NOT NULL COMMENT '来源项目ID',
  `directory_id` bigint DEFAULT NULL COMMENT '所属目录ID',
  `template_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模板名称',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `index_img` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '封面图URL',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `canvas_width` int NOT NULL DEFAULT '1920' COMMENT '画布宽度',
  `canvas_height` int NOT NULL DEFAULT '1080' COMMENT '画布高度',
  `background_color` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '#1e1e2e' COMMENT '背景颜色',
  `component_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '模板组件JSON',
  `publish_status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '发布状态（0未发布 1已发布）',
  `template_scope` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '模板范围（0私有 1公开）',
  `publish_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '发布地址',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `copied_count` int NOT NULL DEFAULT '0' COMMENT '复制次数',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
  PRIMARY KEY (`id`),
  KEY `idx_template_tenant` (`tenant_id`),
  KEY `idx_template_source_project` (`source_project_id`),
  KEY `idx_template_directory` (`directory_id`),
  KEY `idx_template_publish_status` (`publish_status`),
  KEY `idx_template_scope` (`template_scope`),
  KEY `idx_template_create_by` (`create_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模板表';


-- forge_admin_new.biz_leave_request definition

CREATE TABLE `biz_leave_request` (
  `id` varchar(64) NOT NULL COMMENT '主键ID',
  `business_key` varchar(64) NOT NULL COMMENT '业务Key（关联流程）',
  `process_instance_id` varchar(64) DEFAULT NULL COMMENT '流程实例ID',
  `apply_user_id` varchar(64) NOT NULL COMMENT '申请人ID',
  `apply_user_name` varchar(100) NOT NULL COMMENT '申请人姓名',
  `apply_dept_id` varchar(64) DEFAULT NULL COMMENT '申请部门ID',
  `apply_dept_name` varchar(200) DEFAULT NULL COMMENT '申请部门名称',
  `leave_type` varchar(20) NOT NULL COMMENT '请假类型：annual-年假/sick-病假/personal-事假/marriage-婚假/maternity-产假',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `duration` decimal(10,1) NOT NULL COMMENT '请假天数',
  `reason` text COMMENT '请假原因',
  `attachments` text COMMENT '附件JSON列表',
  `status` varchar(20) NOT NULL DEFAULT 'draft' COMMENT '状态：draft-草稿/pending-审批中/approved-已通过/rejected-已驳回/canceled-已取消',
  `approve_user_id` varchar(64) DEFAULT NULL COMMENT '审批人ID',
  `approve_user_name` varchar(100) DEFAULT NULL COMMENT '审批人姓名',
  `approve_time` datetime DEFAULT NULL COMMENT '审批时间',
  `approve_comment` text COMMENT '审批意见',
  `approve_attachments` text COMMENT '审批附件JSON',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_business_key` (`business_key`),
  KEY `idx_apply_user` (`apply_user_id`),
  KEY `idx_process_instance` (`process_instance_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='请假申请表';


-- forge_admin_new.biz_lowcode_demo definition

CREATE TABLE `biz_lowcode_demo` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `name` text COMMENT '名称',
  `status` varchar(32) DEFAULT NULL COMMENT '状态',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_biz_lowcode_demo_tenant` (`tenant_id`),
  KEY `idx_biz_lowcode_demo_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='低代码应用';


-- forge_admin_new.biz_product definition

CREATE TABLE `biz_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `parent_id` bigint DEFAULT '0' COMMENT '父级ID',
  `product_name` varchar(255) NOT NULL COMMENT '产品名称',
  `category` varchar(50) DEFAULT NULL COMMENT '分类',
  `price` decimal(10,2) DEFAULT NULL COMMENT '价格',
  `stock` int DEFAULT '0' COMMENT '库存',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态',
  `sort` int DEFAULT '0' COMMENT '排序',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint(1) DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='产品管理表';


-- forge_admin_new.biz_scm_product definition

CREATE TABLE `biz_scm_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `product_code` varchar(64) NOT NULL COMMENT '商品编码',
  `product_name` varchar(128) NOT NULL COMMENT '商品名称',
  `category_name` varchar(64) DEFAULT NULL COMMENT '商品分类',
  `unit_name` varchar(32) DEFAULT NULL COMMENT '单位',
  `sale_price` decimal(18,2) DEFAULT NULL COMMENT '销售价',
  `status` varchar(32) DEFAULT NULL COMMENT '状态',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_biz_scm_product_tenant` (`tenant_id`),
  KEY `idx_biz_scm_product_create_time` (`create_time`),
  KEY `idx_product_code` (`product_code`),
  KEY `idx_product_name` (`product_name`),
  KEY `idx_category_name` (`category_name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品';


-- forge_admin_new.business_datasource_demo definition

CREATE TABLE `business_datasource_demo` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `title` varchar(128) NOT NULL COMMENT '标题',
  `route_key` varchar(128) DEFAULT NULL COMMENT 'dynamic-datasource路由Key',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_business_datasource_demo_tenant` (`tenant_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户业务数据源路由演示表';


-- forge_admin_new.business_object definition

CREATE TABLE `business_object` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `fieldkxzkp` int DEFAULT NULL COMMENT '计数器',
  `input` varchar(128) DEFAULT NULL COMMENT '输入框',
  `input2` varchar(128) DEFAULT NULL COMMENT '输入框',
  `fieldkxzkp2` int DEFAULT NULL COMMENT '计数器',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_business_object_tenant` (`tenant_id`),
  KEY `idx_business_object_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='dddddd';


-- forge_admin_new.config_properties definition

CREATE TABLE `config_properties` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `key` varchar(200) NOT NULL COMMENT '配置键',
  `value` text COMMENT '配置值',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `group` varchar(100) DEFAULT 'DEFAULT_GROUP' COMMENT '配置分组',
  `type` varchar(50) DEFAULT 'STRING' COMMENT '值类型(STRING/NUMBER/BOOLEAN/JSON)',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key` (`key`),
  KEY `idx_group` (`group`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='配置属性表';


-- forge_admin_new.crm_contact definition

CREATE TABLE `crm_contact` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `customer_id` bigint NOT NULL COMMENT '客户ID',
  `contact_name` varchar(100) NOT NULL COMMENT '联系人姓名',
  `gender` tinyint DEFAULT NULL COMMENT '性别：0-女，1-男',
  `position` varchar(100) DEFAULT NULL COMMENT '职位',
  `phone` varchar(50) DEFAULT NULL COMMENT '手机号码',
  `telephone` varchar(50) DEFAULT NULL COMMENT '固定电话',
  `email` varchar(200) DEFAULT NULL COMMENT '邮箱',
  `wechat` varchar(100) DEFAULT NULL COMMENT '微信号',
  `is_primary` tinyint NOT NULL DEFAULT '0' COMMENT '是否主联系人：0-否，1-是',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `options` text COMMENT '扩展配置JSON',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-删除',
  PRIMARY KEY (`id`),
  KEY `idx_crm_contact_tenant_id` (`tenant_id`),
  KEY `idx_crm_contact_customer` (`tenant_id`,`customer_id`),
  KEY `idx_crm_contact_status` (`tenant_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='CRM联系人表';


-- forge_admin_new.crm_contract definition

CREATE TABLE `crm_contract` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `customer_id` int NOT NULL COMMENT '客户',
  `opportunity_id` int DEFAULT NULL COMMENT '商机',
  `contract_name` varchar(256) NOT NULL COMMENT '合同名称',
  `contract_code` varchar(100) DEFAULT NULL COMMENT '合同编码',
  `contract_type` varchar(50) DEFAULT 'SALES' COMMENT '合同类型：SALES-销售，SERVICE-服务，FRAMEWORK-框架',
  `amount_cent` bigint NOT NULL DEFAULT '0' COMMENT '合同金额（分）',
  `sign_date` date DEFAULT NULL COMMENT '签约日期',
  `start_date` date DEFAULT NULL COMMENT '开始日期',
  `end_date` date DEFAULT NULL COMMENT '结束日期',
  `status` varchar(32) DEFAULT NULL COMMENT '状态',
  `approval_status` varchar(50) DEFAULT NULL COMMENT '审批状态',
  `approval_id` bigint DEFAULT NULL COMMENT '审批流程ID',
  `owner_id` bigint DEFAULT NULL COMMENT '负责人ID',
  `owner_name` varchar(100) DEFAULT NULL COMMENT '负责人名称',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `remark` text COMMENT '备注',
  `options` text COMMENT '扩展配置JSON',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-删除',
  `contract_no` varchar(64) DEFAULT NULL COMMENT '合同编号',
  `amount` int DEFAULT NULL COMMENT '合同金额',
  `assignee_id` int DEFAULT NULL COMMENT '负责人',
  PRIMARY KEY (`id`),
  KEY `idx_crm_contract_tenant_id` (`tenant_id`),
  KEY `idx_crm_contract_customer` (`tenant_id`,`customer_id`),
  KEY `idx_crm_contract_opportunity` (`tenant_id`,`opportunity_id`),
  KEY `idx_crm_contract_status` (`tenant_id`,`status`),
  KEY `idx_crm_contract_owner` (`tenant_id`,`owner_id`),
  KEY `idx_crm_contract_create_time` (`tenant_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='CRM合同表';


-- forge_admin_new.crm_contract_item definition

CREATE TABLE `crm_contract_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `contract_id` bigint NOT NULL COMMENT '合同ID',
  `product_name` varchar(200) NOT NULL COMMENT '产品名称',
  `product_code` varchar(100) DEFAULT NULL COMMENT '产品编码',
  `product_spec` varchar(200) DEFAULT NULL COMMENT '产品规格',
  `unit` varchar(20) DEFAULT NULL COMMENT '单位',
  `quantity` decimal(18,4) DEFAULT '0.0000' COMMENT '数量',
  `unit_price_cent` bigint DEFAULT '0' COMMENT '单价（分）',
  `amount_cent` bigint DEFAULT '0' COMMENT '金额（分）',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `options` text COMMENT '扩展配置JSON',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-删除',
  PRIMARY KEY (`id`),
  KEY `idx_crm_contract_item_tenant_id` (`tenant_id`),
  KEY `idx_crm_contract_item_contract` (`tenant_id`,`contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='CRM合同明细表';


-- forge_admin_new.crm_customer definition

CREATE TABLE `crm_customer` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `customer_name` varchar(128) DEFAULT NULL COMMENT '客户名称',
  `customer_code` varchar(64) DEFAULT NULL COMMENT '客户编码',
  `customer_type` varchar(50) DEFAULT 'ENTERPRISE' COMMENT '客户类型：ENTERPRISE-企业，PERSONAL-个人',
  `industry` varchar(64) DEFAULT NULL COMMENT '所属行业',
  `region_code` varchar(32) DEFAULT NULL COMMENT '所属区域',
  `address` varchar(512) DEFAULT NULL COMMENT '详细地址',
  `contact_name` varchar(64) DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(32) DEFAULT NULL COMMENT '联系电话',
  `contact_email` varchar(128) DEFAULT NULL COMMENT '联系邮箱',
  `source` varchar(50) DEFAULT NULL COMMENT '客户来源',
  `level` varchar(20) DEFAULT 'NORMAL' COMMENT '客户等级：VIP-重要，NORMAL-普通，POTENTIAL-潜在',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `owner_id` bigint DEFAULT NULL COMMENT '负责人ID',
  `owner_name` varchar(100) DEFAULT NULL COMMENT '负责人名称',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `remark` text COMMENT '备注',
  `options` text COMMENT '扩展配置JSON',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-删除',
  `customer_level` varchar(32) DEFAULT NULL COMMENT '客户等级',
  `field1lim1xu` varchar(128) DEFAULT NULL COMMENT '测试',
  `field2` varchar(20) DEFAULT NULL COMMENT '测试2',
  `field1g9xtug` bigint DEFAULT NULL COMMENT '所属组织',
  `fieldgiuxmr` bigint DEFAULT NULL COMMENT '组织选择11',
  `fieldaailwe` bigint DEFAULT NULL COMMENT '人员选择',
  `fieldbvd8sh` varchar(64) DEFAULT NULL COMMENT '字典选择',
  `content` varchar(128) DEFAULT NULL COMMENT '测试内容',
  `test11` varchar(128) DEFAULT NULL COMMENT 'f测试111',
  `ammount` int DEFAULT NULL COMMENT '金额',
  PRIMARY KEY (`id`),
  KEY `idx_crm_customer_tenant_id` (`tenant_id`),
  KEY `idx_crm_customer_status` (`tenant_id`,`status`),
  KEY `idx_crm_customer_owner` (`tenant_id`,`owner_id`),
  KEY `idx_crm_customer_create_time` (`tenant_id`,`create_time`),
  KEY `idx_field1g9xtug` (`field1g9xtug`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='CRM客户表';


-- forge_admin_new.crm_follow_record definition

CREATE TABLE `crm_follow_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `subject` varchar(256) NOT NULL DEFAULT '' COMMENT '主题',
  `customer_id` int DEFAULT NULL COMMENT '关联客户',
  `opportunity_id` int DEFAULT NULL COMMENT '关联商机',
  `type` varchar(32) DEFAULT NULL COMMENT '跟进方式',
  `content` text COMMENT '跟进内容',
  `follow_time` datetime DEFAULT NULL COMMENT '跟进时间',
  `assignee_id` int DEFAULT NULL COMMENT '负责人',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `field_checkbox8` varchar(255) DEFAULT NULL COMMENT '多选框',
  `field_mqgqktno` bigint DEFAULT NULL COMMENT '人员选择',
  `field_mqi5rog5` varchar(64) DEFAULT NULL COMMENT '静态下拉',
  `field_mqi69njp` bigint DEFAULT NULL COMMENT '部门选择',
  `field_mqi` bigint DEFAULT NULL COMMENT '部门选择',
  `org_select` bigint DEFAULT NULL COMMENT '组织选择',
  `field_mqngsofy` bigint DEFAULT NULL COMMENT '人员选择',
  `field_mqngy5gn` varchar(32) DEFAULT NULL COMMENT '行政区划',
  `ceshi_field` varchar(32) DEFAULT NULL COMMENT '行政区划',
  PRIMARY KEY (`id`),
  KEY `idx_crm_follow_record_tenant_id` (`tenant_id`),
  KEY `idx_crm_follow_record_customer` (`tenant_id`,`customer_id`),
  KEY `idx_crm_follow_record_opportunity` (`tenant_id`,`opportunity_id`),
  KEY `idx_crm_follow_record_time` (`tenant_id`,`follow_time`),
  KEY `idx_crm_follow_record_assignee` (`tenant_id`,`assignee_id`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='CRM跟进记录表';


-- forge_admin_new.crm_lead definition

CREATE TABLE `crm_lead` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `lead_name` varchar(128) DEFAULT NULL COMMENT '线索名称',
  `source` varchar(64) DEFAULT NULL COMMENT '来源',
  `contact_name` varchar(64) DEFAULT NULL COMMENT '联系人',
  `phone` varchar(32) DEFAULT NULL COMMENT '电话',
  `status` varchar(32) DEFAULT NULL COMMENT '状态',
  `assignee_id` int DEFAULT NULL COMMENT '负责人',
  `remark` text COMMENT '备注',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_crm_lead_tenant` (`tenant_id`),
  KEY `idx_crm_lead_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='线索';


-- forge_admin_new.crm_opportunity definition

CREATE TABLE `crm_opportunity` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `customer_id` bigint NOT NULL COMMENT '客户ID',
  `opportunity_name` varchar(200) NOT NULL COMMENT '商机名称',
  `opportunity_code` varchar(100) DEFAULT NULL COMMENT '商机编码',
  `document_no` varchar(64) DEFAULT NULL COMMENT '商机单据编号',
  `stage` varchar(50) DEFAULT 'INITIAL' COMMENT '商机阶段：INITIAL-初步接触，FOLLOWING-跟进中，NEGOTIATION-商务谈判，CLOSED_WON-赢单，CLOSED_LOST-丢单',
  `amount_cent` bigint DEFAULT '0' COMMENT '预计金额（分）',
  `probability` int DEFAULT '0' COMMENT '赢单概率（0-100）',
  `expected_close_date` date DEFAULT NULL COMMENT '预计成交日期',
  `actual_close_date` date DEFAULT NULL COMMENT '实际成交日期',
  `source` varchar(50) DEFAULT NULL COMMENT '商机来源',
  `owner_id` bigint DEFAULT NULL COMMENT '负责人ID',
  `owner_name` varchar(100) DEFAULT NULL COMMENT '负责人名称',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `contact_id` bigint DEFAULT NULL COMMENT '联系人ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `document_status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '商机单据状态',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `options` text COMMENT '扩展配置JSON',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-删除',
  PRIMARY KEY (`id`),
  KEY `idx_crm_opportunity_tenant_id` (`tenant_id`),
  KEY `idx_crm_opportunity_customer` (`tenant_id`,`customer_id`),
  KEY `idx_crm_opportunity_stage` (`tenant_id`,`stage`),
  KEY `idx_crm_opportunity_owner` (`tenant_id`,`owner_id`),
  KEY `idx_crm_opportunity_status` (`tenant_id`,`status`),
  KEY `idx_crm_opportunity_create_time` (`tenant_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='CRM商机表';


-- forge_admin_new.crm_payment definition

CREATE TABLE `crm_payment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `contract_id` bigint NOT NULL COMMENT '合同ID',
  `customer_id` bigint NOT NULL COMMENT '客户ID',
  `payment_code` varchar(100) DEFAULT NULL COMMENT '回款编码',
  `amount_cent` bigint NOT NULL DEFAULT '0' COMMENT '回款金额（分）',
  `payment_date` date NOT NULL COMMENT '回款日期',
  `payment_method` varchar(50) DEFAULT 'BANK_TRANSFER' COMMENT '回款方式：BANK_TRANSFER-银行转账，CASH-现金，CHECK-支票，OTHER-其他',
  `payment_status` varchar(50) DEFAULT 'PENDING' COMMENT '回款状态：PENDING-待确认，CONFIRMED-已确认，REJECTED-已驳回',
  `receiver_id` bigint DEFAULT NULL COMMENT '收款人ID',
  `receiver_name` varchar(100) DEFAULT NULL COMMENT '收款人名称',
  `invoice_no` varchar(100) DEFAULT NULL COMMENT '发票号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `options` text COMMENT '扩展配置JSON',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-删除',
  PRIMARY KEY (`id`),
  KEY `idx_crm_payment_tenant_id` (`tenant_id`),
  KEY `idx_crm_payment_contract` (`tenant_id`,`contract_id`),
  KEY `idx_crm_payment_customer` (`tenant_id`,`customer_id`),
  KEY `idx_crm_payment_status` (`tenant_id`,`payment_status`),
  KEY `idx_crm_payment_date` (`tenant_id`,`payment_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='CRM回款表';


-- forge_admin_new.cust_address definition

CREATE TABLE `cust_address` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `address_name` varchar(128) NOT NULL COMMENT '地址名称',
  `address_id` int DEFAULT NULL COMMENT '地址ID',
  `cust_id` bigint NOT NULL COMMENT '客户ID',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_cust_address_tenant` (`tenant_id`),
  KEY `idx_cust_address_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户地址管理';


-- forge_admin_new.cust_info definition

CREATE TABLE `cust_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `name` varchar(128) DEFAULT NULL COMMENT '名称',
  `status` varchar(32) DEFAULT NULL COMMENT '状态',
  `cust_name` varchar(129) DEFAULT NULL COMMENT '客户名称',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `hy` varchar(128) DEFAULT NULL COMMENT '所属行业',
  PRIMARY KEY (`id`),
  KEY `idx_cust_info_tenant` (`tenant_id`),
  KEY `idx_cust_info_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户信息';


-- forge_admin_new.forge_schema_history definition

CREATE TABLE `forge_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `forge_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- forge_admin_new.gen_datasource definition

CREATE TABLE `gen_datasource` (
  `datasource_id` bigint NOT NULL AUTO_INCREMENT COMMENT '数据源ID',
  `datasource_name` varchar(100) NOT NULL COMMENT '数据源名称',
  `datasource_code` varchar(50) NOT NULL COMMENT '数据源编码（唯一标识）',
  `db_type` varchar(20) NOT NULL DEFAULT 'MySQL' COMMENT '数据库类型：MySQL/Oracle/PostgreSQL/SQLServer',
  `driver_class_name` varchar(200) NOT NULL COMMENT '驱动类名',
  `url` varchar(500) NOT NULL COMMENT 'JDBC连接地址',
  `username` varchar(100) NOT NULL COMMENT '用户名',
  `password` varchar(200) NOT NULL COMMENT '密码（加密存储）',
  `is_default` tinyint DEFAULT '0' COMMENT '是否默认数据源（1-是，0-否）',
  `is_enabled` tinyint DEFAULT '1' COMMENT '是否启用（1-启用，0-禁用）',
  `test_query` varchar(200) DEFAULT 'SELECT 1' COMMENT '测试查询SQL',
  `sort` int DEFAULT '0' COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `usage_scope` varchar(32) NOT NULL DEFAULT 'BOTH' COMMENT '数据源用途范围：LOWCODE_RUNTIME/TENANT_BUSINESS/DEVELOPER_IMPORT/BOTH',
  `allow_runtime_write` tinyint NOT NULL DEFAULT '1' COMMENT '是否允许低代码运行时写入',
  `allow_runtime_ddl` tinyint NOT NULL DEFAULT '0' COMMENT '是否允许低代码执行运行时DDL',
  `readonly` tinyint NOT NULL DEFAULT '0' COMMENT '是否只读',
  `risk_level` varchar(16) NOT NULL DEFAULT 'MEDIUM' COMMENT '风险等级：LOW/MEDIUM/HIGH',
  PRIMARY KEY (`datasource_id`),
  UNIQUE KEY `uk_datasource_code` (`datasource_code`),
  KEY `idx_is_default` (`is_default`),
  KEY `idx_is_enabled` (`is_enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成器数据源配置';


-- forge_admin_new.gen_table definition

CREATE TABLE `gen_table` (
  `table_id` bigint NOT NULL AUTO_INCREMENT COMMENT '表ID',
  `datasource_id` bigint unsigned DEFAULT NULL COMMENT '数据源ID',
  `table_name` varchar(200) NOT NULL COMMENT '表名称',
  `table_comment` varchar(500) DEFAULT NULL COMMENT '表描述',
  `class_name` varchar(100) NOT NULL COMMENT '实体类名称',
  `business_name` varchar(100) DEFAULT NULL COMMENT '业务名称',
  `function_name` varchar(100) DEFAULT NULL COMMENT '功能名称',
  `module_name` varchar(100) DEFAULT 'system' COMMENT '模块名称',
  `package_name` varchar(200) DEFAULT 'com.mdframe.forge.plugin' COMMENT '包路径',
  `author` varchar(50) DEFAULT 'Forge Generator' COMMENT '作者',
  `gen_type` varchar(20) DEFAULT 'DOWNLOAD' COMMENT '生成方式：DOWNLOAD-下载代码包/PROJECT-直接生成到项目',
  `gen_path` varchar(500) DEFAULT '/' COMMENT '生成路径',
  `template_engine` varchar(20) DEFAULT 'VELOCITY' COMMENT '模板引擎：VELOCITY/FREEMARKER/AI',
  `options` json DEFAULT NULL COMMENT '其他生成选项',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  PRIMARY KEY (`table_id`),
  UNIQUE KEY `uk_table_name` (`table_name`)
) ENGINE=InnoDB AUTO_INCREMENT=229 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成表配置';


-- forge_admin_new.gen_table_column definition

CREATE TABLE `gen_table_column` (
  `column_id` bigint NOT NULL AUTO_INCREMENT COMMENT '字段ID',
  `table_id` bigint NOT NULL COMMENT '表ID',
  `column_name` varchar(200) NOT NULL COMMENT '字段名称',
  `column_comment` varchar(500) DEFAULT NULL COMMENT '字段描述',
  `column_type` varchar(100) NOT NULL COMMENT '字段类型',
  `java_type` varchar(50) DEFAULT 'String' COMMENT 'Java类型',
  `java_field` varchar(100) DEFAULT NULL COMMENT 'Java字段名',
  `is_pk` tinyint DEFAULT '0' COMMENT '是否主键',
  `is_increment` tinyint DEFAULT '0' COMMENT '是否自增',
  `is_required` tinyint DEFAULT '0' COMMENT '是否必填',
  `is_insert` tinyint DEFAULT '1' COMMENT '是否插入字段',
  `is_edit` tinyint DEFAULT '1' COMMENT '是否编辑字段',
  `is_list` tinyint DEFAULT '1' COMMENT '是否列表字段',
  `is_query` tinyint DEFAULT '0' COMMENT '是否查询字段',
  `query_type` varchar(20) DEFAULT 'EQ' COMMENT '查询方式：EQ/LIKE/BETWEEN/GT/LT等',
  `html_type` varchar(20) DEFAULT 'INPUT' COMMENT '显示类型：INPUT/SELECT/DATETIME/TEXTAREA等',
  `dict_type` varchar(100) DEFAULT NULL COMMENT '字典类型',
  `validate_rule` varchar(500) DEFAULT NULL COMMENT '验证规则（JSON格式）',
  `formula_config` text COMMENT '公式配置JSON {type,expression,mode,dependsOn,aggregate,condition}',
  `ai_recommended` tinyint(1) DEFAULT '0' COMMENT '是否AI推荐（0否 1是）',
  `sort` int DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`column_id`),
  KEY `idx_table_id` (`table_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3693 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成表字段配置';


-- forge_admin_new.gen_template definition

CREATE TABLE `gen_template` (
  `template_id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `template_code` varchar(50) NOT NULL COMMENT '模板编码',
  `template_type` varchar(20) NOT NULL COMMENT '模板类型：ENTITY/MAPPER/SERVICE/CONTROLLER/DTO/VO/SQL',
  `template_engine` varchar(20) DEFAULT 'VELOCITY' COMMENT '模板引擎',
  `template_content` longtext NOT NULL COMMENT '模板内容',
  `file_suffix` varchar(20) DEFAULT '.java' COMMENT '生成文件后缀',
  `file_path` varchar(200) DEFAULT NULL COMMENT '生成文件路径（相对路径）',
  `is_system` tinyint DEFAULT '1' COMMENT '是否系统内置',
  `is_enabled` tinyint DEFAULT '1' COMMENT '是否启用',
  `sort` int DEFAULT '0' COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` bigint unsigned DEFAULT NULL,
  PRIMARY KEY (`template_id`),
  UNIQUE KEY `uk_template_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成模板配置';


-- forge_admin_new.hr_leave_application definition

CREATE TABLE `hr_leave_application` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `application_no` varchar(64) DEFAULT NULL COMMENT '申请单号',
  `employee_id` bigint DEFAULT NULL COMMENT '员工ID',
  `employee_name` varchar(100) DEFAULT NULL COMMENT '员工姓名',
  `department_id` bigint DEFAULT NULL COMMENT '部门ID',
  `department_name` varchar(100) DEFAULT NULL COMMENT '部门名称',
  `position_name` varchar(100) DEFAULT NULL COMMENT '岗位名称',
  `leave_reason` text COMMENT '离职原因',
  `last_work_date` date NOT NULL COMMENT '最后工作日',
  `handover_owner_id` bigint DEFAULT NULL COMMENT '交接负责人ID',
  `handover_owner_name` varchar(100) DEFAULT NULL COMMENT '交接负责人',
  `document_status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0停用 1启用',
  `remark` text COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0正常 1删除',
  `fdeimpzgv313ebc` varchar(128) DEFAULT NULL COMMENT '输入框',
  `frpjmpzgzlc1hfc` varchar(128) DEFAULT NULL COMMENT '输入框',
  `input` varchar(128) DEFAULT NULL COMMENT '员工姓名',
  `price` varchar(18) DEFAULT NULL COMMENT '价格',
  `quantity` varchar(11) DEFAULT NULL COMMENT '数量',
  `total_price` varchar(18) DEFAULT NULL COMMENT '总价',
  `field_input4` varchar(128) DEFAULT NULL COMMENT '测试1',
  PRIMARY KEY (`id`),
  KEY `idx_hr_leave_application_tenant` (`tenant_id`),
  KEY `idx_hr_leave_application_status` (`tenant_id`,`document_status`),
  KEY `idx_hr_leave_application_last_date` (`tenant_id`,`last_work_date`),
  KEY `idx_hr_leave_application_employee` (`tenant_id`,`employee_id`),
  KEY `idx_application_no` (`application_no`),
  KEY `idx_employee_id` (`employee_id`),
  KEY `idx_employee_name` (`employee_name`),
  KEY `idx_department_id` (`department_id`),
  KEY `idx_department_name` (`department_name`),
  KEY `idx_position_name` (`position_name`),
  KEY `idx_last_work_date` (`last_work_date`),
  KEY `idx_handover_owner_id` (`handover_owner_id`),
  KEY `idx_handover_owner_name` (`handover_owner_name`),
  KEY `idx_document_status` (`document_status`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='HR离职申请表';


-- forge_admin_new.hr_leave_handover definition

CREATE TABLE `hr_leave_handover` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `leave_application_id` bigint NOT NULL COMMENT '离职申请ID',
  `handover_title` varchar(200) NOT NULL COMMENT '交接标题',
  `employee_name` varchar(100) NOT NULL COMMENT '员工姓名',
  `department_name` varchar(100) DEFAULT NULL COMMENT '部门名称',
  `position_name` varchar(100) DEFAULT NULL COMMENT '岗位名称',
  `last_work_date` date DEFAULT NULL COMMENT '最后工作日',
  `handover_owner_id` bigint DEFAULT NULL COMMENT '交接负责人ID',
  `handover_owner_name` varchar(100) DEFAULT NULL COMMENT '交接负责人',
  `handover_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '交接状态',
  `completed_time` datetime DEFAULT NULL COMMENT '完成时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (`id`),
  KEY `idx_hr_leave_handover_tenant` (`tenant_id`),
  KEY `idx_hr_leave_handover_application` (`tenant_id`,`leave_application_id`),
  KEY `idx_hr_leave_handover_status` (`tenant_id`,`handover_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='HR离职交接表';


-- forge_admin_new.in_out_demo definition

CREATE TABLE `in_out_demo` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `input` varchar(128) DEFAULT NULL COMMENT '输入框',
  `input2` text COMMENT '多行输入框',
  `fieldkxzkp` int DEFAULT NULL COMMENT '计数器',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_in_out_demo_tenant` (`tenant_id`),
  KEY `idx_in_out_demo_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='测试demo';


-- forge_admin_new.in_out_history definition

CREATE TABLE `in_out_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `checkbox` varchar(255) DEFAULT NULL COMMENT '多选框',
  `radio` varchar(64) DEFAULT NULL COMMENT '单选框',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `input` text COMMENT '多行输入框',
  `input2` varchar(128) DEFAULT NULL COMMENT '输入框',
  `input3` varchar(128) DEFAULT NULL COMMENT '密码输入框',
  `checkbox2` varchar(255) DEFAULT NULL COMMENT '多选框',
  `radio2` varchar(64) DEFAULT NULL COMMENT '单选框',
  `fieldkxzkp` int DEFAULT NULL COMMENT '计数器',
  `selector` varchar(64) DEFAULT NULL COMMENT '选择器',
  `switch` tinyint DEFAULT NULL COMMENT '开关',
  `time` varchar(32) DEFAULT NULL COMMENT '时间',
  `time2` varchar(32) DEFAULT NULL COMMENT '时间区间',
  PRIMARY KEY (`id`),
  KEY `idx_in_out_history_tenant` (`tenant_id`),
  KEY `idx_in_out_history_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='InOutHistory';


-- forge_admin_new.lsjy_desk definition

CREATE TABLE `lsjy_desk` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `desktype` varchar(64) DEFAULT NULL COMMENT '类型',
  `field_input` varchar(128) DEFAULT NULL COMMENT '输入框',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deskname` varchar(128) DEFAULT NULL COMMENT '名称',
  `desk_num` int DEFAULT NULL COMMENT '容纳数',
  PRIMARY KEY (`id`),
  KEY `idx_lsjy_desk_tenant` (`tenant_id`),
  KEY `idx_lsjy_desk_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='桌台';


-- forge_admin_new.order_manage definition

CREATE TABLE `order_manage` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `order_no` varchar(64) NOT NULL COMMENT '订单编号',
  `customer_name` varchar(100) DEFAULT NULL COMMENT '客户姓名',
  `customer_phone` varchar(20) DEFAULT NULL COMMENT '客户电话',
  `order_amount` decimal(10,2) DEFAULT NULL COMMENT '订单金额',
  `order_status` varchar(20) DEFAULT '1' COMMENT '订单状态(1:待付款,2:已付款,3:已发货,4:已完成,5:已取消)',
  `order_type` varchar(20) DEFAULT '1' COMMENT '订单类型(1:普通订单,2:团购订单,3:秒杀订单)',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint(1) DEFAULT '0' COMMENT '删除标志(0:正常,1:删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单管理表';


-- forge_admin_new.procurement_warehouse_product definition

CREATE TABLE `procurement_warehouse_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `field_input` varchar(128) DEFAULT NULL COMMENT '输入框',
  `field_textarea` text COMMENT '多行文本',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_procurement_warehouse_product_tenant` (`tenant_id`),
  KEY `idx_procurement_warehouse_product_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='产品';


-- forge_admin_new.purchase_material definition

CREATE TABLE `purchase_material` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `code` varchar(128) DEFAULT NULL COMMENT '物料编码',
  `name` varchar(128) DEFAULT NULL COMMENT '物料名称',
  `spec_model` varchar(128) DEFAULT NULL COMMENT '规格型号',
  `unit` varchar(128) DEFAULT NULL COMMENT '单位',
  `amount` int DEFAULT NULL COMMENT '成交单价',
  `status` varchar(64) NOT NULL DEFAULT '1' COMMENT '状态',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_purchase_material_tenant` (`tenant_id`),
  KEY `idx_purchase_material_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物料管理';


-- forge_admin_new.purchase_procurement definition

CREATE TABLE `purchase_procurement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `order_no` varchar(128) DEFAULT NULL COMMENT '采购单号',
  `project_name` varchar(128) DEFAULT NULL COMMENT '项目名称',
  `supplier_name` varchar(128) DEFAULT NULL COMMENT '供应商',
  `amount` int DEFAULT NULL COMMENT '采购金额',
  `purchaser` varchar(128) DEFAULT NULL COMMENT '采购人',
  `apply_date` date DEFAULT NULL COMMENT '申请日期',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_purchase_procurement_tenant` (`tenant_id`),
  KEY `idx_purchase_procurement_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采购管理';


-- forge_admin_new.purchase_warehouse_management definition

CREATE TABLE `purchase_warehouse_management` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `warehouse_name` varchar(128) DEFAULT NULL COMMENT '仓库名称/位置',
  `type` varchar(64) NOT NULL DEFAULT '1' COMMENT '类型',
  `material_class` int DEFAULT NULL COMMENT '物料种类',
  `inventory_quantity` varchar(128) DEFAULT NULL COMMENT '库存总量',
  `material_warn` varchar(128) DEFAULT NULL COMMENT '预警物料',
  `inventory_entry` int DEFAULT NULL COMMENT '本月入库',
  `warehouse_dispatch` varchar(128) DEFAULT NULL COMMENT '本月出库',
  `project_id` varchar(128) DEFAULT NULL COMMENT '关联项目',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_purchase_warehouse_management_tenant` (`tenant_id`),
  KEY `idx_purchase_warehouse_management_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='仓库管理';


-- forge_admin_new.pw_material definition

CREATE TABLE `pw_material` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `material_code` varchar(64) DEFAULT NULL COMMENT '物料编号',
  `material_name` varchar(128) DEFAULT NULL COMMENT '物料名称',
  `spec_model` varchar(128) DEFAULT NULL COMMENT '规格型号',
  `unit` varchar(32) DEFAULT NULL COMMENT '单位',
  `category` varchar(64) DEFAULT NULL COMMENT '物料分类',
  `reference_price_cent` varchar(128) DEFAULT NULL COMMENT '参考单价(分)',
  `warning_quantity` varchar(18) DEFAULT NULL COMMENT '预警数量',
  `status` varchar(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
  `remark` text COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pw_material_code` (`tenant_id`,`material_code`),
  KEY `idx_pw_material_name` (`tenant_id`,`material_name`),
  KEY `idx_pw_material_status` (`tenant_id`,`status`),
  KEY `idx_material_code` (`material_code`),
  KEY `idx_material_name` (`material_name`),
  KEY `idx_spec_model` (`spec_model`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=1950000000000009207 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采购仓储-物料';


-- forge_admin_new.pw_outbound_order definition

CREATE TABLE `pw_outbound_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `outbound_no` varchar(64) NOT NULL COMMENT '出库单号',
  `warehouse_id` bigint NOT NULL COMMENT '所属仓库ID',
  `warehouse_name` varchar(128) DEFAULT NULL COMMENT '所属仓库',
  `applicant_name` varchar(64) DEFAULT NULL COMMENT '申请人',
  `outbound_date` date DEFAULT NULL COMMENT '出库日期',
  `outbound_reason` varchar(255) DEFAULT NULL COMMENT '出库原因',
  `order_status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pw_outbound_no` (`tenant_id`,`outbound_no`),
  KEY `idx_pw_outbound_warehouse` (`tenant_id`,`warehouse_id`),
  KEY `idx_pw_outbound_status` (`tenant_id`,`order_status`)
) ENGINE=InnoDB AUTO_INCREMENT=1950000000000009263 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采购仓储-出库单';


-- forge_admin_new.pw_outbound_order_item definition

CREATE TABLE `pw_outbound_order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `outbound_id` varchar(128) DEFAULT NULL COMMENT '出库单ID',
  `warehouse_id` varchar(128) DEFAULT NULL COMMENT '仓库ID',
  `material_id` bigint DEFAULT NULL COMMENT '物料ID',
  `material_code` varchar(64) DEFAULT NULL COMMENT '物料编号',
  `material_name` varchar(128) DEFAULT NULL COMMENT '物料名称',
  `spec_model` varchar(128) DEFAULT NULL COMMENT '规格型号',
  `unit` varchar(32) DEFAULT NULL COMMENT '单位',
  `stock_quantity` varchar(18) DEFAULT NULL COMMENT '库存数量',
  `outbound_quantity` varchar(18) DEFAULT NULL COMMENT '出库数量',
  `lock_code` varchar(128) DEFAULT NULL COMMENT '数量锁定编码',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_pw_outbound_item_order` (`tenant_id`,`outbound_id`),
  KEY `idx_pw_outbound_item_material` (`tenant_id`,`material_id`),
  KEY `idx_outbound_id` (`outbound_id`),
  KEY `idx_material_code` (`material_code`),
  KEY `idx_material_name` (`material_name`)
) ENGINE=InnoDB AUTO_INCREMENT=1950000000000009274 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采购仓储-出库明细';


-- forge_admin_new.pw_purchase_order definition

CREATE TABLE `pw_purchase_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `purchase_no` varchar(64) DEFAULT NULL COMMENT '采购单号',
  `project_name` varchar(128) DEFAULT NULL COMMENT '项目名称',
  `delivery_type` varchar(32) NOT NULL DEFAULT 'WAREHOUSE' COMMENT '送达类型',
  `warehouse_id` bigint DEFAULT NULL COMMENT '目标仓库',
  `warehouse_name` varchar(128) DEFAULT NULL COMMENT '目标仓库',
  `purchaser_name` varchar(64) DEFAULT NULL COMMENT '采购人',
  `purchase_date` date DEFAULT NULL COMMENT '采购日期',
  `supplier_id` bigint DEFAULT NULL COMMENT '供应商ID',
  `supplier_name` varchar(128) DEFAULT NULL COMMENT '供应商',
  `supplier_contact` varchar(64) DEFAULT NULL COMMENT '供应商联系人',
  `supplier_phone` varchar(32) DEFAULT NULL COMMENT '供应商联系电话',
  `purchase_amount_cent` varchar(128) DEFAULT NULL COMMENT '采购金额(分)',
  `order_status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
  `attachment_ids` varchar(500) DEFAULT NULL COMMENT '附件ID集合',
  `remark` text COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pw_purchase_no` (`tenant_id`,`purchase_no`),
  KEY `idx_pw_purchase_project` (`tenant_id`,`project_name`),
  KEY `idx_pw_purchase_status` (`tenant_id`,`order_status`),
  KEY `idx_pw_purchase_warehouse` (`tenant_id`,`warehouse_id`),
  KEY `idx_purchase_no` (`purchase_no`),
  KEY `idx_project_name` (`project_name`),
  KEY `idx_warehouse_name` (`warehouse_name`),
  KEY `idx_supplier_name` (`supplier_name`),
  KEY `idx_order_status` (`order_status`),
  KEY `idx_rel_purchaseno` (`purchase_no`)
) ENGINE=InnoDB AUTO_INCREMENT=1950000000000009253 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采购仓储-采购单';


-- forge_admin_new.pw_purchase_order_item definition

CREATE TABLE `pw_purchase_order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `purchase_id` varchar(128) DEFAULT NULL COMMENT '采购单ID',
  `material_id` varchar(128) DEFAULT NULL COMMENT '物料ID',
  `material_code` varchar(64) DEFAULT NULL COMMENT '物料编号',
  `material_name` varchar(128) DEFAULT NULL COMMENT '物料名称',
  `spec_model` varchar(128) DEFAULT NULL COMMENT '规格型号',
  `unit` varchar(32) DEFAULT NULL COMMENT '单位',
  `quantity` varchar(18) DEFAULT NULL COMMENT '数量',
  `cost_price_cent` bigint NOT NULL DEFAULT '0' COMMENT '成本单价，单位分',
  `deal_price_cent` varchar(128) DEFAULT NULL COMMENT '成交单价(分)',
  `amount_cent` varchar(128) DEFAULT NULL COMMENT '金额(分)',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_pw_purchase_item_order` (`tenant_id`,`purchase_id`),
  KEY `idx_pw_purchase_item_material` (`tenant_id`,`material_id`),
  KEY `idx_purchase_id` (`purchase_id`),
  KEY `idx_material_code` (`material_code`),
  KEY `idx_material_name` (`material_name`)
) ENGINE=InnoDB AUTO_INCREMENT=1950000000000009264 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采购仓储-采购明细';


-- forge_admin_new.pw_supplier definition

CREATE TABLE `pw_supplier` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `supplier_code` varchar(64) DEFAULT NULL COMMENT '供应商编号',
  `supplier_name` varchar(128) DEFAULT NULL COMMENT '供应商名称',
  `contact_name` varchar(64) DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(32) DEFAULT NULL COMMENT '联系电话',
  `status` varchar(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
  `remark` text COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pw_supplier_code` (`tenant_id`,`supplier_code`),
  KEY `idx_pw_supplier_name` (`tenant_id`,`supplier_name`),
  KEY `idx_pw_supplier_status` (`tenant_id`,`status`),
  KEY `idx_supplier_code` (`supplier_code`),
  KEY `idx_supplier_name` (`supplier_name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=1950000000000009214 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采购仓储-供应商';


-- forge_admin_new.pw_supplier_material definition

CREATE TABLE `pw_supplier_material` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `supplier_id` bigint DEFAULT NULL COMMENT '供应商ID',
  `supplier_name` varchar(128) DEFAULT NULL COMMENT '供应商名称',
  `material_id` bigint DEFAULT NULL COMMENT '物料ID',
  `material_code` varchar(64) DEFAULT NULL COMMENT '物料编号',
  `material_name` varchar(128) DEFAULT NULL COMMENT '物料名称',
  `spec_model` varchar(128) DEFAULT NULL COMMENT '规格型号',
  `unit` varchar(32) DEFAULT NULL COMMENT '单位',
  `quote_price_cent` int DEFAULT NULL COMMENT '报价(分)',
  `last_price_cent` bigint NOT NULL DEFAULT '0' COMMENT '上次报价，单位分',
  `effective_date` date DEFAULT NULL COMMENT '报价有效期',
  `status` varchar(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pw_supplier_material` (`tenant_id`,`supplier_id`,`material_id`),
  KEY `idx_pw_supplier_material_supplier` (`tenant_id`,`supplier_id`),
  KEY `idx_pw_supplier_material_material` (`tenant_id`,`material_id`),
  KEY `idx_supplier_id` (`supplier_id`),
  KEY `idx_supplier_name` (`supplier_name`),
  KEY `idx_material_code` (`material_code`),
  KEY `idx_material_name` (`material_name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=1950000000000009236 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采购仓储-供应商物料报价';


-- forge_admin_new.pw_transfer_order definition

CREATE TABLE `pw_transfer_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `transfer_no` varchar(64) NOT NULL COMMENT '调拨单号',
  `from_warehouse_id` bigint NOT NULL COMMENT '调出仓库ID',
  `from_warehouse_name` varchar(128) DEFAULT NULL COMMENT '调出仓库',
  `to_warehouse_id` bigint NOT NULL COMMENT '调入仓库ID',
  `to_warehouse_name` varchar(128) DEFAULT NULL COMMENT '调入仓库',
  `transfer_person_name` varchar(64) DEFAULT NULL COMMENT '调拨人',
  `transfer_date` date DEFAULT NULL COMMENT '调拨日期',
  `transfer_reason` varchar(255) DEFAULT NULL COMMENT '调拨原因',
  `order_status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pw_transfer_no` (`tenant_id`,`transfer_no`),
  KEY `idx_pw_transfer_from` (`tenant_id`,`from_warehouse_id`),
  KEY `idx_pw_transfer_to` (`tenant_id`,`to_warehouse_id`),
  KEY `idx_pw_transfer_status` (`tenant_id`,`order_status`)
) ENGINE=InnoDB AUTO_INCREMENT=1950000000000009282 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采购仓储-调拨单';


-- forge_admin_new.pw_transfer_order_item definition

CREATE TABLE `pw_transfer_order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `transfer_id` bigint NOT NULL COMMENT '调拨单ID',
  `material_id` bigint DEFAULT NULL COMMENT '物料ID',
  `material_code` varchar(64) DEFAULT NULL COMMENT '物料编号',
  `material_name` varchar(128) DEFAULT NULL COMMENT '物料名称',
  `spec_model` varchar(128) DEFAULT NULL COMMENT '规格型号',
  `unit` varchar(32) DEFAULT NULL COMMENT '单位',
  `current_stock_quantity` decimal(18,3) NOT NULL DEFAULT '0.000' COMMENT '当前库存',
  `transfer_quantity` decimal(18,3) NOT NULL DEFAULT '0.000' COMMENT '调拨数量',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_pw_transfer_item_order` (`tenant_id`,`transfer_id`),
  KEY `idx_pw_transfer_item_material` (`tenant_id`,`material_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1950000000000009292 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采购仓储-调拨明细';


-- forge_admin_new.pw_warehouse definition

CREATE TABLE `pw_warehouse` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `warehouse_code` varchar(64) DEFAULT NULL COMMENT '仓库编号',
  `warehouse_name` varchar(128) DEFAULT NULL COMMENT '仓库名称',
  `warehouse_type` varchar(32) NOT NULL DEFAULT 'CENTER' COMMENT '仓库类型',
  `project_name` varchar(128) DEFAULT NULL COMMENT '关联项目',
  `location` varchar(255) DEFAULT NULL COMMENT '位置',
  `related_contract_no` varchar(64) DEFAULT NULL COMMENT '关联合同',
  `status` varchar(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
  `remark` text COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pw_warehouse_code` (`tenant_id`,`warehouse_code`),
  KEY `idx_pw_warehouse_type` (`tenant_id`,`warehouse_type`),
  KEY `idx_pw_warehouse_project` (`tenant_id`,`project_name`),
  KEY `idx_warehouse_code` (`warehouse_code`),
  KEY `idx_warehouse_name` (`warehouse_name`),
  KEY `idx_warehouse_type` (`warehouse_type`),
  KEY `idx_project_name` (`project_name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=1950000000000009225 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采购仓储-仓库';


-- forge_admin_new.QRTZ_CALENDARS definition

DROP TABLE IF EXISTS `QRTZ_CALENDARS`;

CREATE TABLE `QRTZ_CALENDARS` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `calendar_name` varchar(200) NOT NULL COMMENT '日历名称',
  `calendar` blob NOT NULL COMMENT '存放持久化calendar对象',
  PRIMARY KEY (`sched_name`,`calendar_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='日历信息表';


-- forge_admin_new.QRTZ_FIRED_TRIGGERS definition

DROP TABLE IF EXISTS `QRTZ_FIRED_TRIGGERS`;

CREATE TABLE `QRTZ_FIRED_TRIGGERS` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `entry_id` varchar(95) NOT NULL COMMENT '调度器实例id',
  `trigger_name` varchar(200) NOT NULL COMMENT 'QRTZ_TRIGGERS表trigger_name的外键',
  `trigger_group` varchar(200) NOT NULL COMMENT 'QRTZ_TRIGGERS表trigger_group的外键',
  `instance_name` varchar(200) NOT NULL COMMENT '调度器实例名',
  `fired_time` bigint NOT NULL COMMENT '触发的时间',
  `sched_time` bigint NOT NULL COMMENT '定时器制定的时间',
  `priority` int NOT NULL COMMENT '优先级',
  `state` varchar(16) NOT NULL COMMENT '状态',
  `job_name` varchar(200) DEFAULT NULL COMMENT '任务名称',
  `job_group` varchar(200) DEFAULT NULL COMMENT '任务组名',
  `is_nonconcurrent` varchar(1) DEFAULT NULL COMMENT '是否并发',
  `requests_recovery` varchar(1) DEFAULT NULL COMMENT '是否接受恢复执行',
  PRIMARY KEY (`sched_name`,`entry_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='已触发的触发器表';


-- forge_admin_new.QRTZ_JOB_DETAILS definition

DROP TABLE IF EXISTS `QRTZ_JOB_DETAILS`;

CREATE TABLE `QRTZ_JOB_DETAILS` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `job_name` varchar(200) NOT NULL COMMENT '任务名称',
  `job_group` varchar(200) NOT NULL COMMENT '任务组名',
  `description` varchar(250) DEFAULT NULL COMMENT '相关介绍',
  `job_class_name` varchar(250) NOT NULL COMMENT '执行任务类名称',
  `is_durable` varchar(1) NOT NULL COMMENT '是否持久化',
  `is_nonconcurrent` varchar(1) NOT NULL COMMENT '是否并发',
  `is_update_data` varchar(1) NOT NULL COMMENT '是否更新数据',
  `requests_recovery` varchar(1) NOT NULL COMMENT '是否接受恢复执行',
  `job_data` blob COMMENT '存放持久化job对象',
  PRIMARY KEY (`sched_name`,`job_name`,`job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='任务详细信息表';


-- forge_admin_new.QRTZ_LOCKS definition

DROP TABLE IF EXISTS `QRTZ_LOCKS`;

CREATE TABLE `QRTZ_LOCKS` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `lock_name` varchar(40) NOT NULL COMMENT '悲观锁名称',
  PRIMARY KEY (`sched_name`,`lock_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='存储的悲观锁信息表';


-- forge_admin_new.QRTZ_PAUSED_TRIGGER_GRPS definition

DROP TABLE IF EXISTS `QRTZ_PAUSED_TRIGGER_GRPS`;

CREATE TABLE `QRTZ_PAUSED_TRIGGER_GRPS` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `trigger_group` varchar(200) NOT NULL COMMENT 'QRTZ_TRIGGERS表trigger_group的外键',
  PRIMARY KEY (`sched_name`,`trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='暂停的触发器表';


-- forge_admin_new.QRTZ_SCHEDULER_STATE definition

DROP TABLE IF EXISTS `QRTZ_SCHEDULER_STATE`;

CREATE TABLE `QRTZ_SCHEDULER_STATE` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `instance_name` varchar(200) NOT NULL COMMENT '实例名称',
  `last_checkin_time` bigint NOT NULL COMMENT '上次检查时间',
  `checkin_interval` bigint NOT NULL COMMENT '检查间隔时间',
  PRIMARY KEY (`sched_name`,`instance_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='调度器状态表';


-- forge_admin_new.sample_purchase_order definition

CREATE TABLE `sample_purchase_order` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `order_no` varchar(64) NOT NULL COMMENT '采购单号',
  `title` varchar(128) NOT NULL COMMENT '采购主题',
  `supplier_name` varchar(128) NOT NULL COMMENT '供应商',
  `amount_cent` bigint NOT NULL DEFAULT '0' COMMENT '采购金额，单位分',
  `purchase_items` text COMMENT '采购明细',
  `need_date` date DEFAULT NULL COMMENT '需求日期',
  `status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '业务状态',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `applicant_id` bigint DEFAULT NULL COMMENT '申请人ID',
  `applicant_name` varchar(64) DEFAULT NULL COMMENT '申请人名称',
  `applicant_dept_id` bigint DEFAULT NULL COMMENT '申请部门ID',
  `applicant_dept_name` varchar(128) DEFAULT NULL COMMENT '申请部门名称',
  `business_key` varchar(128) DEFAULT NULL COMMENT '流程业务Key',
  `process_instance_id` varchar(128) DEFAULT NULL COMMENT '流程实例ID',
  `dept_leader_id` bigint DEFAULT NULL COMMENT '部门负责人ID',
  `engineering_manager_id` bigint DEFAULT NULL COMMENT '工程部经理ID',
  `countersign_user_ids` varchar(500) DEFAULT NULL COMMENT '会签用户ID，逗号分隔',
  `cc_role_keys` varchar(500) DEFAULT NULL COMMENT '流程完成抄送角色编码，逗号分隔',
  `arrival_list_file_ids` varchar(1000) DEFAULT NULL COMMENT '负责人节点上传清单文件ID',
  `applicant_modify_remark` varchar(500) DEFAULT NULL COMMENT '申请人修改说明',
  `dept_leader_remark` varchar(500) DEFAULT NULL COMMENT '部门负责人意见',
  `engineering_manager_remark` varchar(500) DEFAULT NULL COMMENT '工程部经理意见',
  `countersign_remark` varchar(500) DEFAULT NULL COMMENT '会签意见',
  `reject_reason` varchar(500) DEFAULT NULL COMMENT '驳回原因',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sample_purchase_order_no_active` (`tenant_id`,`order_no`,`del_flag`),
  UNIQUE KEY `uk_sample_purchase_order_business_key_active` (`tenant_id`,`business_key`,`del_flag`),
  KEY `idx_sample_purchase_order_status` (`tenant_id`,`status`,`update_time`),
  KEY `idx_sample_purchase_order_process` (`tenant_id`,`process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采购单审批测试表';


-- forge_admin_new.sys_api_config definition

CREATE TABLE `sys_api_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `api_name` varchar(100) NOT NULL COMMENT '接口名称 (如:查询用户信息)',
  `api_code` varchar(100) DEFAULT NULL COMMENT '接口编码 (用于程序逻辑引用)',
  `req_method` varchar(20) NOT NULL COMMENT '请求方式 (GET, POST, PUT, DELETE, ALL)',
  `url_path` varchar(255) NOT NULL COMMENT '接口请求路径 (支持Ant风格, 如 /api/user/**)',
  `api_version` varchar(20) DEFAULT 'v1.0.0' COMMENT '接口版本号',
  `module_code` varchar(50) NOT NULL COMMENT '所属业务模块 (如: sys, order, pay)',
  `service_id` varchar(100) DEFAULT NULL COMMENT '所属微服务ID (若为微服务架构)',
  `auth_flag` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否需认证/鉴权 (Token校验)',
  `encrypt_flag` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否需报文加解密 (敏感数据保护)',
  `tenant_flag` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用租户隔离 (数据权限)',
  `limit_flag` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否开启限流',
  `sensitive_fields` text COMMENT '需脱敏字段 (JSON数组存储, 如 ["phone", "id_card"])',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态 (1-正常, 0-停用)',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注说明',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_method_url_active` (`url_path`,`req_method`,`del_flag`),
  KEY `idx_module` (`module_code`) USING BTREE COMMENT '模块查询索引',
  KEY `idx_tenant_id` (`tenant_id`) USING BTREE COMMENT '租户索引'
) ENGINE=InnoDB AUTO_INCREMENT=1670 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='REST接口配置管理表';


-- forge_admin_new.sys_auth_online_user definition

CREATE TABLE `sys_auth_online_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `token_value` varchar(255) NOT NULL COMMENT 'Token值',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `username` varchar(100) NOT NULL COMMENT '用户名',
  `real_name` varchar(100) DEFAULT NULL COMMENT '真实姓名',
  `dept_name` varchar(100) DEFAULT NULL COMMENT '部门名称',
  `ip_address` varchar(50) DEFAULT NULL COMMENT '登录IP地址',
  `login_location` varchar(100) DEFAULT NULL COMMENT '登录地点',
  `browser` varchar(50) DEFAULT NULL COMMENT '浏览器类型',
  `os` varchar(50) DEFAULT NULL COMMENT '操作系统',
  `login_time` datetime NOT NULL COMMENT '登录时间',
  `last_activity_time` datetime DEFAULT NULL COMMENT '最后活动时间',
  `expire_time` datetime DEFAULT NULL COMMENT 'Token过期时间',
  `status` tinyint DEFAULT '1' COMMENT '状态: 1-在线, 0-已下线',
  `logout_time` datetime DEFAULT NULL COMMENT '登出时间',
  `logout_type` tinyint DEFAULT NULL COMMENT '登出类型: 1-主动登出, 2-被踢下线, 3-被顶下线, 4-Token过期',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token` (`token_value`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_login_time` (`login_time`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4696 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='在线用户表';


-- forge_admin_new.sys_auth_online_user_history definition

CREATE TABLE `sys_auth_online_user_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `username` varchar(100) NOT NULL COMMENT '用户名',
  `ip_address` varchar(50) DEFAULT NULL COMMENT '登录IP地址',
  `login_location` varchar(100) DEFAULT NULL COMMENT '登录地点',
  `browser` varchar(50) DEFAULT NULL COMMENT '浏览器类型',
  `os` varchar(50) DEFAULT NULL COMMENT '操作系统',
  `login_time` datetime NOT NULL COMMENT '登录时间',
  `logout_time` datetime DEFAULT NULL COMMENT '登出时间',
  `online_duration` int DEFAULT NULL COMMENT '在线时长(秒)',
  `logout_type` tinyint DEFAULT NULL COMMENT '登出类型: 1-主动登出, 2-被踢下线, 3-被顶下线, 4-Token过期',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_login_time` (`login_time`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='在线用户历史表';


-- forge_admin_new.sys_client definition

CREATE TABLE `sys_client` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `client_code` varchar(50) NOT NULL COMMENT '客户端编码',
  `client_name` varchar(100) NOT NULL COMMENT '客户端名称',
  `app_id` varchar(64) NOT NULL COMMENT '应用ID',
  `app_secret` varchar(128) DEFAULT NULL COMMENT '应用密钥摘要，仅client_secret客户端使用',
  `client_auth_method` varchar(32) NOT NULL DEFAULT 'client_secret' COMMENT '客户端认证方式：none/client_secret',
  `token_timeout` bigint DEFAULT '7200' COMMENT 'Token有效期（秒）',
  `token_activity_timeout` bigint DEFAULT '-1' COMMENT 'Token活跃超时（秒）',
  `token_prefix` varchar(20) DEFAULT 'Bearer' COMMENT 'Token前缀',
  `token_name` varchar(50) DEFAULT 'Authorization' COMMENT 'Token名称',
  `concurrent_login` tinyint(1) DEFAULT '0' COMMENT '是否允许并发登录（0-否 1-是）',
  `share_token` tinyint(1) DEFAULT '0' COMMENT '是否共享Token（0-否 1-是）',
  `enable_ip_limit` tinyint(1) DEFAULT '0' COMMENT '是否启用IP限制',
  `ip_whitelist` text COMMENT 'IP白名单（JSON数组）',
  `enable_encrypt` tinyint(1) DEFAULT '0' COMMENT '是否启用加密传输',
  `encrypt_algorithm` varchar(50) DEFAULT 'AES' COMMENT '加密算法',
  `max_user_count` bigint DEFAULT '-1' COMMENT '最大用户数（-1不限）',
  `max_online_count` bigint DEFAULT '-1' COMMENT '最大在线数（-1不限）',
  `auth_types` varchar(255) DEFAULT 'password' COMMENT '支持的认证方式（逗号分隔）',
  `captcha_type` varchar(32) DEFAULT NULL COMMENT '验证码类型：graphical图形，slider滑块，sms短信；为空继承全局配置',
  `status` tinyint DEFAULT '1' COMMENT '状态（0-禁用 1-启用）',
  `description` varchar(500) DEFAULT NULL COMMENT '客户端描述',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `create_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `create_dept` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_client_code` (`client_code`),
  UNIQUE KEY `uk_app_id` (`app_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户端管理表';


-- forge_admin_new.sys_config definition

CREATE TABLE `sys_config` (
  `config_id` bigint NOT NULL AUTO_INCREMENT COMMENT '参数主键',
  `tenant_id` bigint DEFAULT '0' COMMENT '租户编号',
  `config_name` varchar(100) DEFAULT '' COMMENT '参数名称',
  `config_key` varchar(100) DEFAULT '' COMMENT '参数键名',
  `config_value` varchar(500) DEFAULT '' COMMENT '参数键值',
  `config_type` char(1) DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
  `config_desc` varchar(500) DEFAULT NULL COMMENT '参数描述',
  `sort` int DEFAULT '0' COMMENT '排序',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`config_id`),
  UNIQUE KEY `uk_tenant_config_key_active` (`tenant_id`,`config_key`,`del_flag`),
  KEY `idx_config_type` (`config_type`)
) ENGINE=InnoDB AUTO_INCREMENT=448 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统配置表';


-- forge_admin_new.sys_config_group definition

CREATE TABLE `sys_config_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分组编码',
  `group_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分组名称',
  `group_icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分组图标',
  `config_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '配置值(JSON格式)',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '状态(0-禁用 1-启用)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_group_code` (`group_code`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统配置分组表';


-- forge_admin_new.sys_data_scope_config definition

CREATE TABLE `sys_data_scope_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `resource_code` varchar(100) NOT NULL COMMENT '资源编码（对应接口路径或功能模块）',
  `resource_name` varchar(100) DEFAULT NULL COMMENT '资源名称',
  `mapper_method` varchar(200) NOT NULL COMMENT 'Mapper方法（如：com.example.mapper.UserMapper.selectList）',
  `table_alias` varchar(50) DEFAULT 't' COMMENT '主表别名',
  `user_id_column` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '用户ID字段名',
  `org_id_column` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '组织ID字段名',
  `tenant_id_column` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '租户ID字段名',
  `region_code_column` varchar(250) DEFAULT NULL COMMENT '行政区划字段名',
  `user_region_column` varchar(200) DEFAULT NULL COMMENT '用户表行政区划字段名',
  `user_table_alias` varchar(100) DEFAULT NULL COMMENT '用户表别名',
  `enabled` tinyint DEFAULT '1' COMMENT '是否启用（0-禁用，1-启用）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_mapper_active` (`tenant_id`,`mapper_method`,`del_flag`),
  KEY `idx_resource_code` (`resource_code`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据权限配置表';


-- forge_admin_new.sys_dict_data definition

CREATE TABLE `sys_dict_data` (
  `dict_code` bigint NOT NULL AUTO_INCREMENT COMMENT '字典编码',
  `tenant_id` bigint DEFAULT '0' COMMENT '租户编号',
  `dict_sort` int DEFAULT '0' COMMENT '字典排序',
  `dict_label` varchar(100) DEFAULT '' COMMENT '字典标签',
  `dict_value` varchar(100) DEFAULT '' COMMENT '字典键值',
  `dict_type` varchar(100) DEFAULT '' COMMENT '字典类型',
  `css_class` varchar(100) DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
  `list_class` varchar(100) DEFAULT NULL COMMENT '表格回显样式',
  `is_default` char(1) DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
  `parent_dict_code` bigint unsigned DEFAULT NULL COMMENT '上级字典编码',
  `linked_dict_type` varchar(100) DEFAULT NULL COMMENT '关联的字典类型',
  `linked_dict_value` varchar(100) DEFAULT NULL COMMENT '关联的字典值',
  `dict_status` tinyint DEFAULT '1' COMMENT '字典状态（0-禁用，1-正常）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`dict_code`),
  UNIQUE KEY `uk_tenant_dict_data_active` (`tenant_id`,`dict_type`,`dict_value`,`del_flag`),
  KEY `idx_tenant_dict_type` (`tenant_id`,`dict_type`),
  KEY `idx_parent_dict_code` (`parent_dict_code`),
  KEY `idx_linked_dict` (`linked_dict_type`,`linked_dict_value`)
) ENGINE=InnoDB AUTO_INCREMENT=803 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典数据表';


-- forge_admin_new.sys_dict_type definition

CREATE TABLE `sys_dict_type` (
  `dict_id` bigint NOT NULL AUTO_INCREMENT COMMENT '字典主键',
  `tenant_id` bigint DEFAULT '0' COMMENT '租户编号',
  `dict_name` varchar(100) DEFAULT '' COMMENT '字典名称',
  `dict_type` varchar(100) DEFAULT '' COMMENT '字典类型（租户内唯一）',
  `dict_status` tinyint DEFAULT '1' COMMENT '字典状态（0-禁用，1-正常）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`dict_id`),
  UNIQUE KEY `uk_tenant_dict_type_active` (`tenant_id`,`dict_type`,`del_flag`),
  KEY `idx_dict_status` (`dict_status`)
) ENGINE=InnoDB AUTO_INCREMENT=196 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典类型表';


-- forge_admin_new.sys_email_config definition

CREATE TABLE `sys_email_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_id` varchar(64) DEFAULT NULL COMMENT '配置标识',
  `smtp_server` varchar(128) NOT NULL COMMENT 'SMTP服务器地址',
  `port` int DEFAULT '465' COMMENT '端口',
  `username` varchar(128) DEFAULT NULL COMMENT '用户名',
  `password` varchar(256) DEFAULT NULL COMMENT '密码/授权码',
  `from_address` varchar(128) NOT NULL COMMENT '发件人地址',
  `from_name` varchar(64) DEFAULT NULL COMMENT '发件人名称',
  `is_ssl` tinyint DEFAULT '1' COMMENT '是否开启SSL',
  `is_auth` tinyint DEFAULT '1' COMMENT '是否开启验证',
  `retry_interval` int DEFAULT '5' COMMENT '重试间隔(秒)',
  `max_retries` int DEFAULT '1' COMMENT '最大重试次数',
  `status` tinyint DEFAULT '0' COMMENT '状态(0禁用 1启用)',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_status` (`tenant_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='邮件配置表';


-- forge_admin_new.sys_employee definition

CREATE TABLE `sys_employee` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `emp_name` varchar(100) NOT NULL COMMENT '姓名',
  `emp_no` varchar(50) NOT NULL COMMENT '工号',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `position` varchar(100) DEFAULT NULL COMMENT '职位',
  `hire_date` date DEFAULT NULL COMMENT '入职日期',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(1:正常,0:停用)',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_no_active` (`emp_no`,`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='员工信息表';


-- forge_admin_new.sys_excel_column_config definition

CREATE TABLE `sys_excel_column_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key` varchar(100) NOT NULL COMMENT '关联配置键',
  `field_name` varchar(100) NOT NULL COMMENT '字段名（实体属性名）',
  `column_name` varchar(200) NOT NULL COMMENT '列名（Excel表头）',
  `width` int DEFAULT '20' COMMENT '列宽',
  `order_num` int DEFAULT '0' COMMENT '排序（值越小越靠前）',
  `export` tinyint(1) DEFAULT '1' COMMENT '是否导出（1-是，0-否）',
  `date_format` varchar(50) DEFAULT NULL COMMENT '日期格式（如：yyyy-MM-dd）',
  `number_format` varchar(50) DEFAULT NULL COMMENT '数字格式',
  `dict_type` varchar(50) DEFAULT NULL COMMENT '字典类型（关联sys_dict_type）',
  `importable` tinyint(1) DEFAULT '1' COMMENT '是否可导入（1-是，0-否）',
  `required` tinyint(1) DEFAULT '0' COMMENT '是否必填（1-是，0-否）',
  `example_value` varchar(255) DEFAULT NULL COMMENT '示例值（用于模板）',
  `validation_rule` varchar(255) DEFAULT NULL COMMENT '校验规则（正则表达式）',
  `validation_message` varchar(255) DEFAULT NULL COMMENT '校验失败提示信息',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  PRIMARY KEY (`id`),
  KEY `idx_config_key` (`config_key`),
  KEY `idx_order` (`order_num`)
) ENGINE=InnoDB AUTO_INCREMENT=2068887631264264248 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Excel列配置表';


-- forge_admin_new.sys_excel_export_config definition

CREATE TABLE `sys_excel_export_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key` varchar(100) NOT NULL COMMENT '配置键（唯一标识）',
  `config_type` varchar(16) NOT NULL DEFAULT 'BOTH' COMMENT '配置类型：EXPORT导出 IMPORT导入 BOTH导入导出',
  `export_name` varchar(200) NOT NULL COMMENT '导出名称',
  `sheet_name` varchar(100) DEFAULT 'Sheet1' COMMENT 'Sheet名称',
  `file_name_template` varchar(200) DEFAULT NULL COMMENT '文件名模板（支持占位符：{date}、{time}）',
  `data_source_bean` varchar(100) DEFAULT NULL COMMENT '数据源Bean名称（如：userService，导入-only可为空）',
  `query_method` varchar(100) DEFAULT NULL COMMENT '数据查询方法名（如：list、page，导入-only可为空）',
  `auto_trans` tinyint(1) DEFAULT '1' COMMENT '是否自动翻译字典（1-是，0-否）',
  `pageable` tinyint(1) DEFAULT '0' COMMENT '是否分页查询（1-是，0-否）',
  `max_rows` int DEFAULT '10000' COMMENT '最大导出条数',
  `sort_field` varchar(50) DEFAULT NULL COMMENT '排序字段',
  `sort_order` varchar(10) DEFAULT NULL COMMENT '排序方向（ASC/DESC）',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态（1-启用，0-禁用）',
  `include_sample` tinyint(1) DEFAULT '0' COMMENT '是否包含示例数据（用于模板）',
  `allow_import` tinyint(1) DEFAULT '1' COMMENT '是否允许导入',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `config_key` (`config_key`),
  KEY `idx_config_key` (`config_key`)
) ENGINE=InnoDB AUTO_INCREMENT=2068887630966468614 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Excel导出配置表';


-- forge_admin_new.sys_external_api definition

CREATE TABLE `sys_external_api` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `system_id` bigint NOT NULL COMMENT '所属系统ID',
  `api_code` varchar(50) NOT NULL COMMENT '接口编码',
  `api_name` varchar(100) NOT NULL COMMENT '接口名称',
  `api_desc` varchar(500) DEFAULT NULL COMMENT '接口描述',
  `api_path` varchar(255) NOT NULL COMMENT '接口路径',
  `api_method` varchar(10) NOT NULL COMMENT '请求方法(GET/POST/PUT/DELETE)',
  `request_content_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '请求Content-Type',
  `request_headers` text COMMENT '请求头配置(JSON)',
  `request_params` text COMMENT '请求参数配置(JSON)',
  `request_body_template` text COMMENT '请求体模板',
  `response_content_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '响应Content-Type',
  `response_data_path` varchar(100) DEFAULT NULL COMMENT '响应数据路径(JsonPath)',
  `response_total_path` varchar(100) DEFAULT NULL COMMENT '响应总数路径(JsonPath)',
  `param_mapping_enabled` tinyint(1) DEFAULT '0' COMMENT '是否启用参数映射',
  `param_mappings` text COMMENT '参数映射配置(JSON)',
  `response_transform_enabled` tinyint(1) DEFAULT '0' COMMENT '是否启用响应转换',
  `response_transform_script` text COMMENT '响应转换脚本',
  `error_code_path` varchar(100) DEFAULT NULL COMMENT '错误码路径(JsonPath)',
  `error_msg_path` varchar(100) DEFAULT NULL COMMENT '错误消息路径(JsonPath)',
  `success_codes` varchar(100) DEFAULT NULL COMMENT '成功码列表',
  `doc_file_id` varchar(100) DEFAULT NULL COMMENT '接口文档文件ID',
  `doc_file_name` varchar(255) DEFAULT NULL COMMENT '接口文档文件名',
  `rate_limit_enabled` tinyint(1) DEFAULT '0' COMMENT '是否启用限流',
  `rate_limit_qps` int DEFAULT NULL COMMENT '限流QPS',
  `cache_enabled` tinyint(1) DEFAULT '0' COMMENT '是否启用缓存',
  `cache_ttl` int DEFAULT NULL COMMENT '缓存时间(秒)',
  `cache_key_template` varchar(255) DEFAULT NULL COMMENT '缓存Key模板',
  `permission_check_enabled` tinyint(1) DEFAULT '0' COMMENT '是否启用权限检查',
  `required_permission` varchar(100) DEFAULT NULL COMMENT '所需权限',
  `api_status` int DEFAULT '1' COMMENT '状态(0停用 1启用)',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_api_code_system` (`api_code`,`system_id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_system_id` (`system_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='外部接口配置表';


-- forge_admin_new.sys_external_api_log definition

CREATE TABLE `sys_external_api_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `system_id` bigint DEFAULT NULL COMMENT '外部系统ID',
  `api_id` bigint NOT NULL COMMENT '外部接口ID',
  `system_name` varchar(100) DEFAULT NULL COMMENT '系统名称快照',
  `api_name` varchar(100) DEFAULT NULL COMMENT '接口名称快照',
  `api_code` varchar(50) DEFAULT NULL COMMENT '接口编码快照',
  `request_method` varchar(10) DEFAULT NULL COMMENT '请求方法',
  `request_url` varchar(1000) DEFAULT NULL COMMENT '请求URL',
  `request_params` text COMMENT '请求参数',
  `request_body` text COMMENT '请求体',
  `response_body` text COMMENT '响应体',
  `http_status_code` int DEFAULT NULL COMMENT 'HTTP状态码',
  `call_status` tinyint DEFAULT '1' COMMENT '调用状态（0失败 1成功）',
  `error_message` varchar(1000) DEFAULT NULL COMMENT '错误信息',
  `duration_ms` bigint DEFAULT NULL COMMENT '耗时毫秒',
  `debug_flag` tinyint(1) DEFAULT '0' COMMENT '是否调试调用',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_api` (`tenant_id`,`api_id`),
  KEY `idx_system_id` (`system_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2328 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='外部接口调用日志表';


-- forge_admin_new.sys_external_system definition

CREATE TABLE `sys_external_system` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `system_code` varchar(50) NOT NULL COMMENT '系统编码',
  `system_name` varchar(100) NOT NULL COMMENT '系统名称',
  `system_desc` varchar(500) DEFAULT NULL COMMENT '系统描述',
  `base_url` varchar(255) NOT NULL COMMENT '基础URL',
  `auth_type` varchar(20) NOT NULL COMMENT '认证类型(none/basic/token/oauth2/api_key/custom)',
  `basic_username` varchar(100) DEFAULT NULL COMMENT 'Basic认证用户名',
  `basic_password` varchar(255) DEFAULT NULL COMMENT 'Basic认证密码',
  `token_value` varchar(500) DEFAULT NULL COMMENT 'Token值',
  `token_header_name` varchar(50) DEFAULT NULL COMMENT 'Token Header名称',
  `token_prefix` varchar(20) DEFAULT NULL COMMENT 'Token前缀',
  `oauth2_token_url` varchar(255) DEFAULT NULL COMMENT 'OAuth2 Token URL',
  `oauth2_client_id` varchar(100) DEFAULT NULL COMMENT 'OAuth2 Client ID',
  `oauth2_client_secret` varchar(255) DEFAULT NULL COMMENT 'OAuth2 Client Secret',
  `oauth2_grant_type` varchar(20) DEFAULT NULL COMMENT 'OAuth2授权类型',
  `oauth2_scope` varchar(100) DEFAULT NULL COMMENT 'OAuth2 Scope',
  `api_key_name` varchar(50) DEFAULT NULL COMMENT 'API Key名称',
  `api_key_value` varchar(255) DEFAULT NULL COMMENT 'API Key值',
  `api_key_position` varchar(20) DEFAULT NULL COMMENT 'API Key位置(header/query)',
  `custom_auth_adapter` varchar(100) DEFAULT NULL COMMENT '自定义认证适配器',
  `custom_auth_config` text COMMENT '自定义认证配置',
  `trusted_internal` tinyint(1) DEFAULT '0' COMMENT '是否可信内部Forge系统',
  `proxy_enabled` tinyint(1) DEFAULT '0' COMMENT '是否启用代理',
  `proxy_host` varchar(100) DEFAULT NULL COMMENT '代理主机',
  `proxy_port` int DEFAULT NULL COMMENT '代理端口',
  `proxy_username` varchar(100) DEFAULT NULL COMMENT '代理用户名',
  `proxy_password` varchar(255) DEFAULT NULL COMMENT '代理密码',
  `retry_enabled` tinyint(1) DEFAULT '0' COMMENT '是否启用重试',
  `retry_max_attempts` int DEFAULT NULL COMMENT '最大重试次数',
  `retry_backoff_interval` int DEFAULT NULL COMMENT '重试间隔(ms)',
  `connect_timeout` int DEFAULT NULL COMMENT '连接超时(ms)',
  `read_timeout` int DEFAULT NULL COMMENT '读取超时(ms)',
  `write_timeout` int DEFAULT NULL COMMENT '写入超时(ms)',
  `ssl_verify_enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用SSL验证',
  `request_logging_enabled` tinyint(1) DEFAULT '0' COMMENT '是否启用请求日志',
  `system_status` int DEFAULT '1' COMMENT '状态(0停用 1启用)',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_system_code_tenant` (`system_code`,`tenant_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='外部系统配置表';


-- forge_admin_new.sys_file_group definition

CREATE TABLE `sys_file_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `group_name` varchar(100) NOT NULL COMMENT '分组名称',
  `group_code` varchar(100) DEFAULT NULL COMMENT '分组编码',
  `group_type` varchar(50) DEFAULT 'default' COMMENT '分组类型(document-文档,image-图片,video-视频,audio-音频,archive-压缩包,default-默认)',
  `parent_id` bigint DEFAULT NULL COMMENT '父分组ID',
  `sort` int DEFAULT '0' COMMENT '排序',
  `icon` varchar(100) DEFAULT NULL COMMENT '图标',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(1-正常,0-禁用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '删除标记(0-未删除,1-已删除)',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_group_type` (`group_type`),
  KEY `idx_group_code` (`group_code`),
  KEY `idx_sys_file_group_tenant` (`tenant_id`,`deleted`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2077546230840676354 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件分组表';


-- forge_admin_new.sys_file_metadata definition

CREATE TABLE `sys_file_metadata` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `file_id` varchar(64) NOT NULL COMMENT '文件唯一ID',
  `group_id` bigint DEFAULT NULL COMMENT '分组ID',
  `original_name` varchar(500) NOT NULL COMMENT '原始文件名',
  `storage_name` varchar(500) NOT NULL COMMENT '存储文件名',
  `file_path` varchar(1000) NOT NULL COMMENT '文件路径',
  `file_size` bigint NOT NULL COMMENT '文件大小(字节)',
  `mime_type` varchar(200) DEFAULT NULL COMMENT '文件MIME类型',
  `extension` varchar(50) DEFAULT NULL COMMENT '文件扩展名',
  `md5` varchar(64) DEFAULT NULL COMMENT '文件MD5',
  `storage_type` varchar(50) NOT NULL COMMENT '存储策略',
  `bucket` varchar(100) DEFAULT NULL COMMENT '存储桶/命名空间',
  `access_url` varchar(1000) DEFAULT NULL COMMENT '访问URL',
  `thumbnail_url` varchar(1000) DEFAULT NULL COMMENT '缩略图URL',
  `business_type` varchar(100) DEFAULT NULL COMMENT '业务类型',
  `business_id` varchar(100) DEFAULT NULL COMMENT '业务ID',
  `uploader_id` bigint DEFAULT NULL COMMENT '上传者ID',
  `upload_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `is_private` tinyint(1) DEFAULT '0' COMMENT '是否私有',
  `download_count` int DEFAULT '0' COMMENT '下载次数',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(1-正常,0-已删除)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `file_id` (`file_id`),
  KEY `idx_file_id` (`file_id`),
  KEY `idx_md5` (`md5`),
  KEY `idx_business` (`business_type`,`business_id`),
  KEY `idx_uploader` (`uploader_id`),
  KEY `idx_upload_time` (`upload_time`),
  KEY `idx_sys_file_metadata_tenant` (`tenant_id`,`status`,`upload_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2079833685673406466 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件元数据表';


-- forge_admin_new.sys_file_storage_config definition

CREATE TABLE `sys_file_storage_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_name` varchar(100) NOT NULL COMMENT '配置名称',
  `storage_type` varchar(50) NOT NULL COMMENT '存储类型(local/minio/aliyun_oss等)',
  `is_default` tinyint(1) DEFAULT '0' COMMENT '是否默认策略',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `endpoint` varchar(500) DEFAULT NULL COMMENT '访问端点',
  `access_key` varchar(200) DEFAULT NULL COMMENT '访问密钥ID',
  `secret_key` varchar(200) DEFAULT NULL COMMENT '访问密钥Secret',
  `bucket_name` varchar(100) DEFAULT NULL COMMENT '存储桶名称',
  `region` varchar(100) DEFAULT NULL COMMENT '区域',
  `base_path` varchar(200) DEFAULT NULL COMMENT '基础路径',
  `domain` varchar(500) DEFAULT NULL COMMENT '访问域名',
  `use_https` tinyint(1) DEFAULT '1' COMMENT '是否使用HTTPS',
  `max_file_size` int DEFAULT '100' COMMENT '最大文件大小(MB)',
  `allowed_types` varchar(500) NOT NULL COMMENT '允许的文件类型(逗号分隔)',
  `order_num` int DEFAULT '0' COMMENT '排序',
  `extra_config` text COMMENT '扩展配置(JSON)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
  PRIMARY KEY (`id`),
  KEY `idx_storage_type` (`storage_type`),
  KEY `idx_is_default` (`is_default`)
) ENGINE=InnoDB AUTO_INCREMENT=2076579065463427075 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件存储配置表';


-- forge_admin_new.sys_flow_approval_level definition

CREATE TABLE `sys_flow_approval_level` (
  `id` varchar(64) NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `node_config_id` varchar(64) NOT NULL COMMENT '节点配置ID',
  `level_index` int NOT NULL COMMENT '层级序号',
  `level_name` varchar(100) DEFAULT NULL COMMENT '层级名称',
  `assignee_type` varchar(50) DEFAULT NULL COMMENT '审批人类型',
  `assignee_value` text COMMENT '审批人值',
  `condition_expr` varchar(500) DEFAULT NULL COMMENT '层级条件表达式',
  `skip_condition` varchar(500) DEFAULT NULL COMMENT '跳过条件',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
  PRIMARY KEY (`id`),
  KEY `idx_node_config_id` (`node_config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批层级配置表';


-- forge_admin_new.sys_flow_business definition

CREATE TABLE `sys_flow_business` (
  `id` varchar(64) NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `business_key` varchar(100) NOT NULL COMMENT '业务Key',
  `business_type` varchar(50) NOT NULL COMMENT '业务类型（order/contract等）',
  `process_instance_id` varchar(64) DEFAULT NULL COMMENT '流程实例ID',
  `process_def_id` varchar(64) DEFAULT NULL COMMENT '流程定义ID',
  `process_def_key` varchar(100) DEFAULT NULL COMMENT '流程定义KEY',
  `title` varchar(200) DEFAULT NULL COMMENT '流程标题',
  `status` varchar(50) DEFAULT 'draft' COMMENT '业务状态（draft-草稿/running-审批中/approved-已通过/rejected-已驳回/canceled-已取消）',
  `apply_user_id` varchar(64) DEFAULT NULL COMMENT '申请人',
  `apply_user_name` varchar(100) DEFAULT NULL COMMENT '申请人姓名',
  `apply_dept_id` varchar(64) DEFAULT NULL COMMENT '申请部门ID',
  `apply_dept_name` varchar(100) DEFAULT NULL COMMENT '申请部门名称',
  `apply_time` datetime DEFAULT NULL COMMENT '申请时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration` bigint DEFAULT NULL COMMENT '流程耗时（毫秒）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_business_tenant_key` (`tenant_id`,`business_key`),
  KEY `idx_process_instance_id` (`process_instance_id`),
  KEY `idx_apply_user_id` (`apply_user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_sys_flow_business_tenant` (`tenant_id`,`status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程业务关联表';


-- forge_admin_new.sys_flow_category definition

CREATE TABLE `sys_flow_category` (
  `id` varchar(64) NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `category_code` varchar(50) NOT NULL COMMENT '分类编码',
  `category_name` varchar(100) NOT NULL COMMENT '分类名称',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '状态（0-禁用/1-启用）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `parent_id` varchar(64) DEFAULT NULL COMMENT '父分类ID',
  `level` int DEFAULT '1' COMMENT '层级深度',
  `ancestors` varchar(500) DEFAULT NULL COMMENT '祖先路径',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_category_tenant_code` (`tenant_id`,`category_code`),
  KEY `idx_category_code` (`category_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程分类表';


-- forge_admin_new.sys_flow_cc definition

CREATE TABLE `sys_flow_cc` (
  `id` varchar(64) NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `process_instance_id` varchar(64) DEFAULT NULL COMMENT '流程实例ID',
  `process_def_key` varchar(100) DEFAULT NULL COMMENT '流程定义KEY',
  `task_id` varchar(64) DEFAULT NULL COMMENT '来源任务ID',
  `title` varchar(200) DEFAULT NULL COMMENT '标题',
  `content` text COMMENT '内容摘要',
  `business_key` varchar(100) DEFAULT NULL COMMENT '业务Key',
  `cc_user_id` varchar(64) NOT NULL COMMENT '抄送人ID',
  `cc_user_name` varchar(100) DEFAULT NULL COMMENT '抄送人姓名',
  `send_user_id` varchar(64) DEFAULT NULL COMMENT '发送人ID',
  `send_user_name` varchar(100) DEFAULT NULL COMMENT '发送人姓名',
  `cc_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '抄送时间',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `is_read` tinyint DEFAULT '0' COMMENT '是否已读（0-未读/1-已读）',
  PRIMARY KEY (`id`),
  KEY `idx_process_instance_id` (`process_instance_id`),
  KEY `idx_cc_user_id` (`cc_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程抄送表';


-- forge_admin_new.sys_flow_comment definition

CREATE TABLE `sys_flow_comment` (
  `id` varchar(64) NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `process_instance_id` varchar(64) DEFAULT NULL COMMENT '流程实例ID',
  `process_def_key` varchar(100) DEFAULT NULL COMMENT '流程定义KEY',
  `task_id` varchar(64) DEFAULT NULL COMMENT '任务ID',
  `task_name` varchar(200) DEFAULT NULL COMMENT '任务名称',
  `type` varchar(20) DEFAULT 'comment' COMMENT '类型（comment-审批意见/event-流程事件）',
  `message` text COMMENT '意见内容',
  `user_id` varchar(64) DEFAULT NULL COMMENT '用户ID',
  `user_name` varchar(100) DEFAULT NULL COMMENT '用户姓名',
  `full_message` text COMMENT '完整消息（JSON）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_process_instance_id` (`process_instance_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_sys_flow_comment_tenant` (`tenant_id`,`process_instance_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程意见表';


-- forge_admin_new.sys_flow_condition_item definition

CREATE TABLE `sys_flow_condition_item` (
  `id` varchar(64) NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `rule_id` varchar(64) NOT NULL COMMENT '规则ID',
  `field_name` varchar(100) NOT NULL COMMENT '字段名称',
  `field_label` varchar(100) DEFAULT NULL COMMENT '字段标签',
  `field_type` varchar(32) DEFAULT 'string' COMMENT '字段类型：string/number/date/boolean/user/dept/role',
  `operator` varchar(32) NOT NULL COMMENT '操作符：eq/ne/gt/lt/ge/le/contains/startsWith/endsWith/in/notIn/isEmpty/isNotEmpty',
  `value` text COMMENT '比较值（JSON格式，支持多值）',
  `logic_connector` varchar(10) DEFAULT 'and' COMMENT '逻辑连接符：and/or',
  `group_id` varchar(64) DEFAULT NULL COMMENT '分组ID（用于条件分组）',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_rule_id` (`rule_id`),
  KEY `idx_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='条件规则项表';


-- forge_admin_new.sys_flow_condition_rule definition

CREATE TABLE `sys_flow_condition_rule` (
  `id` varchar(64) NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `rule_code` varchar(100) DEFAULT NULL COMMENT '规则编码',
  `model_id` varchar(64) DEFAULT NULL COMMENT '模型ID',
  `sequence_flow_id` varchar(128) NOT NULL COMMENT '序列流ID（BPMN中的SequenceFlow ID）',
  `condition_type` varchar(32) DEFAULT 'simple' COMMENT '条件类型：simple-简单条件，composite-组合条件，script-脚本',
  `condition_expression` text COMMENT '条件表达式（JSON格式存储）',
  `priority` int DEFAULT '0' COMMENT '优先级（数字越小优先级越高）',
  `is_default` tinyint(1) DEFAULT '0' COMMENT '是否默认路径',
  `status` int DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_model_id` (`model_id`),
  KEY `idx_sequence_flow_id` (`sequence_flow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程条件规则表';


-- forge_admin_new.sys_flow_entry definition

CREATE TABLE `sys_flow_entry` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `entry_code` varchar(128) NOT NULL COMMENT '入口编码',
  `entry_name` varchar(128) NOT NULL COMMENT '入口名称',
  `entry_desc` varchar(500) DEFAULT NULL COMMENT '入口说明',
  `model_key` varchar(128) NOT NULL COMMENT '流程模型Key',
  `form_key` varchar(128) NOT NULL COMMENT '表单Key',
  `form_version_id` bigint NOT NULL COMMENT '表单版本ID',
  `data_mode` varchar(32) NOT NULL DEFAULT 'PROCESS_ONLY' COMMENT '数据模式',
  `object_code` varchar(64) DEFAULT NULL COMMENT '业务对象编码',
  `config_key` varchar(128) DEFAULT NULL COMMENT '动态CRUD配置Key',
  `visible_scope` json DEFAULT NULL COMMENT '可见范围配置',
  `title_template` varchar(256) DEFAULT NULL COMMENT '流程标题模板',
  `business_key_template` varchar(256) DEFAULT NULL COMMENT '业务Key模板',
  `submit_strategy` json DEFAULT NULL COMMENT '提交策略配置',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0禁用 1启用',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志：0正常 1删除',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_entry_code` (`tenant_id`,`entry_code`),
  KEY `idx_flow_entry_model` (`tenant_id`,`model_key`,`status`),
  KEY `idx_flow_entry_form` (`tenant_id`,`form_key`,`form_version_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程应用入口表';


-- forge_admin_new.sys_flow_entry_field_mapping definition

CREATE TABLE `sys_flow_entry_field_mapping` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `entry_id` bigint NOT NULL COMMENT '流程入口ID',
  `form_field` varchar(128) NOT NULL COMMENT '表单字段',
  `target_type` varchar(32) NOT NULL DEFAULT 'FLOW_VARIABLE' COMMENT '目标类型：FLOW_VARIABLE/BUSINESS_FIELD',
  `target_field` varchar(128) DEFAULT NULL COMMENT '目标字段',
  `flow_variable` varchar(128) DEFAULT NULL COMMENT '流程变量名',
  `required` tinyint NOT NULL DEFAULT '0' COMMENT '是否必填映射',
  `mapping_config` json DEFAULT NULL COMMENT '映射扩展配置',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志：0正常 1删除',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_entry_mapping` (`tenant_id`,`entry_id`,`form_field`,`target_type`,`target_field`),
  KEY `idx_flow_entry_mapping_entry` (`tenant_id`,`entry_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程入口字段映射表';


-- forge_admin_new.sys_flow_error_log definition

CREATE TABLE `sys_flow_error_log` (
  `id` varchar(64) NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `process_instance_id` varchar(64) DEFAULT NULL COMMENT '流程实例ID',
  `process_def_id` varchar(64) DEFAULT NULL COMMENT '流程定义ID',
  `process_def_key` varchar(100) DEFAULT NULL COMMENT '流程定义KEY',
  `business_key` varchar(100) DEFAULT NULL COMMENT '业务Key',
  `task_id` varchar(64) DEFAULT NULL COMMENT 'Flowable任务ID',
  `task_name` varchar(200) DEFAULT NULL COMMENT '任务名称',
  `activity_id` varchar(100) DEFAULT NULL COMMENT 'BPMN节点ID',
  `activity_name` varchar(200) DEFAULT NULL COMMENT 'BPMN节点名称',
  `error_stage` varchar(80) DEFAULT NULL COMMENT '错误发生环节',
  `error_type` varchar(300) DEFAULT NULL COMMENT '异常类型',
  `error_message` text COMMENT '异常摘要',
  `stack_trace` text COMMENT '异常堆栈',
  `job_id` varchar(64) DEFAULT NULL COMMENT 'Flowable作业ID',
  `job_retries` int DEFAULT NULL COMMENT 'Flowable作业剩余重试次数',
  `status` tinyint DEFAULT '0' COMMENT '状态（0-未处理/1-已重试/2-已解决/3-重试失败）',
  `retry_count` int DEFAULT '0' COMMENT '人工重试次数',
  `last_retry_user_id` varchar(64) DEFAULT NULL COMMENT '最近重试人',
  `last_retry_time` datetime DEFAULT NULL COMMENT '最近重试时间',
  `retry_message` text COMMENT '重试说明或失败原因',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_process_instance_id` (`process_instance_id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_job_id` (`job_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程运行错误日志表';


-- forge_admin_new.sys_flow_fill_batch definition

CREATE TABLE `sys_flow_fill_batch` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `entry_id` bigint NOT NULL COMMENT '流程入口ID',
  `entry_code` varchar(128) NOT NULL COMMENT '入口编码',
  `batch_name` varchar(128) NOT NULL COMMENT '批次名称',
  `period_key` varchar(64) DEFAULT NULL COMMENT '周期标识',
  `target_scope` json DEFAULT NULL COMMENT '目标组织范围',
  `owner_rule` json DEFAULT NULL COMMENT '负责人规则',
  `deadline_time` datetime DEFAULT NULL COMMENT '截止时间',
  `allow_resubmit` tinyint NOT NULL DEFAULT '1' COMMENT '驳回后是否允许重新提交',
  `status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '批次状态：DRAFT/PUBLISHED/CLOSED',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志：0正常 1删除',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_flow_fill_batch_entry` (`tenant_id`,`entry_code`,`status`,`deadline_time`),
  KEY `idx_flow_fill_batch_period` (`tenant_id`,`period_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程组织批量填报批次表';


-- forge_admin_new.sys_flow_fill_batch_item definition

CREATE TABLE `sys_flow_fill_batch_item` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `batch_id` bigint NOT NULL COMMENT '批次ID',
  `entry_code` varchar(128) NOT NULL COMMENT '入口编码',
  `org_id` bigint NOT NULL COMMENT '组织ID',
  `org_name` varchar(128) DEFAULT NULL COMMENT '组织名称',
  `owner_user_id` bigint DEFAULT NULL COMMENT '负责人用户ID',
  `owner_user_name` varchar(100) DEFAULT NULL COMMENT '负责人姓名',
  `form_instance_id` bigint DEFAULT NULL COMMENT '表单实例ID',
  `object_code` varchar(64) DEFAULT NULL COMMENT '业务对象编码',
  `record_id` bigint DEFAULT NULL COMMENT '业务记录ID',
  `process_instance_id` varchar(128) DEFAULT NULL COMMENT '流程实例ID',
  `submit_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '填报状态：PENDING/SUBMITTED/RETURNED/OVERDUE',
  `flow_status` varchar(32) DEFAULT NULL COMMENT '流程状态',
  `deadline_time` datetime DEFAULT NULL COMMENT '截止时间',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志：0正常 1删除',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_fill_batch_org` (`tenant_id`,`batch_id`,`org_id`),
  KEY `idx_flow_fill_item_owner` (`tenant_id`,`owner_user_id`,`submit_status`),
  KEY `idx_flow_fill_item_process` (`tenant_id`,`process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程组织批量填报明细表';


-- forge_admin_new.sys_flow_form definition

CREATE TABLE `sys_flow_form` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `form_key` varchar(100) NOT NULL COMMENT '表单Key（唯一标识）',
  `form_name` varchar(200) NOT NULL COMMENT '表单名称',
  `form_category` varchar(64) DEFAULT NULL COMMENT '表单分类',
  `form_type` varchar(50) DEFAULT 'dynamic' COMMENT '表单类型（dynamic-动态表单/external-外部表单/builtin-内置表单）',
  `form_schema` text COMMENT '表单Schema（JSON格式）',
  `field_registry` json DEFAULT NULL COMMENT '字段目录快照',
  `form_url` varchar(500) DEFAULT NULL COMMENT '外部表单URL（formType为external时使用）',
  `component_path` varchar(200) DEFAULT NULL COMMENT '内置表单组件路径（formType为builtin时使用）',
  `form_config` text COMMENT '表单配置（JSON格式，包含校验规则、事件等）',
  `default_data_mode` varchar(32) NOT NULL DEFAULT 'PROCESS_ONLY' COMMENT '默认数据模式',
  `version` int DEFAULT '1' COMMENT '版本号',
  `status` tinyint DEFAULT '1' COMMENT '状态（0-禁用/1-启用）',
  `publish_status` tinyint NOT NULL DEFAULT '0' COMMENT '发布状态：0草稿 1已发布',
  `current_version_id` bigint DEFAULT NULL COMMENT '当前发布版本ID',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志（0-正常/1-删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_form_tenant_key` (`tenant_id`,`form_key`),
  KEY `idx_form_key` (`form_key`),
  KEY `idx_form_name` (`form_name`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程表单定义表';


-- forge_admin_new.sys_flow_form_instance definition

CREATE TABLE `sys_flow_form_instance` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `entry_id` bigint DEFAULT NULL COMMENT '流程入口ID',
  `entry_code` varchar(128) NOT NULL COMMENT '入口编码',
  `business_key` varchar(256) NOT NULL COMMENT '流程业务Key',
  `process_instance_id` varchar(128) DEFAULT NULL COMMENT '流程实例ID',
  `model_key` varchar(128) NOT NULL COMMENT '流程模型Key',
  `form_key` varchar(128) NOT NULL COMMENT '表单Key',
  `form_version_id` bigint NOT NULL COMMENT '表单版本ID',
  `form_version` int NOT NULL COMMENT '表单版本号',
  `schema_snapshot` longtext COMMENT '表单Schema快照',
  `field_registry` json DEFAULT NULL COMMENT '字段目录快照',
  `form_data` json DEFAULT NULL COMMENT '表单填报数据',
  `data_mode` varchar(32) NOT NULL DEFAULT 'PROCESS_ONLY' COMMENT '数据模式',
  `object_code` varchar(64) DEFAULT NULL COMMENT '业务对象编码',
  `record_id` bigint DEFAULT NULL COMMENT '业务记录ID',
  `title` varchar(256) DEFAULT NULL COMMENT '流程标题',
  `start_user_id` bigint DEFAULT NULL COMMENT '发起人ID',
  `start_user_name` varchar(100) DEFAULT NULL COMMENT '发起人姓名',
  `start_dept_id` bigint DEFAULT NULL COMMENT '发起部门ID',
  `start_dept_name` varchar(128) DEFAULT NULL COMMENT '发起部门名称',
  `status` varchar(32) NOT NULL DEFAULT 'RUNNING' COMMENT '状态：DRAFT/RUNNING/APPROVED/REJECTED/CANCELED/TERMINATED',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志：0正常 1删除',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_form_instance_business` (`tenant_id`,`business_key`),
  KEY `idx_flow_form_instance_process` (`tenant_id`,`process_instance_id`),
  KEY `idx_flow_form_instance_entry` (`tenant_id`,`entry_code`,`status`,`create_time`),
  KEY `idx_flow_form_instance_record` (`tenant_id`,`object_code`,`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程表单填报实例表';


-- forge_admin_new.sys_flow_form_version definition

CREATE TABLE `sys_flow_form_version` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `form_id` bigint NOT NULL COMMENT '流程表单ID',
  `form_key` varchar(128) NOT NULL COMMENT '表单Key',
  `form_name` varchar(128) NOT NULL COMMENT '表单名称',
  `form_category` varchar(64) DEFAULT NULL COMMENT '表单分类',
  `form_type` varchar(32) NOT NULL DEFAULT 'dynamic' COMMENT '表单类型',
  `version` int NOT NULL COMMENT '版本号',
  `form_schema` longtext COMMENT '表单Schema快照',
  `field_registry` json DEFAULT NULL COMMENT '字段目录快照',
  `form_config` json DEFAULT NULL COMMENT '表单配置快照',
  `default_data_mode` varchar(32) NOT NULL DEFAULT 'PROCESS_ONLY' COMMENT '默认数据模式',
  `publish_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `publish_by` bigint DEFAULT NULL COMMENT '发布人ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_form_version` (`tenant_id`,`form_id`,`version`),
  KEY `idx_flow_form_version_key` (`tenant_id`,`form_key`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程表单发布版本表';


-- forge_admin_new.sys_flow_model definition

CREATE TABLE `sys_flow_model` (
  `id` varchar(64) NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `model_key` varchar(100) NOT NULL COMMENT '模型标识',
  `model_name` varchar(200) NOT NULL COMMENT '模型名称',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `category` varchar(100) DEFAULT NULL COMMENT '分类',
  `flow_type` varchar(50) DEFAULT NULL COMMENT '流程类型（leave-请假/expense-报销/approval-审批）',
  `designer_type` varchar(32) NOT NULL DEFAULT 'approval' COMMENT '设计器类型：approval-审批流程/business-业务流程',
  `form_type` varchar(50) DEFAULT 'dynamic' COMMENT '表单类型（dynamic-动态表单/custom-业务表单）',
  `form_id` varchar(64) DEFAULT NULL COMMENT '表单ID（业务表单时使用）',
  `form_json` text COMMENT '动态表单JSON配置',
  `version` int DEFAULT '1' COMMENT '版本号',
  `process_definition_id` varchar(64) DEFAULT NULL COMMENT 'Flowable流程定义ID',
  `deployment_id` varchar(64) DEFAULT NULL COMMENT 'Flowable部署ID',
  `deployment_key` varchar(100) DEFAULT NULL COMMENT '部署KEY（发布后生成）',
  `status` tinyint DEFAULT '0' COMMENT '状态（0-设计/1-已发布/2-禁用）',
  `importance_level` int NOT NULL DEFAULT '1' COMMENT '重要性等级（1-普通/2-重要）',
  `deploy_time` datetime DEFAULT NULL COMMENT '发布时间',
  `last_update_by` varchar(64) DEFAULT NULL COMMENT '最后修改人',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint DEFAULT '0' COMMENT '删除标志（0-正常/1-删除）',
  `bpmn_xml` longtext,
  `notify_type` varchar(30) DEFAULT NULL COMMENT '事件通知方式：none-不通知 / redis-Redis Pub/Sub / webhook-HTTP Webhook（互斥）',
  `webhook_url` varchar(500) DEFAULT NULL COMMENT 'Webhook 回调地址，仅 notify_type=webhook 时生效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_model_tenant_key` (`tenant_id`,`model_key`),
  KEY `idx_model_key` (`model_key`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  KEY `idx_sys_flow_model_tenant` (`tenant_id`,`status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程模型表';


-- forge_admin_new.sys_flow_model_version definition

CREATE TABLE `sys_flow_model_version` (
  `id` varchar(64) NOT NULL COMMENT '主键',
  `model_id` varchar(64) NOT NULL COMMENT '模型ID',
  `version` int NOT NULL COMMENT '版本号',
  `version_name` varchar(100) NOT NULL COMMENT '版本名称',
  `version_tag` varchar(20) NOT NULL DEFAULT 'draft' COMMENT '版本标记（draft/test/release/deprecated）',
  `bpmn_xml` text NOT NULL COMMENT 'BPMN XML',
  `form_json` text COMMENT '表单配置',
  `change_description` varchar(500) DEFAULT NULL COMMENT '变更说明',
  `deployment_id` varchar(64) DEFAULT NULL COMMENT '部署ID',
  `process_definition_id` varchar(64) DEFAULT NULL COMMENT '流程定义ID',
  `publish_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '发布人',
  `publish_time` datetime NOT NULL COMMENT '发布时间',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `del_flag` int NOT NULL DEFAULT '0' COMMENT '删除标志（0-正常/1-删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_model_version` (`model_id`,`version`),
  KEY `idx_model_id` (`model_id`),
  KEY `idx_sys_flow_model_version_tenant` (`tenant_id`,`model_id`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程模型版本历史表';


-- forge_admin_new.sys_flow_node_config definition

CREATE TABLE `sys_flow_node_config` (
  `id` varchar(64) NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `model_id` varchar(64) NOT NULL COMMENT '流程模型ID',
  `node_id` varchar(100) NOT NULL COMMENT '节点ID',
  `node_name` varchar(200) DEFAULT NULL COMMENT '节点名称',
  `node_type` varchar(50) DEFAULT 'approval' COMMENT '节点类型',
  `assignee_type` varchar(50) DEFAULT NULL COMMENT '审批人类型(user/role/dept/post/expr)',
  `assignee_value` text COMMENT '审批人值',
  `assignee_expr` varchar(500) DEFAULT NULL COMMENT '审批人表达式',
  `multi_instance_type` varchar(50) DEFAULT 'none' COMMENT '会签类型(none/sequential/parallel)',
  `completion_condition` varchar(500) DEFAULT NULL COMMENT '完成条件',
  `pass_rate` decimal(5,2) DEFAULT NULL COMMENT '通过比例',
  `due_date_days` int DEFAULT NULL COMMENT '超时天数',
  `due_date_hours` int DEFAULT NULL COMMENT '超时小时数',
  `timeout_action` varchar(50) DEFAULT NULL COMMENT '超时动作(auto_pass/auto_reject/notify)',
  `allow_approve` tinyint DEFAULT '1' COMMENT '允许通过',
  `allow_delegate` tinyint DEFAULT '1' COMMENT '允许转办',
  `allow_transfer` tinyint DEFAULT '1' COMMENT '允许转交',
  `allow_add_sign` tinyint DEFAULT '0' COMMENT '允许加签',
  `allow_counter_sign` tinyint DEFAULT '0' COMMENT '允许减签',
  `allow_return` tinyint DEFAULT '0' COMMENT '允许退回上一审批节点',
  `allow_terminate` tinyint DEFAULT '0' COMMENT '允许终结流程',
  `require_signature` tinyint DEFAULT '0' COMMENT '办理时需要签名',
  `require_comment` tinyint DEFAULT '1' COMMENT '办理时需要审批意见',
  `form_permission` text COMMENT '表单字段权限配置JSON',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `timeout_notify_users` text,
  `allow_reject` varchar(10) DEFAULT NULL,
  `allow_reject_to_start` varchar(10) DEFAULT NULL,
  `allow_withdraw` varchar(10) DEFAULT NULL,
  `form_key` varchar(30) DEFAULT NULL,
  `priority` int DEFAULT NULL,
  `task_listeners` text,
  `ext_config` varchar(100) DEFAULT NULL,
  `status` varchar(10) DEFAULT NULL,
  `last_update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '最后修改人',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建人',
  `del_flag` varchar(64) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `update_by` varchar(20) DEFAULT NULL,
  `overdue_reminder_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否启用逾期提醒',
  `overdue_reminder_template_code` varchar(50) DEFAULT NULL COMMENT '逾期提醒消息模板编码',
  `overdue_reminder_channels` varchar(200) DEFAULT NULL COMMENT '逾期提醒推送渠道，逗号分隔',
  `overdue_reminder_repeat_mode` varchar(32) NOT NULL DEFAULT 'once' COMMENT '逾期提醒重复策略：once/interval',
  `overdue_reminder_interval_minutes` int NOT NULL DEFAULT '1440' COMMENT '重复提醒间隔分钟',
  `overdue_reminder_max_times` int NOT NULL DEFAULT '1' COMMENT '最大提醒次数',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_model_node_active` (`model_id`,`node_id`,`del_flag`),
  KEY `idx_model_id` (`model_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批节点配置表';


-- forge_admin_new.sys_flow_overdue_reminder_record definition

CREATE TABLE `sys_flow_overdue_reminder_record` (
  `id` varchar(64) NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `task_id` varchar(64) NOT NULL COMMENT 'Flowable任务ID',
  `process_instance_id` varchar(64) DEFAULT NULL COMMENT '流程实例ID',
  `process_def_key` varchar(100) DEFAULT NULL COMMENT '流程定义KEY',
  `task_def_key` varchar(100) DEFAULT NULL COMMENT '任务定义Key',
  `reminder_key` varchar(160) NOT NULL COMMENT '提醒批次键',
  `channel` varchar(20) NOT NULL COMMENT '推送渠道',
  `template_code` varchar(50) DEFAULT NULL COMMENT '消息模板编码',
  `receiver_user_ids` text COMMENT '接收人用户ID集合',
  `message_id` bigint DEFAULT NULL COMMENT '消息ID',
  `send_status` tinyint NOT NULL DEFAULT '0' COMMENT '发送状态：0-待发送/1-成功/2-失败',
  `send_time` datetime DEFAULT NULL COMMENT '发送时间',
  `error_message` varchar(1000) DEFAULT NULL COMMENT '失败原因',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_overdue_reminder` (`tenant_id`,`reminder_key`,`channel`),
  KEY `idx_flow_overdue_task` (`tenant_id`,`task_id`,`send_time`),
  KEY `idx_flow_overdue_status` (`tenant_id`,`send_status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程任务逾期提醒记录';


-- forge_admin_new.sys_flow_spel_template definition

CREATE TABLE `sys_flow_spel_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `template_code` varchar(50) NOT NULL COMMENT '模板编码',
  `expression` varchar(500) NOT NULL COMMENT 'SPEL表达式',
  `description` varchar(200) DEFAULT NULL COMMENT '描述说明',
  `category` varchar(50) DEFAULT 'general' COMMENT '分类：general/dept/role/region/custom',
  `example_params` varchar(500) DEFAULT NULL COMMENT '示例参数（JSON格式）',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `sort` int DEFAULT '100' COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`tenant_id`,`template_code`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程SPEL表达式模板';


-- forge_admin_new.sys_flow_statistics definition

CREATE TABLE `sys_flow_statistics` (
  `id` varchar(64) NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `process_def_key` varchar(100) NOT NULL COMMENT '流程定义KEY',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `total_instances` int DEFAULT '0' COMMENT '总实例数',
  `running_instances` int DEFAULT '0' COMMENT '运行中实例数',
  `completed_instances` int DEFAULT '0' COMMENT '已完成实例数',
  `terminated_instances` int DEFAULT '0' COMMENT '已终止实例数',
  `avg_duration` bigint DEFAULT NULL COMMENT '平均耗时(毫秒)',
  `max_duration` bigint DEFAULT NULL COMMENT '最大耗时',
  `min_duration` bigint DEFAULT NULL COMMENT '最小耗时',
  `total_tasks` int DEFAULT '0' COMMENT '总任务数',
  `avg_task_duration` bigint DEFAULT NULL COMMENT '平均任务耗时',
  `timeout_tasks` int DEFAULT '0' COMMENT '超时任务数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_statistics_tenant_date` (`tenant_id`,`process_def_key`,`stat_date`),
  KEY `idx_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程监控统计表';


-- forge_admin_new.sys_flow_task definition

CREATE TABLE `sys_flow_task` (
  `id` varchar(64) NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `task_id` varchar(64) DEFAULT NULL COMMENT 'Flowable任务ID',
  `task_name` varchar(200) DEFAULT NULL COMMENT '任务名称',
  `task_def_key` varchar(100) DEFAULT NULL COMMENT '任务定义Key',
  `task_def_id` varchar(100) DEFAULT NULL COMMENT '任务定义ID',
  `process_instance_id` varchar(64) DEFAULT NULL COMMENT '流程实例ID',
  `process_def_id` varchar(64) DEFAULT NULL COMMENT '流程定义ID',
  `process_def_key` varchar(100) DEFAULT NULL COMMENT '流程定义KEY',
  `business_key` varchar(100) DEFAULT NULL COMMENT '业务Key',
  `business_type` varchar(50) DEFAULT NULL COMMENT '业务类型',
  `title` varchar(200) NOT NULL COMMENT '任务标题',
  `assignee` varchar(64) DEFAULT NULL COMMENT '处理人（签收后）',
  `assignee_name` varchar(100) DEFAULT NULL COMMENT '处理人姓名',
  `candidate_users` varchar(1000) DEFAULT NULL COMMENT '候选人（逗号分隔）',
  `candidate_groups` varchar(1000) DEFAULT NULL COMMENT '候选组（逗号分隔）',
  `owner` varchar(64) DEFAULT NULL COMMENT '任务拥有人',
  `due_date` datetime DEFAULT NULL COMMENT '截止日期',
  `priority` int DEFAULT '50' COMMENT '优先级（0-100）',
  `status` tinyint DEFAULT '0' COMMENT '状态（0-待办/1-已签收/2-已通过/3-已驳回/4-已转办/5-已委派/6-已撤回）',
  `comment` text COMMENT '审批意见',
  `signature` text COMMENT '审批签名',
  `attachment_urls` text COMMENT '附件URL（逗号分隔）',
  `start_user_id` varchar(64) DEFAULT NULL COMMENT '流程发起人',
  `start_user_name` varchar(100) DEFAULT NULL COMMENT '发起人姓名',
  `start_dept_id` varchar(64) DEFAULT NULL COMMENT '发起部门ID',
  `start_dept_name` varchar(100) DEFAULT NULL COMMENT '发起部门名称',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `claim_time` datetime DEFAULT NULL COMMENT '签收时间',
  `complete_time` datetime DEFAULT NULL COMMENT '完成时间',
  `action_idempotency_key` varchar(128) DEFAULT NULL COMMENT '受控流程动作幂等键',
  `action_request_digest` varchar(71) DEFAULT NULL COMMENT '受控流程动作规范请求SHA-256摘要',
  `action_type` varchar(16) DEFAULT NULL COMMENT '受控流程动作类型APPROVE/REJECT',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_process_instance_id` (`process_instance_id`),
  KEY `idx_assignee` (`assignee`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_sys_flow_task_tenant` (`tenant_id`,`status`,`create_time`),
  KEY `idx_flow_task_action_idempotency` (`tenant_id`,`action_idempotency_key`,`action_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程任务表';


-- forge_admin_new.sys_flow_template definition

CREATE TABLE `sys_flow_template` (
  `id` varchar(64) NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `template_key` varchar(100) NOT NULL COMMENT '模板标识',
  `template_name` varchar(200) NOT NULL COMMENT '模板名称',
  `category` varchar(50) DEFAULT NULL COMMENT '分类',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `icon` varchar(100) DEFAULT NULL COMMENT '图标',
  `form_type` varchar(50) DEFAULT NULL COMMENT '表单类型',
  `form_json` text COMMENT '表单JSON',
  `bpmn_xml` text COMMENT 'BPMN流程XML',
  `thumbnail` varchar(500) DEFAULT NULL COMMENT '缩略图',
  `variables` text COMMENT '流程变量定义（JSON）',
  `version` int DEFAULT '1' COMMENT '版本',
  `status` tinyint DEFAULT '1' COMMENT '状态（0-禁用/1-启用）',
  `usage_count` int DEFAULT '0' COMMENT '使用次数',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志：0正常 1删除',
  `is_system` varchar(10) DEFAULT NULL,
  `sort_order` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_template_tenant_key` (`tenant_id`,`template_key`),
  KEY `idx_template_key` (`template_key`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程模板表';


-- forge_admin_new.sys_id_sequence definition

CREATE TABLE `sys_id_sequence` (
  `biz_key` varchar(100) NOT NULL COMMENT '业务维度唯一键',
  `max_id` bigint NOT NULL DEFAULT '0' COMMENT '当前已分配的最大ID',
  `step` int NOT NULL DEFAULT '1000' COMMENT '步长',
  `version` int NOT NULL DEFAULT '0' COMMENT '版本（乐观锁）',
  `reset_policy` varchar(20) DEFAULT 'NONE' COMMENT '重置策略：NONE/DAILY/HOURLY',
  `seq_length` int DEFAULT '8' COMMENT '序列长度（左侧补零）',
  `prefix` varchar(50) DEFAULT NULL COMMENT '前缀',
  PRIMARY KEY (`biz_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分布式ID序列配置表';


-- forge_admin_new.sys_job_api_idempotency definition

CREATE TABLE `sys_job_api_idempotency` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `token_id` bigint NOT NULL COMMENT '服务账号Token ID',
  `job_config_id` bigint NOT NULL COMMENT '任务配置ID',
  `idempotency_key_hash` char(64) NOT NULL COMMENT '幂等键SHA-256 Hash',
  `execution_id` bigint NOT NULL COMMENT '预留执行记录ID',
  `expires_at` datetime NOT NULL COMMENT '幂等记录过期时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_job_api_idempotency_active` (`tenant_id`,`token_id`,`job_config_id`,`idempotency_key_hash`,`del_flag`),
  KEY `idx_job_api_idempotency_execution` (`execution_id`),
  KEY `idx_job_api_idempotency_expires_at` (`expires_at`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='定时任务开放API幂等记录';


-- forge_admin_new.sys_job_api_token definition

CREATE TABLE `sys_job_api_token` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `caller_name` varchar(100) NOT NULL COMMENT '调用方名称',
  `caller_description` varchar(500) DEFAULT NULL COMMENT '调用方说明',
  `token_key_id` varchar(22) NOT NULL COMMENT 'Token不可猜测Key ID',
  `token_prefix` varchar(32) NOT NULL COMMENT 'Token展示前缀',
  `token_hash` char(64) NOT NULL COMMENT 'Token HMAC-SHA256 Hash',
  `scopes` varchar(255) NOT NULL COMMENT '空格分隔Scope',
  `resource_job_ids` json NOT NULL COMMENT '允许访问的任务ID JSON数组',
  `resource_job_groups` json NOT NULL COMMENT '允许访问的任务组 JSON数组',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/REVOKED',
  `issued_at` datetime NOT NULL COMMENT '签发时间',
  `expires_at` datetime NOT NULL COMMENT '过期时间',
  `last_used_at` datetime DEFAULT NULL COMMENT '最后使用时间',
  `revoked_at` datetime DEFAULT NULL COMMENT '吊销时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_job_api_token_key_active` (`token_key_id`,`del_flag`),
  KEY `idx_job_api_token_tenant_status` (`tenant_id`,`status`,`del_flag`),
  KEY `idx_job_api_token_expires_at` (`expires_at`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='定时任务开放API服务账号Token';


-- forge_admin_new.sys_job_config definition

CREATE TABLE `sys_job_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `job_name` varchar(200) NOT NULL COMMENT '任务名称',
  `job_group` varchar(200) NOT NULL DEFAULT 'DEFAULT' COMMENT '任务分组',
  `description` varchar(500) DEFAULT NULL COMMENT '任务描述',
  `executor_bean` varchar(200) DEFAULT NULL COMMENT '执行器Bean名称',
  `executor_method` varchar(200) DEFAULT NULL COMMENT '执行器方法名',
  `executor_handler` varchar(200) DEFAULT NULL COMMENT '执行器Handler',
  `executor_service` varchar(200) DEFAULT NULL COMMENT '执行器服务名（RPC模式）',
  `schedule_type` varchar(20) NOT NULL DEFAULT 'CRON' COMMENT '调度类型：CRON/ONCE',
  `cron_expression` varchar(100) DEFAULT NULL COMMENT 'Cron表达式',
  `fire_once_time` datetime DEFAULT NULL COMMENT '一次性任务本地触发时间',
  `timezone` varchar(64) NOT NULL DEFAULT 'Asia/Shanghai' COMMENT 'IANA时区',
  `job_param` text COMMENT '任务参数',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停止 1-运行',
  `execute_mode` varchar(20) NOT NULL DEFAULT 'HANDLER' COMMENT '执行模式：BEAN/HANDLER',
  `invoke_mode` varchar(16) NOT NULL DEFAULT 'SINGLE' COMMENT '调用方式：SINGLE/FLOW',
  `flow_model_key` varchar(100) DEFAULT NULL COMMENT '已发布流程模型Key',
  `flow_model_version` int DEFAULT NULL COMMENT '保存时固定的流程模型版本',
  `flow_deployment_id` varchar(64) DEFAULT NULL COMMENT '保存时固定的Flowable部署ID',
  `flow_process_definition_id` varchar(64) DEFAULT NULL COMMENT '保存时固定的Flowable流程定义ID',
  `concurrent_policy` varchar(32) NOT NULL DEFAULT 'ALLOW' COMMENT '并发策略：ALLOW/SKIP_IF_RUNNING',
  `misfire_policy` varchar(32) NOT NULL DEFAULT 'DO_NOTHING' COMMENT '错过触发策略：FIRE_ONCE_NOW/DO_NOTHING',
  `idempotent_flag` tinyint NOT NULL DEFAULT '0' COMMENT '是否允许幂等重试：0否 1是',
  `retry_count` int DEFAULT '0' COMMENT '失败重试次数',
  `consecutive_failures` int NOT NULL DEFAULT '0' COMMENT '连续失败次数',
  `last_completion_time` datetime DEFAULT NULL COMMENT '最近推进失败统计的执行完成时间',
  `last_completion_execution_id` bigint DEFAULT NULL COMMENT '最近推进失败统计的执行ID',
  `alarm_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否启用失败告警：0否 1是',
  `alarm_channels` varchar(64) DEFAULT NULL COMMENT '失败告警渠道：WEB,EMAIL',
  `alarm_recipient_user_ids` varchar(2000) DEFAULT NULL COMMENT '站内信接收用户ID，逗号分隔',
  `alarm_email` varchar(500) DEFAULT NULL COMMENT '告警邮箱',
  `webhook_url` varchar(500) DEFAULT NULL COMMENT 'WebHook地址',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  `sync_status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT 'Quartz同步状态',
  `sync_error` varchar(1000) DEFAULT NULL COMMENT '最近一次Quartz同步错误',
  `sync_time` datetime DEFAULT NULL COMMENT '最近一次Quartz同步时间',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_job_name_group_active` (`job_name`,`job_group`,`del_flag`),
  KEY `idx_job_sync_status_del` (`sync_status`,`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务配置表';


-- forge_admin_new.sys_job_log definition

CREATE TABLE `sys_job_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `job_name` varchar(200) NOT NULL COMMENT '任务名称',
  `job_group` varchar(200) NOT NULL COMMENT '任务分组',
  `executor_handler` varchar(200) DEFAULT NULL COMMENT '执行器Handler',
  `job_param` text COMMENT '任务参数',
  `trigger_time` datetime DEFAULT NULL COMMENT '触发时间',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `heartbeat_time` datetime DEFAULT NULL COMMENT '运行中任务最近心跳时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration` bigint DEFAULT NULL COMMENT '执行耗时(ms)',
  `status` tinyint NOT NULL COMMENT '状态：1-成功 0-失败',
  `result` text COMMENT '执行结果',
  `exception_msg` text COMMENT '异常信息',
  `retry_count` int DEFAULT '0' COMMENT '重试次数',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
  `job_config_id` bigint DEFAULT NULL COMMENT '任务配置ID',
  `trigger_type` varchar(20) NOT NULL DEFAULT 'UNKNOWN' COMMENT '触发类型',
  `scheduled_fire_time` datetime DEFAULT NULL COMMENT 'Quartz原计划触发时间',
  `fire_instance_id` varchar(200) DEFAULT NULL COMMENT 'Quartz执行实例ID',
  `process_instance_id` varchar(64) DEFAULT NULL COMMENT 'Flowable流程实例ID',
  PRIMARY KEY (`id`),
  KEY `idx_job_name` (`job_name`),
  KEY `idx_trigger_time` (`trigger_time`),
  KEY `idx_status` (`status`),
  KEY `idx_job_log_config_trigger` (`job_config_id`,`trigger_time`),
  KEY `idx_job_log_observability` (`job_config_id`,`status`,`trigger_type`,`trigger_time`),
  KEY `idx_job_log_process_instance` (`process_instance_id`,`del_flag`),
  KEY `idx_job_log_recovery` (`status`,`heartbeat_time`,`trigger_time`,`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=14800 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务执行日志表';


-- forge_admin_new.sys_login_log definition

CREATE TABLE `sys_login_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `tenant_id` bigint DEFAULT '0' COMMENT '租户编号',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `username` varchar(50) DEFAULT NULL COMMENT '用户名',
  `login_type` varchar(50) DEFAULT NULL COMMENT '登录类型（LOGIN/LOGOUT/REGISTER）',
  `login_status` tinyint DEFAULT '1' COMMENT '登录状态（0-失败，1-成功）',
  `login_ip` varchar(50) DEFAULT NULL COMMENT '登录IP',
  `login_location` varchar(255) DEFAULT NULL COMMENT '登录地点',
  `browser` varchar(100) DEFAULT NULL COMMENT '浏览器',
  `os` varchar(100) DEFAULT NULL COMMENT '操作系统',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理',
  `login_message` varchar(500) DEFAULT NULL COMMENT '登录信息',
  `login_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  `client_code` varchar(50) DEFAULT 'pc' COMMENT '客户端代码：pc/app',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_user` (`tenant_id`,`user_id`),
  KEY `idx_login_time` (`login_time`),
  KEY `idx_login_status` (`login_status`)
) ENGINE=InnoDB AUTO_INCREMENT=11562 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='登录日志表';


-- forge_admin_new.sys_message definition

CREATE TABLE `sys_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `title` varchar(200) NOT NULL COMMENT '消息标题',
  `content` text NOT NULL COMMENT '消息内容',
  `type` varchar(20) NOT NULL DEFAULT 'SYSTEM' COMMENT '消息类型：SYSTEM-系统消息/SMS-短信/EMAIL-邮件/CUSTOM-自定义',
  `send_scope` varchar(20) NOT NULL DEFAULT 'USERS' COMMENT '发送范围：ALL-全员/ORG-指定组织/USERS-指定人员',
  `send_channel` varchar(20) NOT NULL DEFAULT 'WEB' COMMENT '发送渠道：WEB-站内信/SMS-短信/EMAIL-邮件/PUSH-推送',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '消息状态：0-草稿/1-已发送/2-发送失败',
  `sender_id` bigint DEFAULT NULL COMMENT '发送人ID',
  `sender_name` varchar(100) DEFAULT NULL COMMENT '发送人姓名',
  `template_code` varchar(50) DEFAULT NULL COMMENT '模板编码',
  `template_params` json DEFAULT NULL COMMENT '模板参数JSON',
  `biz_type` varchar(50) DEFAULT NULL COMMENT '业务类型（如：ORDER、APPROVAL、TASK等）',
  `biz_key` varchar(100) DEFAULT NULL COMMENT '业务主键（如：订单ID、流程实例ID等）',
  `connection_id` bigint DEFAULT NULL COMMENT '企业协同连接ID（COLLABORATION渠道使用）',
  `idempotency_key` varchar(128) DEFAULT NULL COMMENT '确定性消息幂等键（相同键并发只创建一份逻辑消息）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_idempotency` (`tenant_id`,`idempotency_key`),
  KEY `idx_tenant_type` (`tenant_id`,`type`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_biz` (`biz_type`,`biz_key`)
) ENGINE=InnoDB AUTO_INCREMENT=91 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统消息主表';


-- forge_admin_new.sys_message_biz_type definition

CREATE TABLE `sys_message_biz_type` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `biz_type` varchar(50) NOT NULL COMMENT '业务类型编码',
  `biz_name` varchar(100) NOT NULL COMMENT '业务类型名称',
  `jump_url` varchar(500) DEFAULT NULL COMMENT '跳转URL模板，支持变量：${bizKey}、${messageId}',
  `jump_target` varchar(20) DEFAULT '_self' COMMENT '跳转方式：_self-当前页/_blank-新窗口',
  `icon` varchar(100) DEFAULT NULL COMMENT '图标',
  `sort` int DEFAULT '0' COMMENT '排序',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用：0-禁用/1-启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注说明',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_type_active` (`tenant_id`,`biz_type`,`del_flag`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息业务类型配置表';


-- forge_admin_new.sys_message_receiver definition

CREATE TABLE `sys_message_receiver` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `message_id` bigint NOT NULL COMMENT '消息ID',
  `user_id` bigint NOT NULL COMMENT '接收人用户ID',
  `org_id` bigint DEFAULT NULL COMMENT '接收人所属组织ID',
  `read_flag` tinyint NOT NULL DEFAULT '0' COMMENT '已读标记：0-未读/1-已读',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `delivery_status` varchar(32) DEFAULT NULL COMMENT '外部渠道投递状态：PENDING/SENT/FAILED/SKIPPED（站内信为NULL）',
  `delivery_attempts` int NOT NULL DEFAULT '0' COMMENT '投递尝试次数',
  `external_id` varchar(200) DEFAULT NULL COMMENT '外部渠道逐人消息ID',
  `last_error_code` varchar(64) DEFAULT NULL COMMENT '最近失败错误码',
  `last_attempt_time` datetime DEFAULT NULL COMMENT '最近投递尝试时间',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次补偿重试时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_user` (`message_id`,`user_id`),
  KEY `idx_user_read` (`user_id`,`read_flag`),
  KEY `idx_tenant_user` (`tenant_id`,`user_id`),
  KEY `idx_receiver_delivery_retry` (`delivery_status`,`next_retry_time`)
) ENGINE=InnoDB AUTO_INCREMENT=104 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息接收人表';


-- forge_admin_new.sys_message_send_record definition

CREATE TABLE `sys_message_send_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `message_id` bigint NOT NULL COMMENT '消息ID',
  `channel` varchar(20) NOT NULL COMMENT '发送渠道：WEB/SMS/EMAIL/PUSH/COLLABORATION',
  `connection_id` bigint DEFAULT NULL COMMENT '企业协同连接ID',
  `idempotency_key` varchar(128) DEFAULT NULL COMMENT '本次渠道投递幂等键',
  `attempt_no` int NOT NULL DEFAULT '1' COMMENT '第几次尝试',
  `provider_request_id` varchar(128) DEFAULT NULL COMMENT '供应商请求ID（用于排障，不含敏感内容）',
  `receiver_count` int DEFAULT '0' COMMENT '接收人数量',
  `success_count` int DEFAULT '0' COMMENT '发送成功数量',
  `fail_count` int DEFAULT '0' COMMENT '发送失败数量',
  `external_id` varchar(100) DEFAULT NULL COMMENT '第三方渠道返回的消息ID',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '发送状态：0-发送中/1-成功/2-失败',
  `error_msg` text COMMENT '错误信息',
  `send_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_message_id` (`message_id`),
  KEY `idx_send_time` (`send_time`),
  KEY `idx_send_record_idempotency` (`tenant_id`,`idempotency_key`)
) ENGINE=InnoDB AUTO_INCREMENT=90 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息发送记录表';


-- forge_admin_new.sys_message_template definition

CREATE TABLE `sys_message_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `template_code` varchar(50) NOT NULL COMMENT '模板编码（唯一）',
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `type` varchar(20) NOT NULL DEFAULT 'SYSTEM' COMMENT '消息类型：SYSTEM/SMS/EMAIL/CUSTOM',
  `title_template` varchar(200) DEFAULT NULL COMMENT '标题模板（支持${变量}占位符）',
  `content_template` text NOT NULL COMMENT '内容模板（支持${变量}占位符）',
  `default_channel` varchar(20) DEFAULT 'WEB' COMMENT '默认发送渠道',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用：0-禁用/1-启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注说明',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_code_active` (`tenant_id`,`template_code`,`del_flag`),
  KEY `idx_type` (`type`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=1910000000000005108 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息模板表';


-- forge_admin_new.sys_notice definition

CREATE TABLE `sys_notice` (
  `notice_id` bigint NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `notice_title` varchar(200) NOT NULL COMMENT '公告标题',
  `notice_content` text NOT NULL COMMENT '公告内容',
  `notice_type` varchar(20) NOT NULL DEFAULT 'NOTICE' COMMENT '公告类型：NOTICE-通知公告/ANNOUNCEMENT-系统公告/NEWS-新闻动态',
  `publish_status` tinyint NOT NULL DEFAULT '0' COMMENT '发布状态：0-草稿/1-已发布/2-已撤回',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `publisher_id` bigint DEFAULT NULL COMMENT '发布人ID',
  `publisher_name` varchar(100) DEFAULT NULL COMMENT '发布人姓名',
  `publish_scope` tinyint NOT NULL DEFAULT '0' COMMENT '发布范围：0-全部组织/1-指定组织',
  `effective_time` datetime DEFAULT NULL COMMENT '生效时间',
  `expiration_time` datetime DEFAULT NULL COMMENT '失效时间',
  `is_top` tinyint NOT NULL DEFAULT '0' COMMENT '是否置顶：0-否/1-是',
  `top_sort` int DEFAULT '0' COMMENT '置顶排序（数字越大越靠前）',
  `attachment_ids` varchar(500) DEFAULT NULL COMMENT '附件ID列表（多个附件ID用逗号分隔，关联sys_file_metadata表）',
  `read_count` int DEFAULT '0' COMMENT '阅读次数',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
  PRIMARY KEY (`notice_id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_publish_status` (`publish_status`),
  KEY `idx_notice_type` (`notice_type`),
  KEY `idx_effective_time` (`effective_time`),
  KEY `idx_expiration_time` (`expiration_time`),
  KEY `idx_is_top_sort` (`is_top`,`top_sort`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知公告表';


-- forge_admin_new.sys_notice_org definition

CREATE TABLE `sys_notice_org` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `notice_id` bigint NOT NULL COMMENT '公告ID',
  `org_id` bigint NOT NULL COMMENT '组织ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_notice_id` (`notice_id`),
  KEY `idx_org_id` (`org_id`),
  KEY `idx_sys_notice_org_tenant` (`tenant_id`,`notice_id`,`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知公告组织关联表';


-- forge_admin_new.sys_notice_read_record definition

CREATE TABLE `sys_notice_read_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `notice_id` bigint NOT NULL COMMENT '公告ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_name` varchar(100) DEFAULT NULL COMMENT '用户姓名',
  `org_id` bigint DEFAULT NULL COMMENT '用户所属组织ID',
  `org_name` varchar(100) DEFAULT NULL COMMENT '用户所属组织名称',
  `read_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_user` (`notice_id`,`user_id`),
  KEY `idx_notice_id` (`notice_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_sys_notice_read_tenant` (`tenant_id`,`notice_id`,`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知公告已读记录表';


-- forge_admin_new.sys_operation_log definition

CREATE TABLE `sys_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `tenant_id` bigint DEFAULT '0' COMMENT '租户编号',
  `user_id` bigint DEFAULT NULL COMMENT '操作用户ID',
  `username` varchar(50) DEFAULT NULL COMMENT '操作用户名',
  `operator_name` varchar(100) DEFAULT NULL COMMENT '操作人姓名',
  `operation_module` varchar(100) DEFAULT NULL COMMENT '操作模块',
  `operation_type` varchar(50) DEFAULT NULL COMMENT '操作类型（ADD/UPDATE/DELETE/QUERY/EXPORT/IMPORT）',
  `operation_desc` varchar(500) DEFAULT NULL COMMENT '操作描述',
  `operation_page` varchar(500) DEFAULT NULL COMMENT '操作页面路径',
  `operation_page_title` varchar(200) DEFAULT NULL COMMENT '操作页面标题',
  `operation_content` varchar(1000) DEFAULT NULL COMMENT '操作内容',
  `request_method` varchar(10) DEFAULT NULL COMMENT '请求方式（GET/POST/PUT/DELETE）',
  `request_url` varchar(500) DEFAULT NULL COMMENT '请求URL',
  `request_params` text COMMENT '请求参数',
  `response_result` text COMMENT '响应结果',
  `before_data` mediumtext COMMENT '操作前数据',
  `after_data` mediumtext COMMENT '操作后数据',
  `diff_data` mediumtext COMMENT '操作数据差异',
  `error_msg` text COMMENT '错误信息',
  `operation_status` tinyint DEFAULT '1' COMMENT '操作状态（0-失败，1-成功）',
  `operation_ip` varchar(50) DEFAULT NULL COMMENT '操作IP',
  `operation_location` varchar(255) DEFAULT NULL COMMENT '操作地点',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理',
  `execute_time` bigint DEFAULT '0' COMMENT '执行时长（毫秒）',
  `operation_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_user` (`tenant_id`,`user_id`),
  KEY `idx_operation_time` (`operation_time`),
  KEY `idx_operation_status` (`operation_status`),
  KEY `idx_request_url` (`request_url`(255)),
  KEY `idx_operation_page_time` (`operation_page`(191),`operation_time`),
  KEY `idx_operator_name` (`operator_name`)
) ENGINE=InnoDB AUTO_INCREMENT=58126 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志表';


-- forge_admin_new.sys_org definition

CREATE TABLE `sys_org` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '组织ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `org_code` varchar(100) DEFAULT NULL COMMENT '组织编码',
  `org_parent_code` varchar(100) DEFAULT NULL COMMENT '父级组织编码',
  `org_name` varchar(100) NOT NULL COMMENT '组织名称',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父级组织ID（0为顶级）',
  `ancestors` varchar(500) DEFAULT NULL COMMENT '祖级编码（逗号分隔，如：1,2,3）',
  `sort` int DEFAULT '0' COMMENT '排序（值越小越靠前）',
  `org_type` varchar(64) DEFAULT '1' COMMENT '组织类型（sys_org_type 字典值）',
  `org_status` tinyint NOT NULL DEFAULT '1' COMMENT '组织状态（0-禁用，1-正常）',
  `leader_id` bigint DEFAULT NULL COMMENT '负责人ID（关联sys_user.id）',
  `leader_name` varchar(50) DEFAULT NULL COMMENT '负责人姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '组织联系电话',
  `address` varchar(255) DEFAULT NULL COMMENT '组织地址',
  `region_code` varchar(30) DEFAULT NULL COMMENT '行政区划编码',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_org_name_active` (`tenant_id`,`org_name`,`del_flag`),
  KEY `idx_tenant_parent` (`tenant_id`,`parent_id`),
  KEY `idx_org_status` (`org_status`),
  KEY `idx_ancestors` (`ancestors`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='组织表';


-- forge_admin_new.sys_outbound_whitelist definition

CREATE TABLE `sys_outbound_whitelist` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `scene` varchar(32) NOT NULL COMMENT '出站场景：JOB_WEBHOOK/JOB_RPC/FLOW_API',
  `protocol` varchar(8) NOT NULL COMMENT '协议：http/https',
  `host` varchar(253) NOT NULL COMMENT '规范化精确主机名或IP',
  `port_start` int NOT NULL COMMENT '允许端口起始值',
  `port_end` int NOT NULL COMMENT '允许端口结束值',
  `allow_private` tinyint NOT NULL DEFAULT '0' COMMENT '是否允许RFC1918或ULA私网：0否 1是',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0停用 1启用',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_outbound_whitelist_active` (`tenant_id`,`scene`,`protocol`,`host`,`port_start`,`port_end`,`del_flag`),
  KEY `idx_outbound_whitelist_match` (`tenant_id`,`scene`,`protocol`,`host`,`status`,`del_flag`),
  CONSTRAINT `chk_outbound_port_range` CHECK (((`port_start` between 1 and 65535) and (`port_end` between `port_start` and 65535))),
  CONSTRAINT `chk_outbound_private_scene` CHECK (((`allow_private` = 0) or (`scene` in (_utf8mb4'FLOW_API',_utf8mb4'JOB_RPC'))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台受控出站白名单';


-- forge_admin_new.sys_post definition

CREATE TABLE `sys_post` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `post_code` varchar(50) NOT NULL COMMENT '岗位编码（租户内唯一）',
  `org_id` bigint NOT NULL COMMENT '所属组织ID（关联sys_org.id）',
  `post_name` varchar(100) NOT NULL COMMENT '岗位名称',
  `post_status` tinyint NOT NULL DEFAULT '1' COMMENT '岗位状态（0-禁用，1-正常）',
  `post_type` tinyint DEFAULT '1' COMMENT '岗位类型（1-管理岗，2-技术岗，3-业务岗，4-其他）',
  `sort` int DEFAULT '0' COMMENT '排序（值越小越靠前）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_org_post_active` (`tenant_id`,`org_id`,`post_name`,`del_flag`),
  UNIQUE KEY `uk_tenant_post_code_active` (`tenant_id`,`post_code`,`del_flag`),
  KEY `idx_tenant_org` (`tenant_id`,`org_id`),
  KEY `idx_post_status` (`post_status`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位表';


-- forge_admin_new.sys_region_code definition

CREATE TABLE `sys_region_code` (
  `code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '行政区划代码',
  `name` varchar(100) NOT NULL COMMENT '行政区划名称',
  `level` tinyint NOT NULL COMMENT '行政级别(1-省,2-市,3-区/县,4-街道)',
  `parent_code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '父级代码',
  `full_name` varchar(200) DEFAULT NULL COMMENT '全名',
  `city_code` varchar(20) DEFAULT NULL COMMENT '地市编码',
  PRIMARY KEY (`code`),
  KEY `idx_parent_code` (`parent_code`),
  KEY `sys_region_code_name_IDX` (`name`) USING BTREE,
  KEY `idx_parent_level` (`parent_code`,`level`),
  KEY `idx_code_level` (`code`,`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- forge_admin_new.sys_resource definition

CREATE TABLE `sys_resource` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '资源ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `resource_name` varchar(100) NOT NULL COMMENT '资源名称',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父级资源ID（0为顶级）',
  `resource_type` tinyint NOT NULL COMMENT '资源类型（1-目录，2-菜单，3-按钮，4-API接口）',
  `sort` int DEFAULT '0' COMMENT '排序（值越小越靠前）',
  `path` varchar(255) DEFAULT NULL COMMENT '资源路由（菜单/目录用）',
  `component` varchar(255) DEFAULT NULL COMMENT '前端组件路径（菜单用）',
  `is_external` tinyint DEFAULT '0' COMMENT '是否外链（0-否，1-是）',
  `sso_enabled` tinyint DEFAULT '0' COMMENT '是否启用SSO（0-否，1-是）',
  `sso_target_client` varchar(64) DEFAULT NULL COMMENT 'SSO目标客户端编码',
  `open_target` varchar(20) DEFAULT '_self' COMMENT '打开方式：_self-当前页/_blank-新窗口',
  `is_public` tinyint DEFAULT '0' COMMENT '是否公开资源（0-否，1-是，公开资源无需权限验证）',
  `menu_status` tinyint DEFAULT '1' COMMENT '菜单状态（0-隐藏，1-显示，仅菜单/目录用）',
  `visible` tinyint DEFAULT '1' COMMENT '显示状态（0-隐藏，1-显示，所有资源通用）',
  `perms` varchar(100) DEFAULT NULL COMMENT '权限标识（如：sys:user:list，按钮/API用）',
  `icon` varchar(100) DEFAULT NULL COMMENT '图标（菜单/目录用）',
  `api_method` varchar(10) DEFAULT NULL COMMENT 'API请求方法（GET/POST/PUT/DELETE，仅API用）',
  `api_url` varchar(255) DEFAULT NULL COMMENT 'API接口地址（仅API用）',
  `keep_alive` tinyint DEFAULT '0' COMMENT '是否缓存（0-否，1-是，菜单用）',
  `always_show` tinyint DEFAULT '0' COMMENT '是否总是显示（0-否，1-是，菜单用）',
  `redirect` varchar(255) DEFAULT NULL COMMENT '重定向地址（菜单用）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `client_code` varchar(50) DEFAULT 'pc' COMMENT '客户端代码：pc/app',
  `min_user_type` int NOT NULL DEFAULT '2' COMMENT '最低可访问用户类型（0-系统管理员，1-租户管理员，2-普通用户）',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_resource_active` (`tenant_id`,`resource_type`,`perms`,`del_flag`),
  KEY `idx_tenant_parent` (`tenant_id`,`parent_id`),
  KEY `idx_resource_type` (`resource_type`),
  KEY `idx_api_url_method` (`api_url`,`api_method`) COMMENT 'API查询优化',
  KEY `idx_resource_menu_tree` (`client_code`,`del_flag`,`sort`,`create_time` DESC)
) ENGINE=InnoDB AUTO_INCREMENT=9617 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统资源表（菜单/按钮/API）';


-- forge_admin_new.sys_resource_0607 definition

CREATE TABLE `sys_resource_0607` (
  `id` bigint NOT NULL DEFAULT '0' COMMENT '资源ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `resource_name` varchar(100) NOT NULL COMMENT '资源名称',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父级资源ID（0为顶级）',
  `resource_type` tinyint NOT NULL COMMENT '资源类型（1-目录，2-菜单，3-按钮，4-API接口）',
  `sort` int DEFAULT '0' COMMENT '排序（值越小越靠前）',
  `path` varchar(255) DEFAULT NULL COMMENT '资源路由（菜单/目录用）',
  `component` varchar(255) DEFAULT NULL COMMENT '前端组件路径（菜单用）',
  `is_external` tinyint DEFAULT '0' COMMENT '是否外链（0-否，1-是）',
  `sso_enabled` tinyint DEFAULT '0' COMMENT '是否启用SSO（0-否，1-是）',
  `sso_target_client` varchar(64) DEFAULT NULL COMMENT 'SSO目标客户端编码',
  `open_target` varchar(20) DEFAULT '_self' COMMENT '打开方式：_self-当前页/_blank-新窗口',
  `is_public` tinyint DEFAULT '0' COMMENT '是否公开资源（0-否，1-是，公开资源无需权限验证）',
  `menu_status` tinyint DEFAULT '1' COMMENT '菜单状态（0-隐藏，1-显示，仅菜单/目录用）',
  `visible` tinyint DEFAULT '1' COMMENT '显示状态（0-隐藏，1-显示，所有资源通用）',
  `perms` varchar(100) DEFAULT NULL COMMENT '权限标识（如：sys:user:list，按钮/API用）',
  `icon` varchar(100) DEFAULT NULL COMMENT '图标（菜单/目录用）',
  `api_method` varchar(10) DEFAULT NULL COMMENT 'API请求方法（GET/POST/PUT/DELETE，仅API用）',
  `api_url` varchar(255) DEFAULT NULL COMMENT 'API接口地址（仅API用）',
  `keep_alive` tinyint DEFAULT '0' COMMENT '是否缓存（0-否，1-是，菜单用）',
  `always_show` tinyint DEFAULT '0' COMMENT '是否总是显示（0-否，1-是，菜单用）',
  `redirect` varchar(255) DEFAULT NULL COMMENT '重定向地址（菜单用）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `client_code` varchar(50) DEFAULT 'pc' COMMENT '客户端代码：pc/app'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- forge_admin_new.sys_role definition

CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称（租户内唯一）',
  `role_key` varchar(100) NOT NULL COMMENT '角色权限字符串（如：admin,user:view）',
  `role_type` int DEFAULT '1' COMMENT '角色类型（1-管理角色，2-业务角色，3-审批角色，4-数据角色）',
  `org_scope_type` tinyint NOT NULL DEFAULT '1' COMMENT '组织适用范围类型（1租户全局 2指定组织）',
  `data_scope` tinyint DEFAULT '1' COMMENT '权限范围（1-全部数据，2-本租户数据，3-本组织数据，4-本组织及子组织，5-个人数据）',
  `sort` int DEFAULT '0' COMMENT '排序（值越小越靠前）',
  `role_status` tinyint NOT NULL DEFAULT '1' COMMENT '角色状态（0-禁用，1-正常）',
  `is_system` tinyint DEFAULT '0' COMMENT '是否系统内置角色（0-否，1-是）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_role_key_active` (`tenant_id`,`role_key`,`del_flag`),
  UNIQUE KEY `uk_tenant_role_name_active` (`tenant_id`,`role_name`,`del_flag`),
  KEY `idx_tenant_status` (`tenant_id`,`role_status`),
  KEY `idx_data_scope` (`data_scope`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';


-- forge_admin_new.sys_role_data_scope definition

CREATE TABLE `sys_role_data_scope` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `org_id` bigint NOT NULL COMMENT '组织ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_org` (`tenant_id`,`role_id`,`org_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色-自定义数据权限关联表';


-- forge_admin_new.sys_role_org definition

CREATE TABLE `sys_role_org` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `org_id` bigint NOT NULL COMMENT '组织ID',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_org` (`tenant_id`,`role_id`,`org_id`),
  KEY `idx_org_role` (`tenant_id`,`org_id`,`role_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_org_id` (`org_id`)
) ENGINE=InnoDB AUTO_INCREMENT=126 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色适用组织范围表';


-- forge_admin_new.sys_role_resource definition

CREATE TABLE `sys_role_resource` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `resource_id` bigint NOT NULL COMMENT '资源ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_resource` (`tenant_id`,`role_id`,`resource_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_resource_id` (`resource_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3039 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色-资源关联表';


-- forge_admin_new.sys_sms_config definition

CREATE TABLE `sys_sms_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_id` varchar(64) DEFAULT NULL COMMENT '配置标识',
  `supplier` varchar(32) NOT NULL COMMENT '厂商标识(alibaba/tencent/huawei等)',
  `access_key_id` varchar(128) DEFAULT NULL COMMENT '访问密钥ID',
  `access_key_secret` varchar(256) DEFAULT NULL COMMENT '访问密钥Secret',
  `signature` varchar(64) DEFAULT NULL COMMENT '短信签名',
  `template_id` varchar(64) DEFAULT NULL COMMENT '默认模板ID',
  `weight` int DEFAULT '1' COMMENT '权重(负载均衡)',
  `retry_interval` int DEFAULT '5' COMMENT '重试间隔(秒)',
  `max_retries` int DEFAULT '0' COMMENT '最大重试次数',
  `maximum` int DEFAULT NULL COMMENT '发送上限',
  `extra_config` text COMMENT '额外配置(JSON格式)',
  `daily_limit` int DEFAULT NULL COMMENT '每日发送上限',
  `minute_limit` int DEFAULT NULL COMMENT '每分钟发送上限',
  `status` tinyint DEFAULT '0' COMMENT '状态(0禁用 1启用)',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_status` (`tenant_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='短信配置表';


-- forge_admin_new.sys_social_app_config definition

CREATE TABLE `sys_social_app_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `connection_id` bigint NOT NULL COMMENT '企业协同连接ID',
  `app_code` varchar(64) NOT NULL COMMENT '应用编码（连接内唯一）',
  `app_name` varchar(100) DEFAULT NULL COMMENT '应用名称',
  `client_id` varchar(255) DEFAULT NULL COMMENT '应用ID/Key（企业微信为AgentId所属应用的CorpId沿用连接）',
  `agent_id` varchar(100) DEFAULT NULL COMMENT '企业微信AgentId',
  `secret_mode` varchar(32) NOT NULL DEFAULT 'CIPHER' COMMENT 'Secret存储模式：CIPHER密文/EXTERNAL_REF外部引用',
  `secret_cipher` varchar(1000) DEFAULT NULL COMMENT '应用Secret密文（FPC1版本化密文，禁止明文）',
  `secret_ref` varchar(500) DEFAULT NULL COMMENT '外部Secret引用（extref:前缀）',
  `secret_update_time` datetime DEFAULT NULL COMMENT 'Secret最近轮换时间',
  `callback_token_cipher` varchar(1000) DEFAULT NULL COMMENT '回调Token密文',
  `encoding_aes_key_cipher` varchar(1000) DEFAULT NULL COMMENT '回调EncodingAESKey密文',
  `redirect_uri` varchar(500) DEFAULT NULL COMMENT 'OAuth回调地址',
  `scope` varchar(500) DEFAULT NULL COMMENT '授权范围',
  `config_json` json DEFAULT NULL COMMENT '应用级扩展配置JSON',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0停用 1启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_social_app_active` (`tenant_id`,`connection_id`,`app_code`,`del_flag`),
  KEY `idx_social_app_connection` (`connection_id`,`status`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业协同物理应用配置表';


-- forge_admin_new.sys_social_callback_event definition

CREATE TABLE `sys_social_callback_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `connection_id` bigint NOT NULL COMMENT '企业协同连接ID',
  `app_config_id` bigint DEFAULT NULL COMMENT '定位回调凭据的物理应用ID',
  `event_id` varchar(128) DEFAULT NULL COMMENT '外部事件ID',
  `dedup_hash` varchar(64) NOT NULL COMMENT '去重哈希（签名+时间戳+nonce+正文摘要）',
  `event_type` varchar(64) DEFAULT NULL COMMENT '事件类型',
  `event_time` datetime DEFAULT NULL COMMENT '外部事件时间',
  `signature_status` varchar(32) NOT NULL DEFAULT 'VERIFIED' COMMENT '验签状态：VERIFIED/REJECTED',
  `payload_cipher` mediumtext COMMENT '解密后事件正文密文（FPC1版本化密文，按留存期清理）',
  `process_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态：PENDING/PROCESSING/PROCESSED/FAILED/DISCARDED',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '重试次数',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次重试时间',
  `error_code` varchar(64) DEFAULT NULL COMMENT '错误码',
  `error_summary` varchar(500) DEFAULT NULL COMMENT '脱敏错误摘要',
  `claimed_by` varchar(64) DEFAULT NULL COMMENT '当前处理Worker标识',
  `claim_time` datetime DEFAULT NULL COMMENT '领取时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_social_callback_dedup` (`tenant_id`,`connection_id`,`dedup_hash`),
  KEY `idx_social_callback_process` (`process_status`,`next_retry_time`),
  KEY `idx_social_callback_conn` (`tenant_id`,`connection_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业协同回调事件收件箱';


-- forge_admin_new.sys_social_capability_binding definition

CREATE TABLE `sys_social_capability_binding` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `connection_id` bigint NOT NULL COMMENT '企业协同连接ID',
  `capability` varchar(32) NOT NULL COMMENT '业务能力：LOGIN/DIRECTORY/MESSAGE/TODO',
  `app_config_id` bigint NOT NULL COMMENT '绑定的物理应用ID',
  `config_json` json DEFAULT NULL COMMENT '能力级扩展配置JSON',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0停用 1启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_social_capability_active` (`tenant_id`,`connection_id`,`capability`,`del_flag`),
  KEY `idx_social_capability_app` (`app_config_id`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业协同能力绑定表';


-- forge_admin_new.sys_social_config definition

CREATE TABLE `sys_social_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `platform` varchar(50) NOT NULL COMMENT '平台类型（WECHAT、WECOM、DINGTALK、GITHUB、GITEE等）',
  `platform_name` varchar(100) DEFAULT NULL COMMENT '平台名称',
  `platform_logo` varchar(500) DEFAULT NULL COMMENT '平台Logo',
  `connection_code` varchar(64) DEFAULT NULL COMMENT '连接编码（租户内唯一）',
  `connection_name` varchar(100) DEFAULT NULL COMMENT '连接名称',
  `enterprise_id` varchar(100) DEFAULT NULL COMMENT '外部企业ID（企业微信CorpId等）',
  `connection_type` varchar(32) DEFAULT NULL COMMENT '连接类型：CORP_INTERNAL自建应用/THIRD_PARTY第三方/OAUTH_ONLY仅登录',
  `identity_policy` varchar(32) DEFAULT NULL COMMENT '身份匹配策略：BIND_ONLY仅绑定已有/AUTO_CREATE自动创建/MANUAL人工处理',
  `directory_authority` varchar(32) DEFAULT NULL COMMENT '目录权威来源：EXTERNAL外部权威/LOCAL本地权威/NONE不同步',
  `default_org_id` bigint DEFAULT NULL COMMENT '目录同步默认挂载的根组织ID',
  `client_id` varchar(255) NOT NULL COMMENT '应用ID/Key（兼容旧登录配置，迁移后由应用配置承载）',
  `client_secret` varchar(255) NOT NULL COMMENT '应用Secret（兼容旧登录配置，迁移完成后必须清空）',
  `redirect_uri` varchar(500) DEFAULT NULL COMMENT '回调地址（兼容旧登录配置）',
  `agent_id` varchar(100) DEFAULT NULL COMMENT '企业微信AgentId（兼容旧登录配置）',
  `scope` varchar(500) DEFAULT NULL COMMENT '授权范围（兼容旧登录配置）',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态（1-启用，0-停用）',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注说明',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_social_conn_code_active` (`tenant_id`,`connection_code`,`del_flag`),
  UNIQUE KEY `uk_social_conn_enterprise_active` (`tenant_id`,`platform`,`enterprise_id`,`del_flag`),
  KEY `idx_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_social_platform` (`platform`,`tenant_id`,`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业协同连接表（原三方登录配置表）';


-- forge_admin_new.sys_social_org_mapping definition

CREATE TABLE `sys_social_org_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `connection_id` bigint NOT NULL COMMENT '企业协同连接ID',
  `external_dept_id` varchar(100) NOT NULL COMMENT '外部部门ID',
  `external_parent_id` varchar(100) DEFAULT NULL COMMENT '外部父部门ID',
  `external_dept_name` varchar(200) DEFAULT NULL COMMENT '外部部门名称',
  `org_id` bigint DEFAULT NULL COMMENT 'Forge组织ID',
  `source_hash` varchar(64) DEFAULT NULL COMMENT '外部快照哈希（用于变更检测）',
  `last_seen_run_id` bigint DEFAULT NULL COMMENT '最近一次出现的同步批次ID',
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '映射状态：ACTIVE/INACTIVE/ISSUE',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_social_org_mapping_active` (`tenant_id`,`connection_id`,`external_dept_id`,`del_flag`),
  KEY `idx_social_org_mapping_org` (`tenant_id`,`connection_id`,`org_id`),
  KEY `idx_social_org_mapping_seen` (`tenant_id`,`connection_id`,`last_seen_run_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业协同外部部门映射表';


-- forge_admin_new.sys_social_post_mapping definition

CREATE TABLE `sys_social_post_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `connection_id` bigint NOT NULL COMMENT '企业协同连接ID',
  `external_post_code` varchar(100) NOT NULL COMMENT '外部岗位编码/文本',
  `external_post_name` varchar(200) DEFAULT NULL COMMENT '外部岗位名称',
  `post_id` bigint DEFAULT NULL COMMENT 'Forge岗位ID（企微首期默认不自动创建）',
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '映射状态：ACTIVE/INACTIVE/ISSUE',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_social_post_mapping_active` (`tenant_id`,`connection_id`,`external_post_code`,`del_flag`),
  KEY `idx_social_post_mapping_post` (`tenant_id`,`connection_id`,`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业协同外部岗位映射表';


-- forge_admin_new.sys_social_sync_issue definition

CREATE TABLE `sys_social_sync_issue` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `connection_id` bigint NOT NULL COMMENT '企业协同连接ID',
  `sync_log_id` bigint DEFAULT NULL COMMENT '产生问题的同步批次ID',
  `object_type` varchar(32) NOT NULL COMMENT '对象类型：DEPT/USER/POST/TAG',
  `external_id` varchar(100) DEFAULT NULL COMMENT '外部对象ID',
  `issue_code` varchar(64) NOT NULL COMMENT '问题码',
  `issue_summary` varchar(500) DEFAULT NULL COMMENT '脱敏问题摘要（禁止明文手机号/邮箱/姓名）',
  `process_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态：PENDING/RESOLVED/IGNORED',
  `process_action` varchar(32) DEFAULT NULL COMMENT '处理动作：BIND/IGNORE/RETRY',
  `process_by` bigint DEFAULT NULL COMMENT '处理人ID',
  `process_time` datetime DEFAULT NULL COMMENT '处理时间',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '重试次数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  KEY `idx_social_sync_issue_status` (`tenant_id`,`connection_id`,`process_status`,`del_flag`),
  KEY `idx_social_sync_issue_log` (`sync_log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业协同同步问题单表';


-- forge_admin_new.sys_social_sync_log definition

CREATE TABLE `sys_social_sync_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '批次ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `connection_id` bigint NOT NULL COMMENT '企业协同连接ID',
  `sync_type` varchar(32) NOT NULL COMMENT '同步类型：FULL/DEPT/USER/TAG/INCREMENT',
  `trigger_source` varchar(32) NOT NULL DEFAULT 'MANUAL' COMMENT '触发来源：MANUAL/JOB/CALLBACK',
  `stage` varchar(32) DEFAULT NULL COMMENT '当前阶段：FETCH/VALIDATE/PLAN/APPLY/FINALIZE',
  `status` varchar(32) NOT NULL DEFAULT 'RUNNING' COMMENT '批次状态：RUNNING/SUCCESS/PARTIAL/FAILED',
  `dept_count` int NOT NULL DEFAULT '0' COMMENT '拉取部门数',
  `user_count` int NOT NULL DEFAULT '0' COMMENT '拉取成员数',
  `tag_count` int NOT NULL DEFAULT '0' COMMENT '拉取标签数',
  `created_count` int NOT NULL DEFAULT '0' COMMENT '创建对象数',
  `updated_count` int NOT NULL DEFAULT '0' COMMENT '更新对象数',
  `inactivated_count` int NOT NULL DEFAULT '0' COMMENT '停用对象数',
  `issue_count` int NOT NULL DEFAULT '0' COMMENT '问题单数',
  `cursor_info` varchar(500) DEFAULT NULL COMMENT '断点游标信息',
  `error_code` varchar(64) DEFAULT NULL COMMENT '错误码',
  `error_summary` varchar(1000) DEFAULT NULL COMMENT '脱敏错误摘要',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `create_by` bigint DEFAULT NULL COMMENT '触发人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_social_sync_log_conn` (`tenant_id`,`connection_id`,`create_time`),
  KEY `idx_social_sync_log_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业协同同步批次日志表';


-- forge_admin_new.sys_social_tag definition

CREATE TABLE `sys_social_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `connection_id` bigint NOT NULL COMMENT '企业协同连接ID',
  `external_tag_id` varchar(100) NOT NULL COMMENT '外部标签ID',
  `tag_name` varchar(200) DEFAULT NULL COMMENT '标签名称',
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '标签状态：ACTIVE/INACTIVE',
  `last_seen_run_id` bigint DEFAULT NULL COMMENT '最近一次出现的同步批次ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_social_tag_active` (`tenant_id`,`connection_id`,`external_tag_id`,`del_flag`),
  KEY `idx_social_tag_seen` (`tenant_id`,`connection_id`,`last_seen_run_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业协同外部标签表';


-- forge_admin_new.sys_social_tag_member definition

CREATE TABLE `sys_social_tag_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `connection_id` bigint NOT NULL COMMENT '企业协同连接ID',
  `tag_id` bigint NOT NULL COMMENT '本地标签ID（sys_social_tag.id）',
  `member_type` varchar(16) NOT NULL COMMENT '成员类型：USER/DEPT',
  `external_member_id` varchar(100) NOT NULL COMMENT '外部成员ID（userid或部门ID）',
  `local_target_id` bigint DEFAULT NULL COMMENT '映射到的Forge用户/组织ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_social_tag_member` (`tenant_id`,`tag_id`,`member_type`,`external_member_id`),
  KEY `idx_social_tag_member_conn` (`tenant_id`,`connection_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业协同标签成员关系表（可重建）';


-- forge_admin_new.sys_social_todo_link definition

CREATE TABLE `sys_social_todo_link` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `connection_id` bigint NOT NULL COMMENT '企业协同连接ID',
  `task_id` varchar(64) NOT NULL COMMENT 'Flowable任务ID',
  `user_id` bigint NOT NULL COMMENT 'Forge用户ID',
  `external_user_id` varchar(100) DEFAULT NULL COMMENT '外部用户ID',
  `desired_state` varchar(32) NOT NULL COMMENT '期望状态：ACTIVE/CLOSED',
  `delivery_state` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '投递状态：PENDING/SENT/FAILED/CLOSED',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '状态版本号（CAS）',
  `external_id` varchar(200) DEFAULT NULL COMMENT '外部待办/卡片ID',
  `idempotency_key` varchar(128) DEFAULT NULL COMMENT '幂等键',
  `retry_at` datetime DEFAULT NULL COMMENT '下次重试时间',
  `last_error_code` varchar(64) DEFAULT NULL COMMENT '最近错误码',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_social_todo_link_active` (`tenant_id`,`connection_id`,`task_id`,`user_id`,`del_flag`),
  KEY `idx_social_todo_link_retry` (`delivery_state`,`retry_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业协同待办投影表';


-- forge_admin_new.sys_tenant definition

CREATE TABLE `sys_tenant` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '租户ID',
  `tenant_name` varchar(100) NOT NULL COMMENT '租户名称',
  `contact_person` varchar(50) DEFAULT NULL COMMENT '负责人',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `user_limit` int DEFAULT '0' COMMENT '租户人员数量上限（0表示无限制）',
  `tenant_status` tinyint NOT NULL DEFAULT '1' COMMENT '租户状态（0-禁用，1-正常）',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `tenant_desc` varchar(500) DEFAULT NULL COMMENT '租户描述',
  `create_by` bigint DEFAULT NULL COMMENT '创建者（系统租户ID）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `browser_icon` varchar(255) DEFAULT NULL COMMENT '浏览器icon（存储图标URL/Base64）',
  `browser_title` varchar(100) DEFAULT NULL COMMENT '浏览器标签名称',
  `system_name` varchar(100) DEFAULT NULL COMMENT '系统名称',
  `system_logo` varchar(255) DEFAULT NULL COMMENT '系统logo（存储logo URL/Base64）',
  `system_intro` varchar(500) DEFAULT NULL COMMENT '系统介绍',
  `copyright_info` varchar(200) DEFAULT NULL COMMENT '版权显示文本',
  `system_layout` varchar(50) DEFAULT 'default' COMMENT '系统布局（default-默认，classic-经典，modern-现代等）',
  `system_theme` varchar(50) DEFAULT 'light' COMMENT '系统主题（light-亮色，dark-暗色，auto-跟随系统等）',
  `theme_config` varchar(1000) DEFAULT NULL COMMENT '主题配置',
  `create_dept` bigint unsigned DEFAULT NULL,
  `default_business_datasource_id` bigint DEFAULT NULL COMMENT '默认业务数据源ID',
  `default_business_datasource_code` varchar(64) DEFAULT NULL COMMENT '默认业务数据源编码',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_name_active` (`tenant_name`,`del_flag`),
  KEY `idx_tenant_status` (`tenant_status`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户表';


-- forge_admin_new.sys_user definition

CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `username` varchar(50) NOT NULL COMMENT '用户名（租户内唯一）',
  `real_name` varchar(50) DEFAULT NULL COMMENT '用户真实姓名',
  `user_type` tinyint DEFAULT '1' COMMENT '用户类型（0-系统管理员，1-租户管理员，2-普通用户）',
  `user_client` varchar(20) DEFAULT NULL COMMENT '用户触点（app/pc/h5/wechat）',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `id_card` varchar(18) DEFAULT NULL COMMENT '身份证号',
  `gender` tinyint DEFAULT '0' COMMENT '性别（0-未知，1-男，2-女）',
  `password` varchar(100) NOT NULL COMMENT '密码（加密存储）',
  `salt` varchar(50) DEFAULT NULL COMMENT '密码盐值',
  `user_status` tinyint NOT NULL DEFAULT '1' COMMENT '用户状态（0-禁用，1-正常，2-锁定）',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `region_code` varchar(10) DEFAULT NULL COMMENT '行政区划编码',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(50) DEFAULT NULL COMMENT '最后登录IP',
  `login_count` int DEFAULT '0' COMMENT '登录次数',
  `force_password_change` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否必须修改密码',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `sys_user_unique_active` (`tenant_id`,`username`,`del_flag`),
  KEY `idx_tenant_status` (`tenant_id`,`user_status`),
  KEY `idx_user_type` (`user_type`),
  KEY `idx_region_code` (`region_code`)
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';


-- forge_admin_new.sys_user_org definition

CREATE TABLE `sys_user_org` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `org_id` bigint NOT NULL COMMENT '组织ID',
  `is_main` tinyint DEFAULT '0' COMMENT '是否主组织（0-否，1-是）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_org` (`tenant_id`,`user_id`,`org_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_org_id` (`org_id`),
  KEY `idx_user_main_org` (`user_id`,`is_main`) COMMENT '查询用户主组织优化'
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户-组织关联表';


-- forge_admin_new.sys_user_org_role definition

CREATE TABLE `sys_user_org_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `org_id` bigint NOT NULL COMMENT '组织ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_org_role` (`tenant_id`,`user_id`,`org_id`,`role_id`),
  KEY `idx_user_org` (`tenant_id`,`user_id`,`org_id`),
  KEY `idx_org_role` (`tenant_id`,`org_id`,`role_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_org_id` (`org_id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户组织内角色授权表';


-- forge_admin_new.sys_user_post definition

CREATE TABLE `sys_user_post` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `post_id` bigint NOT NULL COMMENT '岗位ID',
  `is_main` tinyint DEFAULT '0' COMMENT '是否主岗（0-否，1-是）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_post` (`tenant_id`,`user_id`,`post_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_user_main_post` (`user_id`,`is_main`) COMMENT '查询用户主岗优化'
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户-岗位关联表';


-- forge_admin_new.sys_user_role definition

CREATE TABLE `sys_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`tenant_id`,`user_id`,`role_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户-角色关联表';


-- forge_admin_new.sys_user_social definition

CREATE TABLE `sys_user_social` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `platform` varchar(50) NOT NULL COMMENT '平台类型',
  `connection_id` bigint DEFAULT NULL COMMENT '企业协同连接ID（应用层迁移回填，歧义进入阻塞清单）',
  `external_enterprise_id` varchar(100) DEFAULT NULL COMMENT '外部企业ID（企业微信CorpId等）',
  `managed_by_sync` tinyint NOT NULL DEFAULT '0' COMMENT '是否由目录同步管理：0否 1是',
  `external_status` varchar(32) DEFAULT NULL COMMENT '外部账号状态：ACTIVE/DISABLED/DELETED',
  `source_hash` varchar(64) DEFAULT NULL COMMENT '外部资料快照哈希（用于变更检测）',
  `last_sync_time` datetime DEFAULT NULL COMMENT '最近同步时间',
  `uuid` varchar(255) NOT NULL COMMENT '第三方用户唯一标识',
  `username` varchar(100) DEFAULT NULL COMMENT '第三方用户名',
  `nickname` varchar(100) DEFAULT NULL COMMENT '第三方昵称',
  `avatar` varchar(500) DEFAULT NULL COMMENT '头像',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `access_token` varchar(500) DEFAULT NULL COMMENT '访问令牌（已弃用，新绑定不落库）',
  `refresh_token` varchar(500) DEFAULT NULL COMMENT '刷新令牌（已弃用，新绑定不落库）',
  `expire_time` datetime DEFAULT NULL COMMENT '令牌过期时间',
  `bind_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_social_conn_uuid_active` (`tenant_id`,`connection_id`,`uuid`,`del_flag`),
  UNIQUE KEY `uk_user_social_conn_user_active` (`tenant_id`,`connection_id`,`user_id`,`del_flag`),
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_user_social_platform_uuid` (`platform`,`uuid`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户三方账号绑定表';


-- forge_admin_new.sys_user_tenant definition

CREATE TABLE `sys_user_tenant` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `member_type` tinyint NOT NULL DEFAULT '2' COMMENT '成员类型（1-租户管理员，2-普通成员）',
  `is_default` tinyint NOT NULL DEFAULT '0' COMMENT '是否默认租户（0-否，1-是）',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态（0-禁用，1-正常）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_tenant` (`tenant_id`,`user_id`),
  KEY `idx_user_tenant_user` (`user_id`,`status`),
  KEY `idx_user_tenant_default` (`user_id`,`is_default`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户-租户成员关系表';


-- forge_admin_new.tf_f_order definition

CREATE TABLE `tf_f_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `order_code` varchar(128) DEFAULT NULL COMMENT '订单编码',
  `order_name` varchar(128) DEFAULT NULL COMMENT '订单名称',
  `order_time` datetime DEFAULT NULL COMMENT '下单时间',
  `order_status` varchar(128) DEFAULT NULL COMMENT '订单状态',
  `cust_phone` varchar(20) DEFAULT NULL COMMENT '客户手机号',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `order_class` int DEFAULT NULL COMMENT '订单分类',
  `file_id` varchar(50) DEFAULT NULL COMMENT '文件',
  `org_id` varchar(128) DEFAULT NULL COMMENT '归属组织',
  PRIMARY KEY (`id`),
  KEY `idx_tf_f_order_tenant` (`tenant_id`),
  KEY `idx_tf_f_order_create_time` (`create_time`),
  KEY `idx_order_code` (`order_code`),
  KEY `idx_rel_orderclass` (`order_class`),
  KEY `idx_order_name` (`order_name`),
  KEY `idx_order_status` (`order_status`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单基本信息';


-- forge_admin_new.tf_f_order_class definition

CREATE TABLE `tf_f_order_class` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `name` varchar(128) NOT NULL COMMENT '分类名称',
  `parent_id` bigint NOT NULL COMMENT '父级分类ID',
  `class_sort` int NOT NULL COMMENT '排序',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tf_f_order_class_tenant` (`tenant_id`),
  KEY `idx_tf_f_order_class_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单分类';


-- forge_admin_new.tf_f_order_purchase_order definition

CREATE TABLE `tf_f_order_purchase_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `order_no` varchar(64) NOT NULL COMMENT '订单编号',
  `partner_name` varchar(128) DEFAULT NULL COMMENT '往来单位',
  `total_amount` decimal(18,2) DEFAULT NULL COMMENT '订单金额',
  `order_date` date DEFAULT NULL COMMENT '订单日期',
  `status` varchar(32) DEFAULT NULL COMMENT '状态',
  `remark` text COMMENT '备注',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tf_f_order_purchase_order_tenant` (`tenant_id`),
  KEY `idx_tf_f_order_purchase_order_create_time` (`create_time`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_partner_name` (`partner_name`),
  KEY `idx_order_date` (`order_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采购订单';


-- forge_admin_new.tf_f_order_sales_order definition

CREATE TABLE `tf_f_order_sales_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `order_no` varchar(64) NOT NULL COMMENT '订单编号',
  `partner_name` varchar(128) DEFAULT NULL COMMENT '往来单位',
  `total_amount` decimal(18,2) DEFAULT NULL COMMENT '订单金额',
  `order_date` date DEFAULT NULL COMMENT '订单日期',
  `status` varchar(32) DEFAULT NULL COMMENT '状态',
  `remark` text COMMENT '备注',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tf_f_order_sales_order_tenant` (`tenant_id`),
  KEY `idx_tf_f_order_sales_order_create_time` (`create_time`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_partner_name` (`partner_name`),
  KEY `idx_order_date` (`order_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='销售订单';


-- forge_admin_new.worker_node definition

CREATE TABLE `worker_node` (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT 'auto increment id',
  `HOST_NAME` varchar(64) NOT NULL COMMENT 'host name',
  `PORT` varchar(64) NOT NULL COMMENT 'port',
  `TYPE` int NOT NULL COMMENT 'node type: ACTUAL or CONTAINER',
  `LAUNCH_DATE` date NOT NULL COMMENT 'launch date',
  `MODIFIED` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'modified time',
  `CREATED` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=2273 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='DB WorkerID Assigner for UID Generator';


-- forge_admin_new.QRTZ_TRIGGERS definition

DROP TABLE IF EXISTS `QRTZ_TRIGGERS`;

CREATE TABLE `QRTZ_TRIGGERS` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) NOT NULL COMMENT '触发器的名字',
  `trigger_group` varchar(200) NOT NULL COMMENT '触发器所属组的名字',
  `job_name` varchar(200) NOT NULL COMMENT 'QRTZ_JOB_DETAILS表job_name的外键',
  `job_group` varchar(200) NOT NULL COMMENT 'QRTZ_JOB_DETAILS表job_group的外键',
  `description` varchar(250) DEFAULT NULL COMMENT '相关介绍',
  `next_fire_time` bigint DEFAULT NULL COMMENT '上一次触发时间（毫秒）',
  `prev_fire_time` bigint DEFAULT NULL COMMENT '下一次触发时间（默认为-1表示不触发）',
  `priority` int DEFAULT NULL COMMENT '优先级',
  `trigger_state` varchar(16) NOT NULL COMMENT '触发器状态',
  `trigger_type` varchar(8) NOT NULL COMMENT '触发器的类型',
  `start_time` bigint NOT NULL COMMENT '开始时间',
  `end_time` bigint DEFAULT NULL COMMENT '结束时间',
  `calendar_name` varchar(200) DEFAULT NULL COMMENT '日程表名称',
  `misfire_instr` smallint DEFAULT NULL COMMENT '补偿执行的策略',
  `job_data` blob COMMENT '存放持久化job对象',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  KEY `sched_name` (`sched_name`,`job_name`,`job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='触发器详细信息表';


-- forge_admin_new.QRTZ_BLOB_TRIGGERS definition

DROP TABLE IF EXISTS `QRTZ_BLOB_TRIGGERS`;

CREATE TABLE `QRTZ_BLOB_TRIGGERS` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) NOT NULL COMMENT 'QRTZ_TRIGGERS表trigger_name的外键',
  `trigger_group` varchar(200) NOT NULL COMMENT 'QRTZ_TRIGGERS表trigger_group的外键',
  `blob_data` blob COMMENT '存放持久化Trigger对象',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='Blob类型的触发器表';


-- forge_admin_new.QRTZ_CRON_TRIGGERS definition

DROP TABLE IF EXISTS `QRTZ_CRON_TRIGGERS`;

CREATE TABLE `QRTZ_CRON_TRIGGERS` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) NOT NULL COMMENT 'QRTZ_TRIGGERS表trigger_name的外键',
  `trigger_group` varchar(200) NOT NULL COMMENT 'QRTZ_TRIGGERS表trigger_group的外键',
  `cron_expression` varchar(200) NOT NULL COMMENT 'cron表达式',
  `time_zone_id` varchar(80) DEFAULT NULL COMMENT '时区',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='Cron类型的触发器表';


-- forge_admin_new.QRTZ_SIMPLE_TRIGGERS definition

DROP TABLE IF EXISTS `QRTZ_SIMPLE_TRIGGERS`;

CREATE TABLE `QRTZ_SIMPLE_TRIGGERS` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) NOT NULL COMMENT 'QRTZ_TRIGGERS表trigger_name的外键',
  `trigger_group` varchar(200) NOT NULL COMMENT 'QRTZ_TRIGGERS表trigger_group的外键',
  `repeat_count` bigint NOT NULL COMMENT '重复的次数统计',
  `repeat_interval` bigint NOT NULL COMMENT '重复的间隔时间',
  `times_triggered` bigint NOT NULL COMMENT '已经触发的次数',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='简单触发器的信息表';


-- forge_admin_new.QRTZ_SIMPROP_TRIGGERS definition

DROP TABLE IF EXISTS `QRTZ_SIMPROP_TRIGGERS`;

CREATE TABLE `QRTZ_SIMPROP_TRIGGERS` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) NOT NULL COMMENT 'QRTZ_TRIGGERS表trigger_name的外键',
  `trigger_group` varchar(200) NOT NULL COMMENT 'QRTZ_TRIGGERS表trigger_group的外键',
  `str_prop_1` varchar(512) DEFAULT NULL COMMENT 'String类型的trigger的第一个参数',
  `str_prop_2` varchar(512) DEFAULT NULL COMMENT 'String类型的trigger的第二个参数',
  `str_prop_3` varchar(512) DEFAULT NULL COMMENT 'String类型的trigger的第三个参数',
  `int_prop_1` int DEFAULT NULL COMMENT 'int类型的trigger的第一个参数',
  `int_prop_2` int DEFAULT NULL COMMENT 'int类型的trigger的第二个参数',
  `long_prop_1` bigint DEFAULT NULL COMMENT 'long类型的trigger的第一个参数',
  `long_prop_2` bigint DEFAULT NULL COMMENT 'long类型的trigger的第二个参数',
  `dec_prop_1` decimal(13,4) DEFAULT NULL COMMENT 'decimal类型的trigger的第一个参数',
  `dec_prop_2` decimal(13,4) DEFAULT NULL COMMENT 'decimal类型的trigger的第二个参数',
  `bool_prop_1` varchar(1) DEFAULT NULL COMMENT 'Boolean类型的trigger的第一个参数',
  `bool_prop_2` varchar(1) DEFAULT NULL COMMENT 'Boolean类型的trigger的第二个参数',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='同步机制的行锁表';


INSERT IGNORE INTO QRTZ_CRON_TRIGGERS (sched_name,trigger_name,trigger_group,cron_expression,time_zone_id) VALUES
	 ('ForgeScheduler_','aiInvocationLogRetention','AI','0 20 2 * * ?','Asia/Shanghai'),
	 ('ForgeScheduler_','lowcodeBusinessTriggerScanJob','LOWCODE','0 0/5 * * * ?','Asia/Shanghai');
INSERT IGNORE INTO QRTZ_JOB_DETAILS (sched_name,job_name,job_group,description,job_class_name,is_durable,is_nonconcurrent,is_update_data,requests_recovery,job_data) VALUES
	 ('ForgeScheduler_','aiInvocationLogRetention','AI','清理超期AI模型调用治理日志','com.mdframe.forge.plugin.job.scheduler.QuartzJobExecutor','1','0','0','0',0xACED0005737200156F72672E71756172747A2E4A6F62446174614D61709FB083E8BFA9B0CB020000787200266F72672E71756172747A2E7574696C732E537472696E674B65794469727479466C61674D61708208E8C3FBC55D280200015A0013616C6C6F77735472616E7369656E74446174617872001D6F72672E71756172747A2E7574696C732E4469727479466C61674D617013E62EAD28760ACE0200025A000564697274794C00036D617074000F4C6A6176612F7574696C2F4D61703B787001737200116A6176612E7574696C2E486173684D61700507DAC1C31660D103000246000A6C6F6164466163746F724900097468726573686F6C6478703F4000000000000C7708000000100000000B74000B657865637574654D6F64657400044245414E74000A696E766F6B654D6F646574000653494E474C4574000C7363686564756C655479706574000443524F4E74000E6964656D706F74656E74466C6167737200116A6176612E6C616E672E496E746567657212E2A0A4F781873802000149000576616C7565787200106A6176612E6C616E672E4E756D62657286AC951D0B94E08B02000078700000000074000C6578656375746F724265616E74001B6169496E766F636174696F6E4C6F67526574656E74696F6E4A6F6274000A7265747279436F756E7471007E001074000B6A6F62436F6E66696749647372000E6A6176612E6C616E672E4C6F6E673B8BE490CC8F23DF0200014A000576616C75657871007E000F000000000000000B74000D6D697366697265506F6C69637974000A444F5F4E4F5448494E4774000B74726967676572547970657400095343484544554C454474000E6578656375746F724D6574686F6474001A636C65616E45787069726564496E766F636174696F6E4C6F6773740010636F6E63757272656E74506F6C696379740005414C4C4F577800),
	 ('ForgeScheduler_','lowcodeBusinessTriggerScanJob','LOWCODE','低代码定时触发器扫描任务，默认每5分钟执行一次','com.mdframe.forge.plugin.job.scheduler.QuartzJobExecutor','1','0','0','0',0xACED0005737200156F72672E71756172747A2E4A6F62446174614D61709FB083E8BFA9B0CB020000787200266F72672E71756172747A2E7574696C732E537472696E674B65794469727479466C61674D61708208E8C3FBC55D280200015A0013616C6C6F77735472616E7369656E74446174617872001D6F72672E71756172747A2E7574696C732E4469727479466C61674D617013E62EAD28760ACE0200025A000564697274794C00036D617074000F4C6A6176612F7574696C2F4D61703B787001737200116A6176612E7574696C2E486173684D61700507DAC1C31660D103000246000A6C6F6164466163746F724900097468726573686F6C6478703F4000000000000C7708000000100000000B74000B657865637574654D6F64657400044245414E74000A696E766F6B654D6F646574000653494E474C4574000C7363686564756C655479706574000443524F4E74000E6964656D706F74656E74466C6167737200116A6176612E6C616E672E496E746567657212E2A0A4F781873802000149000576616C7565787200106A6176612E6C616E672E4E756D62657286AC951D0B94E08B02000078700000000074000C6578656375746F724265616E74001F627573696E657373547269676765725363686564756C65725365727669636574000A7265747279436F756E7471007E001074000B6A6F62436F6E66696749647372000E6A6176612E6C616E672E4C6F6E673B8BE490CC8F23DF0200014A000576616C75657871007E000F000000000000000674000D6D697366697265506F6C69637974000A444F5F4E4F5448494E4774000B74726967676572547970657400095343484544554C454474000E6578656375746F724D6574686F647400157363616E5363686564756C65645472696767657273740010636F6E63757272656E74506F6C696379740005414C4C4F577800);
INSERT IGNORE INTO QRTZ_LOCKS (sched_name,lock_name) VALUES
	 ('ForgeScheduler_','STATE_ACCESS'),
	 ('ForgeScheduler_','TRIGGER_ACCESS');
INSERT IGNORE INTO QRTZ_SCHEDULER_STATE (sched_name,instance_name,last_checkin_time,checkin_interval) VALUES
	 ('ForgeScheduler_','VM-0-14-opencloudos1784682889644',1784770435657,15000),
	 ('ForgeScheduler_','yaominliangdeMacBook-Pro.local1784683980004',1784770434240,15000);
INSERT IGNORE INTO QRTZ_TRIGGERS (sched_name,trigger_name,trigger_group,job_name,job_group,description,next_fire_time,prev_fire_time,priority,trigger_state,trigger_type,start_time,end_time,calendar_name,misfire_instr,job_data) VALUES
	 ('ForgeScheduler_','aiInvocationLogRetention','AI','aiInvocationLogRetention','AI',NULL,1784830800000,-1,5,'WAITING','CRON',1784769870000,0,NULL,2,X''),
	 ('ForgeScheduler_','lowcodeBusinessTriggerScanJob','LOWCODE','lowcodeBusinessTriggerScanJob','LOWCODE',NULL,1784770500000,1784770200000,5,'WAITING','CRON',1784769869000,0,NULL,2,X'');
INSERT INTO sys_config_group (id,group_code,group_name,group_icon,config_value,sort,status,remark,create_time,update_time) VALUES
	 (2,'login','登录配置','lock','{"enableCaptcha":false,"captchaType":"slider","enableRememberMe":true,"rememberMeDays":30,"enableLoginLog":true,"enableIpLimit":false,"ipWhitelist":""}',2,1,'登录相关配置','2026-02-25 18:07:35','2026-07-10 12:21:51'),
	 (4,'watermark','水印配置','eye','{"enable":true,"content":"name_phone","opacity":0.1,"fontSize":16,"fontColor":"#E00F0FFF","rotate":-20,"gapX":200,"gapY":200,"offsetX":0,"offsetY":0,"showTimestamp":true,"timestampFormat":"yyyy-MM-dd HH:mm:ss","zindex":1000}',4,1,'水印相关配置','2026-02-25 18:07:35','2026-07-09 10:13:48'),
	 (5,'crypto','加解密配置','eye','{"enabled":false,"algorithm":"SM4","enableDynamicKey":true,"sessionKeyExpire":7200,"enableApiCrypto":true,"enableFieldCrypto":true,"enableReplayProtection":false,"replayTimeWindow":300,"replayIncludePaths":[],"replayExcludePaths":["/auth/captcha","/auth/captcha/**","/auth/loginConfig","/crypto/public-key"],"excludePaths":[],"enableDesensitize":true}',4,1,'加解密配置','2026-02-25 18:07:35','2026-07-16 06:44:03'),
	 (6,'auth','认证配置','eye','{"enableApiPermission":true,"apiPermissionExcludePaths":["/auth/**"],"enableLoginLock":true,"maxLoginAttempts":4,"lockDuration":30,"failRecordExpire":15,"sameAccountLoginStrategy":"replace_old","enableOnlineUserManagement":true,"enableClientValidation":true,"enableLegacyClientSecretRead":true}',4,1,'认证配置','2026-02-25 18:07:35','2026-06-12 09:58:33'),
	 (7,'log','日志配置','eye','{"enableOperationLog":true,"enableLoginLog":true,"requestParamsMaxLength":2000,"responseResultMaxLength":2000,"excludePaths":["/auth/captcha","/auth/login","/auth/register","/auth/changePassword","/auth/resetPassword","/auth/online/kickout","/auth/online/batchKickout","/actuator/**","/swagger-ui/**","/v3/api-docs/**"],"printOperationLog":true,"printLoginLog":true,"threadPoolCoreSize":2,"threadPoolMaxSize":5,"threadPoolQueueCapacity":500}',4,1,'日志配置','2026-02-25 18:07:35','2026-07-03 12:27:17'),
	 (8,'security','安全配置','eye','{"saToken":{"timeout":2592000,"activityTimeout":-1,"isConcurrent":false,"isShare":false,"isReadBody":true,"isReadHeader":true,"isReadCookie":false,"tokenPrefix":"Bearer","tokenName":"Authorization"},"passwordPolicy":{"minLength":8,"requireUppercase":true,"requireLowercase":true,"requireNumbers":true,"requireSpecialChars":false,"expireDays":90,"historyCount":5}}',4,1,'安全配置','2026-02-25 18:07:35','2026-06-12 09:58:33');
INSERT INTO sys_data_scope_config (id,tenant_id,resource_code,resource_name,mapper_method,table_alias,user_id_column,org_id_column,tenant_id_column,region_code_column,user_region_column,user_table_alias,enabled,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (1,1,'system:user:list','用户列表查询','com.mdframe.forge.plugin.system.mapper.SysUserMapper.selectUserPage','u','id','<sql>u.id IN (SELECT user_id FROM sys_user_org WHERE org_id IN (#{orgIds}))</sql>','tenant_id','region_code','region_code',NULL,1,'用户列表数据权限控制',NULL,'2025-12-03 15:48:28',2,'2026-05-24 22:18:51',NULL,0),
	 (6,1,'system:listSysRegion','查询行政区划','com.mdframe.forge.plugin.system.mapper.SysRegionMapper.listSysRegion','','','','','code','code','',1,'',NULL,'2026-04-28 16:27:40',NULL,'2026-04-28 16:51:36',NULL,0),
	 (7,1,'system:deptPage:list','组织列表查询-分页','com.mdframe.forge.plugin.system.mapper.SysOrgMapper.selectOrgPage','','<sql>id IN (SELECT org_id FROM sys_user_org WHERE user_id = #{userId} AND tenant_id = #{tenantId})</sql>','id','tenant_id','region_code','region_code',NULL,1,'用户列表数据权限控制',NULL,'2025-12-03 15:48:28',1,'2026-06-10 15:22:05',NULL,0),
	 (8,1,'system:dept:list','组织列表查询','com.mdframe.forge.plugin.system.mapper.SysOrgMapper.selectOrgList','','<sql>id IN (SELECT org_id FROM sys_user_org WHERE user_id = #{userId} AND tenant_id = #{tenantId})</sql>','id','tenant_id','region_code','region_code',NULL,1,'用户列表数据权限控制',NULL,'2025-12-03 15:48:28',1,'2026-06-10 15:22:05',NULL,0),
	 (9,1,'system:org:lazyTree','组织列表查询懒加载','com.mdframe.forge.plugin.system.mapper.SysOrgMapper.selectOrgLazyTree','o','<sql>o.id IN (SELECT org_id FROM sys_user_org WHERE user_id = #{userId} AND tenant_id = #{tenantId})</sql>','id','tenant_id','region_code',NULL,NULL,1,NULL,2,'2026-05-24 22:27:06',2,'2026-06-10 15:22:05',3,0),
	 (10,1,'system:org:children','组织子节点查询','com.mdframe.forge.plugin.system.mapper.SysOrgMapper.selectOrgChildrenByParentId','o','<sql>o.id IN (SELECT org_id FROM sys_user_org WHERE user_id = #{userId} AND tenant_id = #{tenantId})</sql>','id','tenant_id','region_code',NULL,NULL,1,'组织懒加载子节点数据权限配置',NULL,'2026-06-10 14:16:24',1,'2026-07-07 17:21:16',NULL,0),
	 (11,1,'flow:monitor:view','流程监控实例列表','com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper.selectMonitorBusinessPage','','apply_user_id','apply_dept_id','tenant_id',NULL,NULL,NULL,1,'流程监控实例列表数据权限控制',1,'2026-08-07 00:00:00',1,'2026-08-07 00:00:00',1,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (1,1,1,'未知','0','sys_org_type','','success','Y',NULL,NULL,NULL,1,'性别未知',NULL,'2025-11-12 17:41:18',NULL,'2025-12-12 10:50:32',NULL,0),
	 (2,1,2,'男','1','sys_user_sex','','','N',NULL,NULL,NULL,1,'性别男',NULL,'2025-11-12 17:41:18',NULL,'2025-11-12 17:41:18',NULL,0),
	 (3,1,3,'女','2','sys_user_sex','','','N',NULL,NULL,NULL,1,'性别女',NULL,'2025-11-12 17:41:18',NULL,'2025-11-12 17:41:18',NULL,0),
	 (4,1,1,'禁用','0','sys_user_status','','danger','N',NULL,NULL,NULL,1,'用户禁用状态',NULL,'2025-11-12 17:41:18',NULL,'2025-11-12 17:41:18',NULL,0),
	 (5,1,2,'正常','1','sys_user_status','','success','Y',NULL,NULL,NULL,1,'用户正常状态',NULL,'2025-11-12 17:41:18',NULL,'2025-11-12 17:41:18',NULL,0),
	 (6,1,3,'锁定','2','sys_user_status','','warning','N',NULL,NULL,NULL,1,'用户锁定状态',NULL,'2025-11-12 17:41:18',NULL,'2025-11-12 17:41:18',NULL,0),
	 (7,1,1,'公司','1','sys_org_type','','info','Y',NULL,NULL,NULL,1,'公司组织',NULL,'2025-11-12 17:41:18',NULL,'2025-12-12 10:46:21',NULL,0),
	 (8,1,2,'部门','2','sys_org_type','','success','N',NULL,NULL,NULL,1,'部门组织',NULL,'2025-11-12 17:41:18',NULL,'2025-11-12 17:41:18',NULL,0),
	 (9,1,3,'小组','3','sys_org_type','','info','N',NULL,NULL,NULL,1,'小组组织',NULL,'2025-11-12 17:41:18',NULL,'2025-11-12 17:41:18',NULL,0),
	 (10,1,1,'是','Y','yes_no','','','N',NULL,NULL,NULL,1,'小组组织',NULL,'2025-11-12 17:41:18',NULL,'2025-11-12 17:41:18',NULL,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (11,1,2,'否','N','yes_no','','','N',NULL,NULL,NULL,1,'小组组织',NULL,'2025-11-12 17:41:18',NULL,'2025-11-12 17:41:18',NULL,0),
	 (12,1,1,'通知公告','NOTICE','sys_notice_type',NULL,NULL,'N',NULL,NULL,NULL,1,'普通通知公告',NULL,'2025-12-04 10:57:35',NULL,'2025-12-08 16:31:36',NULL,0),
	 (13,1,2,'系统公告','ANNOUNCEMENT','sys_notice_type',NULL,NULL,'N',NULL,NULL,NULL,1,'系统级公告',NULL,'2025-12-04 10:57:35',NULL,'2025-12-08 16:31:36',NULL,0),
	 (14,1,3,'新闻动态','NEWS','sys_notice_type',NULL,NULL,'N',NULL,NULL,NULL,1,'新闻动态信息',NULL,'2025-12-04 10:57:35',NULL,'2025-12-08 16:31:37',NULL,0),
	 (15,1,1,'草稿','0','sys_notice_status',NULL,NULL,'N',NULL,NULL,NULL,1,'草稿状态',NULL,'2025-12-04 10:57:35',NULL,'2025-12-08 16:31:37',NULL,0),
	 (16,1,2,'已发布','1','sys_notice_status',NULL,NULL,'N',NULL,NULL,NULL,1,'已发布状态',NULL,'2025-12-04 10:57:35',NULL,'2025-12-08 16:31:37',NULL,0),
	 (17,1,3,'已撤回','2','sys_notice_status',NULL,NULL,'N',NULL,NULL,NULL,1,'已撤回状态',NULL,'2025-12-04 10:57:35',NULL,'2025-12-08 16:31:37',NULL,0),
	 (18,1,1,'待办','1','case_status',NULL,NULL,'N',NULL,NULL,NULL,1,'',NULL,'2025-12-05 14:37:28',NULL,'2025-12-08 16:31:37',NULL,0),
	 (19,1,2,'已办','0','case_status',NULL,NULL,'N',NULL,NULL,NULL,1,'',NULL,'2025-12-05 14:37:28',NULL,'2025-12-08 16:31:37',NULL,0),
	 (21,1,1,'GET','GET','req_method',NULL,NULL,'N',NULL,NULL,NULL,1,'GET请求',NULL,'2026-02-06 14:59:30',NULL,'2026-06-04 11:43:21',NULL,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (22,1,2,'POST','POST','req_method',NULL,NULL,'N',NULL,NULL,NULL,1,'POST请求',NULL,'2026-02-06 14:59:30',NULL,'2026-06-04 11:43:21',NULL,0),
	 (23,1,3,'PUT','PUT','req_method',NULL,NULL,'N',NULL,NULL,NULL,1,'PUT请求',NULL,'2026-02-06 14:59:30',NULL,'2026-06-04 11:43:21',NULL,0),
	 (24,1,4,'DELETE','DELETE','req_method',NULL,NULL,'N',NULL,NULL,NULL,1,'DELETE请求',NULL,'2026-02-06 14:59:30',NULL,'2026-06-04 11:43:21',NULL,0),
	 (25,1,5,'PATCH','PATCH','req_method',NULL,NULL,'N',NULL,NULL,NULL,1,'PATCH请求',NULL,'2026-02-06 14:59:30',NULL,'2026-06-04 11:43:21',NULL,0),
	 (26,1,6,'ALL','ALL','req_method',NULL,NULL,'N',NULL,NULL,NULL,1,'所有请求方式',NULL,'2026-02-06 14:59:30',NULL,'2026-06-04 11:43:21',NULL,0),
	 (27,1,1,'系统管理','sys','module_code',NULL,NULL,'N',NULL,NULL,NULL,1,'系统管理模块',NULL,'2026-02-06 14:59:37',NULL,'2026-06-04 11:43:21',NULL,0),
	 (28,1,2,'用户管理','user','module_code',NULL,NULL,'N',NULL,NULL,NULL,1,'用户管理模块',NULL,'2026-02-06 14:59:37',NULL,'2026-06-04 11:43:21',NULL,0),
	 (29,1,3,'角色管理','role','module_code',NULL,NULL,'N',NULL,NULL,NULL,1,'角色管理模块',NULL,'2026-02-06 14:59:37',NULL,'2026-06-04 11:43:21',NULL,0),
	 (30,1,4,'菜单管理','menu','module_code',NULL,NULL,'N',NULL,NULL,NULL,1,'菜单管理模块',NULL,'2026-02-06 14:59:37',NULL,'2026-06-04 11:43:21',NULL,0),
	 (31,1,5,'部门管理','dept','module_code',NULL,NULL,'N',NULL,NULL,NULL,1,'部门管理模块',NULL,'2026-02-06 14:59:37',NULL,'2026-06-04 11:43:21',NULL,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (32,1,6,'岗位管理','post','module_code',NULL,NULL,'N',NULL,NULL,NULL,1,'岗位管理模块',NULL,'2026-02-06 14:59:37',NULL,'2026-06-04 11:43:21',NULL,0),
	 (33,1,7,'字典管理','dict','module_code',NULL,NULL,'N',NULL,NULL,NULL,1,'字典管理模块',NULL,'2026-02-06 14:59:37',NULL,'2026-06-04 11:43:21',NULL,0),
	 (34,1,8,'参数管理','config','module_code',NULL,NULL,'N',NULL,NULL,NULL,1,'参数管理模块',NULL,'2026-02-06 14:59:37',NULL,'2026-06-04 11:43:21',NULL,0),
	 (35,1,9,'通知管理','notice','module_code',NULL,NULL,'N',NULL,NULL,NULL,1,'通知管理模块',NULL,'2026-02-06 14:59:37',NULL,'2026-06-04 11:43:21',NULL,0),
	 (36,1,10,'日志管理','log','module_code',NULL,NULL,'N',NULL,NULL,NULL,1,'日志管理模块',NULL,'2026-02-06 14:59:37',NULL,'2026-06-04 11:43:21',NULL,0),
	 (37,1,11,'文件管理','file','module_code',NULL,NULL,'N',NULL,NULL,NULL,1,'文件管理模块',NULL,'2026-02-06 14:59:37',NULL,'2026-06-04 11:43:21',NULL,0),
	 (38,1,12,'任务管理','job','module_code',NULL,NULL,'N',NULL,NULL,NULL,1,'任务管理模块',NULL,'2026-02-06 14:59:37',NULL,'2026-06-04 11:43:21',NULL,0),
	 (39,1,13,'数据权限','datascope','module_code',NULL,NULL,'N',NULL,NULL,NULL,1,'数据权限模块',NULL,'2026-02-06 14:59:37',NULL,'2026-06-04 11:43:21',NULL,0),
	 (40,1,14,'API配置','apiconfig','module_code',NULL,NULL,'N',NULL,NULL,NULL,1,'API配置管理模块',NULL,'2026-02-06 14:59:37',NULL,'2026-06-04 11:43:21',NULL,0),
	 (41,1,0,'图形验证码','graphical','captcha_type',NULL,'default','N',NULL,NULL,NULL,1,NULL,1,'2026-02-26 10:13:12',1,'2026-02-26 10:13:12',2,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (42,1,0,'滑块验证码','slider','captcha_type',NULL,'default','N',NULL,NULL,NULL,1,NULL,1,'2026-02-26 10:13:28',1,'2026-02-26 10:13:28',2,0),
	 (43,1,0,'短信验证码','sms','captcha_type',NULL,'default','N',NULL,NULL,NULL,1,NULL,1,'2026-02-26 10:13:42',1,'2026-02-26 10:13:42',2,0),
	 (44,1,0,'姓名+手机号','name_phone','water_marker_content',NULL,'default','N',NULL,NULL,NULL,1,NULL,1,'2026-02-26 10:19:16',1,'2026-02-26 10:19:16',2,0),
	 (45,1,1,'微信','WECHAT','sys_social_platform',NULL,NULL,'N',NULL,NULL,NULL,1,'社交平台-微信',NULL,'2026-04-01 14:46:00',NULL,'2026-04-01 14:47:32',NULL,0),
	 (46,1,2,'微信开放平台','WECHAT_OPEN','sys_social_platform',NULL,NULL,'N',NULL,NULL,NULL,1,'社交平台-微信开放平台',NULL,'2026-04-01 14:46:00',NULL,'2026-04-01 14:47:33',NULL,0),
	 (47,1,3,'微信小程序','WECHAT_MINI','sys_social_platform',NULL,NULL,'N',NULL,NULL,NULL,1,'社交平台-微信小程序',NULL,'2026-04-01 14:46:00',NULL,'2026-04-01 14:47:33',NULL,0),
	 (48,1,4,'钉钉','DINGTALK','sys_social_platform',NULL,NULL,'N',NULL,NULL,NULL,1,'社交平台-钉钉',NULL,'2026-04-01 14:46:00',NULL,'2026-04-01 14:47:33',NULL,0),
	 (49,1,5,'企业微信','WECHAT_ENTERPRISE','sys_social_platform',NULL,NULL,'N',NULL,NULL,NULL,1,'社交平台-企业微信',NULL,'2026-04-01 14:46:00',NULL,'2026-04-01 14:47:33',NULL,0),
	 (50,1,6,'GitHub','GITHUB','sys_social_platform',NULL,NULL,'N',NULL,NULL,NULL,1,'社交平台-GitHub',NULL,'2026-04-01 14:46:00',NULL,'2026-04-01 14:47:33',NULL,0),
	 (51,1,7,'Gitee','GITEE','sys_social_platform',NULL,NULL,'N',NULL,NULL,NULL,1,'社交平台-Gitee',NULL,'2026-04-01 14:46:00',NULL,'2026-04-01 14:47:33',NULL,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (52,1,8,'QQ','QQ','sys_social_platform',NULL,NULL,'N',NULL,NULL,NULL,1,'社交平台-QQ',NULL,'2026-04-01 14:46:00',NULL,'2026-04-01 14:47:33',NULL,0),
	 (53,1,9,'微博','WEIBO','sys_social_platform',NULL,NULL,'N',NULL,NULL,NULL,1,'社交平台-微博',NULL,'2026-04-01 14:46:00',NULL,'2026-04-01 14:47:33',NULL,0),
	 (54,1,10,'支付宝','ALIPAY','sys_social_platform',NULL,NULL,'N',NULL,NULL,NULL,1,'社交平台-支付宝',NULL,'2026-04-01 14:46:00',NULL,'2026-04-01 14:47:33',NULL,0),
	 (55,1,11,'百度','BAIDU','sys_social_platform',NULL,NULL,'N',NULL,NULL,NULL,1,'社交平台-百度',NULL,'2026-04-01 14:46:00',NULL,'2026-04-01 14:47:33',NULL,0),
	 (56,1,12,'谷歌','GOOGLE','sys_social_platform',NULL,NULL,'N',NULL,NULL,NULL,1,'社交平台-谷歌',NULL,'2026-04-01 14:46:00',NULL,'2026-04-01 14:47:33',NULL,0),
	 (57,1,13,'Facebook','FACEBOOK','sys_social_platform',NULL,NULL,'N',NULL,NULL,NULL,1,'社交平台-Facebook',NULL,'2026-04-01 14:46:00',NULL,'2026-04-01 14:47:34',NULL,0),
	 (58,1,14,'Twitter','TWITTER','sys_social_platform',NULL,NULL,'N',NULL,NULL,NULL,1,'社交平台-Twitter',NULL,'2026-04-01 14:46:00',NULL,'2026-04-01 14:47:34',NULL,0),
	 (59,1,15,'小米','XIAOMI','sys_social_platform',NULL,NULL,'N',NULL,NULL,NULL,1,'社交平台-小米',NULL,'2026-04-01 14:46:00',NULL,'2026-04-01 14:47:34',NULL,0),
	 (60,1,16,'华为','HUAWEI','sys_social_platform',NULL,NULL,'N',NULL,NULL,NULL,1,'社交平台-华为',NULL,'2026-04-01 14:46:00',NULL,'2026-04-01 14:47:34',NULL,0),
	 (61,1,17,'飞书','FEISHU','sys_social_platform',NULL,NULL,'N',NULL,NULL,NULL,1,'社交平台-飞书',NULL,'2026-04-01 14:46:00',NULL,'2026-04-01 14:47:34',NULL,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (62,1,18,'钉钉企业内部','DINGTALK_ACCOUNT','sys_social_platform',NULL,NULL,'N',NULL,NULL,NULL,1,'社交平台-钉钉企业内部',NULL,'2026-04-01 14:46:00',NULL,'2026-04-01 14:47:34',NULL,0),
	 (63,1,19,'自定义','CUSTOM','sys_social_platform',NULL,NULL,'N',NULL,NULL,NULL,1,'社交平台-自定义',NULL,'2026-04-01 14:46:00',NULL,'2026-04-01 14:47:34',NULL,0),
	 (64,1,2,'停用','0','sys_normal_disable',NULL,'default','N',NULL,NULL,NULL,1,'社交平台-自定义',NULL,'2026-04-01 14:46:00',1,'2026-04-26 17:28:46',NULL,0),
	 (65,1,1,'正常','1','sys_normal_disable',NULL,'default','N',NULL,NULL,NULL,1,'社交平台-自定义',NULL,'2026-04-01 14:46:00',1,'2026-04-26 17:28:46',NULL,0),
	 (66,1,1,'阿里百炼','dashscope','ai_provider_type',NULL,'default','N',NULL,NULL,NULL,1,'阿里百炼平台',NULL,'2026-04-17 16:28:59',NULL,'2026-04-17 17:04:24',NULL,0),
	 (67,1,2,'OpenAI','openai','ai_provider_type',NULL,'success','N',NULL,NULL,NULL,1,'OpenAI平台',NULL,'2026-04-17 16:28:59',NULL,'2026-04-17 17:03:34',NULL,0),
	 (68,1,3,'智谱 AI','zhipu','ai_provider_type',NULL,'info','N',NULL,NULL,NULL,1,'智谱AI平台',NULL,'2026-04-17 16:28:59',NULL,'2026-04-17 17:03:34',NULL,0),
	 (69,1,4,'Moonshot','moonshot','ai_provider_type',NULL,'default','N',NULL,NULL,NULL,1,'Moonshot平台',NULL,'2026-04-17 16:28:59',NULL,'2026-04-17 17:03:34',NULL,0),
	 (70,1,5,'DeepSeek','deepseek','ai_provider_type',NULL,'default','N',NULL,NULL,NULL,1,'DeepSeek平台',NULL,'2026-04-17 16:28:59',NULL,'2026-04-17 17:03:34',NULL,0),
	 (71,1,6,'Ollama（本地）','ollama','ai_provider_type',NULL,'warning','N',NULL,NULL,NULL,1,'Ollama本地部署',NULL,'2026-04-17 16:28:59',NULL,'2026-04-17 17:03:34',NULL,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (72,1,7,'Azure','azure','ai_provider_type',NULL,'info','N',NULL,NULL,NULL,1,'Azure OpenAI',NULL,'2026-04-17 16:28:59',NULL,'2026-04-17 17:03:34',NULL,0),
	 (73,1,8,'自定义','custom','ai_provider_type',NULL,'default','N',NULL,NULL,NULL,1,'自定义供应商',NULL,'2026-04-17 16:28:59',NULL,'2026-04-17 17:03:34',NULL,0),
	 (74,1,1,'对话','chat','ai_model_type',NULL,'success','N',NULL,NULL,NULL,1,'对话模型',NULL,'2026-04-17 16:28:59',NULL,'2026-04-17 17:03:34',NULL,0),
	 (75,1,2,'向量化','embedding','ai_model_type',NULL,'info','N',NULL,NULL,NULL,1,'向量化模型',NULL,'2026-04-17 16:28:59',NULL,'2026-04-17 17:03:34',NULL,0),
	 (76,1,3,'图像生成','image','ai_model_type',NULL,'warning','N',NULL,NULL,NULL,1,'图像生成模型',NULL,'2026-04-17 16:28:59',NULL,'2026-04-17 17:03:34',NULL,0),
	 (77,1,4,'语音','audio','ai_model_type',NULL,'default','N',NULL,NULL,NULL,1,'语音模型',NULL,'2026-04-17 16:28:59',NULL,'2026-04-17 17:03:34',NULL,0),
	 (78,1,1,'正常','0','ai_status',NULL,'success','Y',NULL,NULL,NULL,1,'正常状态',NULL,'2026-04-17 16:28:59',NULL,'2026-04-17 17:03:35',NULL,0),
	 (79,1,2,'停用','1','ai_status',NULL,'error','N',NULL,NULL,NULL,1,'停用状态',NULL,'2026-04-17 16:28:59',NULL,'2026-04-17 17:03:35',NULL,0),
	 (80,1,1,'否','0','ai_is_default',NULL,'default','Y',NULL,NULL,NULL,1,'非默认',NULL,'2026-04-17 16:28:59',NULL,'2026-04-17 17:03:35',NULL,0),
	 (81,1,2,'是','1','ai_is_default',NULL,'success','N',NULL,NULL,NULL,1,'默认',NULL,'2026-04-17 16:28:59',NULL,'2026-04-17 17:03:35',NULL,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (82,1,1,'待付款','1','order_status',NULL,'default','N',NULL,NULL,NULL,1,NULL,1,'2026-04-22 13:02:15',1,'2026-04-22 13:02:15',2,0),
	 (83,1,2,'已付款','2','order_status',NULL,'default','N',NULL,NULL,NULL,1,NULL,1,'2026-04-22 13:02:15',1,'2026-04-22 13:02:15',2,0),
	 (84,1,3,'已发货','3','order_status',NULL,'default','N',NULL,NULL,NULL,1,NULL,1,'2026-04-22 13:02:16',1,'2026-04-22 13:02:16',2,0),
	 (87,1,1,'普通订单','1','order_type',NULL,'default','N',NULL,NULL,NULL,1,NULL,1,'2026-04-22 13:02:16',1,'2026-04-22 13:02:16',2,0),
	 (88,1,2,'团购订单','2','order_type',NULL,'default','N',NULL,NULL,NULL,1,NULL,1,'2026-04-22 13:02:16',1,'2026-04-22 13:02:16',2,0),
	 (89,1,3,'秒杀订单','3','order_type',NULL,'default','N',NULL,NULL,NULL,1,NULL,1,'2026-04-22 13:02:16',1,'2026-04-22 13:02:16',2,0),
	 (90,1,4,'测试','4','order_type',NULL,'default','N',NULL,NULL,NULL,1,NULL,1,'2026-04-22 14:45:50',1,'2026-04-22 14:45:50',2,0),
	 (91,1,1,'研发部','1','sys_dept',NULL,'default','N',NULL,NULL,NULL,1,NULL,1,'2026-04-26 17:28:46',1,'2026-04-26 17:28:46',2,0),
	 (92,1,2,'市场部','2','sys_dept',NULL,'default','N',NULL,NULL,NULL,1,NULL,1,'2026-04-26 17:28:46',1,'2026-04-26 17:28:46',2,0),
	 (93,1,3,'人事部','3','sys_dept',NULL,'default','N',NULL,NULL,NULL,1,NULL,1,'2026-04-26 17:28:46',1,'2026-04-26 17:28:46',2,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (94,1,1,'电子产品','1','product_category',NULL,'default','N',NULL,NULL,NULL,1,NULL,1,'2026-04-26 18:09:11',1,'2026-04-26 18:09:11',2,0),
	 (95,1,2,'服装鞋帽','2','product_category',NULL,'default','N',NULL,NULL,NULL,1,NULL,1,'2026-04-26 18:09:11',1,'2026-04-26 18:09:11',2,0),
	 (96,1,3,'家居日用','3','product_category',NULL,'default','N',NULL,NULL,NULL,1,NULL,1,'2026-04-26 18:09:11',1,'2026-04-26 18:09:11',2,0),
	 (97,1,1,'正常','1','common_status',NULL,'default','N',NULL,NULL,NULL,1,NULL,1,'2026-04-26 18:09:12',1,'2026-04-26 18:09:12',2,0),
	 (98,1,2,'停用','0','common_status',NULL,'default','N',NULL,NULL,NULL,1,NULL,1,'2026-04-26 18:09:12',1,'2026-04-26 18:09:12',2,0),
	 (99,1,1,'草稿','draft','flow_version_tag',NULL,'default','N',NULL,NULL,NULL,1,'草稿版本',NULL,'2026-05-09 08:48:57',NULL,'2026-05-09 08:48:57',NULL,0),
	 (100,1,2,'测试','test','flow_version_tag',NULL,'info','N',NULL,NULL,NULL,1,'测试版本',NULL,'2026-05-09 08:48:57',NULL,'2026-05-09 08:48:57',NULL,0),
	 (101,1,3,'正式发布','release','flow_version_tag',NULL,'success','Y',NULL,NULL,NULL,1,'正式发布版本',NULL,'2026-05-09 08:48:57',NULL,'2026-05-09 08:48:57',NULL,0),
	 (102,1,4,'已废弃','deprecated','flow_version_tag',NULL,'warning','N',NULL,NULL,NULL,1,'已废弃版本',NULL,'2026-05-09 08:48:57',NULL,'2026-05-09 08:48:57',NULL,0),
	 (103,1,1,'普通流程','1','flow_importance_level',NULL,'default','Y',NULL,NULL,NULL,1,'普通流程，模型管理员可回退',NULL,'2026-05-09 08:48:57',NULL,'2026-05-09 08:48:57',NULL,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (104,1,2,'重要流程','2','flow_importance_level',NULL,'danger','N',NULL,NULL,NULL,1,'重要流程，需超级管理员权限回退',NULL,'2026-05-09 08:48:57',NULL,'2026-05-09 08:48:57',NULL,0),
	 (105,1,1,'token认证','token','external_auth_adapter',NULL,'danger','N',NULL,NULL,NULL,1,'重要流程，需超级管理员权限回退',NULL,'2026-05-09 08:48:57',NULL,'2026-05-09 08:48:57',NULL,0),
	 (106,1,1,'请求头映射','header_map','external_auth_adapter',NULL,NULL,'N',NULL,NULL,NULL,1,'将自定义认证配置中的 KEY:VALUE 写入请求头',NULL,'2026-05-11 12:54:24',NULL,'2026-05-11 12:54:24',NULL,0),
	 (107,1,1,'本地存储','local','sys_file_storage_type',NULL,'default','Y',NULL,NULL,NULL,1,'本地磁盘文件存储',NULL,'2026-05-21 12:52:20',NULL,'2026-05-21 12:52:20',NULL,0),
	 (108,1,2,'RustFS存储','rustfs','sys_file_storage_type',NULL,'success','N',NULL,NULL,NULL,1,'RustFS 对象存储',NULL,'2026-05-21 12:52:20',NULL,'2026-05-21 12:52:20',NULL,0),
	 (109,1,3,'MinIO','minio','sys_file_storage_type',NULL,'info','N',NULL,NULL,NULL,1,'MinIO 对象存储',NULL,'2026-05-21 12:52:20',NULL,'2026-05-21 12:52:20',NULL,0),
	 (110,1,4,'阿里云OSS','aliyun','sys_file_storage_type',NULL,'warning','N',NULL,NULL,NULL,1,'阿里云 OSS 对象存储',NULL,'2026-05-21 12:52:20',NULL,'2026-05-21 12:52:20',NULL,0),
	 (111,1,5,'腾讯云COS','tencent','sys_file_storage_type',NULL,'success','N',NULL,NULL,NULL,1,'腾讯云 COS 对象存储',NULL,'2026-05-21 12:52:20',NULL,'2026-05-21 12:52:20',NULL,0),
	 (112,1,6,'七牛云','qiniu','sys_file_storage_type',NULL,'error','N',NULL,NULL,NULL,1,'七牛云对象存储',NULL,'2026-05-21 12:52:20',NULL,'2026-05-21 12:52:20',NULL,0),
	 (113,1,7,'AWS S3','s3','sys_file_storage_type',NULL,'primary','N',NULL,NULL,NULL,1,'AWS S3 对象存储',NULL,'2026-05-21 12:52:20',NULL,'2026-05-21 12:52:20',NULL,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (114,1,1,'启用','1','sys_enable_disable',NULL,'success','Y',NULL,NULL,NULL,1,'启用状态',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (115,1,2,'禁用','0','sys_enable_disable',NULL,'error','N',NULL,NULL,NULL,1,'禁用状态',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (116,1,1,'是','1','sys_yes_no',NULL,'success','N',NULL,NULL,NULL,1,'是',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (117,1,2,'否','0','sys_yes_no',NULL,'default','Y',NULL,NULL,NULL,1,'否',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (118,1,1,'系统管理员','0','sys_user_type',NULL,'warning','N',NULL,NULL,NULL,1,'系统管理员',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (119,1,2,'租户管理员','1','sys_user_type',NULL,'info','Y',NULL,NULL,NULL,1,'租户管理员',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (120,1,3,'普通用户','2','sys_user_type',NULL,'default','N',NULL,NULL,NULL,1,'普通用户',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (121,1,1,'未知','0','sys_user_sex',NULL,'default','Y',NULL,NULL,NULL,1,'性别未知',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (122,1,1,'全部数据','1','sys_role_data_scope',NULL,'success','N',NULL,NULL,NULL,1,'全部数据权限',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (123,1,2,'本租户数据','2','sys_role_data_scope',NULL,'info','Y',NULL,NULL,NULL,1,'本租户数据',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (124,1,3,'本组织数据','3','sys_role_data_scope',NULL,'warning','N',NULL,NULL,NULL,1,'本组织数据',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (125,1,4,'本组织及子组织','4','sys_role_data_scope',NULL,'warning','N',NULL,NULL,NULL,1,'本组织及子组织',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (126,1,5,'个人数据','5','sys_role_data_scope',NULL,'default','N',NULL,NULL,NULL,1,'个人数据',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (127,1,7,'本行政区划数据','7','sys_role_data_scope',NULL,'primary','N',NULL,NULL,NULL,1,'本行政区划数据',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (128,1,1,'管理岗','1','sys_post_type',NULL,'success','N',NULL,NULL,NULL,1,'管理岗位',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (129,1,2,'技术岗','2','sys_post_type',NULL,'info','N',NULL,NULL,NULL,1,'技术岗位',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (130,1,3,'业务岗','3','sys_post_type',NULL,'warning','N',NULL,NULL,NULL,1,'业务岗位',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (131,1,4,'其他','4','sys_post_type',NULL,'default','Y',NULL,NULL,NULL,1,'其他岗位',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (132,1,1,'系统消息','SYSTEM','sys_message_type',NULL,'info','Y',NULL,NULL,NULL,1,'系统消息',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (133,1,2,'短信','SMS','sys_message_type',NULL,'warning','N',NULL,NULL,NULL,1,'短信消息',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (134,1,3,'邮件','EMAIL','sys_message_type',NULL,'success','N',NULL,NULL,NULL,1,'邮件消息',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (135,1,4,'自定义','CUSTOM','sys_message_type',NULL,'default','N',NULL,NULL,NULL,1,'自定义消息',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (136,1,1,'站内信','WEB','sys_message_channel',NULL,'default','Y',NULL,NULL,NULL,1,'站内信渠道',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (137,1,2,'短信','SMS','sys_message_channel',NULL,'warning','N',NULL,NULL,NULL,1,'短信渠道',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (138,1,3,'邮件','EMAIL','sys_message_channel',NULL,'success','N',NULL,NULL,NULL,1,'邮件渠道',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (139,1,4,'推送','PUSH','sys_message_channel',NULL,'info','N',NULL,NULL,NULL,1,'推送渠道',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (140,1,1,'指定人员','USERS','sys_message_send_scope',NULL,'info','Y',NULL,NULL,NULL,1,'发送给指定人员',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (141,1,2,'指定组织','ORG','sys_message_send_scope',NULL,'warning','N',NULL,NULL,NULL,1,'发送给指定组织',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (142,1,3,'全员','ALL','sys_message_send_scope',NULL,'success','N',NULL,NULL,NULL,1,'发送给全员',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (143,1,1,'草稿','0','sys_message_send_status',NULL,'default','Y',NULL,NULL,NULL,1,'草稿',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (144,1,2,'已发送','1','sys_message_send_status',NULL,'success','N',NULL,NULL,NULL,1,'已发送',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (145,1,3,'发送失败','2','sys_message_send_status',NULL,'error','N',NULL,NULL,NULL,1,'发送失败',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (146,1,1,'未读','0','sys_message_read_status',NULL,'error','Y',NULL,NULL,NULL,1,'未读',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (147,1,2,'已读','1','sys_message_read_status',NULL,'success','N',NULL,NULL,NULL,1,'已读',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (177,1,1,'动态表单','dynamic','flow_form_type',NULL,'info','Y',NULL,NULL,NULL,1,'动态表单',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (178,1,2,'外置表单','external','flow_form_type',NULL,'warning','N',NULL,NULL,NULL,1,'外置表单',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (179,1,3,'内置表单','builtin','flow_form_type',NULL,'success','N',NULL,NULL,NULL,1,'内置表单',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (180,1,1,'动态表单','dynamic','flow_process_form_type',NULL,'info','Y',NULL,NULL,NULL,1,'动态表单',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (181,1,2,'外置表单','external','flow_process_form_type',NULL,'warning','N',NULL,NULL,NULL,1,'外置表单',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (182,1,3,'无表单','none','flow_process_form_type',NULL,'default','N',NULL,NULL,NULL,1,'无表单',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (183,1,1,'设计中','0','flow_model_status',NULL,'info','Y',NULL,NULL,NULL,1,'设计中',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (184,1,2,'已部署','1','flow_model_status',NULL,'success','N',NULL,NULL,NULL,1,'已部署',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (185,1,3,'已挂起','2','flow_model_status',NULL,'warning','N',NULL,NULL,NULL,1,'已挂起',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (186,1,4,'已禁用','3','flow_model_status',NULL,'error','N',NULL,NULL,NULL,1,'已禁用',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (187,1,1,'通用','general','flow_spel_category',NULL,'default','Y',NULL,NULL,NULL,1,'通用',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (188,1,2,'部门','dept','flow_spel_category',NULL,'info','N',NULL,NULL,NULL,1,'部门',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (189,1,3,'角色','role','flow_spel_category',NULL,'success','N',NULL,NULL,NULL,1,'角色',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (190,1,4,'行政区划','region','flow_spel_category',NULL,'warning','N',NULL,NULL,NULL,1,'行政区划',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (191,1,5,'自定义','custom','flow_spel_category',NULL,'default','N',NULL,NULL,NULL,1,'自定义',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (192,1,1,'待办','0','flow_todo_status',NULL,'warning','Y',NULL,NULL,NULL,1,'待办',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (193,1,2,'已签收','1','flow_todo_status',NULL,'info','N',NULL,NULL,NULL,1,'已签收',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (194,1,1,'已通过','2','flow_done_status',NULL,'success','Y',NULL,NULL,NULL,1,'已通过',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (195,1,2,'已驳回','3','flow_done_status',NULL,'error','N',NULL,NULL,NULL,1,'已驳回',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (196,1,3,'已转办','4','flow_done_status',NULL,'warning','N',NULL,NULL,NULL,1,'已转办',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (197,1,4,'已委派','5','flow_done_status',NULL,'info','N',NULL,NULL,NULL,1,'已委派',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (198,1,5,'已撤回','6','flow_done_status',NULL,'default','N',NULL,NULL,NULL,1,'已撤回',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (199,1,1,'审批中','0','flow_started_status',NULL,'warning','Y',NULL,NULL,NULL,1,'审批中',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (200,1,2,'已签收','1','flow_started_status',NULL,'info','N',NULL,NULL,NULL,1,'已签收',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (201,1,3,'已通过','2','flow_started_status',NULL,'success','N',NULL,NULL,NULL,1,'已通过',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (202,1,4,'已驳回','3','flow_started_status',NULL,'error','N',NULL,NULL,NULL,1,'已驳回',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (203,1,5,'已转办','4','flow_started_status',NULL,'warning','N',NULL,NULL,NULL,1,'已转办',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (204,1,6,'已委派','5','flow_started_status',NULL,'info','N',NULL,NULL,NULL,1,'已委派',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (205,1,7,'已撤回','6','flow_started_status',NULL,'default','N',NULL,NULL,NULL,1,'已撤回',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (206,1,1,'运行中','running','flow_instance_status',NULL,'success','Y',NULL,NULL,NULL,1,'运行中',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (207,1,2,'已挂起','suspended','flow_instance_status',NULL,'warning','N',NULL,NULL,NULL,1,'已挂起',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (208,1,3,'已通过','approved','flow_instance_status',NULL,'success','N',NULL,NULL,NULL,1,'已通过',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (209,1,4,'已驳回','rejected','flow_instance_status',NULL,'error','N',NULL,NULL,NULL,1,'已驳回',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (210,1,5,'已取消','canceled','flow_instance_status',NULL,'default','N',NULL,NULL,NULL,1,'已取消',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (211,1,6,'已终止','terminated','flow_instance_status',NULL,'error','N',NULL,NULL,NULL,1,'已终止',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (212,1,1,'未处理','0','flow_error_log_status',NULL,'error','Y',NULL,NULL,NULL,1,'未处理',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (213,1,2,'已重试','1','flow_error_log_status',NULL,'success','N',NULL,NULL,NULL,1,'已重试',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (214,1,3,'已解决','2','flow_error_log_status',NULL,'default','N',NULL,NULL,NULL,1,'已解决',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (215,1,4,'重试失败','3','flow_error_log_status',NULL,'warning','N',NULL,NULL,NULL,1,'重试失败',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (216,1,1,'未读','0','flow_read_status',NULL,'error','Y',NULL,NULL,NULL,1,'未读',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (217,1,2,'已读','1','flow_read_status',NULL,'success','N',NULL,NULL,NULL,1,'已读',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (218,1,1,'低','0','flow_priority',NULL,'default','N',NULL,NULL,NULL,1,'低优先级',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (219,1,2,'普通','1','flow_priority',NULL,'default','Y',NULL,NULL,NULL,1,'普通优先级',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (220,1,3,'高','2','flow_priority',NULL,'warning','N',NULL,NULL,NULL,1,'高优先级',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (221,1,4,'紧急','3','flow_priority',NULL,'error','N',NULL,NULL,NULL,1,'紧急优先级',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (222,1,1,'处理条件','approval','flow_rule_type',NULL,'info','Y',NULL,NULL,NULL,1,'处理条件',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (223,1,2,'流转条件','flow','flow_rule_type',NULL,'success','N',NULL,NULL,NULL,1,'流转条件',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (224,1,3,'通知条件','notify','flow_rule_type',NULL,'warning','N',NULL,NULL,NULL,1,'通知条件',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (225,1,1,'等于','eq','flow_rule_operator',NULL,'default','Y',NULL,NULL,NULL,1,'等于',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (226,1,2,'不等于','ne','flow_rule_operator',NULL,'default','N',NULL,NULL,NULL,1,'不等于',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (227,1,3,'大于','gt','flow_rule_operator',NULL,'default','N',NULL,NULL,NULL,1,'大于',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (228,1,4,'大于等于','ge','flow_rule_operator',NULL,'default','N',NULL,NULL,NULL,1,'大于等于',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (229,1,5,'小于','lt','flow_rule_operator',NULL,'default','N',NULL,NULL,NULL,1,'小于',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (230,1,6,'小于等于','le','flow_rule_operator',NULL,'default','N',NULL,NULL,NULL,1,'小于等于',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (231,1,7,'包含','contains','flow_rule_operator',NULL,'default','N',NULL,NULL,NULL,1,'包含',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (232,1,8,'为空','empty','flow_rule_operator',NULL,'default','N',NULL,NULL,NULL,1,'为空',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (233,1,9,'不为空','notEmpty','flow_rule_operator',NULL,'default','N',NULL,NULL,NULL,1,'不为空',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (240,1,1,'MySQL','MYSQL','data_db_type',NULL,'success','Y',NULL,NULL,NULL,1,'MySQL数据库',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0),
	 (241,1,2,'Oracle','ORACLE','data_db_type',NULL,'default','N',NULL,NULL,NULL,1,'Oracle数据库',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0),
	 (242,1,3,'PostgreSQL','POSTGRESQL','data_db_type',NULL,'info','N',NULL,NULL,NULL,1,'PostgreSQL数据库',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0),
	 (243,1,4,'SQLServer','SQLSERVER','data_db_type',NULL,'warning','N',NULL,NULL,NULL,1,'SQLServer数据库',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0),
	 (244,1,1,'手动维护','MANUAL','data_dimension_source_type',NULL,'info','Y',NULL,NULL,NULL,1,'手动维护维度值',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0),
	 (245,1,2,'SQL同步','SQL','data_dimension_source_type',NULL,'warning','N',NULL,NULL,NULL,1,'从数据源同步维度值',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0),
	 (246,1,1,'单表数据集','TABLE','data_dataset_type',NULL,'info','Y',NULL,NULL,NULL,1,'单表数据集',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0),
	 (247,1,2,'SQL数据集','SQL','data_dataset_type',NULL,'warning','N',NULL,NULL,NULL,1,'SQL数据集',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0),
	 (248,1,1,'未发布','0','data_dataset_publish_status',NULL,'default','Y',NULL,NULL,NULL,1,'草稿状态',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (249,1,2,'已发布','1','data_dataset_publish_status',NULL,'success','N',NULL,NULL,NULL,1,'已发布状态',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0),
	 (250,1,3,'已下架','2','data_dataset_publish_status',NULL,'warning','N',NULL,NULL,NULL,1,'已下架状态',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0),
	 (251,1,1,'公开','PUBLIC','data_dataset_access_mode',NULL,'success','Y',NULL,NULL,NULL,1,'公开访问',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0),
	 (252,1,2,'私有','PRIVATE','data_dataset_access_mode',NULL,'warning','N',NULL,NULL,NULL,1,'私有访问',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0),
	 (253,1,1,'角色','ROLE','data_acl_subject_type',NULL,'info','Y',NULL,NULL,NULL,1,'角色授权',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0),
	 (254,1,2,'用户','USER','data_acl_subject_type',NULL,'success','N',NULL,NULL,NULL,1,'用户授权',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0),
	 (255,1,3,'组织','ORG','data_acl_subject_type',NULL,'warning','N',NULL,NULL,NULL,1,'组织授权',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0),
	 (256,1,1,'查看','VIEW','data_acl_access_level',NULL,'info','Y',NULL,NULL,NULL,1,'查看权限',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0),
	 (257,1,2,'查询','QUERY','data_acl_access_level',NULL,'success','N',NULL,NULL,NULL,1,'查询权限',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0),
	 (258,1,3,'管理','MANAGE','data_acl_access_level',NULL,'warning','N',NULL,NULL,NULL,1,'管理权限',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (259,1,1,'维度','DIMENSION','data_field_role',NULL,'info','Y',NULL,NULL,NULL,1,'维度字段',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0),
	 (260,1,2,'指标','MEASURE','data_field_role',NULL,'success','N',NULL,NULL,NULL,1,'指标字段',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0),
	 (261,1,1,'不脱敏','NONE','data_field_sensitive_level',NULL,'default','Y',NULL,NULL,NULL,1,'不脱敏',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0),
	 (262,1,2,'脱敏展示','MASK','data_field_sensitive_level',NULL,'warning','N',NULL,NULL,NULL,1,'脱敏展示',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0),
	 (263,1,3,'隐藏字段','HIDDEN','data_field_sensitive_level',NULL,'error','N',NULL,NULL,NULL,1,'隐藏字段',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0),
	 (271,1,1,'AI大屏生成','dashboard_generate','ai_prompt_usage_scene',NULL,'success','Y',NULL,NULL,NULL,1,'AI大屏生成场景',NULL,'2026-05-21 12:52:23',NULL,'2026-05-21 12:52:23',NULL,0),
	 (272,1,2,'通用对话','chat','ai_prompt_usage_scene',NULL,'info','N',NULL,NULL,NULL,1,'通用对话场景',NULL,'2026-05-21 12:52:23',NULL,'2026-05-21 12:52:23',NULL,0),
	 (273,1,3,'代码生成','code_generate','ai_prompt_usage_scene',NULL,'warning','N',NULL,NULL,NULL,1,'代码生成场景',NULL,'2026-05-21 12:52:23',NULL,'2026-05-21 12:52:23',NULL,0),
	 (274,1,4,'流程编排','flow_design','ai_prompt_usage_scene',NULL,'primary','N',NULL,NULL,NULL,1,'流程编排场景',NULL,'2026-05-21 12:52:23',NULL,'2026-05-21 12:52:23',NULL,0),
	 (275,1,1,'启用','0','ai_prompt_status',NULL,'success','Y',NULL,NULL,NULL,1,'启用状态',NULL,'2026-05-21 12:52:23',NULL,'2026-05-21 12:52:23',NULL,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (276,1,2,'停用','1','ai_prompt_status',NULL,'error','N',NULL,NULL,NULL,1,'停用状态',NULL,'2026-05-21 12:52:23',NULL,'2026-05-21 12:52:23',NULL,0),
	 (277,1,1,'推荐','1','ai_prompt_recommended',NULL,'success','N',NULL,NULL,NULL,1,'推荐模板',NULL,'2026-05-21 12:52:23',NULL,'2026-05-21 12:52:23',NULL,0),
	 (278,1,2,'普通','0','ai_prompt_recommended',NULL,'default','Y',NULL,NULL,NULL,1,'普通模板',NULL,'2026-05-21 12:52:23',NULL,'2026-05-21 12:52:23',NULL,0),
	 (279,1,1,'成功','success','ai_dashboard_generate_status',NULL,'success','Y',NULL,NULL,NULL,1,'生成成功',NULL,'2026-05-21 12:52:23',NULL,'2026-05-21 12:52:23',NULL,0),
	 (280,1,2,'失败','failed','ai_dashboard_generate_status',NULL,'error','N',NULL,NULL,NULL,1,'生成失败',NULL,'2026-05-21 12:52:23',NULL,'2026-05-21 12:52:23',NULL,0),
	 (281,1,3,'解析失败','parse_failed','ai_dashboard_generate_status',NULL,'warning','N',NULL,NULL,NULL,1,'解析失败',NULL,'2026-05-21 12:52:23',NULL,'2026-05-21 12:52:23',NULL,0),
	 (282,1,4,'已停止','stopped','ai_dashboard_generate_status',NULL,'default','N',NULL,NULL,NULL,1,'已停止',NULL,'2026-05-21 12:52:23',NULL,'2026-05-21 12:52:23',NULL,0),
	 (283,1,1,'草稿','DRAFT','lowcode_app_publish_status',NULL,'default','Y',NULL,NULL,NULL,1,'草稿状态',NULL,'2026-05-21 12:52:23',NULL,'2026-05-21 12:52:23',NULL,0),
	 (284,1,2,'已发布','PUBLISHED','lowcode_app_publish_status',NULL,'success','N',NULL,NULL,NULL,1,'已发布状态',NULL,'2026-05-21 12:52:23',NULL,'2026-05-21 12:52:23',NULL,0),
	 (285,1,3,'已停用','STOPPED','lowcode_app_publish_status',NULL,'warning','N',NULL,NULL,NULL,1,'已停用状态',NULL,'2026-05-21 12:52:23',NULL,'2026-05-21 12:52:23',NULL,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (286,1,1,'启用','ENABLED','lowcode_model_status',NULL,'success','Y',NULL,NULL,NULL,1,'启用状态',NULL,'2026-05-21 12:52:23',NULL,'2026-05-21 12:52:23',NULL,0),
	 (287,1,2,'停用','DISABLED','lowcode_model_status',NULL,'warning','N',NULL,NULL,NULL,1,'停用状态',NULL,'2026-05-21 12:52:23',NULL,'2026-05-21 12:52:23',NULL,0),
	 (288,1,1,'SPEC','SPEC','ai_context_type',NULL,'info','Y',NULL,NULL,NULL,1,'SPEC类型',NULL,'2026-05-21 12:52:23',NULL,'2026-05-21 12:52:23',NULL,0),
	 (289,1,2,'SAMPLE','SAMPLE','ai_context_type',NULL,'success','N',NULL,NULL,NULL,1,'SAMPLE类型',NULL,'2026-05-21 12:52:23',NULL,'2026-05-21 12:52:23',NULL,0),
	 (290,1,3,'RULE','RULE','ai_context_type',NULL,'warning','N',NULL,NULL,NULL,1,'RULE类型',NULL,'2026-05-21 12:52:23',NULL,'2026-05-21 12:52:23',NULL,0),
	 (302,1,1,'登录','LOGIN','sys_login_type',NULL,'success','Y',NULL,NULL,NULL,1,'用户登录',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (303,1,2,'登出','LOGOUT','sys_login_type',NULL,'default','N',NULL,NULL,NULL,1,'用户登出',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (304,1,3,'注册','REGISTER','sys_login_type',NULL,'info','N',NULL,NULL,NULL,1,'用户注册',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (305,1,4,'被踢下线','KICKOUT','sys_login_type',NULL,'error','N',NULL,NULL,NULL,1,'被踢下线',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (306,1,5,'被顶下线','REPLACED','sys_login_type',NULL,'warning','N',NULL,NULL,NULL,1,'被顶下线',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (307,1,1,'桌面端','pc','sys_client_type',NULL,'info','Y',NULL,NULL,NULL,1,'PC桌面端',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (308,1,2,'移动APP','app','sys_client_type',NULL,'success','N',NULL,NULL,NULL,1,'移动应用',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (309,1,3,'网页H5','h5','sys_client_type',NULL,'warning','N',NULL,NULL,NULL,1,'H5网页',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (310,1,4,'微信','wechat','sys_client_type',NULL,'error','N',NULL,NULL,NULL,1,'微信客户端',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (311,1,1,'成功','1','sys_common_status',NULL,'success','Y',NULL,NULL,NULL,1,'成功状态',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (312,1,2,'失败','0','sys_common_status',NULL,'error','N',NULL,NULL,NULL,1,'失败状态',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (313,1,1,'查询','QUERY','sys_operation_type',NULL,'info','Y',NULL,NULL,NULL,1,'查询操作',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (314,1,2,'新增','INSERT','sys_operation_type',NULL,'success','N',NULL,NULL,NULL,0,'历史 INSERT 新增类型已归并到 ADD',NULL,'2026-05-21 12:54:11',1,'2026-07-23 09:24:01',NULL,314),
	 (315,1,3,'更新','UPDATE','sys_operation_type',NULL,'warning','N',NULL,NULL,NULL,1,'更新操作',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (316,1,4,'删除','DELETE','sys_operation_type',NULL,'error','N',NULL,NULL,NULL,1,'删除操作',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (317,1,5,'导入','IMPORT','sys_operation_type',NULL,'info','N',NULL,NULL,NULL,1,'导入操作',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (318,1,6,'导出','EXPORT','sys_operation_type',NULL,'success','N',NULL,NULL,NULL,1,'导出操作',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (319,1,7,'其他','OTHER','sys_operation_type',NULL,'default','N',NULL,NULL,NULL,1,'其他操作',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (320,1,1,'停止','0','sys_job_status',NULL,'default','Y',NULL,NULL,NULL,1,'任务停止',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (321,1,2,'运行中','1','sys_job_status',NULL,'success','N',NULL,NULL,NULL,1,'任务运行',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (322,1,1,'Bean模式（本地调用）','BEAN','sys_job_run_mode',NULL,'info','Y',NULL,NULL,NULL,1,'Bean模式',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (323,1,2,'Handler模式（扩展调用）','HANDLER','sys_job_run_mode',NULL,'success','N',NULL,NULL,NULL,1,'Handler模式',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (324,1,1,'GET','GET','sys_req_method',NULL,'info','Y',NULL,NULL,NULL,1,'GET请求',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (325,1,2,'POST','POST','sys_req_method',NULL,'success','N',NULL,NULL,NULL,1,'POST请求',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (326,1,3,'PUT','PUT','sys_req_method',NULL,'warning','N',NULL,NULL,NULL,1,'PUT请求',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (327,1,4,'DELETE','DELETE','sys_req_method',NULL,'error','N',NULL,NULL,NULL,1,'DELETE请求',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (328,1,5,'ALL','ALL','sys_req_method',NULL,'default','N',NULL,NULL,NULL,1,'所有方法',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (329,1,1,'目录','1','sys_resource_type',NULL,'info','Y',NULL,NULL,NULL,1,'目录类型',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (330,1,2,'菜单','2','sys_resource_type',NULL,'success','N',NULL,NULL,NULL,1,'菜单类型',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (331,1,3,'按钮','3','sys_resource_type',NULL,'warning','N',NULL,NULL,NULL,1,'按钮类型',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (332,1,4,'API接口','4','sys_resource_type',NULL,'error','N',NULL,NULL,NULL,1,'API接口类型',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (333,1,1,'显示','1','sys_show_hide',NULL,'success','Y',NULL,NULL,NULL,1,'显示状态',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (334,1,2,'隐藏','0','sys_show_hide',NULL,'default','N',NULL,NULL,NULL,1,'隐藏状态',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (335,1,1,'当前页','_self','sys_link_open_target',NULL,'info','Y',NULL,NULL,NULL,1,'当前页面打开',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (336,1,2,'新窗口','_blank','sys_link_open_target',NULL,'success','N',NULL,NULL,NULL,1,'新窗口打开',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (337,1,1,'是','Y','sys_config_type',NULL,'success','N',NULL,NULL,NULL,1,'系统内置参数',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (338,1,2,'否','N','sys_config_type',NULL,'default','Y',NULL,NULL,NULL,1,'非内置参数',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (339,1,1,'默认','default','sys_file_group_type',NULL,'default','Y',NULL,NULL,NULL,1,'默认分组',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (340,1,2,'文档','document','sys_file_group_type',NULL,'info','N',NULL,NULL,NULL,1,'文档分组',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (341,1,3,'图片','image','sys_file_group_type',NULL,'success','N',NULL,NULL,NULL,1,'图片分组',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (342,1,4,'视频','video','sys_file_group_type',NULL,'warning','N',NULL,NULL,NULL,1,'视频分组',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (343,1,5,'音频','audio','sys_file_group_type',NULL,'info','N',NULL,NULL,NULL,1,'音频分组',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (344,1,6,'压缩包','archive','sys_file_group_type',NULL,'default','N',NULL,NULL,NULL,1,'压缩包分组',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (365,1,1,'成功','1','order_status1',NULL,'default','N',0,NULL,NULL,1,NULL,1,'2026-05-22 11:22:04',1,'2026-05-22 11:22:04',2,0),
	 (366,1,2,'失败','2','order_status1',NULL,'info','N',0,NULL,NULL,1,NULL,1,'2026-05-22 11:22:04',1,'2026-05-22 11:22:04',2,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (367,1,8,'已退回','7','flow_todo_status',NULL,'warning','N',NULL,NULL,NULL,1,'审批任务已退回上一节点',NULL,'2026-05-23 21:58:22',NULL,'2026-05-23 21:58:22',NULL,0),
	 (368,1,9,'已终结','8','flow_todo_status',NULL,'error','N',NULL,NULL,NULL,1,'审批人终结流程',NULL,'2026-05-23 21:58:22',NULL,'2026-05-23 21:58:22',NULL,0),
	 (369,1,6,'已退回','7','flow_done_status',NULL,'warning','N',NULL,NULL,NULL,1,'审批任务已退回上一节点',NULL,'2026-05-23 21:58:22',NULL,'2026-05-23 21:58:22',NULL,0),
	 (370,1,7,'已终结','8','flow_done_status',NULL,'error','N',NULL,NULL,NULL,1,'审批人终结流程',NULL,'2026-05-23 21:58:22',NULL,'2026-05-23 21:58:22',NULL,0),
	 (375,1,1,'仅导出','EXPORT','sys_excel_config_type',NULL,'info','N',NULL,NULL,NULL,1,'仅用于导出',NULL,'2026-05-27 13:01:00',NULL,'2026-05-27 13:01:00',NULL,0),
	 (376,1,2,'仅导入','IMPORT','sys_excel_config_type',NULL,'warning','N',NULL,NULL,NULL,1,'仅用于导入',NULL,'2026-05-27 13:01:00',NULL,'2026-05-27 13:01:00',NULL,0),
	 (377,1,3,'导入导出','BOTH','sys_excel_config_type',NULL,'success','Y',NULL,NULL,NULL,1,'同时用于导入和导出',NULL,'2026-05-27 13:01:00',NULL,'2026-05-27 13:01:00',NULL,0),
	 (378,1,1,'CRM','CRM','ai_business_suite',NULL,'success','Y',NULL,NULL,NULL,1,'客户关系管理业务套件',1,'2026-05-27 13:01:02',1,'2026-05-27 13:01:02',1,0),
	 (379,1,2,'合同','CONTRACT','ai_business_suite',NULL,'info','N',NULL,NULL,NULL,1,'合同管理业务套件',1,'2026-05-27 13:01:02',1,'2026-05-27 13:01:02',1,0),
	 (380,1,3,'项目','PROJECT','ai_business_suite',NULL,'primary','N',NULL,NULL,NULL,1,'项目管理业务套件',1,'2026-05-27 13:01:02',1,'2026-05-27 13:01:02',1,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (381,1,4,'采购','PURCHASE','ai_business_suite',NULL,'warning','N',NULL,NULL,NULL,1,'采购管理业务套件',1,'2026-05-27 13:01:02',1,'2026-05-27 13:01:02',1,0),
	 (382,1,5,'服务','SERVICE','ai_business_suite',NULL,'default','N',NULL,NULL,NULL,1,'服务工单业务套件',1,'2026-05-27 13:01:02',1,'2026-05-27 13:01:02',1,0),
	 (383,1,1,'主数据对象','MASTER','ai_business_object_type',NULL,'success','Y',NULL,NULL,NULL,1,'客户、线索等主数据对象',1,'2026-05-27 13:01:02',1,'2026-05-27 13:01:02',1,0),
	 (384,1,2,'明细对象','DETAIL','ai_business_object_type',NULL,'info','N',NULL,NULL,NULL,1,'合同明细、跟进记录等明细对象',1,'2026-05-27 13:01:02',1,'2026-05-27 13:01:02',1,0),
	 (385,1,3,'查找对象','LOOKUP','ai_business_object_type',NULL,'default','N',NULL,NULL,NULL,1,'字典式或引用型对象',1,'2026-05-27 13:01:02',1,'2026-05-27 13:01:02',1,0),
	 (386,1,4,'业务单据','TRANSACTION','ai_business_object_type',NULL,'warning','N',NULL,NULL,NULL,1,'商机、合同、回款等业务单据',1,'2026-05-27 13:01:02',1,'2026-05-27 13:01:02',1,0),
	 (387,1,1,'引用关系','REFERENCE','ai_business_relation_type',NULL,'info','Y',NULL,NULL,NULL,1,'对象通过业务字段引用另一个对象',1,'2026-05-27 13:01:02',1,'2026-05-27 13:01:02',1,0),
	 (388,1,2,'明细关系','DETAIL','ai_business_relation_type',NULL,'success','N',NULL,NULL,NULL,1,'明细对象依附主对象',1,'2026-05-27 13:01:02',1,'2026-05-27 13:01:02',1,0),
	 (389,1,3,'关联列表','CHILD_LIST','ai_business_relation_type',NULL,'primary','N',NULL,NULL,NULL,1,'详情页展示下级或关联对象列表',1,'2026-05-27 13:01:02',1,'2026-05-27 13:01:02',1,0),
	 (390,1,4,'多对多关系','MANY_TO_MANY','ai_business_relation_type',NULL,'warning','N',NULL,NULL,NULL,1,'通过中间关系表达多对多关联',1,'2026-05-27 13:01:02',1,'2026-05-27 13:01:02',1,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (391,1,1,'业务应用','BUSINESS','ai_business_app_type',NULL,'success','Y',NULL,NULL,NULL,1,'客户管理、合同管理等标准业务入口',1,'2026-05-27 13:01:02',1,'2026-05-27 13:01:02',1,0),
	 (392,1,2,'嵌入应用','EMBEDDED','ai_business_app_type',NULL,'info','N',NULL,NULL,NULL,1,'大屏、BI、外部页面等嵌入入口',1,'2026-05-27 13:01:02',1,'2026-05-27 13:01:02',1,0),
	 (393,1,3,'移动应用','MOBILE','ai_business_app_type',NULL,'primary','N',NULL,NULL,NULL,1,'H5、移动审批和移动业务入口',1,'2026-05-27 13:01:02',1,'2026-05-27 13:01:02',1,0),
	 (394,1,4,'集成应用','INTEGRATION','ai_business_app_type',NULL,'warning','N',NULL,NULL,NULL,1,'开放接口、Webhook、第三方平台连接',1,'2026-05-27 13:01:02',1,'2026-05-27 13:01:02',1,0),
	 (395,1,1,'业务页面','RUNTIME','ai_business_app_entry_mode',NULL,'success','Y',NULL,NULL,NULL,1,'打开平台托管的业务页面',1,'2026-05-27 13:01:02',1,'2026-06-14 21:18:18',1,0),
	 (396,1,2,'内部路由','ROUTE','ai_business_app_entry_mode',NULL,'info','N',NULL,NULL,NULL,1,'打开平台内部路由',1,'2026-05-27 13:01:02',1,'2026-05-27 13:01:02',1,0),
	 (397,1,3,'内嵌页面','IFRAME','ai_business_app_entry_mode',NULL,'primary','N',NULL,NULL,NULL,1,'通过iframe内嵌访问',1,'2026-05-27 13:01:02',1,'2026-05-27 13:01:02',1,0),
	 (398,1,4,'外部打开','EXTERNAL','ai_business_app_entry_mode',NULL,'warning','N',NULL,NULL,NULL,1,'新窗口打开外部地址',1,'2026-05-27 13:01:02',1,'2026-05-27 13:01:02',1,0),
	 (399,1,5,'H5 入口','H5','ai_business_app_entry_mode',NULL,'default','N',NULL,NULL,NULL,1,'移动H5入口',1,'2026-05-27 13:01:02',1,'2026-06-14 21:18:18',1,0),
	 (400,1,6,'接口入口','API','ai_business_app_entry_mode',NULL,'info','N',NULL,NULL,NULL,1,'登记接口或集成资源',1,'2026-05-27 13:01:02',1,'2026-06-14 21:18:18',1,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (401,1,1,'流程','FLOW','ai_business_binding_type',NULL,'info','N',NULL,NULL,NULL,1,'流程能力',1,'2026-05-27 13:01:02',1,'2026-05-27 13:01:02',1,0),
	 (402,1,2,'审批','APPROVAL','ai_business_binding_type',NULL,'success','N',NULL,NULL,NULL,1,'审批能力',1,'2026-05-27 13:01:02',1,'2026-05-27 13:01:02',1,0),
	 (403,1,3,'报表','REPORT','ai_business_binding_type',NULL,'primary','N',NULL,NULL,NULL,1,'报表和看板能力',1,'2026-05-27 13:01:02',1,'2026-05-27 13:01:02',1,0),
	 (404,1,4,'权限','PERMISSION','ai_business_binding_type',NULL,'warning','N',NULL,NULL,NULL,1,'对象级权限能力',1,'2026-05-27 13:01:02',1,'2026-05-27 13:01:02',1,0),
	 (405,1,5,'消息','MESSAGE','ai_business_binding_type',NULL,'info','N',NULL,NULL,NULL,1,'站内信、邮件、短信等消息能力',1,'2026-05-27 13:01:02',1,'2026-05-27 13:01:02',1,0),
	 (406,1,6,'触发器','TRIGGER','ai_business_binding_type',NULL,'error','N',NULL,NULL,NULL,1,'轻量对象自动化能力',1,'2026-05-27 13:01:02',1,'2026-05-27 13:01:02',1,0),
	 (407,1,7,'导入','IMPORT','ai_business_binding_type',NULL,'default','N',NULL,NULL,NULL,1,'对象级导入能力',1,'2026-05-27 13:01:02',1,'2026-05-27 13:01:02',1,0),
	 (408,1,8,'导出','EXPORT','ai_business_binding_type',NULL,'default','N',NULL,NULL,NULL,1,'对象级导出能力',1,'2026-05-27 13:01:02',1,'2026-05-27 13:01:02',1,0),
	 (409,1,9,'移动','MOBILE','ai_business_binding_type',NULL,'primary','N',NULL,NULL,NULL,1,'移动端入口能力',1,'2026-05-27 13:01:02',1,'2026-05-27 13:01:02',1,0),
	 (410,1,10,'集成','INTEGRATION','ai_business_binding_type',NULL,'warning','N',NULL,NULL,NULL,1,'第三方平台和开放接口能力',1,'2026-05-27 13:01:02',1,'2026-05-27 13:01:02',1,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (441,1,1,'1级','1','crm_customer_level',NULL,'success','N',0,NULL,NULL,1,NULL,1,'2026-05-30 07:41:38',1,'2026-05-30 07:41:38',2,0),
	 (442,1,2,'2级','2','crm_customer_level',NULL,'error','N',0,NULL,NULL,1,NULL,1,'2026-05-30 07:41:38',1,'2026-05-30 07:41:38',2,0),
	 (443,1,1,'管理角色','1','sys_role_type',NULL,'info','Y',NULL,NULL,NULL,1,'用于系统管理的角色',NULL,'2026-06-01 16:10:54',NULL,'2026-06-01 16:10:54',NULL,0),
	 (444,1,2,'业务角色','2','sys_role_type',NULL,'success','N',NULL,NULL,NULL,1,'用于业务操作的角色',NULL,'2026-06-01 16:10:54',NULL,'2026-06-01 16:10:54',NULL,0),
	 (445,1,3,'审批角色','3','sys_role_type',NULL,'warning','N',NULL,NULL,NULL,1,'用于审批流程的角色',NULL,'2026-06-01 16:10:54',NULL,'2026-06-01 16:10:54',NULL,0),
	 (446,1,4,'数据角色','4','sys_role_type',NULL,'default','N',NULL,NULL,NULL,1,'用于数据权限控制的角色',NULL,'2026-06-01 16:10:54',NULL,'2026-06-01 16:10:54',NULL,0),
	 (450,1,1,'草稿','DRAFT','business_document_status',NULL,'default','Y',NULL,NULL,NULL,1,'草稿状态',NULL,'2026-06-02 06:48:46',NULL,'2026-06-02 06:48:46',NULL,0),
	 (451,1,2,'已提交','SUBMITTED','business_document_status',NULL,'info','N',NULL,NULL,NULL,1,'已提交状态',NULL,'2026-06-02 06:48:46',NULL,'2026-06-02 06:48:46',NULL,0),
	 (452,1,3,'流程中','IN_PROCESS','business_document_status',NULL,'warning','N',NULL,NULL,NULL,1,'流程流转中',NULL,'2026-06-02 06:48:46',NULL,'2026-06-02 06:48:46',NULL,0),
	 (453,1,4,'已通过','APPROVED','business_document_status',NULL,'success','N',NULL,NULL,NULL,1,'流程通过',NULL,'2026-06-02 06:48:46',NULL,'2026-06-02 06:48:46',NULL,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (454,1,5,'已驳回','REJECTED','business_document_status',NULL,'error','N',NULL,NULL,NULL,1,'流程驳回',NULL,'2026-06-02 06:48:46',NULL,'2026-06-02 06:48:46',NULL,0),
	 (455,1,6,'已撤回','CANCELED','business_document_status',NULL,'default','N',NULL,NULL,NULL,1,'流程撤回',NULL,'2026-06-02 06:48:46',NULL,'2026-06-02 06:48:46',NULL,0),
	 (456,1,7,'已关闭','CLOSED','business_document_status',NULL,'default','N',NULL,NULL,NULL,1,'业务关闭',NULL,'2026-06-02 06:48:46',NULL,'2026-06-02 06:48:46',NULL,0),
	 (457,1,1,'待交接','PENDING','hr_leave_handover_status',NULL,'warning','Y',NULL,NULL,NULL,1,'待交接状态',NULL,'2026-06-02 06:48:47',NULL,'2026-06-02 06:48:47',NULL,0),
	 (458,1,2,'交接中','PROCESSING','hr_leave_handover_status',NULL,'info','N',NULL,NULL,NULL,1,'交接中状态',NULL,'2026-06-02 06:48:47',NULL,'2026-06-02 06:48:47',NULL,0),
	 (459,1,3,'已完成','COMPLETED','hr_leave_handover_status',NULL,'success','N',NULL,NULL,NULL,1,'已完成状态',NULL,'2026-06-02 06:48:47',NULL,'2026-06-02 06:48:47',NULL,0),
	 (460,1,1,'仅流程快照','PROCESS_ONLY','sys_flow_data_mode',NULL,'info','Y',NULL,NULL,NULL,1,'只保存流程变量和表单实例快照',1,'2026-06-06 16:36:36',1,'2026-06-06 16:36:36',1,0),
	 (461,1,2,'业务对象落表','BUSINESS_OBJECT','sys_flow_data_mode',NULL,'success','N',NULL,NULL,NULL,1,'写入业务对象实际数据表后发起流程',1,'2026-06-06 16:36:36',1,'2026-06-06 16:36:36',1,0),
	 (462,1,3,'混合模式','HYBRID','sys_flow_data_mode',NULL,'warning','N',NULL,NULL,NULL,1,'同时保存业务记录和流程表单快照',1,'2026-06-06 16:36:36',1,'2026-06-06 16:36:36',1,0),
	 (463,1,1,'启用','1','sys_flow_entry_status',NULL,'success','Y',NULL,NULL,NULL,1,'入口启用',1,'2026-06-06 16:36:36',1,'2026-06-06 16:36:36',1,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (464,1,2,'禁用','0','sys_flow_entry_status',NULL,'default','N',NULL,NULL,NULL,1,'入口禁用',1,'2026-06-06 16:36:36',1,'2026-06-06 16:36:36',1,0),
	 (465,1,1,'草稿','DRAFT','sys_flow_batch_status',NULL,'default','Y',NULL,NULL,NULL,1,'批次配置草稿',1,'2026-06-06 16:36:36',1,'2026-06-06 16:36:36',1,0),
	 (466,1,2,'已发布','PUBLISHED','sys_flow_batch_status',NULL,'success','N',NULL,NULL,NULL,1,'批次已发布并生成填报明细',1,'2026-06-06 16:36:36',1,'2026-06-06 16:36:36',1,0),
	 (467,1,3,'已关闭','CLOSED','sys_flow_batch_status',NULL,'default','N',NULL,NULL,NULL,1,'批次已关闭',1,'2026-06-06 16:36:36',1,'2026-06-06 16:36:36',1,0),
	 (468,1,1,'待填报','PENDING','sys_flow_fill_status',NULL,'default','Y',NULL,NULL,NULL,1,'待组织负责人填报',1,'2026-06-06 16:36:36',1,'2026-06-06 16:36:36',1,0),
	 (469,1,2,'已提交','SUBMITTED','sys_flow_fill_status',NULL,'info','N',NULL,NULL,NULL,1,'负责人已提交',1,'2026-06-06 16:36:36',1,'2026-06-06 16:36:36',1,0),
	 (470,1,3,'审核中','RUNNING','sys_flow_fill_status',NULL,'warning','N',NULL,NULL,NULL,1,'流程审核中',1,'2026-06-06 16:36:36',1,'2026-06-06 16:36:36',1,0),
	 (471,1,4,'已通过','APPROVED','sys_flow_fill_status',NULL,'success','N',NULL,NULL,NULL,1,'审核通过',1,'2026-06-06 16:36:36',1,'2026-06-06 16:36:36',1,0),
	 (472,1,5,'已驳回','REJECTED','sys_flow_fill_status',NULL,'error','N',NULL,NULL,NULL,1,'审核驳回，可重新提交',1,'2026-06-06 16:36:36',1,'2026-06-06 16:36:36',1,0),
	 (473,1,6,'已逾期','OVERDUE','sys_flow_fill_status',NULL,'error','N',NULL,NULL,NULL,1,'超过截止时间未提交',1,'2026-06-06 16:36:36',1,'2026-06-06 16:36:36',1,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (479,0,0,'123','123','123',NULL,'default','N',NULL,NULL,NULL,1,NULL,1,'2026-06-10 12:59:19',1,'2026-06-10 12:59:19',2,0),
	 (481,1,1,'消防工程','fire_engineering','pm_project_type',NULL,'primary','Y',NULL,NULL,NULL,1,'消防工程',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (482,1,2,'消防维保','fire_maintenance','pm_project_type',NULL,'success','N',NULL,NULL,NULL,1,'消防维保',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (483,1,3,'消防检测','fire_inspection','pm_project_type',NULL,'warning','N',NULL,NULL,NULL,1,'消防检测',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (484,1,4,'消防维修','fire_repair','pm_project_type',NULL,'warning','N',NULL,NULL,NULL,1,'消防维修',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (485,1,5,'消防改造','fire_renovation','pm_project_type',NULL,'info','N',NULL,NULL,NULL,1,'消防改造',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (486,1,6,'材料单','material_order','pm_project_type',NULL,'default','N',NULL,NULL,NULL,1,'材料单',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (487,1,7,'物联网','iot','pm_project_type',NULL,'info','N',NULL,NULL,NULL,1,'物联网',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (488,1,8,'销售项目','sales_project','pm_project_type',NULL,'primary','N',NULL,NULL,NULL,1,'销售项目',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (489,1,9,'其他','other','pm_project_type',NULL,'default','N',NULL,NULL,NULL,1,'其他',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (490,1,1,'一分公司','branch_1','pm_implementation_unit',NULL,'primary','Y',NULL,NULL,NULL,1,'一分公司',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (491,1,2,'二分公司','branch_2','pm_implementation_unit',NULL,'success','N',NULL,NULL,NULL,1,'二分公司',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (492,1,3,'三分公司','branch_3','pm_implementation_unit',NULL,'warning','N',NULL,NULL,NULL,1,'三分公司',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (493,1,4,'总公司直属','headquarters','pm_implementation_unit',NULL,'info','N',NULL,NULL,NULL,1,'总公司直属',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (494,1,1,'决策人','decision_maker','pm_contact_role',NULL,'primary','Y',NULL,NULL,NULL,1,'决策人',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (495,1,2,'关键影响人','key_influencer','pm_contact_role',NULL,'info','N',NULL,NULL,NULL,1,'关键影响人',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (496,1,3,'使用者','user','pm_contact_role',NULL,'success','N',NULL,NULL,NULL,1,'使用者',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (497,1,4,'对接人','liaison','pm_contact_role',NULL,'default','N',NULL,NULL,NULL,1,'对接人',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (498,1,1,'有效','valid','pm_lead_status',NULL,'success','Y',NULL,NULL,NULL,1,'有效线索',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (499,1,2,'已转商机','converted','pm_lead_status',NULL,'primary','N',NULL,NULL,NULL,1,'已转商机',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (500,1,3,'无效','invalid','pm_lead_status',NULL,'default','N',NULL,NULL,NULL,1,'无效线索',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (501,1,1,'客户无需求','no_demand','pm_lead_invalid_reason',NULL,'default','N',NULL,NULL,NULL,1,'客户无需求',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (502,1,2,'竞品已签约','competitor_signed','pm_lead_invalid_reason',NULL,'error','N',NULL,NULL,NULL,1,'竞品已签约',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (503,1,3,'信息不实','false_info','pm_lead_invalid_reason',NULL,'warning','N',NULL,NULL,NULL,1,'信息不实',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (504,1,4,'预算不足','budget_insufficient','pm_lead_invalid_reason',NULL,'warning','N',NULL,NULL,NULL,1,'预算不足',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (505,1,5,'其他','other','pm_lead_invalid_reason',NULL,'default','N',NULL,NULL,NULL,1,'其他',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (506,1,1,'跟踪中','tracking','pm_opportunity_status',NULL,'success','Y',NULL,NULL,NULL,1,'持续跟踪',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (507,1,2,'输单','lost','pm_opportunity_status',NULL,'error','N',NULL,NULL,NULL,1,'输单归档',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (508,1,3,'转合同','contract','pm_opportunity_status',NULL,'primary','N',NULL,NULL,NULL,1,'转合同',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (509,1,4,'转投标待审核','bid_pending','pm_opportunity_status',NULL,'warning','N',NULL,NULL,NULL,1,'转投标待审核',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (510,1,5,'转投标','bid','pm_opportunity_status',NULL,'info','N',NULL,NULL,NULL,1,'已转投标',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (511,1,6,'无效','invalid','pm_opportunity_status',NULL,'default','N',NULL,NULL,NULL,1,'无效商机',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (512,1,1,'价格偏高','price_high','pm_opportunity_lost_reason',NULL,'warning','N',NULL,NULL,NULL,1,'价格偏高',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (513,1,2,'资质不足','qualification','pm_opportunity_lost_reason',NULL,'error','N',NULL,NULL,NULL,1,'资质不足',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (514,1,3,'方案不满足','solution_mismatch','pm_opportunity_lost_reason',NULL,'warning','N',NULL,NULL,NULL,1,'方案不满足',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (515,1,4,'关系不到位','relationship','pm_opportunity_lost_reason',NULL,'default','N',NULL,NULL,NULL,1,'关系不到位',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (516,1,5,'客户无需求','no_demand','pm_opportunity_lost_reason',NULL,'default','N',NULL,NULL,NULL,1,'客户无需求',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (517,1,6,'其他','other','pm_opportunity_lost_reason',NULL,'default','N',NULL,NULL,NULL,1,'其他',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (518,1,1,'电话','phone','pm_follow_type',NULL,'info','Y',NULL,NULL,NULL,1,'电话',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (519,1,2,'拜访','visit','pm_follow_type',NULL,'success','N',NULL,NULL,NULL,1,'拜访',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (520,1,3,'微信','wechat','pm_follow_type',NULL,'primary','N',NULL,NULL,NULL,1,'微信',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (521,1,4,'会议','meeting','pm_follow_type',NULL,'warning','N',NULL,NULL,NULL,1,'会议',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (522,1,5,'其他','other','pm_follow_type',NULL,'default','N',NULL,NULL,NULL,1,'其他',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (523,1,1,'未投标','not_bid','pm_bid_status',NULL,'default','Y',NULL,NULL,NULL,1,'未投标',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (524,1,2,'投标中','bidding','pm_bid_status',NULL,'warning','N',NULL,NULL,NULL,1,'投标中',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (525,1,3,'已中标','won','pm_bid_status',NULL,'success','N',NULL,NULL,NULL,1,'已中标',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (526,1,4,'未中标','lost','pm_bid_status',NULL,'default','N',NULL,NULL,NULL,1,'未中标',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (527,1,1,'价格偏高','price_high','pm_bid_lost_reason',NULL,'warning','N',NULL,NULL,NULL,1,'价格偏高',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (528,1,2,'资质不足','qualification','pm_bid_lost_reason',NULL,'error','N',NULL,NULL,NULL,1,'资质不足',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (529,1,3,'方案不满足','solution_mismatch','pm_bid_lost_reason',NULL,'warning','N',NULL,NULL,NULL,1,'方案不满足',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (530,1,4,'关系不到位','relationship','pm_bid_lost_reason',NULL,'default','N',NULL,NULL,NULL,1,'关系不到位',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (531,1,5,'其他','other','pm_bid_lost_reason',NULL,'default','N',NULL,NULL,NULL,1,'其他',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (544,0,0,'ABC','ABC','pm_bid_lost_reason','33','success','N',529,NULL,NULL,1,'234',1,'2026-06-14 02:11:52',1,'2026-06-14 02:12:05',2,0),
	 (545,1,1,'在线运行','DYNAMIC_RENDER','ai_business_app_mode',NULL,'success','Y',NULL,NULL,NULL,1,'在线搭建并由平台动态运行',1,'2026-06-14 21:18:18',1,'2026-06-14 21:18:18',1,0),
	 (546,1,2,'下载代码','CODE_DOWNLOAD','ai_business_app_mode',NULL,'primary','N',NULL,NULL,NULL,1,'下载完整功能代码后导入本地工程二次开发',1,'2026-06-14 21:18:18',1,'2026-06-14 21:18:18',1,0),
	 (547,1,1,'仅一次','once','sys_flow_overdue_repeat_mode',NULL,'info','Y',NULL,NULL,NULL,1,'逾期后仅提醒一次',1,'2026-06-21 10:43:46',1,'2026-06-21 10:43:46',1,0),
	 (548,1,2,'按间隔重复','interval','sys_flow_overdue_repeat_mode',NULL,'warning','N',NULL,NULL,NULL,1,'逾期后按配置间隔重复提醒',1,'2026-06-21 10:43:46',1,'2026-06-21 10:43:46',1,0),
	 (550,1,1,'草稿','DRAFT','sample_purchase_order_status',NULL,'default','Y',NULL,NULL,NULL,1,'草稿',NULL,'2026-06-28 15:07:29',NULL,'2026-06-28 15:07:29',NULL,0),
	 (551,1,2,'审批中','IN_PROCESS','sample_purchase_order_status',NULL,'info','N',NULL,NULL,NULL,1,'审批中',NULL,'2026-06-28 15:07:29',NULL,'2026-06-28 15:07:29',NULL,0),
	 (552,1,3,'待修改','NEED_MODIFY','sample_purchase_order_status',NULL,'warning','N',NULL,NULL,NULL,1,'被驳回后等待申请人修改',NULL,'2026-06-28 15:07:29',NULL,'2026-06-28 15:07:29',NULL,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (553,1,4,'已通过','APPROVED','sample_purchase_order_status',NULL,'success','N',NULL,NULL,NULL,1,'审批通过',NULL,'2026-06-28 15:07:29',NULL,'2026-06-28 15:07:29',NULL,0),
	 (554,1,5,'已拒绝','REJECTED','sample_purchase_order_status',NULL,'error','N',NULL,NULL,NULL,1,'流程拒绝结束',NULL,'2026-06-28 15:07:29',NULL,'2026-06-28 15:07:29',NULL,0),
	 (555,1,6,'已取消','CANCELED','sample_purchase_order_status',NULL,'default','N',NULL,NULL,NULL,1,'流程取消',NULL,'2026-06-28 15:07:29',NULL,'2026-06-28 15:07:29',NULL,0),
	 (557,0,1,'启用','1','marter_status',NULL,'success','N',0,NULL,NULL,1,NULL,1,'2026-07-01 14:55:31',1,'2026-07-01 14:55:31',6,0),
	 (558,0,2,'禁用','0','marter_status',NULL,'error','N',0,NULL,NULL,1,NULL,1,'2026-07-01 14:55:31',1,'2026-07-01 14:55:31',6,0),
	 (559,0,1,'中心仓库','1','workhosue_type',NULL,'success','N',0,NULL,NULL,1,NULL,1,'2026-07-01 17:00:01',1,'2026-07-01 17:00:01',6,0),
	 (560,0,2,'项目仓库','0','workhosue_type',NULL,'error','N',0,NULL,NULL,1,NULL,1,'2026-07-01 17:00:02',1,'2026-07-01 17:00:02',6,0),
	 (561,1,1,'启用','ENABLED','pw_common_status',NULL,'success','Y',NULL,NULL,NULL,1,'默认启用',1,'2026-07-03 16:30:06',1,'2026-07-03 16:30:06',1,0),
	 (562,1,2,'停用','DISABLED','pw_common_status',NULL,'error','N',NULL,NULL,NULL,1,'停用状态',1,'2026-07-03 16:30:06',1,'2026-07-03 16:30:06',1,0),
	 (563,1,1,'中心仓库','CENTER','pw_warehouse_type',NULL,'success','Y',NULL,NULL,NULL,1,'中心仓库',1,'2026-07-03 16:30:06',1,'2026-07-03 16:30:06',1,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (564,1,2,'项目现场仓','SITE','pw_warehouse_type',NULL,'info','N',NULL,NULL,NULL,1,'项目现场仓',1,'2026-07-03 16:30:06',1,'2026-07-03 16:30:06',1,0),
	 (565,1,3,'临时仓','TEMP','pw_warehouse_type',NULL,'warning','N',NULL,NULL,NULL,1,'临时仓',1,'2026-07-03 16:30:06',1,'2026-07-03 16:30:06',1,0),
	 (566,1,1,'草稿','DRAFT','pw_order_status',NULL,'default','Y',NULL,NULL,NULL,1,'尚未提交审批',1,'2026-07-03 16:30:06',1,'2026-07-03 16:30:06',1,0),
	 (567,1,2,'审批中','SUBMITTED','pw_order_status',NULL,'warning','N',NULL,NULL,NULL,1,'已提交审批',1,'2026-07-03 16:30:06',1,'2026-07-03 16:30:06',1,0),
	 (568,1,3,'流转中','IN_PROCESS','pw_order_status',NULL,'info','N',NULL,NULL,NULL,1,'流程流转中',1,'2026-07-03 16:30:06',1,'2026-07-03 16:30:06',1,0),
	 (569,1,4,'已通过','APPROVED','pw_order_status',NULL,'success','N',NULL,NULL,NULL,1,'审批通过',1,'2026-07-03 16:30:06',1,'2026-07-03 16:30:06',1,0),
	 (570,1,5,'已驳回','REJECTED','pw_order_status',NULL,'error','N',NULL,NULL,NULL,1,'审批驳回',1,'2026-07-03 16:30:06',1,'2026-07-03 16:30:06',1,0),
	 (571,1,6,'已取消','CANCELED','pw_order_status',NULL,'default','N',NULL,NULL,NULL,1,'单据取消',1,'2026-07-03 16:30:06',1,'2026-07-03 16:30:06',1,0),
	 (572,1,7,'已关闭','CLOSED','pw_order_status',NULL,'default','N',NULL,NULL,NULL,1,'单据关闭',1,'2026-07-03 16:30:06',1,'2026-07-03 16:30:06',1,0),
	 (576,1,2,'新增','ADD','sys_operation_type',NULL,'success','N',NULL,NULL,NULL,1,'新增操作',1,'2026-07-08 08:56:39',1,'2026-07-08 10:16:12',1,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (577,1,1,'OpenAI Compatible','openai_compatible','ai_provider_adapter_type',NULL,'info','Y',NULL,NULL,NULL,1,'OpenAI兼容协议',1,'2026-07-11 08:17:36',1,'2026-07-11 08:17:36',1,0),
	 (578,1,2,'DashScope 原生','dashscope_native','ai_provider_adapter_type',NULL,'success','N',NULL,NULL,NULL,1,'阿里百炼DashScope原生协议',1,'2026-07-11 08:17:36',1,'2026-07-11 08:17:36',1,0),
	 (580,1,1,'流式输出','streaming','ai_model_capability_type',NULL,'info','N',NULL,NULL,NULL,1,'支持流式响应',1,'2026-07-11 19:21:23',1,'2026-07-11 19:21:23',1,0),
	 (581,1,2,'推理','reasoning','ai_model_capability_type',NULL,'warning','N',NULL,NULL,NULL,1,'支持推理模型',1,'2026-07-11 19:21:23',1,'2026-07-11 19:21:23',1,0),
	 (582,1,3,'工具调用','tool_calling','ai_model_capability_type',NULL,'success','N',NULL,NULL,NULL,1,'支持Tool Calling',1,'2026-07-11 19:21:23',1,'2026-07-11 19:21:23',1,0),
	 (583,1,4,'视觉','vision','ai_model_capability_type',NULL,'error','N',NULL,NULL,NULL,1,'支持视觉输入',1,'2026-07-11 19:21:23',1,'2026-07-11 19:21:23',1,0),
	 (584,1,5,'结构化输出','structured_output','ai_model_capability_type',NULL,'primary','N',NULL,NULL,NULL,1,'支持结构化输出',1,'2026-07-11 19:21:23',1,'2026-07-11 19:21:23',1,0),
	 (585,1,1,'固定模型','PINNED','ai_agent_model_selection_mode',NULL,'info','Y',NULL,NULL,NULL,1,'固定供应商和模型',1,'2026-07-11 19:21:23',1,'2026-07-11 19:21:23',1,0),
	 (586,1,2,'路由策略','POLICY','ai_agent_model_selection_mode',NULL,'success','N',NULL,NULL,NULL,1,'按显式候选策略路由',1,'2026-07-11 19:21:23',1,'2026-07-11 19:21:23',1,0),
	 (587,1,1,'未知','UNKNOWN','ai_model_health_status',NULL,'default','N',NULL,NULL,NULL,1,'尚无健康样本',1,'2026-07-11 19:21:23',1,'2026-07-11 19:21:23',1,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (588,1,2,'健康','HEALTHY','ai_model_health_status',NULL,'success','N',NULL,NULL,NULL,1,'调用健康',1,'2026-07-11 19:21:23',1,'2026-07-11 19:21:23',1,0),
	 (589,1,3,'降级','DEGRADED','ai_model_health_status',NULL,'warning','N',NULL,NULL,NULL,1,'存在失败样本',1,'2026-07-11 19:21:23',1,'2026-07-11 19:21:23',1,0),
	 (590,1,4,'熔断','OPEN','ai_model_health_status',NULL,'error','N',NULL,NULL,NULL,1,'暂停接收调用',1,'2026-07-11 19:21:23',1,'2026-07-11 19:21:23',1,0),
	 (591,1,5,'试探','HALF_OPEN','ai_model_health_status',NULL,'warning','N',NULL,NULL,NULL,1,'等待单次试探',1,'2026-07-11 19:21:23',1,'2026-07-11 19:21:23',1,0),
	 (592,1,1,'成功','SUCCESS','ai_invocation_outcome',NULL,'success','N',NULL,NULL,NULL,1,'调用成功',1,'2026-07-11 19:21:23',1,'2026-07-11 19:21:23',1,0),
	 (593,1,2,'失败','FAILED','ai_invocation_outcome',NULL,'error','N',NULL,NULL,NULL,1,'调用失败',1,'2026-07-11 19:21:23',1,'2026-07-11 19:21:23',1,0),
	 (594,1,3,'取消','CANCELLED','ai_invocation_outcome',NULL,'warning','N',NULL,NULL,NULL,1,'调用方取消',1,'2026-07-11 19:21:23',1,'2026-07-11 19:21:23',1,0),
	 (595,1,4,'路由失败','ROUTING_FAILED','ai_invocation_outcome',NULL,'error','N',NULL,NULL,NULL,1,'未选择可用模型',1,'2026-07-11 19:21:23',1,'2026-07-11 19:21:23',1,0),
	 (611,1,1,'私有','PRIVATE','ai_capability_visibility',NULL,'default','Y',NULL,NULL,NULL,1,'仅管理员可见',1,'2026-07-12 11:13:01',1,'2026-07-12 11:13:01',1,0),
	 (612,1,2,'可发现','DISCOVERABLE','ai_capability_visibility',NULL,'info','N',NULL,NULL,NULL,1,'同租户管理员可申请授权',1,'2026-07-12 11:13:01',1,'2026-07-12 11:13:01',1,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (613,1,1,'已发布','PUBLISHED','ai_capability_publish_status',NULL,'success','Y',NULL,NULL,NULL,1,'当前发布版本可用',1,'2026-07-12 11:13:01',1,'2026-07-12 11:13:01',1,0),
	 (614,1,2,'已停用','DISABLED','ai_capability_publish_status',NULL,'error','N',NULL,NULL,NULL,1,'能力停止发现和调用',1,'2026-07-12 11:13:01',1,'2026-07-12 11:13:01',1,0),
	 (615,1,1,'启用','ENABLED','ai_capability_client_status',NULL,'success','Y',NULL,NULL,NULL,1,'客户端可认证',1,'2026-07-12 11:13:01',1,'2026-07-12 11:13:01',1,0),
	 (616,1,2,'已吊销','REVOKED','ai_capability_client_status',NULL,'error','N',NULL,NULL,NULL,1,'客户端凭据已吊销',1,'2026-07-12 11:13:01',1,'2026-07-12 11:13:01',1,0),
	 (617,1,1,'启用','ENABLED','ai_capability_grant_status',NULL,'success','Y',NULL,NULL,NULL,1,'授权有效',1,'2026-07-12 11:13:01',1,'2026-07-12 11:13:01',1,0),
	 (618,1,2,'已撤销','REVOKED','ai_capability_grant_status',NULL,'error','N',NULL,NULL,NULL,1,'授权已撤销',1,'2026-07-12 11:13:01',1,'2026-07-12 11:13:01',1,0),
	 (619,1,1,'固定版本','PINNED','ai_capability_version_strategy',NULL,'info','Y',NULL,NULL,NULL,1,'只允许指定版本',1,'2026-07-12 11:13:01',1,'2026-07-12 11:13:01',1,0),
	 (620,1,2,'跟随主版本','FOLLOW_MAJOR','ai_capability_version_strategy',NULL,'success','N',NULL,NULL,NULL,1,'跟随同主版本最新版本',1,'2026-07-12 11:13:01',1,'2026-07-12 11:13:01',1,0),
	 (621,1,1,'低风险','LOW','ai_capability_risk_level',NULL,'success','Y',NULL,NULL,NULL,1,'元数据和安全只读能力',1,'2026-07-12 11:13:01',1,'2026-07-12 11:13:01',1,0),
	 (622,1,2,'中风险','MEDIUM','ai_capability_risk_level',NULL,'warning','N',NULL,NULL,NULL,1,'需要数据权限的只读能力',1,'2026-07-12 11:13:01',1,'2026-07-12 11:13:01',1,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (623,1,3,'高风险','HIGH','ai_capability_risk_level',NULL,'error','N',NULL,NULL,NULL,1,'当前阶段禁止授权',1,'2026-07-12 11:13:01',1,'2026-07-12 11:13:01',1,0),
	 (626,1,1,'机密客户端','CONFIDENTIAL','ai_capability_oauth_client_type',NULL,'info','Y',NULL,NULL,NULL,1,'服务端客户端，换令牌时必须验证客户端密钥',1,'2026-07-12 11:13:02',1,'2026-07-12 11:13:02',1,0),
	 (627,1,2,'公共客户端','PUBLIC','ai_capability_oauth_client_type',NULL,'warning','N',NULL,NULL,NULL,1,'桌面或本地客户端，只允许授权码加PKCE S256',1,'2026-07-12 11:13:02',1,'2026-07-12 11:13:02',1,0),
	 (629,1,1,'草稿','DRAFT','ai_business_application_design_status',NULL,'default','Y',NULL,NULL,NULL,1,'应用仍在设计中',1,'2026-07-14 06:48:55',1,'2026-07-14 06:48:55',1,0),
	 (630,1,2,'待发布','READY','ai_business_application_design_status',NULL,'info','N',NULL,NULL,NULL,1,'应用已满足基础发布条件',1,'2026-07-14 06:48:55',1,'2026-07-14 06:48:55',1,0),
	 (631,1,3,'已发布','PUBLISHED','ai_business_application_design_status',NULL,'success','N',NULL,NULL,NULL,1,'应用当前版本已发布',1,'2026-07-14 06:48:55',1,'2026-07-14 06:48:55',1,0),
	 (632,1,4,'有未发布变更','CHANGED','ai_business_application_design_status',NULL,'warning','N',NULL,NULL,NULL,1,'已发布应用存在草稿变更',1,'2026-07-14 06:48:55',1,'2026-07-14 06:48:55',1,0),
	 (633,1,1,'主对象','PRIMARY','ai_business_application_object_role',NULL,'success','Y',NULL,NULL,NULL,1,'应用的唯一主业务对象',1,'2026-07-14 06:48:55',1,'2026-07-14 06:48:55',1,0),
	 (634,1,2,'明细对象','DETAIL','ai_business_application_object_role',NULL,'info','N',NULL,NULL,NULL,1,'主对象的明细或子对象',1,'2026-07-14 06:48:55',1,'2026-07-14 06:48:55',1,0),
	 (635,1,3,'引用对象','REFERENCE','ai_business_application_object_role',NULL,'warning','N',NULL,NULL,NULL,1,'应用引用的共享查找对象',1,'2026-07-14 06:48:55',1,'2026-07-14 06:48:55',1,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (636,1,4,'共享对象','SHARED','ai_business_application_object_role',NULL,'default','N',NULL,NULL,NULL,1,'应用使用的其他共享对象',1,'2026-07-14 06:48:55',1,'2026-07-14 06:48:55',1,0),
	 (644,1,1,'可视化规则','VISUAL_RULE','ai_business_extension_type',NULL,'success','Y',NULL,NULL,NULL,1,'面向业务人员的条件和动作配置',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (645,1,2,'客户端 JS','CLIENT_JS','ai_business_extension_type',NULL,'info','N',NULL,NULL,NULL,1,'在专用 Worker 沙箱执行的受限脚本',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (646,1,3,'作用域 CSS','SCOPED_CSS','ai_business_extension_type',NULL,'warning','N',NULL,NULL,NULL,1,'只作用于应用和页面根节点的样式',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (647,1,4,'Java 服务增强','SERVER_BINDING','ai_business_extension_type',NULL,'default','N',NULL,NULL,NULL,1,'绑定已开发、部署并注册的 LowcodeExtensionHandler，不在线编译任意 Java',1,'2026-07-14 06:48:56',1,'2026-07-14 14:32:28',1,0),
	 (648,1,1,'草稿','DRAFT','ai_business_extension_status',NULL,'default','Y',NULL,NULL,NULL,1,'内容待校验和测试',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (649,1,2,'已测试','TESTED','ai_business_extension_status',NULL,'info','N',NULL,NULL,NULL,1,'当前草稿校验和测试通过',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (650,1,3,'已启用','ENABLED','ai_business_extension_status',NULL,'success','N',NULL,NULL,NULL,1,'运行态使用已启用版本',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (651,1,4,'已停用','DISABLED','ai_business_extension_status',NULL,'warning','N',NULL,NULL,NULL,1,'运行态不再执行',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (652,1,1,'新增前','BEFORE_CREATE','ai_business_extension_hook',NULL,'warning','N',NULL,NULL,NULL,1,'新增记录前',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (653,1,2,'新增后','AFTER_CREATE','ai_business_extension_hook',NULL,'default','N',NULL,NULL,NULL,1,'新增记录后',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (654,1,3,'修改前','BEFORE_UPDATE','ai_business_extension_hook',NULL,'warning','N',NULL,NULL,NULL,1,'修改记录前',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (655,1,4,'修改后','AFTER_UPDATE','ai_business_extension_hook',NULL,'default','N',NULL,NULL,NULL,1,'修改记录后',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (656,1,5,'删除前','BEFORE_DELETE','ai_business_extension_hook',NULL,'error','N',NULL,NULL,NULL,1,'删除记录前',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (657,1,6,'删除后','AFTER_DELETE','ai_business_extension_hook',NULL,'default','N',NULL,NULL,NULL,1,'删除记录后',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (658,1,7,'导入前','BEFORE_IMPORT','ai_business_extension_hook',NULL,'warning','N',NULL,NULL,NULL,1,'导入前',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (659,1,8,'导入后','AFTER_IMPORT','ai_business_extension_hook',NULL,'default','N',NULL,NULL,NULL,1,'导入后',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (660,1,9,'导出前','BEFORE_EXPORT','ai_business_extension_hook',NULL,'warning','N',NULL,NULL,NULL,1,'导出前',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (661,1,10,'导出后','AFTER_EXPORT','ai_business_extension_hook',NULL,'default','N',NULL,NULL,NULL,1,'导出后',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (662,1,11,'列表查询前','BEFORE_LIST','ai_business_extension_hook',NULL,'warning','N',NULL,NULL,NULL,1,'列表查询前',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (663,1,12,'列表查询后','AFTER_LIST','ai_business_extension_hook',NULL,'default','N',NULL,NULL,NULL,1,'列表查询后',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (664,1,13,'详情查询前','BEFORE_DETAIL','ai_business_extension_hook',NULL,'warning','N',NULL,NULL,NULL,1,'详情查询前',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (665,1,14,'详情查询后','AFTER_DETAIL','ai_business_extension_hook',NULL,'default','N',NULL,NULL,NULL,1,'详情查询后',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (666,1,15,'汇总前','BEFORE_SUMMARY','ai_business_extension_hook',NULL,'warning','N',NULL,NULL,NULL,1,'汇总统计前',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (667,1,16,'汇总后','AFTER_SUMMARY','ai_business_extension_hook',NULL,'default','N',NULL,NULL,NULL,1,'汇总统计后',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (668,1,17,'页面初始化','PAGE_INIT','ai_business_extension_hook',NULL,'info','N',NULL,NULL,NULL,1,'页面根节点初始化',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (669,1,18,'表单变化','FORM_CHANGE','ai_business_extension_hook',NULL,'info','N',NULL,NULL,NULL,1,'白名单表单字段变化',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (670,1,19,'提交前','BEFORE_SUBMIT','ai_business_extension_hook',NULL,'warning','Y',NULL,NULL,NULL,1,'表单提交前',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (671,1,20,'提交后','AFTER_SUBMIT','ai_business_extension_hook',NULL,'default','N',NULL,NULL,NULL,1,'表单提交后',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (672,1,21,'行操作','ROW_ACTION','ai_business_extension_hook',NULL,'info','N',NULL,NULL,NULL,1,'受控列表行动作',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (673,1,1,'阻断','BLOCK','ai_business_extension_failure_policy',NULL,'error','Y',NULL,NULL,NULL,1,'失败时阻断当前业务动作',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (674,1,2,'警告并继续','WARN','ai_business_extension_failure_policy',NULL,'warning','N',NULL,NULL,NULL,1,'记录警告后继续',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (675,1,3,'忽略并继续','IGNORE','ai_business_extension_failure_policy',NULL,'default','N',NULL,NULL,NULL,1,'仅低风险后置钩子可选',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (676,1,1,'低风险','LOW','ai_business_extension_risk_level',NULL,'success','Y',NULL,NULL,NULL,1,'只读或低影响扩展',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (677,1,2,'中风险','MEDIUM','ai_business_extension_risk_level',NULL,'warning','N',NULL,NULL,NULL,1,'可能修改当前页面或业务字段',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (678,1,3,'高风险','HIGH','ai_business_extension_risk_level',NULL,'error','N',NULL,NULL,NULL,1,'前置或服务端高影响扩展',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (679,1,1,'等于','EQ','ai_business_extension_rule_operator',NULL,'default','Y',NULL,NULL,NULL,1,'字段值等于目标值',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (680,1,2,'不等于','NE','ai_business_extension_rule_operator',NULL,'default','N',NULL,NULL,NULL,1,'字段值不等于目标值',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (681,1,3,'大于','GT','ai_business_extension_rule_operator',NULL,'default','N',NULL,NULL,NULL,1,'字段值大于目标值',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (682,1,4,'大于等于','GE','ai_business_extension_rule_operator',NULL,'default','N',NULL,NULL,NULL,1,'字段值大于等于目标值',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (683,1,5,'小于','LT','ai_business_extension_rule_operator',NULL,'default','N',NULL,NULL,NULL,1,'字段值小于目标值',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (684,1,6,'小于等于','LE','ai_business_extension_rule_operator',NULL,'default','N',NULL,NULL,NULL,1,'字段值小于等于目标值',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (685,1,7,'包含','CONTAINS','ai_business_extension_rule_operator',NULL,'default','N',NULL,NULL,NULL,1,'字段值包含目标值',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (686,1,8,'为空','EMPTY','ai_business_extension_rule_operator',NULL,'default','N',NULL,NULL,NULL,1,'字段值为空',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (687,1,1,'设置字段','SET_FIELD','ai_business_extension_rule_action',NULL,'info','Y',NULL,NULL,NULL,1,'设置白名单字段值',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (688,1,2,'显示消息','SHOW_MESSAGE','ai_business_extension_rule_action',NULL,'default','N',NULL,NULL,NULL,1,'显示页面消息',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (689,1,3,'触发白名单动作','TRIGGER_ACTION','ai_business_extension_rule_action',NULL,'warning','N',NULL,NULL,NULL,1,'触发当前应用已注册动作',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (707,1,1,'待执行','CREATED','ai_business_application_publish_status',NULL,'info','Y',NULL,NULL,NULL,1,'运行单已预留，等待执行器认领',1,'2026-07-14 06:48:57',1,'2026-07-14 06:48:57',1,0),
	 (708,1,2,'执行中','RUNNING','ai_business_application_publish_status',NULL,'info','N',NULL,NULL,NULL,1,'协调发布正在执行',1,'2026-07-14 06:48:57',1,'2026-07-14 06:48:57',1,0),
	 (709,1,3,'部分完成','PARTIAL','ai_business_application_publish_status',NULL,'warning','N',NULL,NULL,NULL,1,'部分步骤完成，可从失败位置恢复',1,'2026-07-14 06:48:57',1,'2026-07-14 06:48:57',1,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (710,1,4,'发布失败','FAILED','ai_business_application_publish_status',NULL,'error','N',NULL,NULL,NULL,1,'尚未产生可用发布版本',1,'2026-07-14 06:48:57',1,'2026-07-14 06:48:57',1,0),
	 (711,1,5,'发布成功','SUCCESS','ai_business_application_publish_status',NULL,'success','N',NULL,NULL,NULL,1,'不可变应用版本已经提交',1,'2026-07-14 06:48:57',1,'2026-07-14 06:48:57',1,0),
	 (712,1,6,'已发布版本','PUBLISHED','ai_business_application_publish_status',NULL,'success','N',NULL,NULL,NULL,1,'正常协调发布生成的版本',1,'2026-07-14 06:48:57',1,'2026-07-14 06:48:57',1,0),
	 (713,1,7,'回滚版本','ROLLBACK','ai_business_application_publish_status',NULL,'warning','N',NULL,NULL,NULL,1,'从历史版本恢复生成的新版本',1,'2026-07-14 06:48:57',1,'2026-07-14 06:48:57',1,0),
	 (714,1,1,'发布预检查','PRECHECK','ai_business_application_publish_step',NULL,'default','Y',NULL,NULL,NULL,1,'聚合发布阻断项和依赖',1,'2026-07-14 06:48:57',1,'2026-07-14 06:48:57',1,0),
	 (715,1,2,'准备快照','SNAPSHOT','ai_business_application_publish_step',NULL,'default','N',NULL,NULL,NULL,1,'生成候选快照和稳定摘要',1,'2026-07-14 06:48:57',1,'2026-07-14 06:48:57',1,0),
	 (716,1,3,'发布业务对象','OBJECTS','ai_business_application_publish_step',NULL,'info','N',NULL,NULL,NULL,1,'发布选择对象及自动补齐依赖',1,'2026-07-14 06:48:57',1,'2026-07-14 06:48:57',1,0),
	 (717,1,4,'切换页面入口','ENTRIES','ai_business_application_publish_step',NULL,'info','N',NULL,NULL,NULL,1,'校验入口并同步运行菜单',1,'2026-07-14 06:48:57',1,'2026-07-14 06:48:57',1,0),
	 (718,1,5,'启用业务扩展','EXTENSIONS','ai_business_application_publish_step',NULL,'warning','N',NULL,NULL,NULL,1,'启用已测试扩展版本',1,'2026-07-14 06:48:57',1,'2026-07-14 06:48:57',1,0),
	 (719,1,6,'提交应用版本','COMMIT','ai_business_application_publish_step',NULL,'success','N',NULL,NULL,NULL,1,'写入不可变版本并提交应用状态',1,'2026-07-14 06:48:57',1,'2026-07-14 06:48:57',1,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (722,1,1,'通用编码','COMMON','sys_code_rule_category',NULL,'default','Y',NULL,NULL,NULL,1,'跨业务通用编码',1,'2026-07-16 20:51:34',1,'2026-07-16 20:51:34',1,0),
	 (723,1,2,'单据编码','DOCUMENT','sys_code_rule_category',NULL,'info','N',NULL,NULL,NULL,1,'订单、合同和业务单据',1,'2026-07-16 20:51:34',1,'2026-07-16 20:51:34',1,0),
	 (724,1,3,'物料编码','MATERIAL','sys_code_rule_category',NULL,'success','N',NULL,NULL,NULL,1,'物料和产品主数据',1,'2026-07-16 20:51:34',1,'2026-07-16 20:51:34',1,0),
	 (725,1,4,'客户编码','CUSTOMER','sys_code_rule_category',NULL,'info','N',NULL,NULL,NULL,1,'客户主数据',1,'2026-07-16 20:51:34',1,'2026-07-16 20:51:34',1,0),
	 (726,1,5,'供应商编码','SUPPLIER','sys_code_rule_category',NULL,'warning','N',NULL,NULL,NULL,1,'供应商主数据',1,'2026-07-16 20:51:34',1,'2026-07-16 20:51:34',1,0),
	 (727,1,6,'仓库编码','WAREHOUSE','sys_code_rule_category',NULL,'success','N',NULL,NULL,NULL,1,'仓库主数据',1,'2026-07-16 20:51:34',1,'2026-07-16 20:51:34',1,0),
	 (728,1,7,'载具编码','CARRIER','sys_code_rule_category',NULL,'default','N',NULL,NULL,NULL,1,'托盘和载具',1,'2026-07-16 20:51:34',1,'2026-07-16 20:51:34',1,0),
	 (729,1,8,'容器编码','CONTAINER','sys_code_rule_category',NULL,'default','N',NULL,NULL,NULL,1,'料箱和料盒',1,'2026-07-16 20:51:34',1,'2026-07-16 20:51:34',1,0),
	 (730,1,9,'设备编码','EQUIPMENT','sys_code_rule_category',NULL,'warning','N',NULL,NULL,NULL,1,'设备资产',1,'2026-07-16 20:51:34',1,'2026-07-16 20:51:34',1,0),
	 (731,1,10,'库位编码','LOCATION','sys_code_rule_category',NULL,'info','N',NULL,NULL,NULL,1,'库区和库位',1,'2026-07-16 20:51:34',1,'2026-07-16 20:51:34',1,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (737,1,1,'通用场景','COMMON','sys_code_rule_scene',NULL,'default','Y',NULL,NULL,NULL,1,'所有低代码业务对象均可筛选',1,'2026-07-17 06:50:11',1,'2026-07-17 06:50:11',1,0),
	 (738,1,2,'单据场景','DOCUMENT','sys_code_rule_scene',NULL,'info','N',NULL,NULL,NULL,1,'订单、合同和业务单据',1,'2026-07-17 06:50:11',1,'2026-07-17 06:50:11',1,0),
	 (739,1,3,'订单场景','ORDER','sys_code_rule_scene',NULL,'info','N',NULL,NULL,NULL,1,'兼容历史订单规则筛选',1,'2026-07-17 06:50:11',1,'2026-07-17 06:50:11',1,0),
	 (740,1,4,'合同场景','CONTRACT','sys_code_rule_scene',NULL,'info','N',NULL,NULL,NULL,1,'兼容历史合同规则筛选',1,'2026-07-17 06:50:11',1,'2026-07-17 06:50:11',1,0),
	 (741,1,5,'物料场景','MATERIAL','sys_code_rule_scene',NULL,'success','N',NULL,NULL,NULL,1,'物料和产品主数据',1,'2026-07-17 06:50:11',1,'2026-07-17 06:50:11',1,0),
	 (742,1,6,'客户场景','CUSTOMER','sys_code_rule_scene',NULL,'info','N',NULL,NULL,NULL,1,'客户主数据',1,'2026-07-17 06:50:11',1,'2026-07-17 06:50:11',1,0),
	 (743,1,7,'供应商场景','SUPPLIER','sys_code_rule_scene',NULL,'warning','N',NULL,NULL,NULL,1,'供应商主数据',1,'2026-07-17 06:50:11',1,'2026-07-17 06:50:11',1,0),
	 (744,1,8,'仓库场景','WAREHOUSE','sys_code_rule_scene',NULL,'success','N',NULL,NULL,NULL,1,'仓库主数据',1,'2026-07-17 06:50:11',1,'2026-07-17 06:50:11',1,0),
	 (745,1,9,'载具场景','CARRIER','sys_code_rule_scene',NULL,'default','N',NULL,NULL,NULL,1,'托盘和载具',1,'2026-07-17 06:50:11',1,'2026-07-17 06:50:11',1,0),
	 (746,1,10,'容器场景','CONTAINER','sys_code_rule_scene',NULL,'default','N',NULL,NULL,NULL,1,'料箱和料盒',1,'2026-07-17 06:50:11',1,'2026-07-17 06:50:11',1,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (747,1,11,'设备场景','EQUIPMENT','sys_code_rule_scene',NULL,'warning','N',NULL,NULL,NULL,1,'设备资产',1,'2026-07-17 06:50:11',1,'2026-07-17 06:50:11',1,0),
	 (748,1,12,'库位场景','LOCATION','sys_code_rule_scene',NULL,'info','N',NULL,NULL,NULL,1,'库区和库位',1,'2026-07-17 06:50:11',1,'2026-07-17 06:50:11',1,0),
	 (752,1,1,'待同步','PENDING','sys_job_sync_status',NULL,'warning','Y',NULL,NULL,NULL,1,'等待同步到Quartz',1,'2026-07-21 16:02:54',1,'2026-07-21 16:02:54',1,0),
	 (753,1,2,'已同步','SYNCED','sys_job_sync_status',NULL,'success','N',NULL,NULL,NULL,1,'数据库与Quartz状态一致',1,'2026-07-21 16:02:54',1,'2026-07-21 16:02:54',1,0),
	 (754,1,3,'同步失败','FAILED','sys_job_sync_status',NULL,'error','N',NULL,NULL,NULL,1,'同步失败，可手动重试',1,'2026-07-21 16:02:54',1,'2026-07-21 16:02:54',1,0),
	 (755,1,4,'待删除','DELETE_PENDING','sys_job_sync_status',NULL,'warning','N',NULL,NULL,NULL,1,'等待从Quartz移除',1,'2026-07-21 16:02:54',1,'2026-07-21 16:02:54',1,0),
	 (759,1,1,'周期执行','CRON','sys_job_schedule_type',NULL,'success','Y',NULL,NULL,NULL,1,'按固定周期重复执行',1,'2026-07-21 16:02:57',1,'2026-07-21 16:02:57',1,0),
	 (760,1,2,'仅执行一次','ONCE','sys_job_schedule_type',NULL,'info','N',NULL,NULL,NULL,1,'在指定时间执行一次后结束',1,'2026-07-21 16:02:57',1,'2026-07-21 16:02:57',1,0),
	 (762,1,3,'已结束','2','sys_job_status',NULL,'info','N',NULL,NULL,NULL,1,'一次性任务已完成计划触发',1,'2026-07-21 16:02:57',1,'2026-07-21 16:02:57',1,0),
	 (763,1,1,'允许并行','ALLOW','sys_job_concurrent_policy',NULL,'success','Y',NULL,NULL,NULL,1,'允许同一任务同时执行',1,'2026-07-21 16:02:59',1,'2026-07-21 16:02:59',1,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (764,1,2,'运行中跳过','SKIP_IF_RUNNING','sys_job_concurrent_policy',NULL,'warning','N',NULL,NULL,NULL,1,'上一轮未结束时立即跳过本轮',1,'2026-07-21 16:02:59',1,'2026-07-21 16:02:59',1,0),
	 (765,1,1,'立即补偿一次','FIRE_ONCE_NOW','sys_job_misfire_policy',NULL,'warning','N',NULL,NULL,NULL,1,'错过计划时间后立即补偿执行一次',1,'2026-07-21 16:02:59',1,'2026-07-21 16:02:59',1,0),
	 (766,1,2,'不补偿','DO_NOTHING','sys_job_misfire_policy',NULL,'info','Y',NULL,NULL,NULL,1,'错过计划时间后等待下次正常触发',1,'2026-07-21 16:02:59',1,'2026-07-21 16:02:59',1,0),
	 (767,1,1,'失败','0','sys_job_log_status',NULL,'error','N',NULL,NULL,NULL,1,'任务最终执行失败',1,'2026-07-21 16:02:59',1,'2026-07-21 16:02:59',1,0),
	 (768,1,2,'成功','1','sys_job_log_status',NULL,'success','N',NULL,NULL,NULL,1,'任务最终执行成功',1,'2026-07-21 16:02:59',1,'2026-07-21 16:02:59',1,0),
	 (769,1,3,'运行中','2','sys_job_log_status',NULL,'info','N',NULL,NULL,NULL,1,'任务已接受并正在执行',1,'2026-07-21 16:02:59',1,'2026-07-21 16:02:59',1,0),
	 (770,1,4,'已跳过','3','sys_job_log_status',NULL,'warning','N',NULL,NULL,NULL,1,'并发竞争或执行保护导致跳过',1,'2026-07-21 16:02:59',1,'2026-07-21 16:02:59',1,0),
	 (778,1,1,'计划触发','SCHEDULED','sys_job_trigger_type',NULL,'info','Y',NULL,NULL,NULL,1,'Quartz计划触发',1,'2026-07-21 16:03:01',1,'2026-07-21 16:03:01',1,0),
	 (779,1,2,'手动触发','MANUAL','sys_job_trigger_type',NULL,'warning','N',NULL,NULL,NULL,1,'管理员立即运行触发',1,'2026-07-21 16:03:01',1,'2026-07-21 16:03:01',1,0),
	 (780,1,3,'未知来源','UNKNOWN','sys_job_trigger_type',NULL,'default','N',NULL,NULL,NULL,1,'历史日志或未标记来源',1,'2026-07-21 16:03:01',1,'2026-07-21 16:03:01',1,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (781,1,1,'站内信','WEB','sys_job_alarm_channel',NULL,'info','Y',NULL,NULL,NULL,1,'发送给指定平台用户',1,'2026-07-21 16:03:02',1,'2026-07-21 16:03:02',1,0),
	 (782,1,2,'邮件','EMAIL','sys_job_alarm_channel',NULL,'success','N',NULL,NULL,NULL,1,'发送给指定邮箱地址',1,'2026-07-21 16:03:02',1,'2026-07-21 16:03:02',1,0),
	 (784,1,1,'生效中','ACTIVE','sys_job_api_token_status',NULL,'success','Y',NULL,NULL,NULL,1,'Token当前有效',1,'2026-07-21 16:03:03',1,'2026-07-21 16:03:03',1,0),
	 (785,1,2,'已吊销','REVOKED','sys_job_api_token_status',NULL,'error','N',NULL,NULL,NULL,1,'Token已被主动吊销',1,'2026-07-21 16:03:03',1,'2026-07-21 16:03:03',1,0),
	 (786,1,3,'已过期','EXPIRED','sys_job_api_token_status',NULL,'warning','N',NULL,NULL,NULL,1,'Token已超过有效期',1,'2026-07-21 16:03:03',1,'2026-07-21 16:03:03',1,0),
	 (787,1,1,'读取任务','jobs:read','sys_job_api_scope',NULL,'info','Y',NULL,NULL,NULL,1,'查询授权任务摘要',1,'2026-07-21 16:03:03',1,'2026-07-21 16:03:03',1,0),
	 (788,1,2,'触发任务','jobs:trigger','sys_job_api_scope',NULL,'warning','N',NULL,NULL,NULL,1,'幂等触发授权任务',1,'2026-07-21 16:03:03',1,'2026-07-21 16:03:03',1,0),
	 (789,1,3,'读取执行','executions:read','sys_job_api_scope',NULL,'success','N',NULL,NULL,NULL,1,'查询授权任务执行状态',1,'2026-07-21 16:03:03',1,'2026-07-21 16:03:03',1,0),
	 (791,1,5,'已接受','4','sys_job_log_status',NULL,'info','N',NULL,NULL,NULL,1,'开放API已预留执行记录，等待Quartz启动',1,'2026-07-21 16:03:03',1,'2026-07-21 16:03:03',1,0),
	 (792,1,1,'任务Webhook','JOB_WEBHOOK','sys_outbound_scene',NULL,'warning','N',NULL,NULL,NULL,1,'定时任务Webhook场景，永不允许私网',1,'2026-07-21 16:03:04',1,'2026-07-21 16:03:04',1,0);
INSERT INTO sys_dict_data (dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,parent_dict_code,linked_dict_type,linked_dict_value,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (793,1,2,'流程API','FLOW_API','sys_outbound_scene',NULL,'info','N',NULL,NULL,NULL,1,'流程API场景，管理员可显式授权私网',1,'2026-07-21 16:03:04',1,'2026-07-21 16:03:04',1,0),
	 (794,1,1,'HTTP','http','sys_outbound_protocol',NULL,'warning','N',NULL,NULL,NULL,1,'HTTP协议',1,'2026-07-21 16:03:04',1,'2026-07-21 16:03:04',1,0),
	 (795,1,2,'HTTPS','https','sys_outbound_protocol',NULL,'success','Y',NULL,NULL,NULL,1,'HTTPS协议',1,'2026-07-21 16:03:04',1,'2026-07-21 16:03:04',1,0),
	 (796,1,1,'启用','1','sys_outbound_status',NULL,'success','Y',NULL,NULL,NULL,1,'白名单生效',1,'2026-07-21 16:03:04',1,'2026-07-21 16:03:04',1,0),
	 (797,1,2,'停用','0','sys_outbound_status',NULL,'default','N',NULL,NULL,NULL,1,'白名单停用',1,'2026-07-21 16:03:04',1,'2026-07-21 16:03:04',1,0),
	 (799,1,1,'单一执行器','SINGLE','sys_job_invoke_mode',NULL,'info','Y',NULL,NULL,NULL,1,'按BEAN/HANDLER/RPC执行一个任务目标',1,'2026-07-21 16:03:06',1,'2026-07-21 16:03:06',1,0),
	 (800,1,2,'流程编排','FLOW','sys_job_invoke_mode',NULL,'success','N',NULL,NULL,NULL,1,'启动保存时固定的已发布Flowable流程定义',1,'2026-07-21 16:03:06',1,'2026-07-21 16:03:06',1,0),
	 (802,1,3,'任务RPC','JOB_RPC','sys_outbound_scene',NULL,'primary','N',NULL,NULL,NULL,1,'任务执行器RPC场景，管理员可显式授权私网',1,'2026-07-21 16:03:07',1,'2026-07-21 16:03:07',1,0);
INSERT INTO sys_dict_type (dict_id,tenant_id,dict_name,dict_type,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (1,1,'用户性别','sys_user_sex',1,'用户性别列表',NULL,'2025-11-12 17:41:18',NULL,'2025-11-12 17:41:18',NULL,0),
	 (2,1,'用户状态','sys_user_status',1,'用户状态列表',NULL,'2025-11-12 17:41:18',NULL,'2025-11-12 17:41:18',NULL,0),
	 (3,1,'组织类型','sys_org_type',1,'组织类型列表',NULL,'2025-11-12 17:41:18',NULL,'2025-11-12 17:41:18',NULL,0),
	 (4,1,'是否','yes_no',1,'是否',NULL,'2025-11-12 17:41:18',NULL,'2025-11-12 17:41:18',NULL,0),
	 (7,1,'案件状态','case_status',1,'案件信息案件状态字典',NULL,'2025-12-05 14:37:28',NULL,'2026-06-04 11:43:21',NULL,0),
	 (8,1,'123123','123123123123',1,NULL,NULL,'2025-12-11 16:34:43',NULL,'2025-12-11 16:34:43',NULL,0),
	 (9,1,'测试','121',1,NULL,1,'2026-01-20 14:34:25',1,'2026-01-20 14:34:25',1,0),
	 (10,1,'请求方式','req_method',1,'API请求方式字典',NULL,'2026-02-06 14:58:52',NULL,'2026-06-04 11:43:21',NULL,0),
	 (11,1,'业务模块','module_code',1,'业务模块编码字典',NULL,'2026-02-06 14:59:33',NULL,'2026-06-04 11:43:21',NULL,0),
	 (12,1,'验证码类型','captcha_type',1,NULL,1,'2026-02-26 10:12:04',1,'2026-02-26 10:12:04',2,0);
INSERT INTO sys_dict_type (dict_id,tenant_id,dict_name,dict_type,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (13,1,'水印内容','water_marker_content',1,NULL,1,'2026-02-26 10:18:47',1,'2026-02-26 10:18:47',2,0),
	 (14,1,'三方登录平台','sys_social_platform',1,NULL,1,'2026-02-26 10:18:47',1,'2026-02-26 10:18:47',2,0),
	 (15,1,'通用状态','sys_normal_disable',1,NULL,1,'2026-02-26 10:18:47',1,'2026-02-26 10:18:47',2,0),
	 (16,1,'AI供应商类型','ai_provider_type',1,'AI供应商类型列表',NULL,'2026-04-17 16:28:58',NULL,'2026-04-17 17:03:19',NULL,0),
	 (17,1,'AI模型类型','ai_model_type',1,'AI模型类型列表',NULL,'2026-04-17 16:28:58',NULL,'2026-04-17 17:03:19',NULL,0),
	 (18,1,'AI状态','ai_status',1,'AI模块状态(0正常 1停用)',NULL,'2026-04-17 16:28:59',NULL,'2026-04-17 17:03:19',NULL,0),
	 (19,1,'AI是否默认','ai_is_default',1,'AI模块是否默认(0否 1是)',NULL,'2026-04-17 16:28:59',NULL,'2026-04-17 17:03:19',NULL,0),
	 (22,1,'订单状态','order_status',1,'AI CRUD 生成器自动创建',1,'2026-04-22 13:02:15',1,'2026-04-22 13:02:15',2,0),
	 (23,1,'订单类型','order_type',1,'AI CRUD 生成器自动创建',1,'2026-04-22 13:02:16',1,'2026-04-22 13:02:16',2,0),
	 (29,1,'订单状态','订单状态 (order_status)',1,'AI CRUD 生成器自动创建',1,'2026-04-22 15:08:42',1,'2026-04-22 15:08:42',2,0);
INSERT INTO sys_dict_type (dict_id,tenant_id,dict_name,dict_type,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (30,1,'订单类型','订单类型 (order_type)',1,'AI CRUD 生成器自动创建',1,'2026-04-22 15:08:42',1,'2026-04-22 15:08:42',2,0),
	 (32,1,'产品分类','product_category',1,'AI CRUD 生成器自动创建',1,'2026-04-26 18:09:11',1,'2026-04-26 18:09:11',2,0),
	 (33,1,'通用状态','common_status',1,'AI CRUD 生成器自动创建',1,'2026-04-26 18:09:12',1,'2026-04-26 18:09:12',2,0),
	 (34,1,'流程版本标记','flow_version_tag',1,'流程版本状态标记',NULL,'2026-05-09 08:48:57',NULL,'2026-05-09 08:48:57',NULL,0),
	 (35,1,'流程重要性等级','flow_importance_level',1,'流程重要性等级分类',NULL,'2026-05-09 08:48:57',NULL,'2026-05-09 08:48:57',NULL,0),
	 (36,1,'外部认证适配器','external_auth_adapter',1,'外部系统自定义认证适配器字典',NULL,'2026-05-11 10:57:35',NULL,'2026-05-11 10:57:35',NULL,0),
	 (37,1,'文件存储类型','sys_file_storage_type',1,'文件存储配置的存储类型选项',NULL,'2026-05-21 12:52:20',NULL,'2026-05-21 12:52:20',NULL,0),
	 (38,1,'通用启用禁用','sys_enable_disable',1,'通用启用/禁用状态',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (39,1,'通用是否','sys_yes_no',1,'通用是否选项',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (40,1,'用户类型','sys_user_type',1,'系统用户类型',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0);
INSERT INTO sys_dict_type (dict_id,tenant_id,dict_name,dict_type,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (41,1,'角色数据范围','sys_role_data_scope',1,'角色数据权限范围',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (42,1,'岗位类型','sys_post_type',1,'系统岗位类型',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (43,1,'公告类型','sys_notice_type',1,'通知公告类型',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (44,1,'公告发布状态','sys_notice_status',1,'通知公告发布状态',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (45,1,'消息类型','sys_message_type',1,'消息中心消息类型',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (46,1,'消息渠道','sys_message_channel',1,'消息中心发送渠道',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (47,1,'消息发送范围','sys_message_send_scope',1,'消息中心发送范围',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (48,1,'消息发送状态','sys_message_send_status',1,'消息中心发送状态',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (49,1,'消息阅读状态','sys_message_read_status',1,'消息中心阅读状态',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (53,1,'流程表单类型','flow_form_type',1,'流程表单管理使用的表单类型',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0);
INSERT INTO sys_dict_type (dict_id,tenant_id,dict_name,dict_type,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (54,1,'流程定义表单类型','flow_process_form_type',1,'流程模型和模板使用的表单类型',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (55,1,'流程模型状态','flow_model_status',1,'流程模型生命周期状态',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (56,1,'SPEL 模板分类','flow_spel_category',1,'审批人 SPEL 模板分类',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (57,1,'流程待办状态','flow_todo_status',1,'我的待办任务状态',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (58,1,'流程已办状态','flow_done_status',1,'我的已办审批结果',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (59,1,'我发起的流程状态','flow_started_status',1,'我发起的流程状态',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (60,1,'流程实例状态','flow_instance_status',1,'流程监控实例状态',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (61,1,'流程错误日志状态','flow_error_log_status',1,'流程监控错误日志处理状态',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (62,1,'流程阅读状态','flow_read_status',1,'流程抄送阅读状态',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (63,1,'流程优先级','flow_priority',1,'流程任务优先级',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0);
INSERT INTO sys_dict_type (dict_id,tenant_id,dict_name,dict_type,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (64,1,'流程条件规则类型','flow_rule_type',1,'流程条件规则业务类型',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (65,1,'流程条件运算符','flow_rule_operator',1,'流程条件规则运算符',NULL,'2026-05-21 12:52:21',NULL,'2026-05-21 12:52:21',NULL,0),
	 (68,1,'数据库类型','data_db_type',1,'数据连接支持的数据库类型',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0),
	 (69,1,'维度来源类型','data_dimension_source_type',1,'维度值录入方式',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0),
	 (70,1,'数据集类型','data_dataset_type',1,'数据集建模方式',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0),
	 (71,1,'数据集发布状态','data_dataset_publish_status',1,'数据集生命周期发布状态',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0),
	 (72,1,'数据集访问模式','data_dataset_access_mode',1,'数据集访问权限范围',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0),
	 (73,1,'授权主体类型','data_acl_subject_type',1,'数据集授权主体类型',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0),
	 (74,1,'访问级别','data_acl_access_level',1,'数据集授权访问级别',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0),
	 (75,1,'字段角色','data_field_role',1,'数据集字段角色类型',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0);
INSERT INTO sys_dict_type (dict_id,tenant_id,dict_name,dict_type,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (76,1,'敏感级别','data_field_sensitive_level',1,'数据集字段敏感级别',NULL,'2026-05-21 12:52:22',NULL,'2026-05-21 12:52:22',NULL,0),
	 (83,1,'提示词适用场景','ai_prompt_usage_scene',1,'AI提示词模板适用场景',NULL,'2026-05-21 12:52:23',NULL,'2026-05-21 12:52:23',NULL,0),
	 (84,1,'提示词状态','ai_prompt_status',1,'AI提示词模板状态',NULL,'2026-05-21 12:52:23',NULL,'2026-05-21 12:52:23',NULL,0),
	 (85,1,'提示词是否推荐','ai_prompt_recommended',1,'AI提示词模板推荐标记',NULL,'2026-05-21 12:52:23',NULL,'2026-05-21 12:52:23',NULL,0),
	 (86,1,'大屏生成状态','ai_dashboard_generate_status',1,'AI大屏生成记录状态',NULL,'2026-05-21 12:52:23',NULL,'2026-05-21 12:52:23',NULL,0),
	 (87,1,'低代码应用发布状态','lowcode_app_publish_status',1,'低代码应用发布生命周期状态',NULL,'2026-05-21 12:52:23',NULL,'2026-05-21 12:52:23',NULL,0),
	 (88,1,'低代码模型状态','lowcode_model_status',1,'低代码数据模型状态',NULL,'2026-05-21 12:52:23',NULL,'2026-05-21 12:52:23',NULL,0),
	 (89,1,'AI上下文类型','ai_context_type',1,'AI上下文配置类型',NULL,'2026-05-21 12:52:23',NULL,'2026-05-21 12:52:23',NULL,0),
	 (90,1,'登录类型','sys_login_type',1,'登录行为类型',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (91,1,'客户端类型','sys_client_type',1,'登录客户端类型',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0);
INSERT INTO sys_dict_type (dict_id,tenant_id,dict_name,dict_type,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (92,1,'通用成功失败','sys_common_status',1,'通用成功/失败状态',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (93,1,'操作类型','sys_operation_type',1,'操作日志类型',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (94,1,'任务状态','sys_job_status',1,'定时任务运行状态',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (95,1,'任务执行模式','sys_job_run_mode',1,'定时任务执行模式',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (96,1,'请求方法','sys_req_method',1,'HTTP请求方法',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (97,1,'资源类型','sys_resource_type',1,'菜单资源类型',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (98,1,'显示状态','sys_show_hide',1,'菜单显示/隐藏',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (99,1,'链接打开方式','sys_link_open_target',1,'链接打开目标',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (100,1,'参数内置类型','sys_config_type',1,'系统参数是否内置',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0),
	 (101,1,'文件分组类型','sys_file_group_type',1,'文件分组类型',NULL,'2026-05-21 12:54:11',NULL,'2026-05-21 12:54:11',NULL,0);
INSERT INTO sys_dict_type (dict_id,tenant_id,dict_name,dict_type,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (106,1,'订单状态','order_status1',1,'',1,'2026-05-22 11:22:04',1,'2026-05-22 11:22:04',2,0),
	 (107,1,'Excel配置类型','sys_excel_config_type',1,'Excel导入导出配置类型',NULL,'2026-05-27 13:01:00',NULL,'2026-05-27 13:01:00',NULL,0),
	 (108,1,'业务域','ai_business_suite',1,'企业应用装配平台业务域',1,'2026-05-27 13:01:02',1,'2026-06-14 21:18:18',1,0),
	 (109,1,'业务单元类型','ai_business_object_type',1,'业务单元/实体类型',1,'2026-05-27 13:01:02',1,'2026-06-14 21:18:18',1,0),
	 (110,1,'业务单元关系类型','ai_business_relation_type',1,'业务单元之间的引用、明细和关联列表关系',1,'2026-05-27 13:01:02',1,'2026-06-14 21:18:18',1,0),
	 (111,1,'业务应用类型','ai_business_app_type',1,'应用入口和渠道类型',1,'2026-05-27 13:01:02',1,'2026-05-27 13:01:02',1,0),
	 (112,1,'访问入口打开方式','ai_business_app_entry_mode',1,'访问入口打开方式',1,'2026-05-27 13:01:02',1,'2026-06-14 21:18:18',1,0),
	 (113,1,'业务能力挂接类型','ai_business_binding_type',1,'对象和应用可接入的引擎能力',1,'2026-05-27 13:01:02',1,'2026-05-27 13:01:02',1,0),
	 (115,1,'客户登记','crm_customer_level',1,'',1,'2026-05-30 07:41:37',1,'2026-05-30 07:41:37',2,0),
	 (116,1,'角色类型','sys_role_type',1,'用于区分不同类型的角色以控制业务',NULL,'2026-06-01 16:10:54',NULL,'2026-06-01 16:10:54',NULL,0);
INSERT INTO sys_dict_type (dict_id,tenant_id,dict_name,dict_type,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (117,1,'业务单据状态','business_document_status',1,'低代码业务单据生命周期状态',NULL,'2026-06-02 06:48:46',NULL,'2026-06-02 06:48:46',NULL,0),
	 (118,1,'离职交接状态','hr_leave_handover_status',1,'离职交接任务状态',NULL,'2026-06-02 06:48:47',NULL,'2026-06-02 06:48:47',NULL,0),
	 (119,1,'流程数据模式','sys_flow_data_mode',1,'流程入口数据保存模式',1,'2026-06-06 16:36:36',1,'2026-06-06 16:36:36',1,0),
	 (120,1,'流程入口状态','sys_flow_entry_status',1,'流程入口启停状态',1,'2026-06-06 16:36:36',1,'2026-06-06 16:36:36',1,0),
	 (121,1,'组织填报批次状态','sys_flow_batch_status',1,'组织批量填报批次状态',1,'2026-06-06 16:36:36',1,'2026-06-06 16:36:36',1,0),
	 (122,1,'组织填报状态','sys_flow_fill_status',1,'组织批量填报明细状态',1,'2026-06-06 16:36:36',1,'2026-06-06 16:36:36',1,0),
	 (128,1,'项目类型','pm_project_type',1,'客户管理项目类型',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (129,1,'内部合同实施单位','pm_implementation_unit',1,'内部合同实施单位',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (130,1,'联系人项目角色','pm_contact_role',1,'联系人在项目中的角色',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (131,1,'线索状态','pm_lead_status',1,'线索状态',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0);
INSERT INTO sys_dict_type (dict_id,tenant_id,dict_name,dict_type,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (132,1,'线索无效原因','pm_lead_invalid_reason',1,'线索标记无效原因',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (133,1,'商机状态','pm_opportunity_status',1,'商机状态',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (134,1,'商机输单原因','pm_opportunity_lost_reason',1,'商机输单或无效原因',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (135,1,'商机跟踪方式','pm_follow_type',1,'商机跟踪方式',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (136,1,'投标状态','pm_bid_status',1,'投标状态',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (137,1,'未中标原因','pm_bid_lost_reason',1,'投标未中标原因',1,'2026-06-12 23:17:17',1,'2026-06-12 23:17:17',1,0),
	 (143,1,'访问入口使用模式','ai_business_app_mode',1,'访问入口在线运行或下载代码模式',1,'2026-06-14 21:18:18',1,'2026-06-14 21:18:18',1,0),
	 (144,1,'流程逾期提醒重复策略','sys_flow_overdue_repeat_mode',1,'审批任务逾期提醒重复发送策略',1,'2026-06-21 10:43:46',1,'2026-06-21 10:43:46',1,0),
	 (145,1,'采购单审批测试状态','sample_purchase_order_status',1,'采购单审批测试业务状态',NULL,'2026-06-28 15:07:29',NULL,'2026-06-28 15:07:29',NULL,0),
	 (146,0,'物料状态','marter_status',1,'',1,'2026-07-01 14:55:31',1,'2026-07-01 14:55:31',6,0);
INSERT INTO sys_dict_type (dict_id,tenant_id,dict_name,dict_type,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (147,0,'仓库类型','workhosue_type',1,'',1,'2026-07-01 17:00:01',1,'2026-07-01 17:00:01',6,0),
	 (148,1,'采购仓储通用状态','pw_common_status',1,'采购仓储主数据启停状态',1,'2026-07-03 16:30:06',1,'2026-07-03 16:30:06',1,0),
	 (149,1,'采购仓储仓库类型','pw_warehouse_type',1,'采购仓储仓库类型',1,'2026-07-03 16:30:06',1,'2026-07-03 16:30:06',1,0),
	 (150,1,'采购仓储单据状态','pw_order_status',1,'采购、出库、调拨单据状态',1,'2026-07-03 16:30:06',1,'2026-07-03 16:30:06',1,0),
	 (151,1,'AI供应商连接协议','ai_provider_adapter_type',1,'AI供应商底层连接协议',1,'2026-07-11 08:17:36',1,'2026-07-11 08:17:36',1,0),
	 (152,1,'AI模型能力','ai_model_capability_type',1,'AI模型可路由能力',1,'2026-07-11 19:21:23',1,'2026-07-11 19:21:23',1,0),
	 (153,1,'Agent模型选择模式','ai_agent_model_selection_mode',1,'固定模型或路由策略',1,'2026-07-11 19:21:23',1,'2026-07-11 19:21:23',1,0),
	 (154,1,'AI模型健康状态','ai_model_health_status',1,'模型健康状态',1,'2026-07-11 19:21:23',1,'2026-07-11 19:21:23',1,0),
	 (155,1,'AI调用结果','ai_invocation_outcome',1,'模型调用结果',1,'2026-07-11 19:21:23',1,'2026-07-11 19:21:23',1,0),
	 (159,1,'AI能力可见性','ai_capability_visibility',1,'能力目录可见性',1,'2026-07-12 11:13:01',1,'2026-07-12 11:13:01',1,0);
INSERT INTO sys_dict_type (dict_id,tenant_id,dict_name,dict_type,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (160,1,'AI能力发布状态','ai_capability_publish_status',1,'能力发布状态',1,'2026-07-12 11:13:01',1,'2026-07-12 11:13:01',1,0),
	 (161,1,'AI能力客户端状态','ai_capability_client_status',1,'机器客户端状态',1,'2026-07-12 11:13:01',1,'2026-07-12 11:13:01',1,0),
	 (162,1,'AI能力授权状态','ai_capability_grant_status',1,'客户端授权状态',1,'2026-07-12 11:13:01',1,'2026-07-12 11:13:01',1,0),
	 (163,1,'AI能力版本策略','ai_capability_version_strategy',1,'授权版本选择策略',1,'2026-07-12 11:13:01',1,'2026-07-12 11:13:01',1,0),
	 (164,1,'AI能力风险等级','ai_capability_risk_level',1,'能力风险等级',1,'2026-07-12 11:13:01',1,'2026-07-12 11:13:01',1,0),
	 (166,1,'AI能力OAuth客户端类型','ai_capability_oauth_client_type',1,'MCP OAuth客户端是PUBLIC或CONFIDENTIAL',1,'2026-07-12 11:13:02',1,'2026-07-12 11:13:02',1,0),
	 (167,1,'业务应用设计状态','ai_business_application_design_status',1,'业务应用草稿、就绪、发布和变更状态',1,'2026-07-14 06:48:55',1,'2026-07-14 06:48:55',1,0),
	 (168,1,'业务应用对象角色','ai_business_application_object_role',1,'业务对象在应用内的主对象、明细、引用和共享角色',1,'2026-07-14 06:48:55',1,'2026-07-14 06:48:55',1,0),
	 (170,1,'业务扩展类型','ai_business_extension_type',1,'可视化规则、客户端脚本、作用域样式和服务端白名单绑定',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (171,1,'业务扩展状态','ai_business_extension_status',1,'扩展草稿、测试、启用和停用状态',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0);
INSERT INTO sys_dict_type (dict_id,tenant_id,dict_name,dict_type,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (172,1,'业务扩展钩子','ai_business_extension_hook',1,'对象级和页面级标准扩展钩子',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (173,1,'业务扩展失败策略','ai_business_extension_failure_policy',1,'阻断、警告和忽略失败策略',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (174,1,'业务扩展风险级别','ai_business_extension_risk_level',1,'低、中、高扩展风险级别',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (175,1,'可视化规则操作符','ai_business_extension_rule_operator',1,'可视化条件比较操作符',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (176,1,'可视化规则动作','ai_business_extension_rule_action',1,'可视化规则受限动作',1,'2026-07-14 06:48:56',1,'2026-07-14 06:48:56',1,0),
	 (177,1,'业务应用发布状态','ai_business_application_publish_status',1,'应用协调发布运行单和不可变版本状态',1,'2026-07-14 06:48:57',1,'2026-07-14 06:48:57',1,0),
	 (178,1,'业务应用发布步骤','ai_business_application_publish_step',1,'应用协调发布的预检查、对象、入口、扩展和提交步骤',1,'2026-07-14 06:48:57',1,'2026-07-14 06:48:57',1,0),
	 (180,1,'编码规则分类','sys_code_rule_category',1,'平台级编码规则业务分类',1,'2026-07-16 20:51:34',1,'2026-07-16 20:51:34',1,0),
	 (181,1,'编码规则适用场景','sys_code_rule_scene',1,'编码规则兼容筛选和适用范围',1,'2026-07-17 06:50:11',1,'2026-07-17 06:50:11',1,0),
	 (182,1,'定时任务同步状态','sys_job_sync_status',1,'任务配置与Quartz运行态同步状态',1,'2026-07-21 16:02:54',1,'2026-07-21 16:02:54',1,0);
INSERT INTO sys_dict_type (dict_id,tenant_id,dict_name,dict_type,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (183,1,'定时任务调度类型','sys_job_schedule_type',1,'Cron周期调度或一次性调度',1,'2026-07-21 16:02:57',1,'2026-07-21 16:02:57',1,0),
	 (184,1,'定时任务并发策略','sys_job_concurrent_policy',1,'控制同一任务执行实例是否允许并发',1,'2026-07-21 16:02:59',1,'2026-07-21 16:02:59',1,0),
	 (185,1,'定时任务Misfire策略','sys_job_misfire_policy',1,'控制错过计划触发时间后的行为',1,'2026-07-21 16:02:59',1,'2026-07-21 16:02:59',1,0),
	 (186,1,'定时任务执行状态','sys_job_log_status',1,'定时任务单次顶层执行生命周期状态',1,'2026-07-21 16:02:59',1,'2026-07-21 16:02:59',1,0),
	 (187,1,'定时任务触发来源','sys_job_trigger_type',1,'定时任务执行日志触发来源',1,'2026-07-21 16:03:01',1,'2026-07-21 16:03:01',1,0),
	 (188,1,'定时任务告警渠道','sys_job_alarm_channel',1,'定时任务最终失败通知渠道',1,'2026-07-21 16:03:02',1,'2026-07-21 16:03:02',1,0),
	 (189,1,'任务开放API Token状态','sys_job_api_token_status',1,'服务账号Token状态',1,'2026-07-21 16:03:03',1,'2026-07-21 16:03:03',1,0),
	 (190,1,'任务开放API Scope','sys_job_api_scope',1,'服务账号Token授权范围',1,'2026-07-21 16:03:03',1,'2026-07-21 16:03:03',1,0),
	 (192,1,'出站安全场景','sys_outbound_scene',1,'受控出站策略隔离场景',1,'2026-07-21 16:03:04',1,'2026-07-21 16:03:04',1,0),
	 (193,1,'出站协议','sys_outbound_protocol',1,'受控出站HTTP协议',1,'2026-07-21 16:03:04',1,'2026-07-21 16:03:04',1,0);
INSERT INTO sys_dict_type (dict_id,tenant_id,dict_name,dict_type,dict_status,remark,create_by,create_time,update_by,update_time,create_dept,del_flag) VALUES
	 (194,1,'出站白名单状态','sys_outbound_status',1,'受控出站白名单启停状态',1,'2026-07-21 16:03:04',1,'2026-07-21 16:03:04',1,0),
	 (195,1,'定时任务调用方式','sys_job_invoke_mode',1,'控制任务执行单一执行器或启动固定Flowable流程',1,'2026-07-21 16:03:06',1,'2026-07-21 16:03:06',1,0);
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('110000','北京市',1,NULL,'北京市','110000'),
	 ('110101','东城区',2,'110000','北京市东城区','110101'),
	 ('110102','西城区',2,'110000','北京市西城区','110102'),
	 ('110105','朝阳区',2,'110000','北京市朝阳区','110105'),
	 ('110106','丰台区',2,'110000','北京市丰台区','110106'),
	 ('110107','石景山区',2,'110000','北京市石景山区','110107'),
	 ('110108','海淀区',2,'110000','北京市海淀区','110108'),
	 ('110109','门头沟区',2,'110000','北京市门头沟区','110109'),
	 ('110111','房山区',2,'110000','北京市房山区','110111'),
	 ('110112','通州区',2,'110000','北京市通州区','110112');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('110113','顺义区',2,'110000','北京市顺义区','110113'),
	 ('110114','昌平区',2,'110000','北京市昌平区','110114'),
	 ('110115','大兴区',2,'110000','北京市大兴区','110115'),
	 ('110116','怀柔区',2,'110000','北京市怀柔区','110116'),
	 ('110117','平谷区',2,'110000','北京市平谷区','110117'),
	 ('110118','密云区',2,'110000','北京市密云区','110118'),
	 ('110119','延庆区',2,'110000','北京市延庆区','110119'),
	 ('120000','天津市',1,NULL,'天津市','120000'),
	 ('120101','和平区',2,'120000','天津市和平区','120101'),
	 ('120102','河东区',2,'120000','天津市河东区','120102');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('120103','河西区',2,'120000','天津市河西区','120103'),
	 ('120104','南开区',2,'120000','天津市南开区','120104'),
	 ('120105','河北区',2,'120000','天津市河北区','120105'),
	 ('120106','红桥区',2,'120000','天津市红桥区','120106'),
	 ('120110','东丽区',2,'120000','天津市东丽区','120110'),
	 ('120111','西青区',2,'120000','天津市西青区','120111'),
	 ('120112','津南区',2,'120000','天津市津南区','120112'),
	 ('120113','北辰区',2,'120000','天津市北辰区','120113'),
	 ('120114','武清区',2,'120000','天津市武清区','120114'),
	 ('120115','宝坻区',2,'120000','天津市宝坻区','120115');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('120116','滨海新区',2,'120000','天津市滨海新区','120116'),
	 ('120117','宁河区',2,'120000','天津市宁河区','120117'),
	 ('120118','静海区',2,'120000','天津市静海区','120118'),
	 ('120119','蓟州区',2,'120000','天津市蓟州区','120119'),
	 ('130000','河北省',1,NULL,'河北省','130000'),
	 ('130100','石家庄市',2,'130000','河北省石家庄市','130100'),
	 ('130102','长安区',3,'130100','河北省石家庄市长安区','130100'),
	 ('130104','桥西区',3,'130100','河北省石家庄市桥西区','130100'),
	 ('130105','新华区',3,'130100','河北省石家庄市新华区','130100'),
	 ('130107','井陉矿区',3,'130100','河北省石家庄市井陉矿区','130100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('130108','裕华区',3,'130100','河北省石家庄市裕华区','130100'),
	 ('130109','藁城区',3,'130100','河北省石家庄市藁城区','130100'),
	 ('130110','鹿泉区',3,'130100','河北省石家庄市鹿泉区','130100'),
	 ('130111','栾城区',3,'130100','河北省石家庄市栾城区','130100'),
	 ('130121','井陉县',3,'130100','河北省石家庄市井陉县','130100'),
	 ('130123','正定县',3,'130100','河北省石家庄市正定县','130100'),
	 ('130125','行唐县',3,'130100','河北省石家庄市行唐县','130100'),
	 ('130126','灵寿县',3,'130100','河北省石家庄市灵寿县','130100'),
	 ('130127','高邑县',3,'130100','河北省石家庄市高邑县','130100'),
	 ('130128','深泽县',3,'130100','河北省石家庄市深泽县','130100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('130129','赞皇县',3,'130100','河北省石家庄市赞皇县','130100'),
	 ('130130','无极县',3,'130100','河北省石家庄市无极县','130100'),
	 ('130131','平山县',3,'130100','河北省石家庄市平山县','130100'),
	 ('130132','元氏县',3,'130100','河北省石家庄市元氏县','130100'),
	 ('130133','赵县',3,'130100','河北省石家庄市赵县','130100'),
	 ('130181','辛集市',3,'130100','河北省石家庄市辛集市','130100'),
	 ('130183','晋州市',3,'130100','河北省石家庄市晋州市','130100'),
	 ('130184','新乐市',3,'130100','河北省石家庄市新乐市','130100'),
	 ('130200','唐山市',2,'130000','河北省唐山市','130200'),
	 ('130202','路南区',3,'130200','河北省唐山市路南区','130200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('130203','路北区',3,'130200','河北省唐山市路北区','130200'),
	 ('130204','古冶区',3,'130200','河北省唐山市古冶区','130200'),
	 ('130205','开平区',3,'130200','河北省唐山市开平区','130200'),
	 ('130207','丰南区',3,'130200','河北省唐山市丰南区','130200'),
	 ('130208','丰润区',3,'130200','河北省唐山市丰润区','130200'),
	 ('130209','曹妃甸区',3,'130200','河北省唐山市曹妃甸区','130200'),
	 ('130224','滦南县',3,'130200','河北省唐山市滦南县','130200'),
	 ('130225','乐亭县',3,'130200','河北省唐山市乐亭县','130200'),
	 ('130227','迁西县',3,'130200','河北省唐山市迁西县','130200'),
	 ('130229','玉田县',3,'130200','河北省唐山市玉田县','130200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('130281','遵化市',3,'130200','河北省唐山市遵化市','130200'),
	 ('130283','迁安市',3,'130200','河北省唐山市迁安市','130200'),
	 ('130284','滦州市',3,'130200','河北省唐山市滦州市','130200'),
	 ('130300','秦皇岛市',2,'130000','河北省秦皇岛市','130300'),
	 ('130302','海港区',3,'130300','河北省秦皇岛市海港区','130300'),
	 ('130303','山海关区',3,'130300','河北省秦皇岛市山海关区','130300'),
	 ('130304','北戴河区',3,'130300','河北省秦皇岛市北戴河区','130300'),
	 ('130306','抚宁区',3,'130300','河北省秦皇岛市抚宁区','130300'),
	 ('130321','青龙满族自治县',3,'130300','河北省秦皇岛市青龙满族自治县','130300'),
	 ('130322','昌黎县',3,'130300','河北省秦皇岛市昌黎县','130300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('130324','卢龙县',3,'130300','河北省秦皇岛市卢龙县','130300'),
	 ('130400','邯郸市',2,'130000','河北省邯郸市','130400'),
	 ('130402','邯山区',3,'130400','河北省邯郸市邯山区','130400'),
	 ('130403','丛台区',3,'130400','河北省邯郸市丛台区','130400'),
	 ('130404','复兴区',3,'130400','河北省邯郸市复兴区','130400'),
	 ('130406','峰峰矿区',3,'130400','河北省邯郸市峰峰矿区','130400'),
	 ('130407','肥乡区',3,'130400','河北省邯郸市肥乡区','130400'),
	 ('130408','永年区',3,'130400','河北省邯郸市永年区','130400'),
	 ('130423','临漳县',3,'130400','河北省邯郸市临漳县','130400'),
	 ('130424','成安县',3,'130400','河北省邯郸市成安县','130400');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('130425','大名县',3,'130400','河北省邯郸市大名县','130400'),
	 ('130426','涉县',3,'130400','河北省邯郸市涉县','130400'),
	 ('130427','磁县',3,'130400','河北省邯郸市磁县','130400'),
	 ('130430','邱县',3,'130400','河北省邯郸市邱县','130400'),
	 ('130431','鸡泽县',3,'130400','河北省邯郸市鸡泽县','130400'),
	 ('130432','广平县',3,'130400','河北省邯郸市广平县','130400'),
	 ('130433','馆陶县',3,'130400','河北省邯郸市馆陶县','130400'),
	 ('130434','魏县',3,'130400','河北省邯郸市魏县','130400'),
	 ('130435','曲周县',3,'130400','河北省邯郸市曲周县','130400'),
	 ('130481','武安市',3,'130400','河北省邯郸市武安市','130400');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('130500','邢台市',2,'130000','河北省邢台市','130500'),
	 ('130502','襄都区',3,'130500','河北省邢台市襄都区','130500'),
	 ('130503','信都区',3,'130500','河北省邢台市信都区','130500'),
	 ('130505','任泽区',3,'130500','河北省邢台市任泽区','130500'),
	 ('130506','南和区',3,'130500','河北省邢台市南和区','130500'),
	 ('130522','临城县',3,'130500','河北省邢台市临城县','130500'),
	 ('130523','内丘县',3,'130500','河北省邢台市内丘县','130500'),
	 ('130524','柏乡县',3,'130500','河北省邢台市柏乡县','130500'),
	 ('130525','隆尧县',3,'130500','河北省邢台市隆尧县','130500'),
	 ('130528','宁晋县',3,'130500','河北省邢台市宁晋县','130500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('130529','巨鹿县',3,'130500','河北省邢台市巨鹿县','130500'),
	 ('130530','新河县',3,'130500','河北省邢台市新河县','130500'),
	 ('130531','广宗县',3,'130500','河北省邢台市广宗县','130500'),
	 ('130532','平乡县',3,'130500','河北省邢台市平乡县','130500'),
	 ('130533','威县',3,'130500','河北省邢台市威县','130500'),
	 ('130534','清河县',3,'130500','河北省邢台市清河县','130500'),
	 ('130535','临西县',3,'130500','河北省邢台市临西县','130500'),
	 ('130581','南宫市',3,'130500','河北省邢台市南宫市','130500'),
	 ('130582','沙河市',3,'130500','河北省邢台市沙河市','130500'),
	 ('130600','保定市',2,'130000','河北省保定市','130600');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('130602','竞秀区',3,'130600','河北省保定市竞秀区','130600'),
	 ('130606','莲池区',3,'130600','河北省保定市莲池区','130600'),
	 ('130607','满城区',3,'130600','河北省保定市满城区','130600'),
	 ('130608','清苑区',3,'130600','河北省保定市清苑区','130600'),
	 ('130609','徐水区',3,'130600','河北省保定市徐水区','130600'),
	 ('130623','涞水县',3,'130600','河北省保定市涞水县','130600'),
	 ('130624','阜平县',3,'130600','河北省保定市阜平县','130600'),
	 ('130626','定兴县',3,'130600','河北省保定市定兴县','130600'),
	 ('130627','唐县',3,'130600','河北省保定市唐县','130600'),
	 ('130628','高阳县',3,'130600','河北省保定市高阳县','130600');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('130629','容城县',3,'130600','河北省保定市容城县','130600'),
	 ('130630','涞源县',3,'130600','河北省保定市涞源县','130600'),
	 ('130631','望都县',3,'130600','河北省保定市望都县','130600'),
	 ('130632','安新县',3,'130600','河北省保定市安新县','130600'),
	 ('130633','易县',3,'130600','河北省保定市易县','130600'),
	 ('130634','曲阳县',3,'130600','河北省保定市曲阳县','130600'),
	 ('130635','蠡县',3,'130600','河北省保定市蠡县','130600'),
	 ('130636','顺平县',3,'130600','河北省保定市顺平县','130600'),
	 ('130637','博野县',3,'130600','河北省保定市博野县','130600'),
	 ('130638','雄县',3,'130600','河北省保定市雄县','130600');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('130681','涿州市',3,'130600','河北省保定市涿州市','130600'),
	 ('130682','定州市',3,'130600','河北省保定市定州市','130600'),
	 ('130683','安国市',3,'130600','河北省保定市安国市','130600'),
	 ('130684','高碑店市',3,'130600','河北省保定市高碑店市','130600'),
	 ('130700','张家口市',2,'130000','河北省张家口市','130700'),
	 ('130702','桥东区',3,'130700','河北省张家口市桥东区','130700'),
	 ('130703','桥西区',3,'130700','河北省张家口市桥西区','130700'),
	 ('130705','宣化区',3,'130700','河北省张家口市宣化区','130700'),
	 ('130706','下花园区',3,'130700','河北省张家口市下花园区','130700'),
	 ('130708','万全区',3,'130700','河北省张家口市万全区','130700');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('130709','崇礼区',3,'130700','河北省张家口市崇礼区','130700'),
	 ('130722','张北县',3,'130700','河北省张家口市张北县','130700'),
	 ('130723','康保县',3,'130700','河北省张家口市康保县','130700'),
	 ('130724','沽源县',3,'130700','河北省张家口市沽源县','130700'),
	 ('130725','尚义县',3,'130700','河北省张家口市尚义县','130700'),
	 ('130726','蔚县',3,'130700','河北省张家口市蔚县','130700'),
	 ('130727','阳原县',3,'130700','河北省张家口市阳原县','130700'),
	 ('130728','怀安县',3,'130700','河北省张家口市怀安县','130700'),
	 ('130730','怀来县',3,'130700','河北省张家口市怀来县','130700'),
	 ('130731','涿鹿县',3,'130700','河北省张家口市涿鹿县','130700');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('130732','赤城县',3,'130700','河北省张家口市赤城县','130700'),
	 ('130800','承德市',2,'130000','河北省承德市','130800'),
	 ('130802','双桥区',3,'130800','河北省承德市双桥区','130800'),
	 ('130803','双滦区',3,'130800','河北省承德市双滦区','130800'),
	 ('130804','鹰手营子矿区',3,'130800','河北省承德市鹰手营子矿区','130800'),
	 ('130821','承德县',3,'130800','河北省承德市承德县','130800'),
	 ('130822','兴隆县',3,'130800','河北省承德市兴隆县','130800'),
	 ('130824','滦平县',3,'130800','河北省承德市滦平县','130800'),
	 ('130825','隆化县',3,'130800','河北省承德市隆化县','130800'),
	 ('130826','丰宁满族自治县',3,'130800','河北省承德市丰宁满族自治县','130800');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('130827','宽城满族自治县',3,'130800','河北省承德市宽城满族自治县','130800'),
	 ('130828','围场满族蒙古族自治县',3,'130800','河北省承德市围场满族蒙古族自治县','130800'),
	 ('130881','平泉市',3,'130800','河北省承德市平泉市','130800'),
	 ('130900','沧州市',2,'130000','河北省沧州市','130900'),
	 ('130902','新华区',3,'130900','河北省沧州市新华区','130900'),
	 ('130903','运河区',3,'130900','河北省沧州市运河区','130900'),
	 ('130921','沧县',3,'130900','河北省沧州市沧县','130900'),
	 ('130922','青县',3,'130900','河北省沧州市青县','130900'),
	 ('130923','东光县',3,'130900','河北省沧州市东光县','130900'),
	 ('130924','海兴县',3,'130900','河北省沧州市海兴县','130900');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('130925','盐山县',3,'130900','河北省沧州市盐山县','130900'),
	 ('130926','肃宁县',3,'130900','河北省沧州市肃宁县','130900'),
	 ('130927','南皮县',3,'130900','河北省沧州市南皮县','130900'),
	 ('130928','吴桥县',3,'130900','河北省沧州市吴桥县','130900'),
	 ('130929','献县',3,'130900','河北省沧州市献县','130900'),
	 ('130930','孟村回族自治县',3,'130900','河北省沧州市孟村回族自治县','130900'),
	 ('130981','泊头市',3,'130900','河北省沧州市泊头市','130900'),
	 ('130982','任丘市',3,'130900','河北省沧州市任丘市','130900'),
	 ('130983','黄骅市',3,'130900','河北省沧州市黄骅市','130900'),
	 ('130984','河间市',3,'130900','河北省沧州市河间市','130900');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('131000','廊坊市',2,'130000','河北省廊坊市','131000'),
	 ('131002','安次区',3,'131000','河北省廊坊市安次区','131000'),
	 ('131003','广阳区',3,'131000','河北省廊坊市广阳区','131000'),
	 ('131022','固安县',3,'131000','河北省廊坊市固安县','131000'),
	 ('131023','永清县',3,'131000','河北省廊坊市永清县','131000'),
	 ('131024','香河县',3,'131000','河北省廊坊市香河县','131000'),
	 ('131025','大城县',3,'131000','河北省廊坊市大城县','131000'),
	 ('131026','文安县',3,'131000','河北省廊坊市文安县','131000'),
	 ('131028','大厂回族自治县',3,'131000','河北省廊坊市大厂回族自治县','131000'),
	 ('131081','霸州市',3,'131000','河北省廊坊市霸州市','131000');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('131082','三河市',3,'131000','河北省廊坊市三河市','131000'),
	 ('131100','衡水市',2,'130000','河北省衡水市','131100'),
	 ('131102','桃城区',3,'131100','河北省衡水市桃城区','131100'),
	 ('131103','冀州区',3,'131100','河北省衡水市冀州区','131100'),
	 ('131121','枣强县',3,'131100','河北省衡水市枣强县','131100'),
	 ('131122','武邑县',3,'131100','河北省衡水市武邑县','131100'),
	 ('131123','武强县',3,'131100','河北省衡水市武强县','131100'),
	 ('131124','饶阳县',3,'131100','河北省衡水市饶阳县','131100'),
	 ('131125','安平县',3,'131100','河北省衡水市安平县','131100'),
	 ('131126','故城县',3,'131100','河北省衡水市故城县','131100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('131127','景县',3,'131100','河北省衡水市景县','131100'),
	 ('131128','阜城县',3,'131100','河北省衡水市阜城县','131100'),
	 ('131182','深州市',3,'131100','河北省衡水市深州市','131100'),
	 ('140000','山西省',1,NULL,'山西省','140000'),
	 ('140100','太原市',2,'140000','山西省太原市','140100'),
	 ('140105','小店区',3,'140100','山西省太原市小店区','140100'),
	 ('140106','迎泽区',3,'140100','山西省太原市迎泽区','140100'),
	 ('140107','杏花岭区',3,'140100','山西省太原市杏花岭区','140100'),
	 ('140108','尖草坪区',3,'140100','山西省太原市尖草坪区','140100'),
	 ('140109','万柏林区',3,'140100','山西省太原市万柏林区','140100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('140110','晋源区',3,'140100','山西省太原市晋源区','140100'),
	 ('140121','清徐县',3,'140100','山西省太原市清徐县','140100'),
	 ('140122','阳曲县',3,'140100','山西省太原市阳曲县','140100'),
	 ('140123','娄烦县',3,'140100','山西省太原市娄烦县','140100'),
	 ('140181','古交市',3,'140100','山西省太原市古交市','140100'),
	 ('140200','大同市',2,'140000','山西省大同市','140200'),
	 ('140212','新荣区',3,'140200','山西省大同市新荣区','140200'),
	 ('140213','平城区',3,'140200','山西省大同市平城区','140200'),
	 ('140214','云冈区',3,'140200','山西省大同市云冈区','140200'),
	 ('140215','云州区',3,'140200','山西省大同市云州区','140200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('140221','阳高县',3,'140200','山西省大同市阳高县','140200'),
	 ('140222','天镇县',3,'140200','山西省大同市天镇县','140200'),
	 ('140223','广灵县',3,'140200','山西省大同市广灵县','140200'),
	 ('140224','灵丘县',3,'140200','山西省大同市灵丘县','140200'),
	 ('140225','浑源县',3,'140200','山西省大同市浑源县','140200'),
	 ('140226','左云县',3,'140200','山西省大同市左云县','140200'),
	 ('140300','阳泉市',2,'140000','山西省阳泉市','140300'),
	 ('140302','城区',3,'140300','山西省阳泉市城区','140300'),
	 ('140303','矿区',3,'140300','山西省阳泉市矿区','140300'),
	 ('140311','郊区',3,'140300','山西省阳泉市郊区','140300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('140321','平定县',3,'140300','山西省阳泉市平定县','140300'),
	 ('140322','盂县',3,'140300','山西省阳泉市盂县','140300'),
	 ('140400','长治市',2,'140000','山西省长治市','140400'),
	 ('140403','潞州区',3,'140400','山西省长治市潞州区','140400'),
	 ('140404','上党区',3,'140400','山西省长治市上党区','140400'),
	 ('140405','屯留区',3,'140400','山西省长治市屯留区','140400'),
	 ('140406','潞城区',3,'140400','山西省长治市潞城区','140400'),
	 ('140423','襄垣县',3,'140400','山西省长治市襄垣县','140400'),
	 ('140425','平顺县',3,'140400','山西省长治市平顺县','140400'),
	 ('140426','黎城县',3,'140400','山西省长治市黎城县','140400');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('140427','壶关县',3,'140400','山西省长治市壶关县','140400'),
	 ('140428','长子县',3,'140400','山西省长治市长子县','140400'),
	 ('140429','武乡县',3,'140400','山西省长治市武乡县','140400'),
	 ('140430','沁县',3,'140400','山西省长治市沁县','140400'),
	 ('140431','沁源县',3,'140400','山西省长治市沁源县','140400'),
	 ('140500','晋城市',2,'140000','山西省晋城市','140500'),
	 ('140502','城区',3,'140500','山西省晋城市城区','140500'),
	 ('140521','沁水县',3,'140500','山西省晋城市沁水县','140500'),
	 ('140522','阳城县',3,'140500','山西省晋城市阳城县','140500'),
	 ('140524','陵川县',3,'140500','山西省晋城市陵川县','140500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('140525','泽州县',3,'140500','山西省晋城市泽州县','140500'),
	 ('140581','高平市',3,'140500','山西省晋城市高平市','140500'),
	 ('140600','朔州市',2,'140000','山西省朔州市','140600'),
	 ('140602','朔城区',3,'140600','山西省朔州市朔城区','140600'),
	 ('140603','平鲁区',3,'140600','山西省朔州市平鲁区','140600'),
	 ('140621','山阴县',3,'140600','山西省朔州市山阴县','140600'),
	 ('140622','应县',3,'140600','山西省朔州市应县','140600'),
	 ('140623','右玉县',3,'140600','山西省朔州市右玉县','140600'),
	 ('140681','怀仁市',3,'140600','山西省朔州市怀仁市','140600'),
	 ('140700','晋中市',2,'140000','山西省晋中市','140700');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('140702','榆次区',3,'140700','山西省晋中市榆次区','140700'),
	 ('140703','太谷区',3,'140700','山西省晋中市太谷区','140700'),
	 ('140721','榆社县',3,'140700','山西省晋中市榆社县','140700'),
	 ('140722','左权县',3,'140700','山西省晋中市左权县','140700'),
	 ('140723','和顺县',3,'140700','山西省晋中市和顺县','140700'),
	 ('140724','昔阳县',3,'140700','山西省晋中市昔阳县','140700'),
	 ('140725','寿阳县',3,'140700','山西省晋中市寿阳县','140700'),
	 ('140727','祁县',3,'140700','山西省晋中市祁县','140700'),
	 ('140728','平遥县',3,'140700','山西省晋中市平遥县','140700'),
	 ('140729','灵石县',3,'140700','山西省晋中市灵石县','140700');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('140781','介休市',3,'140700','山西省晋中市介休市','140700'),
	 ('140800','运城市',2,'140000','山西省运城市','140800'),
	 ('140802','盐湖区',3,'140800','山西省运城市盐湖区','140800'),
	 ('140821','临猗县',3,'140800','山西省运城市临猗县','140800'),
	 ('140822','万荣县',3,'140800','山西省运城市万荣县','140800'),
	 ('140823','闻喜县',3,'140800','山西省运城市闻喜县','140800'),
	 ('140824','稷山县',3,'140800','山西省运城市稷山县','140800'),
	 ('140825','新绛县',3,'140800','山西省运城市新绛县','140800'),
	 ('140826','绛县',3,'140800','山西省运城市绛县','140800'),
	 ('140827','垣曲县',3,'140800','山西省运城市垣曲县','140800');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('140828','夏县',3,'140800','山西省运城市夏县','140800'),
	 ('140829','平陆县',3,'140800','山西省运城市平陆县','140800'),
	 ('140830','芮城县',3,'140800','山西省运城市芮城县','140800'),
	 ('140881','永济市',3,'140800','山西省运城市永济市','140800'),
	 ('140882','河津市',3,'140800','山西省运城市河津市','140800'),
	 ('140900','忻州市',2,'140000','山西省忻州市','140900'),
	 ('140902','忻府区',3,'140900','山西省忻州市忻府区','140900'),
	 ('140921','定襄县',3,'140900','山西省忻州市定襄县','140900'),
	 ('140922','五台县',3,'140900','山西省忻州市五台县','140900'),
	 ('140923','代县',3,'140900','山西省忻州市代县','140900');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('140924','繁峙县',3,'140900','山西省忻州市繁峙县','140900'),
	 ('140925','宁武县',3,'140900','山西省忻州市宁武县','140900'),
	 ('140926','静乐县',3,'140900','山西省忻州市静乐县','140900'),
	 ('140927','神池县',3,'140900','山西省忻州市神池县','140900'),
	 ('140928','五寨县',3,'140900','山西省忻州市五寨县','140900'),
	 ('140929','岢岚县',3,'140900','山西省忻州市岢岚县','140900'),
	 ('140930','河曲县',3,'140900','山西省忻州市河曲县','140900'),
	 ('140931','保德县',3,'140900','山西省忻州市保德县','140900'),
	 ('140932','偏关县',3,'140900','山西省忻州市偏关县','140900'),
	 ('140981','原平市',3,'140900','山西省忻州市原平市','140900');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('141000','临汾市',2,'140000','山西省临汾市','141000'),
	 ('141002','尧都区',3,'141000','山西省临汾市尧都区','141000'),
	 ('141021','曲沃县',3,'141000','山西省临汾市曲沃县','141000'),
	 ('141022','翼城县',3,'141000','山西省临汾市翼城县','141000'),
	 ('141023','襄汾县',3,'141000','山西省临汾市襄汾县','141000'),
	 ('141024','洪洞县',3,'141000','山西省临汾市洪洞县','141000'),
	 ('141025','古县',3,'141000','山西省临汾市古县','141000'),
	 ('141026','安泽县',3,'141000','山西省临汾市安泽县','141000'),
	 ('141027','浮山县',3,'141000','山西省临汾市浮山县','141000'),
	 ('141028','吉县',3,'141000','山西省临汾市吉县','141000');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('141029','乡宁县',3,'141000','山西省临汾市乡宁县','141000'),
	 ('141030','大宁县',3,'141000','山西省临汾市大宁县','141000'),
	 ('141031','隰县',3,'141000','山西省临汾市隰县','141000'),
	 ('141032','永和县',3,'141000','山西省临汾市永和县','141000'),
	 ('141033','蒲县',3,'141000','山西省临汾市蒲县','141000'),
	 ('141034','汾西县',3,'141000','山西省临汾市汾西县','141000'),
	 ('141081','侯马市',3,'141000','山西省临汾市侯马市','141000'),
	 ('141082','霍州市',3,'141000','山西省临汾市霍州市','141000'),
	 ('141100','吕梁市',2,'140000','山西省吕梁市','141100'),
	 ('141102','离石区',3,'141100','山西省吕梁市离石区','141100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('141121','文水县',3,'141100','山西省吕梁市文水县','141100'),
	 ('141122','交城县',3,'141100','山西省吕梁市交城县','141100'),
	 ('141123','兴县',3,'141100','山西省吕梁市兴县','141100'),
	 ('141124','临县',3,'141100','山西省吕梁市临县','141100'),
	 ('141125','柳林县',3,'141100','山西省吕梁市柳林县','141100'),
	 ('141126','石楼县',3,'141100','山西省吕梁市石楼县','141100'),
	 ('141127','岚县',3,'141100','山西省吕梁市岚县','141100'),
	 ('141128','方山县',3,'141100','山西省吕梁市方山县','141100'),
	 ('141129','中阳县',3,'141100','山西省吕梁市中阳县','141100'),
	 ('141130','交口县',3,'141100','山西省吕梁市交口县','141100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('141181','孝义市',3,'141100','山西省吕梁市孝义市','141100'),
	 ('141182','汾阳市',3,'141100','山西省吕梁市汾阳市','141100'),
	 ('150000','内蒙古自治区',1,NULL,'内蒙古自治区','150000'),
	 ('150100','呼和浩特市',2,'150000','内蒙古自治区呼和浩特市','150100'),
	 ('150102','新城区',3,'150100','内蒙古自治区呼和浩特市新城区','150100'),
	 ('150103','回民区',3,'150100','内蒙古自治区呼和浩特市回民区','150100'),
	 ('150104','玉泉区',3,'150100','内蒙古自治区呼和浩特市玉泉区','150100'),
	 ('150105','赛罕区',3,'150100','内蒙古自治区呼和浩特市赛罕区','150100'),
	 ('150121','土默特左旗',3,'150100','内蒙古自治区呼和浩特市土默特左旗','150100'),
	 ('150122','托克托县',3,'150100','内蒙古自治区呼和浩特市托克托县','150100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('150123','和林格尔县',3,'150100','内蒙古自治区呼和浩特市和林格尔县','150100'),
	 ('150124','清水河县',3,'150100','内蒙古自治区呼和浩特市清水河县','150100'),
	 ('150125','武川县',3,'150100','内蒙古自治区呼和浩特市武川县','150100'),
	 ('150200','包头市',2,'150000','内蒙古自治区包头市','150200'),
	 ('150202','东河区',3,'150200','内蒙古自治区包头市东河区','150200'),
	 ('150203','昆都仑区',3,'150200','内蒙古自治区包头市昆都仑区','150200'),
	 ('150204','青山区',3,'150200','内蒙古自治区包头市青山区','150200'),
	 ('150205','石拐区',3,'150200','内蒙古自治区包头市石拐区','150200'),
	 ('150206','白云鄂博矿区',3,'150200','内蒙古自治区包头市白云鄂博矿区','150200'),
	 ('150207','九原区',3,'150200','内蒙古自治区包头市九原区','150200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('150221','土默特右旗',3,'150200','内蒙古自治区包头市土默特右旗','150200'),
	 ('150222','固阳县',3,'150200','内蒙古自治区包头市固阳县','150200'),
	 ('150223','达尔罕茂明安联合旗',3,'150200','内蒙古自治区包头市达尔罕茂明安联合旗','150200'),
	 ('150300','乌海市',2,'150000','内蒙古自治区乌海市','150300'),
	 ('150302','海勃湾区',3,'150300','内蒙古自治区乌海市海勃湾区','150300'),
	 ('150303','海南区',3,'150300','内蒙古自治区乌海市海南区','150300'),
	 ('150304','乌达区',3,'150300','内蒙古自治区乌海市乌达区','150300'),
	 ('150400','赤峰市',2,'150000','内蒙古自治区赤峰市','150400'),
	 ('150402','红山区',3,'150400','内蒙古自治区赤峰市红山区','150400'),
	 ('150403','元宝山区',3,'150400','内蒙古自治区赤峰市元宝山区','150400');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('150404','松山区',3,'150400','内蒙古自治区赤峰市松山区','150400'),
	 ('150421','阿鲁科尔沁旗',3,'150400','内蒙古自治区赤峰市阿鲁科尔沁旗','150400'),
	 ('150422','巴林左旗',3,'150400','内蒙古自治区赤峰市巴林左旗','150400'),
	 ('150423','巴林右旗',3,'150400','内蒙古自治区赤峰市巴林右旗','150400'),
	 ('150424','林西县',3,'150400','内蒙古自治区赤峰市林西县','150400'),
	 ('150425','克什克腾旗',3,'150400','内蒙古自治区赤峰市克什克腾旗','150400'),
	 ('150426','翁牛特旗',3,'150400','内蒙古自治区赤峰市翁牛特旗','150400'),
	 ('150428','喀喇沁旗',3,'150400','内蒙古自治区赤峰市喀喇沁旗','150400'),
	 ('150429','宁城县',3,'150400','内蒙古自治区赤峰市宁城县','150400'),
	 ('150430','敖汉旗',3,'150400','内蒙古自治区赤峰市敖汉旗','150400');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('150500','通辽市',2,'150000','内蒙古自治区通辽市','150500'),
	 ('150502','科尔沁区',3,'150500','内蒙古自治区通辽市科尔沁区','150500'),
	 ('150521','科尔沁左翼中旗',3,'150500','内蒙古自治区通辽市科尔沁左翼中旗','150500'),
	 ('150522','科尔沁左翼后旗',3,'150500','内蒙古自治区通辽市科尔沁左翼后旗','150500'),
	 ('150523','开鲁县',3,'150500','内蒙古自治区通辽市开鲁县','150500'),
	 ('150524','库伦旗',3,'150500','内蒙古自治区通辽市库伦旗','150500'),
	 ('150525','奈曼旗',3,'150500','内蒙古自治区通辽市奈曼旗','150500'),
	 ('150526','扎鲁特旗',3,'150500','内蒙古自治区通辽市扎鲁特旗','150500'),
	 ('150571','通辽经济技术开发区',3,'150500','内蒙古自治区通辽市经济技术开发区','150500'),
	 ('150581','霍林郭勒市',3,'150500','内蒙古自治区通辽市霍林郭勒市','150500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('150600','鄂尔多斯市',2,'150000','内蒙古自治区鄂尔多斯市','150600'),
	 ('150602','东胜区',3,'150600','内蒙古自治区鄂尔多斯市东胜区','150600'),
	 ('150603','康巴什区',3,'150600','内蒙古自治区鄂尔多斯市康巴什区','150600'),
	 ('150621','达拉特旗',3,'150600','内蒙古自治区鄂尔多斯市达拉特旗','150600'),
	 ('150622','准格尔旗',3,'150600','内蒙古自治区鄂尔多斯市准格尔旗','150600'),
	 ('150623','鄂托克前旗',3,'150600','内蒙古自治区鄂尔多斯市鄂托克前旗','150600'),
	 ('150624','鄂托克旗',3,'150600','内蒙古自治区鄂尔多斯市鄂托克旗','150600'),
	 ('150625','杭锦旗',3,'150600','内蒙古自治区鄂尔多斯市杭锦旗','150600'),
	 ('150626','乌审旗',3,'150600','内蒙古自治区鄂尔多斯市乌审旗','150600'),
	 ('150627','伊金霍洛旗',3,'150600','内蒙古自治区鄂尔多斯市伊金霍洛旗','150600');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('150700','呼伦贝尔市',2,'150000','内蒙古自治区呼伦贝尔市','150700'),
	 ('150702','海拉尔区',3,'150700','内蒙古自治区呼伦贝尔市海拉尔区','150700'),
	 ('150703','扎赉诺尔区',3,'150700','内蒙古自治区呼伦贝尔市扎赉诺尔区','150700'),
	 ('150721','阿荣旗',3,'150700','内蒙古自治区呼伦贝尔市阿荣旗','150700'),
	 ('150722','莫力达瓦达斡尔族自治旗',3,'150700','内蒙古自治区呼伦贝尔市莫力达瓦达斡尔族自治旗','150700'),
	 ('150723','鄂伦春自治旗',3,'150700','内蒙古自治区呼伦贝尔市鄂伦春自治旗','150700'),
	 ('150724','鄂温克族自治旗',3,'150700','内蒙古自治区呼伦贝尔市鄂温克族自治旗','150700'),
	 ('150725','陈巴尔虎旗',3,'150700','内蒙古自治区呼伦贝尔市陈巴尔虎旗','150700'),
	 ('150726','新巴尔虎左旗',3,'150700','内蒙古自治区呼伦贝尔市新巴尔虎左旗','150700'),
	 ('150727','新巴尔虎右旗',3,'150700','内蒙古自治区呼伦贝尔市新巴尔虎右旗','150700');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('150781','满洲里市',3,'150700','内蒙古自治区呼伦贝尔市满洲里市','150700'),
	 ('150782','牙克石市',3,'150700','内蒙古自治区呼伦贝尔市牙克石市','150700'),
	 ('150783','扎兰屯市',3,'150700','内蒙古自治区呼伦贝尔市扎兰屯市','150700'),
	 ('150784','额尔古纳市',3,'150700','内蒙古自治区呼伦贝尔市额尔古纳市','150700'),
	 ('150785','根河市',3,'150700','内蒙古自治区呼伦贝尔市根河市','150700'),
	 ('150800','巴彦淖尔市',2,'150000','内蒙古自治区巴彦淖尔市','150800'),
	 ('150802','临河区',3,'150800','内蒙古自治区巴彦淖尔市临河区','150800'),
	 ('150821','五原县',3,'150800','内蒙古自治区巴彦淖尔市五原县','150800'),
	 ('150822','磴口县',3,'150800','内蒙古自治区巴彦淖尔市磴口县','150800'),
	 ('150823','乌拉特前旗',3,'150800','内蒙古自治区巴彦淖尔市乌拉特前旗','150800');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('150824','乌拉特中旗',3,'150800','内蒙古自治区巴彦淖尔市乌拉特中旗','150800'),
	 ('150825','乌拉特后旗',3,'150800','内蒙古自治区巴彦淖尔市乌拉特后旗','150800'),
	 ('150826','杭锦后旗',3,'150800','内蒙古自治区巴彦淖尔市杭锦后旗','150800'),
	 ('150871','巴彦淖尔经济开发区',3,'150800','内蒙古自治区巴彦淖尔市经济开发区','150800'),
	 ('150900','乌兰察布市',2,'150000','内蒙古自治区乌兰察布市','150900'),
	 ('150902','集宁区',3,'150900','内蒙古自治区乌兰察布市集宁区','150900'),
	 ('150921','卓资县',3,'150900','内蒙古自治区乌兰察布市卓资县','150900'),
	 ('150922','化德县',3,'150900','内蒙古自治区乌兰察布市化德县','150900'),
	 ('150923','商都县',3,'150900','内蒙古自治区乌兰察布市商都县','150900'),
	 ('150924','兴和县',3,'150900','内蒙古自治区乌兰察布市兴和县','150900');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('150925','凉城县',3,'150900','内蒙古自治区乌兰察布市凉城县','150900'),
	 ('150926','察哈尔右翼前旗',3,'150900','内蒙古自治区乌兰察布市察哈尔右翼前旗','150900'),
	 ('150927','察哈尔右翼中旗',3,'150900','内蒙古自治区乌兰察布市察哈尔右翼中旗','150900'),
	 ('150928','察哈尔右翼后旗',3,'150900','内蒙古自治区乌兰察布市察哈尔右翼后旗','150900'),
	 ('150929','四子王旗',3,'150900','内蒙古自治区乌兰察布市四子王旗','150900'),
	 ('150981','丰镇市',3,'150900','内蒙古自治区乌兰察布市丰镇市','150900'),
	 ('152200','兴安盟',2,'150000','内蒙古自治区兴安盟','152200'),
	 ('152201','乌兰浩特市',3,'152200','内蒙古自治区兴安盟乌兰浩特市','152200'),
	 ('152202','阿尔山',3,'152200','内蒙古自治区兴安盟阿尔山市','152200'),
	 ('152221','科尔沁右翼前旗',3,'152200','内蒙古自治区兴安盟科尔沁右翼前旗','152200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('152222','科尔沁右翼中旗',3,'152200','内蒙古自治区兴安盟科尔沁右翼中旗','152200'),
	 ('152223','扎赉特旗',3,'152200','内蒙古自治区兴安盟扎赉特旗','152200'),
	 ('152224','突泉县',3,'152200','内蒙古自治区兴安盟突泉县','152200'),
	 ('152500','锡林郭勒盟',2,'150000','内蒙古自治区锡林郭勒盟','152500'),
	 ('152501','二连浩特市',3,'152500','内蒙古自治区锡林郭勒盟二连浩特市','152500'),
	 ('152502','锡林浩特市',3,'152500','内蒙古自治区锡林郭勒盟锡林浩特市','152500'),
	 ('152522','阿巴嘎旗',3,'152500','内蒙古自治区锡林郭勒盟阿巴嘎旗','152500'),
	 ('152523','苏尼特左旗',3,'152500','内蒙古自治区锡林郭勒盟苏尼特左旗','152500'),
	 ('152524','苏尼特右旗',3,'152500','内蒙古自治区锡林郭勒盟苏尼特右旗','152500'),
	 ('152525','东乌珠穆沁旗',3,'152500','内蒙古自治区锡林郭勒盟东乌珠穆沁旗','152500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('152526','西乌珠穆沁旗',3,'152500','内蒙古自治区锡林郭勒盟西乌珠穆沁旗','152500'),
	 ('152527','太仆寺旗',3,'152500','内蒙古自治区锡林郭勒盟太仆寺旗','152500'),
	 ('152528','镶黄旗',3,'152500','内蒙古自治区锡林郭勒盟镶黄旗','152500'),
	 ('152529','正镶白旗',3,'152500','内蒙古自治区锡林郭勒盟正镶白旗','152500'),
	 ('152530','正蓝旗',3,'152500','内蒙古自治区锡林郭勒盟正蓝旗','152500'),
	 ('152531','多伦县',3,'152500','内蒙古自治区锡林郭勒盟多伦县','152500'),
	 ('152571','乌拉盖管理区',3,'152500','内蒙古自治区锡林郭勒盟乌拉盖管理区','152500'),
	 ('152900','阿拉善盟',2,'150000','内蒙古自治区阿拉善盟','152900'),
	 ('152921','阿拉善左旗',3,'152900','内蒙古自治区阿拉善盟阿拉善左旗','152900'),
	 ('152922','阿拉善右旗',3,'152900','内蒙古自治区阿拉善盟阿拉善右旗','152900');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('152923','额济纳旗',3,'152900','内蒙古自治区阿拉善盟额济纳旗','152900'),
	 ('152924','阿拉善经济开发区',3,'152900','内蒙古自治区阿拉善经济开发区','152900'),
	 ('210000','辽宁省',1,NULL,'辽宁省','210000'),
	 ('210100','沈阳市',2,'210000','辽宁省沈阳市','210100'),
	 ('210102','和平区',3,'210100','辽宁省沈阳市和平区','210100'),
	 ('210103','沈河区',3,'210100','辽宁省沈阳市沈河区','210100'),
	 ('210104','大东区',3,'210100','辽宁省沈阳市大东区','210100'),
	 ('210105','皇姑区',3,'210100','辽宁省沈阳市皇姑区','210100'),
	 ('210106','铁西区',3,'210100','辽宁省沈阳市铁西区','210100'),
	 ('210111','苏家屯区',3,'210100','辽宁省沈阳市苏家屯区','210100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('210112','浑南区',3,'210100','辽宁省沈阳市浑南区','210100'),
	 ('210113','沈北新区',3,'210100','辽宁省沈阳市沈北新区','210100'),
	 ('210114','于洪区',3,'210100','辽宁省沈阳市于洪区','210100'),
	 ('210115','辽中区',3,'210100','辽宁省沈阳市辽中区','210100'),
	 ('210123','康平县',3,'210100','辽宁省沈阳市康平县','210100'),
	 ('210124','法库县',3,'210100','辽宁省沈阳市法库县','210100'),
	 ('210181','新民市',3,'210100','辽宁省沈阳市新民市','210100'),
	 ('210200','大连市',2,'210000','辽宁省大连市','210200'),
	 ('210202','中山区',3,'210200','辽宁省大连市中山区','210200'),
	 ('210203','西岗区',3,'210200','辽宁省大连市西岗区','210200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('210204','沙河口区',3,'210200','辽宁省大连市沙河口区','210200'),
	 ('210211','甘井子区',3,'210200','辽宁省大连市甘井子区','210200'),
	 ('210212','旅顺口区',3,'210200','辽宁省大连市旅顺口区','210200'),
	 ('210213','金州区',3,'210200','辽宁省大连市金州区','210200'),
	 ('210214','普兰店区',3,'210200','辽宁省大连市普兰店区','210200'),
	 ('210224','长海县',3,'210200','辽宁省大连市长海县','210200'),
	 ('210281','瓦房店市',3,'210200','辽宁省大连市瓦房店市','210200'),
	 ('210283','庄河市',3,'210200','辽宁省大连市庄河市','210200'),
	 ('210300','鞍山市',2,'210000','辽宁省鞍山市','210300'),
	 ('210302','铁东区',3,'210300','辽宁省鞍山市铁东区','210300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('210303','铁西区',3,'210300','辽宁省鞍山市铁西区','210300'),
	 ('210304','立山区',3,'210300','辽宁省鞍山市立山区','210300'),
	 ('210311','千山区',3,'210300','辽宁省鞍山市千山区','210300'),
	 ('210321','台安县',3,'210300','辽宁省鞍山市台安县','210300'),
	 ('210323','岫岩满族自治县',3,'210300','辽宁省鞍山市岫岩满族自治县','210300'),
	 ('210381','海城市',3,'210300','辽宁省鞍山市海城市','210300'),
	 ('210400','抚顺市',2,'210000','辽宁省抚顺市','210400'),
	 ('210402','新抚区',3,'210400','辽宁省抚顺市新抚区','210400'),
	 ('210403','东洲区',3,'210400','辽宁省抚顺市东洲区','210400'),
	 ('210404','望花区',3,'210400','辽宁省抚顺市望花区','210400');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('210411','顺城区',3,'210400','辽宁省抚顺市顺城区','210400'),
	 ('210421','抚顺县',3,'210400','辽宁省抚顺市抚顺县','210400'),
	 ('210422','新宾满族自治县',3,'210400','辽宁省抚顺市新宾满族自治县','210400'),
	 ('210423','清原满族自治县',3,'210400','辽宁省抚顺市清原满族自治县','210400'),
	 ('210500','本溪市',2,'210000','辽宁省本溪市','210500'),
	 ('210502','平山区',3,'210500','辽宁省本溪市平山区','210500'),
	 ('210503','溪湖区',3,'210500','辽宁省本溪市溪湖区','210500'),
	 ('210504','明山区',3,'210500','辽宁省本溪市明山区','210500'),
	 ('210505','南芬区',3,'210500','辽宁省本溪市南芬区','210500'),
	 ('210521','本溪满族自治县',3,'210500','辽宁省本溪市本溪满族自治县','210500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('210522','桓仁满族自治县',3,'210500','辽宁省本溪市桓仁满族自治县','210500'),
	 ('210600','丹东市',2,'210000','辽宁省丹东市','210600'),
	 ('210602','元宝区',3,'210600','辽宁省丹东市元宝区','210600'),
	 ('210603','振兴区',3,'210600','辽宁省丹东市振兴区','210600'),
	 ('210604','振安区',3,'210600','辽宁省丹东市振安区','210600'),
	 ('210624','宽甸满族自治县',3,'210600','辽宁省丹东市宽甸满族自治县','210600'),
	 ('210681','东港市',3,'210600','辽宁省丹东市东港市','210600'),
	 ('210682','凤城市',3,'210600','辽宁省丹东市凤城市','210600'),
	 ('210700','锦州市',2,'210000','辽宁省锦州市','210700'),
	 ('210702','古塔区',3,'210700','辽宁省锦州市古塔区','210700');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('210703','凌河区',3,'210700','辽宁省锦州市凌河区','210700'),
	 ('210711','太和区',3,'210700','辽宁省锦州市太和区','210700'),
	 ('210726','黑山县',3,'210700','辽宁省锦州市黑山县','210700'),
	 ('210727','义县',3,'210700','辽宁省锦州市义县','210700'),
	 ('210781','凌海市',3,'210700','辽宁省锦州市凌海市','210700'),
	 ('210782','北镇市',3,'210700','辽宁省锦州市北镇市','210700'),
	 ('210800','营口市',2,'210000','辽宁省营口市','210800'),
	 ('210802','站前区',3,'210800','辽宁省营口市站前区','210800'),
	 ('210803','西市区',3,'210800','辽宁省营口市西市区','210800'),
	 ('210804','鲅鱼圈区',3,'210800','辽宁省营口市鲅鱼圈区','210800');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('210811','老边区',3,'210800','辽宁省营口市老边区','210800'),
	 ('210881','盖州市',3,'210800','辽宁省营口市盖州市','210800'),
	 ('210882','大石桥市',3,'210800','辽宁省营口市大石桥市','210800'),
	 ('210900','阜新市',2,'210000','辽宁省阜新市','210900'),
	 ('210902','海州区',3,'210900','辽宁省阜新市海州区','210900'),
	 ('210903','新邱区',3,'210900','辽宁省阜新市新邱区','210900'),
	 ('210904','太平区',3,'210900','辽宁省阜新市太平区','210900'),
	 ('210905','清河门区',3,'210900','辽宁省阜新市清河门区','210900'),
	 ('210911','细河区',3,'210900','辽宁省阜新市细河区','210900'),
	 ('210921','阜新蒙古族自治县',3,'210900','辽宁省阜新市阜新蒙古族自治县','210900');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('210922','彰武县',3,'210900','辽宁省阜新市彰武县','210900'),
	 ('211000','辽阳市',2,'210000','辽宁省辽阳市','211000'),
	 ('211002','白塔区',3,'211000','辽宁省辽阳市白塔区','211000'),
	 ('211003','文圣区',3,'211000','辽宁省辽阳市文圣区','211000'),
	 ('211004','宏伟区',3,'211000','辽宁省辽阳市宏伟区','211000'),
	 ('211005','弓长岭区',3,'211000','辽宁省辽阳市弓长岭区','211000'),
	 ('211011','太子河区',3,'211000','辽宁省辽阳市太子河区','211000'),
	 ('211021','辽阳县',3,'211000','辽宁省辽阳市辽阳县','211000'),
	 ('211081','灯塔市',3,'211000','辽宁省辽阳市灯塔市','211000'),
	 ('211100','盘锦市',2,'210000','辽宁省盘锦市','211100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('211102','双台子区',3,'211100','辽宁省盘锦市双台子区','211100'),
	 ('211103','兴隆台区',3,'211100','辽宁省盘锦市兴隆台区','211100'),
	 ('211104','大洼区',3,'211100','辽宁省盘锦市大洼区','211100'),
	 ('211122','盘山县',3,'211100','辽宁省盘锦市盘山县','211100'),
	 ('211200','铁岭市',2,'210000','辽宁省铁岭市','211200'),
	 ('211202','银州区',3,'211200','辽宁省铁岭市银州区','211200'),
	 ('211204','清河区',3,'211200','辽宁省铁岭市清河区','211200'),
	 ('211221','铁岭县',3,'211200','辽宁省铁岭市铁岭县','211200'),
	 ('211223','西丰县',3,'211200','辽宁省铁岭市西丰县','211200'),
	 ('211224','昌图县',3,'211200','辽宁省铁岭市昌图县','211200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('211281','调兵山市',3,'211200','辽宁省铁岭市调兵山市','211200'),
	 ('211282','开原市',3,'211200','辽宁省铁岭市开原市','211200'),
	 ('211300','朝阳市',2,'210000','辽宁省朝阳市','211300'),
	 ('211302','双塔区',3,'211300','辽宁省朝阳市双塔区','211300'),
	 ('211303','龙城区',3,'211300','辽宁省朝阳市龙城区','211300'),
	 ('211321','朝阳县',3,'211300','辽宁省朝阳市朝阳县','211300'),
	 ('211322','建平县',3,'211300','辽宁省朝阳市建平县','211300'),
	 ('211324','喀喇沁左翼蒙古族自治县',3,'211300','辽宁省朝阳市喀喇沁左翼蒙古族自治县','211300'),
	 ('211381','北票市',3,'211300','辽宁省朝阳市北票市','211300'),
	 ('211382','凌源市',3,'211300','辽宁省朝阳市凌源市','211300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('211400','葫芦岛市',2,'210000','辽宁省葫芦岛市','211400'),
	 ('211402','连山区',3,'211400','辽宁省葫芦岛市连山区','211400'),
	 ('211403','龙港区',3,'211400','辽宁省葫芦岛市龙港区','211400'),
	 ('211404','南票区',3,'211400','辽宁省葫芦岛市南票区','211400'),
	 ('211421','绥中县',3,'211400','辽宁省葫芦岛市绥中县','211400'),
	 ('211422','建昌县',3,'211400','辽宁省葫芦岛市建昌县','211400'),
	 ('211481','兴城市',3,'211400','辽宁省葫芦岛市兴城市','211400'),
	 ('220000','吉林省',1,NULL,'吉林省','220000'),
	 ('220100','长春市',2,'220000','吉林省长春市','220100'),
	 ('220102','南关区',3,'220100','吉林省长春市南关区','220100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('220103','宽城区',3,'220100','吉林省长春市宽城区','220100'),
	 ('220104','朝阳区',3,'220100','吉林省长春市朝阳区','220100'),
	 ('220105','二道区',3,'220100','吉林省长春市二道区','220100'),
	 ('220106','绿园区',3,'220100','吉林省长春市绿园区','220100'),
	 ('220112','双阳区',3,'220100','吉林省长春市双阳区','220100'),
	 ('220113','九台区',3,'220100','吉林省长春市九台区','220100'),
	 ('220122','农安县',3,'220100','吉林省长春市农安县','220100'),
	 ('220182','榆树市',3,'220100','吉林省长春市榆树市','220100'),
	 ('220183','德惠市',3,'220100','吉林省长春市德惠市','220100'),
	 ('220184','公主岭市',3,'220100','吉林省长春市公主岭市','220100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('220200','吉林市',2,'220000','吉林省吉林市','220200'),
	 ('220202','昌邑区',3,'220200','吉林省吉林市昌邑区','220200'),
	 ('220203','龙潭区',3,'220200','吉林省吉林市龙潭区','220200'),
	 ('220204','船营区',3,'220200','吉林省吉林市船营区','220200'),
	 ('220211','丰满区',3,'220200','吉林省吉林市丰满区','220200'),
	 ('220221','永吉县',3,'220200','吉林省吉林市永吉县','220200'),
	 ('220281','蛟河市',3,'220200','吉林省吉林市蛟河市','220200'),
	 ('220282','桦甸市',3,'220200','吉林省吉林市桦甸市','220200'),
	 ('220283','舒兰市',3,'220200','吉林省吉林市舒兰市','220200'),
	 ('220284','磐石市',3,'220200','吉林省吉林市磐石市','220200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('220300','四平市',2,'220000','吉林省四平市','220300'),
	 ('220302','铁西区',3,'220300','吉林省四平市铁西区','220300'),
	 ('220303','铁东区',3,'220300','吉林省四平市铁东区','220300'),
	 ('220322','梨树县',3,'220300','吉林省四平市梨树县','220300'),
	 ('220323','伊通满族自治县',3,'220300','吉林省四平市伊通满族自治县','220300'),
	 ('220382','双辽市',3,'220300','吉林省四平市双辽市','220300'),
	 ('220400','辽源市',2,'220000','吉林省辽源市','220400'),
	 ('220402','龙山区',3,'220400','吉林省辽源市龙山区','220400'),
	 ('220403','西安区',3,'220400','吉林省辽源市西安区','220400'),
	 ('220421','东丰县',3,'220400','吉林省辽源市东丰县','220400');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('220422','东辽县',3,'220400','吉林省辽源市东辽县','220400'),
	 ('220500','通化市',2,'220000','吉林省通化市','220500'),
	 ('220502','东昌区',3,'220500','吉林省通化市东昌区','220500'),
	 ('220503','二道江区',3,'220500','吉林省通化市二道江区','220500'),
	 ('220521','通化县',3,'220500','吉林省通化市通化县','220500'),
	 ('220523','辉南县',3,'220500','吉林省通化市辉南县','220500'),
	 ('220524','柳河县',3,'220500','吉林省通化市柳河县','220500'),
	 ('220581','梅河口市',3,'220500','吉林省通化市梅河口市','220500'),
	 ('220582','集安市',3,'220500','吉林省通化市集安市','220500'),
	 ('220600','白山市',2,'220000','吉林省白山市','220600');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('220602','浑江区',3,'220600','吉林省白山市浑江区','220600'),
	 ('220605','江源区',3,'220600','吉林省白山市江源区','220600'),
	 ('220621','抚松县',3,'220600','吉林省白山市抚松县','220600'),
	 ('220622','靖宇县',3,'220600','吉林省白山市靖宇县','220600'),
	 ('220623','长白朝鲜族自治县',3,'220600','吉林省白山市长白朝鲜族自治县','220600'),
	 ('220681','临江市',3,'220600','吉林省白山市临江市','220600'),
	 ('220700','松原市',2,'220000','吉林省松原市','220700'),
	 ('220702','宁江区',3,'220700','吉林省松原市宁江区','220700'),
	 ('220721','前郭尔罗斯蒙古族自治县',3,'220700','吉林省松原市前郭尔罗斯蒙古族自治县','220700'),
	 ('220722','长岭县',3,'220700','吉林省松原市长岭县','220700');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('220723','乾安县',3,'220700','吉林省松原市乾安县','220700'),
	 ('220781','扶余市',3,'220700','吉林省松原市扶余市','220700'),
	 ('220800','白城市',2,'220000','吉林省白城市','220800'),
	 ('220802','洮北区',3,'220800','吉林省白城市洮北区','220800'),
	 ('220821','镇赉县',3,'220800','吉林省白城市镇赉县','220800'),
	 ('220822','通榆县',3,'220800','吉林省白城市通榆县','220800'),
	 ('220881','洮南市',3,'220800','吉林省白城市洮南市','220800'),
	 ('220882','大安市',3,'220800','吉林省白城市大安市','220800'),
	 ('222400','延边朝鲜族自治州',2,'220000','吉林省延边朝鲜族自治州','222400'),
	 ('222401','延吉市',3,'222400','吉林省延边朝鲜族自治州延吉市','222400');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('222402','图们市',3,'222400','吉林省延边朝鲜族自治州图们市','222400'),
	 ('222403','敦化市',3,'222400','吉林省延边朝鲜族自治州敦化市','222400'),
	 ('222404','珲春市',3,'222400','吉林省延边朝鲜族自治州珲春市','222400'),
	 ('222405','龙井市',3,'222400','吉林省延边朝鲜族自治州龙井市','222400'),
	 ('222406','和龙市',3,'222400','吉林省延边朝鲜族自治州和龙市','222400'),
	 ('222424','汪清县',3,'222400','吉林省延边朝鲜族自治州汪清县','222400'),
	 ('222426','安图县',3,'222400','吉林省延边朝鲜族自治州安图县','222400'),
	 ('230000','黑龙江省',1,NULL,'黑龙江省','230000'),
	 ('230100','哈尔滨市',2,'230000','黑龙江省哈尔滨市','230100'),
	 ('230102','道里区',3,'230100','黑龙江省哈尔滨市道里区','230100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('230103','南岗区',3,'230100','黑龙江省哈尔滨市南岗区','230100'),
	 ('230104','道外区',3,'230100','黑龙江省哈尔滨市道外区','230100'),
	 ('230108','平房区',3,'230100','黑龙江省哈尔滨市平房区','230100'),
	 ('230109','松北区',3,'230100','黑龙江省哈尔滨市松北区','230100'),
	 ('230110','香坊区',3,'230100','黑龙江省哈尔滨市香坊区','230100'),
	 ('230111','呼兰区',3,'230100','黑龙江省哈尔滨市呼兰区','230100'),
	 ('230112','阿城区',3,'230100','黑龙江省哈尔滨市阿城区','230100'),
	 ('230113','双城区',3,'230100','黑龙江省哈尔滨市双城区','230100'),
	 ('230123','依兰县',3,'230100','黑龙江省哈尔滨市依兰县','230100'),
	 ('230124','方正县',3,'230100','黑龙江省哈尔滨市方正县','230100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('230125','宾县',3,'230100','黑龙江省哈尔滨市宾县','230100'),
	 ('230126','巴彦县',3,'230100','黑龙江省哈尔滨市巴彦县','230100'),
	 ('230127','木兰县',3,'230100','黑龙江省哈尔滨市木兰县','230100'),
	 ('230128','通河县',3,'230100','黑龙江省哈尔滨市通河县','230100'),
	 ('230129','延寿县',3,'230100','黑龙江省哈尔滨市延寿县','230100'),
	 ('230183','尚志市',3,'230100','黑龙江省哈尔滨市尚志市','230100'),
	 ('230184','五常市',3,'230100','黑龙江省哈尔滨市五常市','230100'),
	 ('230200','齐齐哈尔市',2,'230000','黑龙江省齐齐哈尔市','230200'),
	 ('230202','龙沙区',3,'230200','黑龙江省齐齐哈尔市龙沙区','230200'),
	 ('230203','建华区',3,'230200','黑龙江省齐齐哈尔市建华区','230200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('230204','铁锋区',3,'230200','黑龙江省齐齐哈尔市铁锋区','230200'),
	 ('230205','昂昂溪区',3,'230200','黑龙江省齐齐哈尔市昂昂溪区','230200'),
	 ('230206','富拉尔基区',3,'230200','黑龙江省齐齐哈尔市富拉尔基区','230200'),
	 ('230207','碾子山区',3,'230200','黑龙江省齐齐哈尔市碾子山区','230200'),
	 ('230208','梅里斯达斡尔族区',3,'230200','黑龙江省齐齐哈尔市梅里斯达斡尔族区','230200'),
	 ('230221','龙江县',3,'230200','黑龙江省齐齐哈尔市龙江县','230200'),
	 ('230223','依安县',3,'230200','黑龙江省齐齐哈尔市依安县','230200'),
	 ('230224','泰来县',3,'230200','黑龙江省齐齐哈尔市泰来县','230200'),
	 ('230225','甘南县',3,'230200','黑龙江省齐齐哈尔市甘南县','230200'),
	 ('230227','富裕县',3,'230200','黑龙江省齐齐哈尔市富裕县','230200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('230229','克山县',3,'230200','黑龙江省齐齐哈尔市克山县','230200'),
	 ('230230','克东县',3,'230200','黑龙江省齐齐哈尔市克东县','230200'),
	 ('230231','拜泉县',3,'230200','黑龙江省齐齐哈尔市拜泉县','230200'),
	 ('230281','讷河市',3,'230200','黑龙江省齐齐哈尔市讷河市','230200'),
	 ('230300','鸡西市',2,'230000','黑龙江省鸡西市','230300'),
	 ('230302','鸡冠区',3,'230300','黑龙江省鸡西市鸡冠区','230300'),
	 ('230303','恒山区',3,'230300','黑龙江省鸡西市恒山区','230300'),
	 ('230304','滴道区',3,'230300','黑龙江省鸡西市滴道区','230300'),
	 ('230305','梨树区',3,'230300','黑龙江省鸡西市梨树区','230300'),
	 ('230306','城子河区',3,'230300','黑龙江省鸡西市城子河区','230300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('230307','麻山区',3,'230300','黑龙江省鸡西市麻山区','230300'),
	 ('230321','鸡东县',3,'230300','黑龙江省鸡西市鸡东县','230300'),
	 ('230381','虎林市',3,'230300','黑龙江省鸡西市虎林市','230300'),
	 ('230382','密山市',3,'230300','黑龙江省鸡西市密山市','230300'),
	 ('230400','鹤岗市',2,'230000','黑龙江省鹤岗市','230400'),
	 ('230402','向阳区',3,'230400','黑龙江省鹤岗市向阳区','230400'),
	 ('230403','工农区',3,'230400','黑龙江省鹤岗市工农区','230400'),
	 ('230404','南山区',3,'230400','黑龙江省鹤岗市南山区','230400'),
	 ('230405','兴安区',3,'230400','黑龙江省鹤岗市兴安区','230400'),
	 ('230406','东山区',3,'230400','黑龙江省鹤岗市东山区','230400');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('230407','兴山区',3,'230400','黑龙江省鹤岗市兴山区','230400'),
	 ('230421','萝北县',3,'230400','黑龙江省鹤岗市萝北县','230400'),
	 ('230422','绥滨县',3,'230400','黑龙江省鹤岗市绥滨县','230400'),
	 ('230500','双鸭山市',2,'230000','黑龙江省双鸭山市','230500'),
	 ('230502','尖山区',3,'230500','黑龙江省双鸭山市尖山区','230500'),
	 ('230503','岭东区',3,'230500','黑龙江省双鸭山市岭东区','230500'),
	 ('230505','四方台区',3,'230500','黑龙江省双鸭山市四方台区','230500'),
	 ('230506','宝山区',3,'230500','黑龙江省双鸭山市宝山区','230500'),
	 ('230521','集贤县',3,'230500','黑龙江省双鸭山市集贤县','230500'),
	 ('230522','友谊县',3,'230500','黑龙江省双鸭山市友谊县','230500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('230523','宝清县',3,'230500','黑龙江省双鸭山市宝清县','230500'),
	 ('230524','饶河县',3,'230500','黑龙江省双鸭山市饶河县','230500'),
	 ('230600','大庆市',2,'230000','黑龙江省大庆市','230600'),
	 ('230602','萨尔图区',3,'230600','黑龙江省大庆市萨尔图区','230600'),
	 ('230603','龙凤区',3,'230600','黑龙江省大庆市龙凤区','230600'),
	 ('230604','让胡路区',3,'230600','黑龙江省大庆市让胡路区','230600'),
	 ('230605','红岗区',3,'230600','黑龙江省大庆市红岗区','230600'),
	 ('230606','大同区',3,'230600','黑龙江省大庆市大同区','230600'),
	 ('230621','肇州县',3,'230600','黑龙江省大庆市肇州县','230600'),
	 ('230622','肇源县',3,'230600','黑龙江省大庆市肇源县','230600');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('230623','林甸县',3,'230600','黑龙江省大庆市林甸县','230600'),
	 ('230624','杜尔伯特蒙古族自治县',3,'230600','黑龙江省大庆市杜尔伯特蒙古族自治县','230600'),
	 ('230700','伊春市',2,'230000','黑龙江省伊春市','230700'),
	 ('230717','伊美区',3,'230700','黑龙江省伊春市伊美区','230700'),
	 ('230718','乌翠区',3,'230700','黑龙江省伊春市乌翠区','230700'),
	 ('230719','友好区',3,'230700','黑龙江省伊春市友好区','230700'),
	 ('230722','嘉荫县',3,'230700','黑龙江省伊春市嘉荫县','230700'),
	 ('230723','汤旺县',3,'230700','黑龙江省伊春市汤旺县','230700'),
	 ('230724','丰林县',3,'230700','黑龙江省伊春市丰林县','230700'),
	 ('230725','大箐山县',3,'230700','黑龙江省伊春市大箐山县','230700');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('230726','南岔县',3,'230700','黑龙江省伊春市南岔县','230700'),
	 ('230751','金林区',3,'230700','黑龙江省伊春市金林区','230700'),
	 ('230781','铁力市',3,'230700','黑龙江省伊春市铁力市','230700'),
	 ('230800','佳木斯市',2,'230000','黑龙江省佳木斯市','230800'),
	 ('230803','向阳区',3,'230800','黑龙江省佳木斯市向阳区','230800'),
	 ('230804','前进区',3,'230800','黑龙江省佳木斯市前进区','230800'),
	 ('230805','东风区',3,'230800','黑龙江省佳木斯市东风区','230800'),
	 ('230811','郊区',3,'230800','黑龙江省佳木斯市郊区','230800'),
	 ('230822','桦南县',3,'230800','黑龙江省佳木斯市桦南县','230800'),
	 ('230826','桦川县',3,'230800','黑龙江省佳木斯市桦川县','230800');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('230828','汤原县',3,'230800','黑龙江省佳木斯市汤原县','230800'),
	 ('230881','同江市',3,'230800','黑龙江省佳木斯市同江市','230800'),
	 ('230882','富锦市',3,'230800','黑龙江省佳木斯市富锦市','230800'),
	 ('230883','抚远市',3,'230800','黑龙江省佳木斯市抚远市','230800'),
	 ('230900','七台河市',2,'230000','黑龙江省七台河市','230900'),
	 ('230902','新兴区',3,'230900','黑龙江省七台河市新兴区','230900'),
	 ('230903','桃山区',3,'230900','黑龙江省七台河市桃山区','230900'),
	 ('230904','茄子河区',3,'230900','黑龙江省七台河市茄子河区','230900'),
	 ('230921','勃利县',3,'230900','黑龙江省七台河市勃利县','230900'),
	 ('231000','牡丹江市',2,'230000','黑龙江省牡丹江市','231000');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('231002','东安区',3,'231000','黑龙江省牡丹江市东安区','231000'),
	 ('231003','阳明区',3,'231000','黑龙江省牡丹江市阳明区','231000'),
	 ('231004','爱民区',3,'231000','黑龙江省牡丹江市爱民区','231000'),
	 ('231005','西安区',3,'231000','黑龙江省牡丹江市西安区','231000'),
	 ('231025','林口县',3,'231000','黑龙江省牡丹江市林口县','231000'),
	 ('231081','绥芬河市',3,'231000','黑龙江省牡丹江市绥芬河市','231000'),
	 ('231083','海林市',3,'231000','黑龙江省牡丹江市海林市','231000'),
	 ('231084','宁安市',3,'231000','黑龙江省牡丹江市宁安市','231000'),
	 ('231085','穆棱市',3,'231000','黑龙江省牡丹江市穆棱市','231000'),
	 ('231086','东宁市',3,'231000','黑龙江省牡丹江市东宁市','231000');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('231100','黑河市',2,'230000','黑龙江省黑河市','231100'),
	 ('231102','爱辉区',3,'231100','黑龙江省黑河市爱辉区','231100'),
	 ('231123','逊克县',3,'231100','黑龙江省黑河市逊克县','231100'),
	 ('231124','孙吴县',3,'231100','黑龙江省黑河市孙吴县','231100'),
	 ('231181','北安市',3,'231100','黑龙江省黑河市北安市','231100'),
	 ('231182','五大连池市',3,'231100','黑龙江省黑河市五大连池市','231100'),
	 ('231183','嫩江市',3,'231100','黑龙江省黑河市嫩江市','231100'),
	 ('231200','绥化市',2,'230000','黑龙江省绥化市','231200'),
	 ('231202','北林区',3,'231200','黑龙江省绥化市北林区','231200'),
	 ('231221','望奎县',3,'231200','黑龙江省绥化市望奎县','231200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('231222','兰西县',3,'231200','黑龙江省绥化市兰西县','231200'),
	 ('231223','青冈县',3,'231200','黑龙江省绥化市青冈县','231200'),
	 ('231224','庆安县',3,'231200','黑龙江省绥化市庆安县','231200'),
	 ('231225','明水县',3,'231200','黑龙江省绥化市明水县','231200'),
	 ('231226','绥棱县',3,'231200','黑龙江省绥化市绥棱县','231200'),
	 ('231281','安达市',3,'231200','黑龙江省绥化市安达市','231200'),
	 ('231282','肇东市',3,'231200','黑龙江省绥化市肇东市','231200'),
	 ('231283','海伦市',3,'231200','黑龙江省绥化市海伦市','231200'),
	 ('232700','大兴安岭地区',2,'230000','黑龙江省大兴安岭地区','232700'),
	 ('232701','漠河市',3,'232700','黑龙江省大兴安岭地区漠河市','232700');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('232718','加格达奇区',3,'232700','黑龙江省大兴安岭地区加格达奇区','232700'),
	 ('232721','呼玛县',3,'232700','黑龙江省大兴安岭地区呼玛县','232700'),
	 ('232722','塔河县',3,'232700','黑龙江省大兴安岭地区塔河县','232700'),
	 ('310000','上海市',1,NULL,'上海市','310000'),
	 ('310101','黄浦区',2,'310000','上海市黄浦区','310101'),
	 ('310104','徐汇区',2,'310000','上海市徐汇区','310104'),
	 ('310105','长宁区',2,'310000','上海市长宁区','310105'),
	 ('310106','静安区',2,'310000','上海市静安区','310106'),
	 ('310107','普陀区',2,'310000','上海市普陀区','310107'),
	 ('310109','虹口区',2,'310000','上海市虹口区','310109');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('310110','杨浦区',2,'310000','上海市杨浦区','310110'),
	 ('310112','闵行区',2,'310000','上海市闵行区','310112'),
	 ('310113','宝山区',2,'310000','上海市宝山区','310113'),
	 ('310114','嘉定区',2,'310000','上海市嘉定区','310114'),
	 ('310115','浦东新区',2,'310000','上海市浦东新区','310115'),
	 ('310116','金山区',2,'310000','上海市金山区','310116'),
	 ('310117','松江区',2,'310000','上海市松江区','310117'),
	 ('310118','青浦区',2,'310000','上海市青浦区','310118'),
	 ('310120','奉贤区',2,'310000','上海市奉贤区','310120'),
	 ('310151','崇明区',2,'310000','上海市崇明区','310151');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('320000','江苏省',1,NULL,'江苏省','320000'),
	 ('320100','南京市',2,'320000','江苏省南京市','320100'),
	 ('320102','玄武区',3,'320100','江苏省南京市玄武区','320100'),
	 ('320104','秦淮区',3,'320100','江苏省南京市秦淮区','320100'),
	 ('320105','建邺区',3,'320100','江苏省南京市建邺区','320100'),
	 ('320106','鼓楼区',3,'320100','江苏省南京市鼓楼区','320100'),
	 ('320111','浦口区',3,'320100','江苏省南京市浦口区','320100'),
	 ('320113','栖霞区',3,'320100','江苏省南京市栖霞区','320100'),
	 ('320114','雨花台区',3,'320100','江苏省南京市雨花台区','320100'),
	 ('320115','江宁区',3,'320100','江苏省南京市江宁区','320100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('320116','六合区',3,'320100','江苏省南京市六合区','320100'),
	 ('320117','溧水区',3,'320100','江苏省南京市溧水区','320100'),
	 ('320118','高淳区',3,'320100','江苏省南京市高淳区','320100'),
	 ('320200','无锡市',2,'320000','江苏省无锡市','320200'),
	 ('320205','锡山区',3,'320200','江苏省无锡市锡山区','320200'),
	 ('320206','惠山区',3,'320200','江苏省无锡市惠山区','320200'),
	 ('320211','滨湖区',3,'320200','江苏省无锡市滨湖区','320200'),
	 ('320213','梁溪区',3,'320200','江苏省无锡市梁溪区','320200'),
	 ('320214','新吴区',3,'320200','江苏省无锡市新吴区','320200'),
	 ('320281','江阴市',3,'320200','江苏省无锡市江阴市','320200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('320282','宜兴市',3,'320200','江苏省无锡市宜兴市','320200'),
	 ('320300','徐州市',2,'320000','江苏省徐州市','320300'),
	 ('320302','鼓楼区',3,'320300','江苏省徐州市鼓楼区','320300'),
	 ('320303','云龙区',3,'320300','江苏省徐州市云龙区','320300'),
	 ('320305','贾汪区',3,'320300','江苏省徐州市贾汪区','320300'),
	 ('320311','泉山区',3,'320300','江苏省徐州市泉山区','320300'),
	 ('320312','铜山区',3,'320300','江苏省徐州市铜山区','320300'),
	 ('320321','丰县',3,'320300','江苏省徐州市丰县','320300'),
	 ('320322','沛县',3,'320300','江苏省徐州市沛县','320300'),
	 ('320324','睢宁县',3,'320300','江苏省徐州市睢宁县','320300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('320381','新沂市',3,'320300','江苏省徐州市新沂市','320300'),
	 ('320382','邳州市',3,'320300','江苏省徐州市邳州市','320300'),
	 ('320400','常州市',2,'320000','江苏省常州市','320400'),
	 ('320402','天宁区',3,'320400','江苏省常州市天宁区','320400'),
	 ('320404','钟楼区',3,'320400','江苏省常州市钟楼区','320400'),
	 ('320411','新北区',3,'320400','江苏省常州市新北区','320400'),
	 ('320412','武进区',3,'320400','江苏省常州市武进区','320400'),
	 ('320413','金坛区',3,'320400','江苏省常州市金坛区','320400'),
	 ('320481','溧阳市',3,'320400','江苏省常州市溧阳市','320400'),
	 ('320500','苏州市',2,'320000','江苏省苏州市','320500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('320505','虎丘区',3,'320500','江苏省苏州市虎丘区','320500'),
	 ('320506','吴中区',3,'320500','江苏省苏州市吴中区','320500'),
	 ('320507','相城区',3,'320500','江苏省苏州市相城区','320500'),
	 ('320508','姑苏区',3,'320500','江苏省苏州市姑苏区','320500'),
	 ('320509','吴江区',3,'320500','江苏省苏州市吴江区','320500'),
	 ('320581','常熟市',3,'320500','江苏省苏州市常熟市','320500'),
	 ('320582','张家港市',3,'320500','江苏省苏州市张家港市','320500'),
	 ('320583','昆山市',3,'320500','江苏省苏州市昆山市','320500'),
	 ('320585','太仓市',3,'320500','江苏省苏州市太仓市','320500'),
	 ('320600','南通市',2,'320000','江苏省南通市','320600');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('320602','崇川区',3,'320600','江苏省南通市崇川区','320600'),
	 ('320612','通州区',3,'320600','江苏省南通市通州区','320600'),
	 ('320623','如东县',3,'320600','江苏省南通市如东县','320600'),
	 ('320681','启东市',3,'320600','江苏省南通市启东市','320600'),
	 ('320682','如皋市',3,'320600','江苏省南通市如皋市','320600'),
	 ('320684','海门区',3,'320600','江苏省南通市海门区','320600'),
	 ('320685','海安市',3,'320600','江苏省南通市海安市','320600'),
	 ('320700','连云港市',2,'320000','江苏省连云港市','320700'),
	 ('320703','连云区',3,'320700','江苏省连云港市连云区','320700'),
	 ('320706','海州区',3,'320700','江苏省连云港市海州区','320700');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('320707','赣榆区',3,'320700','江苏省连云港市赣榆区','320700'),
	 ('320722','东海县',3,'320700','江苏省连云港市东海县','320700'),
	 ('320723','灌云县',3,'320700','江苏省连云港市灌云县','320700'),
	 ('320724','灌南县',3,'320700','江苏省连云港市灌南县','320700'),
	 ('320800','淮安市',2,'320000','江苏省淮安市','320800'),
	 ('320803','淮安区',3,'320800','江苏省淮安市淮安区','320800'),
	 ('320804','淮阴区',3,'320800','江苏省淮安市淮阴区','320800'),
	 ('320812','清江浦区',3,'320800','江苏省淮安市清江浦区','320800'),
	 ('320813','洪泽区',3,'320800','江苏省淮安市洪泽区','320800'),
	 ('320826','涟水县',3,'320800','江苏省淮安市涟水县','320800');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('320830','盱眙县',3,'320800','江苏省淮安市盱眙县','320800'),
	 ('320831','金湖县',3,'320800','江苏省淮安市金湖县','320800'),
	 ('320900','盐城市',2,'320000','江苏省盐城市','320900'),
	 ('320902','亭湖区',3,'320900','江苏省盐城市亭湖区','320900'),
	 ('320903','盐都区',3,'320900','江苏省盐城市盐都区','320900'),
	 ('320904','大丰区',3,'320900','江苏省盐城市大丰区','320900'),
	 ('320921','响水县',3,'320900','江苏省盐城市响水县','320900'),
	 ('320922','滨海县',3,'320900','江苏省盐城市滨海县','320900'),
	 ('320923','阜宁县',3,'320900','江苏省盐城市阜宁县','320900'),
	 ('320924','射阳县',3,'320900','江苏省盐城市射阳县','320900');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('320925','建湖县',3,'320900','江苏省盐城市建湖县','320900'),
	 ('320981','东台市',3,'320900','江苏省盐城市东台市','320900'),
	 ('321000','扬州市',2,'320000','江苏省扬州市','321000'),
	 ('321002','广陵区',3,'321000','江苏省扬州市广陵区','321000'),
	 ('321003','邗江区',3,'321000','江苏省扬州市邗江区','321000'),
	 ('321012','江都区',3,'321000','江苏省扬州市江都区','321000'),
	 ('321023','宝应县',3,'321000','江苏省扬州市宝应县','321000'),
	 ('321081','仪征市',3,'321000','江苏省扬州市仪征市','321000'),
	 ('321084','高邮市',3,'321000','江苏省扬州市高邮市','321000'),
	 ('321100','镇江市',2,'320000','江苏省镇江市','321100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('321102','京口区',3,'321100','江苏省镇江市京口区','321100'),
	 ('321111','润州区',3,'321100','江苏省镇江市润州区','321100'),
	 ('321112','丹徒区',3,'321100','江苏省镇江市丹徒区','321100'),
	 ('321181','丹阳市',3,'321100','江苏省镇江市丹阳市','321100'),
	 ('321182','扬中市',3,'321100','江苏省镇江市扬中市','321100'),
	 ('321183','句容市',3,'321100','江苏省镇江市句容市','321100'),
	 ('321200','泰州市',2,'320000','江苏省泰州市','321200'),
	 ('321202','海陵区',3,'321200','江苏省泰州市海陵区','321200'),
	 ('321203','高港区',3,'321200','江苏省泰州市高港区','321200'),
	 ('321204','姜堰区',3,'321200','江苏省泰州市姜堰区','321200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('321281','兴化市',3,'321200','江苏省泰州市兴化市','321200'),
	 ('321282','靖江市',3,'321200','江苏省泰州市靖江市','321200'),
	 ('321283','泰兴市',3,'321200','江苏省泰州市泰兴市','321200'),
	 ('321300','宿迁市',2,'320000','江苏省宿迁市','321300'),
	 ('321302','宿城区',3,'321300','江苏省宿迁市宿城区','321300'),
	 ('321311','宿豫区',3,'321300','江苏省宿迁市宿豫区','321300'),
	 ('321322','沭阳县',3,'321300','江苏省宿迁市沭阳县','321300'),
	 ('321323','泗阳县',3,'321300','江苏省宿迁市泗阳县','321300'),
	 ('321324','泗洪县',3,'321300','江苏省宿迁市泗洪县','321300'),
	 ('330000','浙江省',1,NULL,'浙江省','330000');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('330100','杭州市',2,'330000','浙江省杭州市','330100'),
	 ('330102','上城区',3,'330100','浙江省杭州市上城区','330100'),
	 ('330105','拱墅区',3,'330100','浙江省杭州市拱墅区','330100'),
	 ('330106','西湖区',3,'330100','浙江省杭州市西湖区','330100'),
	 ('330108','滨江区',3,'330100','浙江省杭州市滨江区','330100'),
	 ('330109','萧山区',3,'330100','浙江省杭州市萧山区','330100'),
	 ('330110','余杭区',3,'330100','浙江省杭州市余杭区','330100'),
	 ('330111','富阳区',3,'330100','浙江省杭州市富阳区','330100'),
	 ('330112','临安区',3,'330100','浙江省杭州市临安区','330100'),
	 ('330113','临平区',3,'330100','浙江省杭州市临平区','330100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('330114','钱塘区',3,'330100','浙江省杭州市钱塘区','330100'),
	 ('330122','桐庐县',3,'330100','浙江省杭州市桐庐县','330100'),
	 ('330127','淳安县',3,'330100','浙江省杭州市淳安县','330100'),
	 ('330182','建德市',3,'330100','浙江省杭州市建德市','330100'),
	 ('330200','宁波市',2,'330000','浙江省宁波市','330200'),
	 ('330203','海曙区',3,'330200','浙江省宁波市海曙区','330200'),
	 ('330205','江北区',3,'330200','浙江省宁波市江北区','330200'),
	 ('330206','北仑区',3,'330200','浙江省宁波市北仑区','330200'),
	 ('330211','镇海区',3,'330200','浙江省宁波市镇海区','330200'),
	 ('330212','鄞州区',3,'330200','浙江省宁波市鄞州区','330200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('330213','奉化区',3,'330200','浙江省宁波市奉化区','330200'),
	 ('330225','象山县',3,'330200','浙江省宁波市象山县','330200'),
	 ('330226','宁海县',3,'330200','浙江省宁波市宁海县','330200'),
	 ('330281','余姚市',3,'330200','浙江省宁波市余姚市','330200'),
	 ('330282','慈溪市',3,'330200','浙江省宁波市慈溪市','330200'),
	 ('330300','温州市',2,'330000','浙江省温州市','330300'),
	 ('330302','鹿城区',3,'330300','浙江省温州市鹿城区','330300'),
	 ('330303','龙湾区',3,'330300','浙江省温州市龙湾区','330300'),
	 ('330304','瓯海区',3,'330300','浙江省温州市瓯海区','330300'),
	 ('330305','洞头区',3,'330300','浙江省温州市洞头区','330300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('330324','永嘉县',3,'330300','浙江省温州市永嘉县','330300'),
	 ('330326','平阳县',3,'330300','浙江省温州市平阳县','330300'),
	 ('330327','苍南县',3,'330300','浙江省温州市苍南县','330300'),
	 ('330328','文成县',3,'330300','浙江省温州市文成县','330300'),
	 ('330329','泰顺县',3,'330300','浙江省温州市泰顺县','330300'),
	 ('330381','瑞安市',3,'330300','浙江省温州市瑞安市','330300'),
	 ('330382','乐清市',3,'330300','浙江省温州市乐清市','330300'),
	 ('330383','龙港市',3,'330300','浙江省温州市龙港市','330300'),
	 ('330400','嘉兴市',2,'330000','浙江省嘉兴市','330400'),
	 ('330402','南湖区',3,'330400','浙江省嘉兴市南湖区','330400');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('330411','秀洲区',3,'330400','浙江省嘉兴市秀洲区','330400'),
	 ('330421','嘉善县',3,'330400','浙江省嘉兴市嘉善县','330400'),
	 ('330424','海盐县',3,'330400','浙江省嘉兴市海盐县','330400'),
	 ('330481','海宁市',3,'330400','浙江省嘉兴市海宁市','330400'),
	 ('330482','平湖市',3,'330400','浙江省嘉兴市平湖市','330400'),
	 ('330483','桐乡市',3,'330400','浙江省嘉兴市桐乡市','330400'),
	 ('330500','湖州市',2,'330000','浙江省湖州市','330500'),
	 ('330502','吴兴区',3,'330500','浙江省湖州市吴兴区','330500'),
	 ('330503','南浔区',3,'330500','浙江省湖州市南浔区','330500'),
	 ('330521','德清县',3,'330500','浙江省湖州市德清县','330500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('330522','长兴县',3,'330500','浙江省湖州市长兴县','330500'),
	 ('330523','安吉县',3,'330500','浙江省湖州市安吉县','330500'),
	 ('330600','绍兴市',2,'330000','浙江省绍兴市','330600'),
	 ('330602','越城区',3,'330600','浙江省绍兴市越城区','330600'),
	 ('330603','柯桥区',3,'330600','浙江省绍兴市柯桥区','330600'),
	 ('330604','上虞区',3,'330600','浙江省绍兴市上虞区','330600'),
	 ('330624','新昌县',3,'330600','浙江省绍兴市新昌县','330600'),
	 ('330681','诸暨市',3,'330600','浙江省绍兴市诸暨市','330600'),
	 ('330683','嵊州市',3,'330600','浙江省绍兴市嵊州市','330600'),
	 ('330700','金华市',2,'330000','浙江省金华市','330700');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('330702','婺城区',3,'330700','浙江省金华市婺城区','330700'),
	 ('330703','金东区',3,'330700','浙江省金华市金东区','330700'),
	 ('330723','武义县',3,'330700','浙江省金华市武义县','330700'),
	 ('330726','浦江县',3,'330700','浙江省金华市浦江县','330700'),
	 ('330727','磐安县',3,'330700','浙江省金华市磐安县','330700'),
	 ('330781','兰溪市',3,'330700','浙江省金华市兰溪市','330700'),
	 ('330782','义乌市',3,'330700','浙江省金华市义乌市','330700'),
	 ('330783','东阳市',3,'330700','浙江省金华市东阳市','330700'),
	 ('330784','永康市',3,'330700','浙江省金华市永康市','330700'),
	 ('330800','衢州市',2,'330000','浙江省衢州市','330800');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('330802','柯城区',3,'330800','浙江省衢州市柯城区','330800'),
	 ('330803','衢江区',3,'330800','浙江省衢州市衢江区','330800'),
	 ('330822','常山县',3,'330800','浙江省衢州市常山县','330800'),
	 ('330824','开化县',3,'330800','浙江省衢州市开化县','330800'),
	 ('330825','龙游县',3,'330800','浙江省衢州市龙游县','330800'),
	 ('330881','江山市',3,'330800','浙江省衢州市江山市','330800'),
	 ('330900','舟山市',2,'330000','浙江省舟山市','330900'),
	 ('330902','定海区',3,'330900','浙江省舟山市定海区','330900'),
	 ('330903','普陀区',3,'330900','浙江省舟山市普陀区','330900'),
	 ('330921','岱山县',3,'330900','浙江省舟山市岱山县','330900');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('330922','嵊泗县',3,'330900','浙江省舟山市嵊泗县','330900'),
	 ('331000','台州市',2,'330000','浙江省台州市','331000'),
	 ('331002','椒江区',3,'331000','浙江省台州市椒江区','331000'),
	 ('331003','黄岩区',3,'331000','浙江省台州市黄岩区','331000'),
	 ('331004','路桥区',3,'331000','浙江省台州市路桥区','331000'),
	 ('331022','三门县',3,'331000','浙江省台州市三门县','331000'),
	 ('331023','天台县',3,'331000','浙江省台州市天台县','331000'),
	 ('331024','仙居县',3,'331000','浙江省台州市仙居县','331000'),
	 ('331081','温岭市',3,'331000','浙江省台州市温岭市','331000'),
	 ('331082','临海市',3,'331000','浙江省台州市临海市','331000');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('331083','玉环市',3,'331000','浙江省台州市玉环市','331000'),
	 ('331100','丽水市',2,'330000','浙江省丽水市','331100'),
	 ('331102','莲都区',3,'331100','浙江省丽水市莲都区','331100'),
	 ('331121','青田县',3,'331100','浙江省丽水市青田县','331100'),
	 ('331122','缙云县',3,'331100','浙江省丽水市缙云县','331100'),
	 ('331123','遂昌县',3,'331100','浙江省丽水市遂昌县','331100'),
	 ('331124','松阳县',3,'331100','浙江省丽水市松阳县','331100'),
	 ('331125','云和县',3,'331100','浙江省丽水市云和县','331100'),
	 ('331126','庆元县',3,'331100','浙江省丽水市庆元县','331100'),
	 ('331127','景宁畲族自治县',3,'331100','浙江省丽水市景宁畲族自治县','331100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('331181','龙泉市',3,'331100','浙江省丽水市龙泉市','331100'),
	 ('340000','安徽省',1,NULL,'安徽省','340000'),
	 ('340100','合肥市',2,'340000','安徽省合肥市','340100'),
	 ('340102','瑶海区',3,'340100','安徽省合肥市瑶海区','340100'),
	 ('340103','庐阳区',3,'340100','安徽省合肥市庐阳区','340100'),
	 ('340104','蜀山区',3,'340100','安徽省合肥市蜀山区','340100'),
	 ('340111','包河区',3,'340100','安徽省合肥市包河区','340100'),
	 ('340121','长丰县',3,'340100','安徽省合肥市长丰县','340100'),
	 ('340122','肥东县',3,'340100','安徽省合肥市肥东县','340100'),
	 ('340123','肥西县',3,'340100','安徽省合肥市肥西县','340100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('340124','庐江县',3,'340100','安徽省合肥市庐江县','340100'),
	 ('340181','巢湖市',3,'340100','安徽省合肥市巢湖市','340100'),
	 ('340200','芜湖市',2,'340000','安徽省芜湖市','340200'),
	 ('340202','镜湖区',3,'340200','安徽省芜湖市镜湖区','340200'),
	 ('340207','鸠江区',3,'340200','安徽省芜湖市鸠江区','340200'),
	 ('340209','弋江区',3,'340200','安徽省芜湖市弋江区','340200'),
	 ('340210','湾沚区',3,'340200','安徽省芜湖市湾沚区','340200'),
	 ('340211','繁昌区',3,'340200','安徽省芜湖市繁昌区','340200'),
	 ('340223','南陵县',3,'340200','安徽省芜湖市南陵县','340200'),
	 ('340281','无为市',3,'340200','安徽省芜湖市无为市','340200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('340300','蚌埠市',2,'340000','安徽省蚌埠市','340300'),
	 ('340302','龙子湖区',3,'340300','安徽省蚌埠市龙子湖区','340300'),
	 ('340303','蚌山区',3,'340300','安徽省蚌埠市蚌山区','340300'),
	 ('340304','禹会区',3,'340300','安徽省蚌埠市禹会区','340300'),
	 ('340311','淮上区',3,'340300','安徽省蚌埠市淮上区','340300'),
	 ('340321','怀远县',3,'340300','安徽省蚌埠市怀远县','340300'),
	 ('340322','五河县',3,'340300','安徽省蚌埠市五河县','340300'),
	 ('340323','固镇县',3,'340300','安徽省蚌埠市固镇县','340300'),
	 ('340400','淮南市',2,'340000','安徽省淮南市','340400'),
	 ('340402','大通区',3,'340400','安徽省淮南市大通区','340400');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('340403','田家庵区',3,'340400','安徽省淮南市田家庵区','340400'),
	 ('340404','谢家集区',3,'340400','安徽省淮南市谢家集区','340400'),
	 ('340405','八公山区',3,'340400','安徽省淮南市八公山区','340400'),
	 ('340406','潘集区',3,'340400','安徽省淮南市潘集区','340400'),
	 ('340421','凤台县',3,'340400','安徽省淮南市凤台县','340400'),
	 ('340422','寿县',3,'340400','安徽省淮南市寿县','340400'),
	 ('340500','马鞍山市',2,'340000','安徽省马鞍山市','340500'),
	 ('340503','花山区',3,'340500','安徽省马鞍山市花山区','340500'),
	 ('340504','雨山区',3,'340500','安徽省马鞍山市雨山区','340500'),
	 ('340506','博望区',3,'340500','安徽省马鞍山市博望区','340500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('340521','当涂县',3,'340500','安徽省马鞍山市当涂县','340500'),
	 ('340522','含山县',3,'340500','安徽省马鞍山市含山县','340500'),
	 ('340523','和县',3,'340500','安徽省马鞍山市和县','340500'),
	 ('340600','淮北市',2,'340000','安徽省淮北市','340600'),
	 ('340602','杜集区',3,'340600','安徽省淮北市杜集区','340600'),
	 ('340603','相山区',3,'340600','安徽省淮北市相山区','340600'),
	 ('340604','烈山区',3,'340600','安徽省淮北市烈山区','340600'),
	 ('340621','濉溪县',3,'340600','安徽省淮北市濉溪县','340600'),
	 ('340700','铜陵市',2,'340000','安徽省铜陵市','340700'),
	 ('340705','铜官区',3,'340700','安徽省铜陵市铜官区','340700');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('340706','义安区',3,'340700','安徽省铜陵市义安区','340700'),
	 ('340711','郊区',3,'340700','安徽省铜陵市郊区','340700'),
	 ('340722','枞阳县',3,'340700','安徽省铜陵市枞阳县','340700'),
	 ('340800','安庆市',2,'340000','安徽省安庆市','340800'),
	 ('340802','迎江区',3,'340800','安徽省安庆市迎江区','340800'),
	 ('340803','大观区',3,'340800','安徽省安庆市大观区','340800'),
	 ('340811','宜秀区',3,'340800','安徽省安庆市宜秀区','340800'),
	 ('340822','怀宁县',3,'340800','安徽省安庆市怀宁县','340800'),
	 ('340825','太湖县',3,'340800','安徽省安庆市太湖县','340800'),
	 ('340826','宿松县',3,'340800','安徽省安庆市宿松县','340800');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('340827','望江县',3,'340800','安徽省安庆市望江县','340800'),
	 ('340828','岳西县',3,'340800','安徽省安庆市岳西县','340800'),
	 ('340881','桐城市',3,'340800','安徽省安庆市桐城市','340800'),
	 ('340882','潜山市',3,'340800','安徽省安庆市潜山市','340800'),
	 ('341000','黄山市',2,'340000','安徽省黄山市','341000'),
	 ('341002','屯溪区',3,'341000','安徽省黄山市屯溪区','341000'),
	 ('341003','黄山区',3,'341000','安徽省黄山市黄山区','341000'),
	 ('341004','徽州区',3,'341000','安徽省黄山市徽州区','341000'),
	 ('341021','歙县',3,'341000','安徽省黄山市歙县','341000'),
	 ('341022','休宁县',3,'341000','安徽省黄山市休宁县','341000');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('341023','黟县',3,'341000','安徽省黄山市黟县','341000'),
	 ('341024','祁门县',3,'341000','安徽省黄山市祁门县','341000'),
	 ('341100','滁州市',2,'340000','安徽省滁州市','341100'),
	 ('341102','琅琊区',3,'341100','安徽省滁州市琅琊区','341100'),
	 ('341103','南谯区',3,'341100','安徽省滁州市南谯区','341100'),
	 ('341122','来安县',3,'341100','安徽省滁州市来安县','341100'),
	 ('341124','全椒县',3,'341100','安徽省滁州市全椒县','341100'),
	 ('341125','定远县',3,'341100','安徽省滁州市定远县','341100'),
	 ('341126','凤阳县',3,'341100','安徽省滁州市凤阳县','341100'),
	 ('341181','天长市',3,'341100','安徽省滁州市天长市','341100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('341182','明光市',3,'341100','安徽省滁州市明光市','341100'),
	 ('341200','阜阳市',2,'340000','安徽省阜阳市','341200'),
	 ('341202','颍州区',3,'341200','安徽省阜阳市颍州区','341200'),
	 ('341203','颍东区',3,'341200','安徽省阜阳市颍东区','341200'),
	 ('341204','颍泉区',3,'341200','安徽省阜阳市颍泉区','341200'),
	 ('341221','临泉县',3,'341200','安徽省阜阳市临泉县','341200'),
	 ('341222','太和县',3,'341200','安徽省阜阳市太和县','341200'),
	 ('341225','阜南县',3,'341200','安徽省阜阳市阜南县','341200'),
	 ('341226','颍上县',3,'341200','安徽省阜阳市颍上县','341200'),
	 ('341282','界首市',3,'341200','安徽省阜阳市界首市','341200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('341300','宿州市',2,'340000','安徽省宿州市','341300'),
	 ('341302','埇桥区',3,'341300','安徽省宿州市埇桥区','341300'),
	 ('341321','砀山县',3,'341300','安徽省宿州市砀山县','341300'),
	 ('341322','萧县',3,'341300','安徽省宿州市萧县','341300'),
	 ('341323','灵璧县',3,'341300','安徽省宿州市灵璧县','341300'),
	 ('341324','泗县',3,'341300','安徽省宿州市泗县','341300'),
	 ('341500','六安市',2,'340000','安徽省六安市','341500'),
	 ('341502','金安区',3,'341500','安徽省六安市金安区','341500'),
	 ('341503','裕安区',3,'341500','安徽省六安市裕安区','341500'),
	 ('341504','叶集区',3,'341500','安徽省六安市叶集区','341500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('341522','霍邱县',3,'341500','安徽省六安市霍邱县','341500'),
	 ('341523','舒城县',3,'341500','安徽省六安市舒城县','341500'),
	 ('341524','金寨县',3,'341500','安徽省六安市金寨县','341500'),
	 ('341525','霍山县',3,'341500','安徽省六安市霍山县','341500'),
	 ('341600','亳州市',2,'340000','安徽省亳州市','341600'),
	 ('341602','谯城区',3,'341600','安徽省亳州市谯城区','341600'),
	 ('341621','涡阳县',3,'341600','安徽省亳州市涡阳县','341600'),
	 ('341622','蒙城县',3,'341600','安徽省亳州市蒙城县','341600'),
	 ('341623','利辛县',3,'341600','安徽省亳州市利辛县','341600'),
	 ('341700','池州市',2,'340000','安徽省池州市','341700');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('341702','贵池区',3,'341700','安徽省池州市贵池区','341700'),
	 ('341721','东至县',3,'341700','安徽省池州市东至县','341700'),
	 ('341722','石台县',3,'341700','安徽省池州市石台县','341700'),
	 ('341723','青阳县',3,'341700','安徽省池州市青阳县','341700'),
	 ('341800','宣城市',2,'340000','安徽省宣城市','341800'),
	 ('341802','宣州区',3,'341800','安徽省宣城市宣州区','341800'),
	 ('341821','郎溪县',3,'341800','安徽省宣城市郎溪县','341800'),
	 ('341823','泾县',3,'341800','安徽省宣城市泾县','341800'),
	 ('341824','绩溪县',3,'341800','安徽省宣城市绩溪县','341800'),
	 ('341825','旌德县',3,'341800','安徽省宣城市旌德县','341800');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('341881','宁国市',3,'341800','安徽省宣城市宁国市','341800'),
	 ('341882','广德市',3,'341800','安徽省宣城市广德市','341800'),
	 ('350000','福建省',1,NULL,'福建省','350000'),
	 ('350100','福州市',2,'350000','福建省福州市','350100'),
	 ('350102','鼓楼区',3,'350100','福建省福州市鼓楼区','350100'),
	 ('350103','台江区',3,'350100','福建省福州市台江区','350100'),
	 ('350104','仓山区',3,'350100','福建省福州市仓山区','350100'),
	 ('350105','马尾区',3,'350100','福建省福州市马尾区','350100'),
	 ('350111','晋安区',3,'350100','福建省福州市晋安区','350100'),
	 ('350112','长乐区',3,'350100','福建省福州市长乐区','350100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('350121','闽侯县',3,'350100','福建省福州市闽侯县','350100'),
	 ('350122','连江县',3,'350100','福建省福州市连江县','350100'),
	 ('350123','罗源县',3,'350100','福建省福州市罗源县','350100'),
	 ('350124','闽清县',3,'350100','福建省福州市闽清县','350100'),
	 ('350125','永泰县',3,'350100','福建省福州市永泰县','350100'),
	 ('350128','平潭县',3,'350100','福建省福州市平潭县','350100'),
	 ('350181','福清市',3,'350100','福建省福州市福清市','350100'),
	 ('350200','厦门市',2,'350000','福建省厦门市','350200'),
	 ('350203','思明区',3,'350200','福建省厦门市思明区','350200'),
	 ('350205','海沧区',3,'350200','福建省厦门市海沧区','350200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('350206','湖里区',3,'350200','福建省厦门市湖里区','350200'),
	 ('350211','集美区',3,'350200','福建省厦门市集美区','350200'),
	 ('350212','同安区',3,'350200','福建省厦门市同安区','350200'),
	 ('350213','翔安区',3,'350200','福建省厦门市翔安区','350200'),
	 ('350300','莆田市',2,'350000','福建省莆田市','350300'),
	 ('350302','城厢区',3,'350300','福建省莆田市城厢区','350300'),
	 ('350303','涵江区',3,'350300','福建省莆田市涵江区','350300'),
	 ('350304','荔城区',3,'350300','福建省莆田市荔城区','350300'),
	 ('350305','秀屿区',3,'350300','福建省莆田市秀屿区','350300'),
	 ('350322','仙游县',3,'350300','福建省莆田市仙游县','350300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('350400','三明市',2,'350000','福建省三明市','350400'),
	 ('350403','三元区',3,'350400','福建省三明市三元区','350400'),
	 ('350421','明溪县',3,'350400','福建省三明市明溪县','350400'),
	 ('350423','清流县',3,'350400','福建省三明市清流县','350400'),
	 ('350424','宁化县',3,'350400','福建省三明市宁化县','350400'),
	 ('350425','大田县',3,'350400','福建省三明市大田县','350400'),
	 ('350426','尤溪县',3,'350400','福建省三明市尤溪县','350400'),
	 ('350427','沙县区',3,'350400','福建省三明市沙县区','350400'),
	 ('350428','将乐县',3,'350400','福建省三明市将乐县','350400'),
	 ('350429','泰宁县',3,'350400','福建省三明市泰宁县','350400');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('350430','建宁县',3,'350400','福建省三明市建宁县','350400'),
	 ('350481','永安市',3,'350400','福建省三明市永安市','350400'),
	 ('350500','泉州市',2,'350000','福建省泉州市','350500'),
	 ('350502','鲤城区',3,'350500','福建省泉州市鲤城区','350500'),
	 ('350503','丰泽区',3,'350500','福建省泉州市丰泽区','350500'),
	 ('350504','洛江区',3,'350500','福建省泉州市洛江区','350500'),
	 ('350505','泉港区',3,'350500','福建省泉州市泉港区','350500'),
	 ('350521','惠安县',3,'350500','福建省泉州市惠安县','350500'),
	 ('350524','安溪县',3,'350500','福建省泉州市安溪县','350500'),
	 ('350525','永春县',3,'350500','福建省泉州市永春县','350500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('350526','德化县',3,'350500','福建省泉州市德化县','350500'),
	 ('350527','金门县',3,'350500','福建省泉州市金门县','350500'),
	 ('350581','石狮市',3,'350500','福建省泉州市石狮市','350500'),
	 ('350582','晋江市',3,'350500','福建省泉州市晋江市','350500'),
	 ('350583','南安市',3,'350500','福建省泉州市南安市','350500'),
	 ('350600','漳州市',2,'350000','福建省漳州市','350600'),
	 ('350602','芗城区',3,'350600','福建省漳州市芗城区','350600'),
	 ('350603','龙文区',3,'350600','福建省漳州市龙文区','350600'),
	 ('350622','云霄县',3,'350600','福建省漳州市云霄县','350600'),
	 ('350623','漳浦县',3,'350600','福建省漳州市漳浦县','350600');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('350624','诏安县',3,'350600','福建省漳州市诏安县','350600'),
	 ('350625','长泰区',3,'350600','福建省漳州市长泰区','350600'),
	 ('350626','东山县',3,'350600','福建省漳州市东山县','350600'),
	 ('350627','南靖县',3,'350600','福建省漳州市南靖县','350600'),
	 ('350628','平和县',3,'350600','福建省漳州市平和县','350600'),
	 ('350629','华安县',3,'350600','福建省漳州市华安县','350600'),
	 ('350681','龙海区',3,'350600','福建省漳州市龙海区','350600'),
	 ('350700','南平市',2,'350000','福建省南平市','350700'),
	 ('350702','延平区',3,'350700','福建省南平市延平区','350700'),
	 ('350703','建阳区',3,'350700','福建省南平市建阳区','350700');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('350721','顺昌县',3,'350700','福建省南平市顺昌县','350700'),
	 ('350722','浦城县',3,'350700','福建省南平市浦城县','350700'),
	 ('350723','光泽县',3,'350700','福建省南平市光泽县','350700'),
	 ('350724','松溪县',3,'350700','福建省南平市松溪县','350700'),
	 ('350725','政和县',3,'350700','福建省南平市政和县','350700'),
	 ('350781','邵武市',3,'350700','福建省南平市邵武市','350700'),
	 ('350782','武夷山市',3,'350700','福建省南平市武夷山市','350700'),
	 ('350783','建瓯市',3,'350700','福建省南平市建瓯市','350700'),
	 ('350800','龙岩市',2,'350000','福建省龙岩市','350800'),
	 ('350802','新罗区',3,'350800','福建省龙岩市新罗区','350800');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('350803','永定区',3,'350800','福建省龙岩市永定区','350800'),
	 ('350821','长汀县',3,'350800','福建省龙岩市长汀县','350800'),
	 ('350823','上杭县',3,'350800','福建省龙岩市上杭县','350800'),
	 ('350824','武平县',3,'350800','福建省龙岩市武平县','350800'),
	 ('350825','连城县',3,'350800','福建省龙岩市连城县','350800'),
	 ('350881','漳平市',3,'350800','福建省龙岩市漳平市','350800'),
	 ('350900','宁德市',2,'350000','福建省宁德市','350900'),
	 ('350902','蕉城区',3,'350900','福建省宁德市蕉城区','350900'),
	 ('350921','霞浦县',3,'350900','福建省宁德市霞浦县','350900'),
	 ('350922','古田县',3,'350900','福建省宁德市古田县','350900');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('350923','屏南县',3,'350900','福建省宁德市屏南县','350900'),
	 ('350924','寿宁县',3,'350900','福建省宁德市寿宁县','350900'),
	 ('350925','周宁县',3,'350900','福建省宁德市周宁县','350900'),
	 ('350926','柘荣县',3,'350900','福建省宁德市柘荣县','350900'),
	 ('350981','福安市',3,'350900','福建省宁德市福安市','350900'),
	 ('350982','福鼎市',3,'350900','福建省宁德市福鼎市','350900'),
	 ('360000','江西省',1,NULL,'江西省','360000'),
	 ('360100','南昌市',2,'360000','江西省南昌市','360100'),
	 ('360102','东湖区',3,'360100','江西省南昌市东湖区','360100'),
	 ('360103','西湖区',3,'360100','江西省南昌市西湖区','360100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('360104','青云谱区',3,'360100','江西省南昌市青云谱区','360100'),
	 ('360111','青山湖区',3,'360100','江西省南昌市青山湖区','360100'),
	 ('360112','新建区',3,'360100','江西省南昌市新建区','360100'),
	 ('360113','红谷滩区',3,'360100','江西省南昌市红谷滩区','360100'),
	 ('360121','南昌县',3,'360100','江西省南昌市南昌县','360100'),
	 ('360123','安义县',3,'360100','江西省南昌市安义县','360100'),
	 ('360124','进贤县',3,'360100','江西省南昌市进贤县','360100'),
	 ('360200','景德镇市',2,'360000','江西省景德镇市','360200'),
	 ('360202','昌江区',3,'360200','江西省景德镇市昌江区','360200'),
	 ('360203','珠山区',3,'360200','江西省景德镇市珠山区','360200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('360222','浮梁县',3,'360200','江西省景德镇市浮梁县','360200'),
	 ('360281','乐平市',3,'360200','江西省景德镇市乐平市','360200'),
	 ('360300','萍乡市',2,'360000','江西省萍乡市','360300'),
	 ('360302','安源区',3,'360300','江西省萍乡市安源区','360300'),
	 ('360313','湘东区',3,'360300','江西省萍乡市湘东区','360300'),
	 ('360321','莲花县',3,'360300','江西省萍乡市莲花县','360300'),
	 ('360322','上栗县',3,'360300','江西省萍乡市上栗县','360300'),
	 ('360323','芦溪县',3,'360300','江西省萍乡市芦溪县','360300'),
	 ('360400','九江市',2,'360000','江西省九江市','360400'),
	 ('360402','濂溪区',3,'360400','江西省九江市濂溪区','360400');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('360403','浔阳区',3,'360400','江西省九江市浔阳区','360400'),
	 ('360404','柴桑区',3,'360400','江西省九江市柴桑区','360400'),
	 ('360423','武宁县',3,'360400','江西省九江市武宁县','360400'),
	 ('360424','修水县',3,'360400','江西省九江市修水县','360400'),
	 ('360425','永修县',3,'360400','江西省九江市永修县','360400'),
	 ('360426','德安县',3,'360400','江西省九江市德安县','360400'),
	 ('360428','都昌县',3,'360400','江西省九江市都昌县','360400'),
	 ('360429','湖口县',3,'360400','江西省九江市湖口县','360400'),
	 ('360430','彭泽县',3,'360400','江西省九江市彭泽县','360400'),
	 ('360481','瑞昌市',3,'360400','江西省九江市瑞昌市','360400');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('360482','共青城市',3,'360400','江西省九江市共青城市','360400'),
	 ('360483','庐山市',3,'360400','江西省九江市庐山市','360400'),
	 ('360500','新余市',2,'360000','江西省新余市','360500'),
	 ('360502','渝水区',3,'360500','江西省新余市渝水区','360500'),
	 ('360521','分宜县',3,'360500','江西省新余市分宜县','360500'),
	 ('360600','鹰潭市',2,'360000','江西省鹰潭市','360600'),
	 ('360602','月湖区',3,'360600','江西省鹰潭市月湖区','360600'),
	 ('360603','余江区',3,'360600','江西省鹰潭市余江区','360600'),
	 ('360681','贵溪市',3,'360600','江西省鹰潭市贵溪市','360600'),
	 ('360700','赣州市',2,'360000','江西省赣州市','360700');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('360702','章贡区',3,'360700','江西省赣州市章贡区','360700'),
	 ('360703','南康区',3,'360700','江西省赣州市南康区','360700'),
	 ('360704','赣县区',3,'360700','江西省赣州市赣县区','360700'),
	 ('360722','信丰县',3,'360700','江西省赣州市信丰县','360700'),
	 ('360723','大余县',3,'360700','江西省赣州市大余县','360700'),
	 ('360724','上犹县',3,'360700','江西省赣州市上犹县','360700'),
	 ('360725','崇义县',3,'360700','江西省赣州市崇义县','360700'),
	 ('360726','安远县',3,'360700','江西省赣州市安远县','360700'),
	 ('360728','定南县',3,'360700','江西省赣州市定南县','360700'),
	 ('360729','全南县',3,'360700','江西省赣州市全南县','360700');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('360730','宁都县',3,'360700','江西省赣州市宁都县','360700'),
	 ('360731','于都县',3,'360700','江西省赣州市于都县','360700'),
	 ('360732','兴国县',3,'360700','江西省赣州市兴国县','360700'),
	 ('360733','会昌县',3,'360700','江西省赣州市会昌县','360700'),
	 ('360734','寻乌县',3,'360700','江西省赣州市寻乌县','360700'),
	 ('360735','石城县',3,'360700','江西省赣州市石城县','360700'),
	 ('360781','瑞金市',3,'360700','江西省赣州市瑞金市','360700'),
	 ('360783','龙南市',3,'360700','江西省赣州市龙南市','360700'),
	 ('360800','吉安市',2,'360000','江西省吉安市','360800'),
	 ('360802','吉州区',3,'360800','江西省吉安市吉州区','360800');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('360803','青原区',3,'360800','江西省吉安市青原区','360800'),
	 ('360821','吉安县',3,'360800','江西省吉安市吉安县','360800'),
	 ('360822','吉水县',3,'360800','江西省吉安市吉水县','360800'),
	 ('360823','峡江县',3,'360800','江西省吉安市峡江县','360800'),
	 ('360824','新干县',3,'360800','江西省吉安市新干县','360800'),
	 ('360825','永丰县',3,'360800','江西省吉安市永丰县','360800'),
	 ('360826','泰和县',3,'360800','江西省吉安市泰和县','360800'),
	 ('360827','遂川县',3,'360800','江西省吉安市遂川县','360800'),
	 ('360828','万安县',3,'360800','江西省吉安市万安县','360800'),
	 ('360829','安福县',3,'360800','江西省吉安市安福县','360800');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('360830','永新县',3,'360800','江西省吉安市永新县','360800'),
	 ('360881','井冈山市',3,'360800','江西省吉安市井冈山市','360800'),
	 ('360900','宜春市',2,'360000','江西省宜春市','360900'),
	 ('360902','袁州区',3,'360900','江西省宜春市袁州区','360900'),
	 ('360921','奉新县',3,'360900','江西省宜春市奉新县','360900'),
	 ('360922','万载县',3,'360900','江西省宜春市万载县','360900'),
	 ('360923','上高县',3,'360900','江西省宜春市上高县','360900'),
	 ('360924','宜丰县',3,'360900','江西省宜春市宜丰县','360900'),
	 ('360925','靖安县',3,'360900','江西省宜春市靖安县','360900'),
	 ('360926','铜鼓县',3,'360900','江西省宜春市铜鼓县','360900');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('360981','丰城市',3,'360900','江西省宜春市丰城市','360900'),
	 ('360982','樟树市',3,'360900','江西省宜春市樟树市','360900'),
	 ('360983','高安市',3,'360900','江西省宜春市高安市','360900'),
	 ('361000','抚州市',2,'360000','江西省抚州市','361000'),
	 ('361002','临川区',3,'361000','江西省抚州市临川区','361000'),
	 ('361003','东乡区',3,'361000','江西省抚州市东乡区','361000'),
	 ('361021','南城县',3,'361000','江西省抚州市南城县','361000'),
	 ('361022','黎川县',3,'361000','江西省抚州市黎川县','361000'),
	 ('361023','南丰县',3,'361000','江西省抚州市南丰县','361000'),
	 ('361024','崇仁县',3,'361000','江西省抚州市崇仁县','361000');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('361025','乐安县',3,'361000','江西省抚州市乐安县','361000'),
	 ('361026','宜黄县',3,'361000','江西省抚州市宜黄县','361000'),
	 ('361027','金溪县',3,'361000','江西省抚州市金溪县','361000'),
	 ('361028','资溪县',3,'361000','江西省抚州市资溪县','361000'),
	 ('361030','广昌县',3,'361000','江西省抚州市广昌县','361000'),
	 ('361100','上饶市',2,'360000','江西省上饶市','361100'),
	 ('361102','信州区',3,'361100','江西省上饶市信州区','361100'),
	 ('361103','广丰区',3,'361100','江西省上饶市广丰区','361100'),
	 ('361104','广信区',3,'361100','江西省上饶市广信区','361100'),
	 ('361123','玉山县',3,'361100','江西省上饶市玉山县','361100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('361124','铅山县',3,'361100','江西省上饶市铅山县','361100'),
	 ('361125','横峰县',3,'361100','江西省上饶市横峰县','361100'),
	 ('361126','弋阳县',3,'361100','江西省上饶市弋阳县','361100'),
	 ('361127','余干县',3,'361100','江西省上饶市余干县','361100'),
	 ('361128','鄱阳县',3,'361100','江西省上饶市鄱阳县','361100'),
	 ('361129','万年县',3,'361100','江西省上饶市万年县','361100'),
	 ('361130','婺源县',3,'361100','江西省上饶市婺源县','361100'),
	 ('361181','德兴市',3,'361100','江西省上饶市德兴市','361100'),
	 ('370000','山东省',1,NULL,'山东省','370000'),
	 ('370100','济南市',2,'370000','山东省济南市','370100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('370102','历下区',3,'370100','山东省济南市历下区','370100'),
	 ('370103','市中区',3,'370100','山东省济南市市中区','370100'),
	 ('370104','槐荫区',3,'370100','山东省济南市槐荫区','370100'),
	 ('370105','天桥区',3,'370100','山东省济南市天桥区','370100'),
	 ('370112','历城区',3,'370100','山东省济南市历城区','370100'),
	 ('370113','长清区',3,'370100','山东省济南市长清区','370100'),
	 ('370114','章丘区',3,'370100','山东省济南市章丘区','370100'),
	 ('370115','济阳区',3,'370100','山东省济南市济阳区','370100'),
	 ('370116','莱芜区',3,'370100','山东省济南市莱芜区','370100'),
	 ('370117','钢城区',3,'370100','山东省济南市钢城区','370100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('370124','平阴县',3,'370100','山东省济南市平阴县','370100'),
	 ('370126','商河县',3,'370100','山东省济南市商河县','370100'),
	 ('370200','青岛市',2,'370000','山东省青岛市','370200'),
	 ('370202','市南区',3,'370200','山东省青岛市市南区','370200'),
	 ('370203','市北区',3,'370200','山东省青岛市市北区','370200'),
	 ('370211','黄岛区',3,'370200','山东省青岛市黄岛区','370200'),
	 ('370212','崂山区',3,'370200','山东省青岛市崂山区','370200'),
	 ('370213','李沧区',3,'370200','山东省青岛市李沧区','370200'),
	 ('370214','城阳区',3,'370200','山东省青岛市城阳区','370200'),
	 ('370215','即墨区',3,'370200','山东省青岛市即墨区','370200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('370281','胶州市',3,'370200','山东省青岛市胶州市','370200'),
	 ('370283','平度市',3,'370200','山东省青岛市平度市','370200'),
	 ('370285','莱西市',3,'370200','山东省青岛市莱西市','370200'),
	 ('370300','淄博市',2,'370000','山东省淄博市','370300'),
	 ('370302','淄川区',3,'370300','山东省淄博市淄川区','370300'),
	 ('370303','张店区',3,'370300','山东省淄博市张店区','370300'),
	 ('370304','博山区',3,'370300','山东省淄博市博山区','370300'),
	 ('370305','临淄区',3,'370300','山东省淄博市临淄区','370300'),
	 ('370306','周村区',3,'370300','山东省淄博市周村区','370300'),
	 ('370321','桓台县',3,'370300','山东省淄博市桓台县','370300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('370322','高青县',3,'370300','山东省淄博市高青县','370300'),
	 ('370323','沂源县',3,'370300','山东省淄博市沂源县','370300'),
	 ('370400','枣庄市',2,'370000','山东省枣庄市','370400'),
	 ('370402','市中区',3,'370400','山东省枣庄市市中区','370400'),
	 ('370403','薛城区',3,'370400','山东省枣庄市薛城区','370400'),
	 ('370404','峄城区',3,'370400','山东省枣庄市峄城区','370400'),
	 ('370405','台儿庄区',3,'370400','山东省枣庄市台儿庄区','370400'),
	 ('370406','山亭区',3,'370400','山东省枣庄市山亭区','370400'),
	 ('370481','滕州市',3,'370400','山东省枣庄市滕州市','370400'),
	 ('370500','东营市',2,'370000','山东省东营市','370500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('370502','东营区',3,'370500','山东省东营市东营区','370500'),
	 ('370503','河口区',3,'370500','山东省东营市河口区','370500'),
	 ('370505','垦利区',3,'370500','山东省东营市垦利区','370500'),
	 ('370522','利津县',3,'370500','山东省东营市利津县','370500'),
	 ('370523','广饶县',3,'370500','山东省东营市广饶县','370500'),
	 ('370600','烟台市',2,'370000','山东省烟台市','370600'),
	 ('370602','芝罘区',3,'370600','山东省烟台市芝罘区','370600'),
	 ('370611','福山区',3,'370600','山东省烟台市福山区','370600'),
	 ('370612','牟平区',3,'370600','山东省烟台市牟平区','370600'),
	 ('370613','莱山区',3,'370600','山东省烟台市莱山区','370600');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('370614','蓬莱区',3,'370600','山东省烟台市蓬莱区','370600'),
	 ('370681','龙口市',3,'370600','山东省烟台市龙口市','370600'),
	 ('370682','莱阳市',3,'370600','山东省烟台市莱阳市','370600'),
	 ('370683','莱州市',3,'370600','山东省烟台市莱州市','370600'),
	 ('370685','招远市',3,'370600','山东省烟台市招远市','370600'),
	 ('370686','栖霞市',3,'370600','山东省烟台市栖霞市','370600'),
	 ('370687','海阳市',3,'370600','山东省烟台市海阳市','370600'),
	 ('370700','潍坊市',2,'370000','山东省潍坊市','370700'),
	 ('370702','潍城区',3,'370700','山东省潍坊市潍城区','370700'),
	 ('370703','寒亭区',3,'370700','山东省潍坊市寒亭区','370700');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('370704','坊子区',3,'370700','山东省潍坊市坊子区','370700'),
	 ('370705','奎文区',3,'370700','山东省潍坊市奎文区','370700'),
	 ('370724','临朐县',3,'370700','山东省潍坊市临朐县','370700'),
	 ('370725','昌乐县',3,'370700','山东省潍坊市昌乐县','370700'),
	 ('370781','青州市',3,'370700','山东省潍坊市青州市','370700'),
	 ('370782','诸城市',3,'370700','山东省潍坊市诸城市','370700'),
	 ('370783','寿光市',3,'370700','山东省潍坊市寿光市','370700'),
	 ('370784','安丘市',3,'370700','山东省潍坊市安丘市','370700'),
	 ('370785','高密市',3,'370700','山东省潍坊市高密市','370700'),
	 ('370786','昌邑市',3,'370700','山东省潍坊市昌邑市','370700');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('370800','济宁市',2,'370000','山东省济宁市','370800'),
	 ('370811','任城区',3,'370800','山东省济宁市任城区','370800'),
	 ('370812','兖州区',3,'370800','山东省济宁市兖州区','370800'),
	 ('370826','微山县',3,'370800','山东省济宁市微山县','370800'),
	 ('370827','鱼台县',3,'370800','山东省济宁市鱼台县','370800'),
	 ('370828','金乡县',3,'370800','山东省济宁市金乡县','370800'),
	 ('370829','嘉祥县',3,'370800','山东省济宁市嘉祥县','370800'),
	 ('370830','汶上县',3,'370800','山东省济宁市汶上县','370800'),
	 ('370831','泗水县',3,'370800','山东省济宁市泗水县','370800'),
	 ('370832','梁山县',3,'370800','山东省济宁市梁山县','370800');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('370881','曲阜市',3,'370800','山东省济宁市曲阜市','370800'),
	 ('370883','邹城市',3,'370800','山东省济宁市邹城市','370800'),
	 ('370900','泰安市',2,'370000','山东省泰安市','370900'),
	 ('370902','泰山区',3,'370900','山东省泰安市泰山区','370900'),
	 ('370911','岱岳区',3,'370900','山东省泰安市岱岳区','370900'),
	 ('370921','宁阳县',3,'370900','山东省泰安市宁阳县','370900'),
	 ('370923','东平县',3,'370900','山东省泰安市东平县','370900'),
	 ('370982','新泰市',3,'370900','山东省泰安市新泰市','370900'),
	 ('370983','肥城市',3,'370900','山东省泰安市肥城市','370900'),
	 ('371000','威海市',2,'370000','山东省威海市','371000');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('371002','环翠区',3,'371000','山东省威海市环翠区','371000'),
	 ('371003','文登区',3,'371000','山东省威海市文登区','371000'),
	 ('371082','荣成市',3,'371000','山东省威海市荣成市','371000'),
	 ('371083','乳山市',3,'371000','山东省威海市乳山市','371000'),
	 ('371100','日照市',2,'370000','山东省日照市','371100'),
	 ('371102','东港区',3,'371100','山东省日照市东港区','371100'),
	 ('371103','岚山区',3,'371100','山东省日照市岚山区','371100'),
	 ('371121','五莲县',3,'371100','山东省日照市五莲县','371100'),
	 ('371122','莒县',3,'371100','山东省日照市莒县','371100'),
	 ('371300','临沂市',2,'370000','山东省临沂市','371300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('371302','兰山区',3,'371300','山东省临沂市兰山区','371300'),
	 ('371311','罗庄区',3,'371300','山东省临沂市罗庄区','371300'),
	 ('371312','河东区',3,'371300','山东省临沂市河东区','371300'),
	 ('371321','沂南县',3,'371300','山东省临沂市沂南县','371300'),
	 ('371322','郯城县',3,'371300','山东省临沂市郯城县','371300'),
	 ('371323','沂水县',3,'371300','山东省临沂市沂水县','371300'),
	 ('371324','兰陵县',3,'371300','山东省临沂市兰陵县','371300'),
	 ('371325','费县',3,'371300','山东省临沂市费县','371300'),
	 ('371326','平邑县',3,'371300','山东省临沂市平邑县','371300'),
	 ('371327','莒南县',3,'371300','山东省临沂市莒南县','371300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('371328','蒙阴县',3,'371300','山东省临沂市蒙阴县','371300'),
	 ('371329','临沭县',3,'371300','山东省临沂市临沭县','371300'),
	 ('371400','德州市',2,'370000','山东省德州市','371400'),
	 ('371402','德城区',3,'371400','山东省德州市德城区','371400'),
	 ('371403','陵城区',3,'371400','山东省德州市陵城区','371400'),
	 ('371422','宁津县',3,'371400','山东省德州市宁津县','371400'),
	 ('371423','庆云县',3,'371400','山东省德州市庆云县','371400'),
	 ('371424','临邑县',3,'371400','山东省德州市临邑县','371400'),
	 ('371425','齐河县',3,'371400','山东省德州市齐河县','371400'),
	 ('371426','平原县',3,'371400','山东省德州市平原县','371400');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('371427','夏津县',3,'371400','山东省德州市夏津县','371400'),
	 ('371428','武城县',3,'371400','山东省德州市武城县','371400'),
	 ('371481','乐陵市',3,'371400','山东省德州市乐陵市','371400'),
	 ('371482','禹城市',3,'371400','山东省德州市禹城市','371400'),
	 ('371500','聊城市',2,'370000','山东省聊城市','371500'),
	 ('371502','东昌府区',3,'371500','山东省聊城市东昌府区','371500'),
	 ('371503','茌平区',3,'371500','山东省聊城市茌平区','371500'),
	 ('371521','阳谷县',3,'371500','山东省聊城市阳谷县','371500'),
	 ('371522','莘县',3,'371500','山东省聊城市莘县','371500'),
	 ('371524','东阿县',3,'371500','山东省聊城市东阿县','371500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('371525','冠县',3,'371500','山东省聊城市冠县','371500'),
	 ('371526','高唐县',3,'371500','山东省聊城市高唐县','371500'),
	 ('371581','临清市',3,'371500','山东省聊城市临清市','371500'),
	 ('371600','滨州市',2,'370000','山东省滨州市','371600'),
	 ('371602','滨城区',3,'371600','山东省滨州市滨城区','371600'),
	 ('371603','沾化区',3,'371600','山东省滨州市沾化区','371600'),
	 ('371621','惠民县',3,'371600','山东省滨州市惠民县','371600'),
	 ('371622','阳信县',3,'371600','山东省滨州市阳信县','371600'),
	 ('371623','无棣县',3,'371600','山东省滨州市无棣县','371600'),
	 ('371625','博兴县',3,'371600','山东省滨州市博兴县','371600');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('371681','邹平市',3,'371600','山东省滨州市邹平市','371600'),
	 ('371700','菏泽市',2,'370000','山东省菏泽市','371700'),
	 ('371702','牡丹区',3,'371700','山东省菏泽市牡丹区','371700'),
	 ('371703','定陶区',3,'371700','山东省菏泽市定陶区','371700'),
	 ('371721','曹县',3,'371700','山东省菏泽市曹县','371700'),
	 ('371722','单县',3,'371700','山东省菏泽市单县','371700'),
	 ('371723','成武县',3,'371700','山东省菏泽市成武县','371700'),
	 ('371724','巨野县',3,'371700','山东省菏泽市巨野县','371700'),
	 ('371725','郓城县',3,'371700','山东省菏泽市郓城县','371700'),
	 ('371726','鄄城县',3,'371700','山东省菏泽市鄄城县','371700');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('371728','东明县',3,'371700','山东省菏泽市东明县','371700'),
	 ('410000','河南省',1,NULL,'河南省','410000'),
	 ('410100','郑州市',2,'410000','河南省郑州市','410100'),
	 ('410102','中原区',3,'410100','河南省郑州市中原区','410100'),
	 ('410103','二七区',3,'410100','河南省郑州市二七区','410100'),
	 ('410104','管城回族区',3,'410100','河南省郑州市管城回族区','410100'),
	 ('410105','金水区',3,'410100','河南省郑州市金水区','410100'),
	 ('410106','上街区',3,'410100','河南省郑州市上街区','410100'),
	 ('410108','惠济区',3,'410100','河南省郑州市惠济区','410100'),
	 ('410122','中牟县',3,'410100','河南省郑州市中牟县','410100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('410181','巩义市',3,'410100','河南省郑州市巩义市','410100'),
	 ('410182','荥阳市',3,'410100','河南省郑州市荥阳市','410100'),
	 ('410183','新密市',3,'410100','河南省郑州市新密市','410100'),
	 ('410184','新郑市',3,'410100','河南省郑州市新郑市','410100'),
	 ('410185','登封市',3,'410100','河南省郑州市登封市','410100'),
	 ('410200','开封市',2,'410000','河南省开封市','410200'),
	 ('410202','龙亭区',3,'410200','河南省开封市龙亭区','410200'),
	 ('410203','顺河回族区',3,'410200','河南省开封市顺河回族区','410200'),
	 ('410204','鼓楼区',3,'410200','河南省开封市鼓楼区','410200'),
	 ('410205','禹王台区',3,'410200','河南省开封市禹王台区','410200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('410212','祥符区',3,'410200','河南省开封市祥符区','410200'),
	 ('410221','杞县',3,'410200','河南省开封市杞县','410200'),
	 ('410222','通许县',3,'410200','河南省开封市通许县','410200'),
	 ('410223','尉氏县',3,'410200','河南省开封市尉氏县','410200'),
	 ('410225','兰考县',3,'410200','河南省开封市兰考县','410200'),
	 ('410300','洛阳市',2,'410000','河南省洛阳市','410300'),
	 ('410302','老城区',3,'410300','河南省洛阳市老城区','410300'),
	 ('410303','西工区',3,'410300','河南省洛阳市西工区','410300'),
	 ('410304','瀍河回族区',3,'410300','河南省洛阳市瀍河回族区','410300'),
	 ('410305','涧西区',3,'410300','河南省洛阳市涧西区','410300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('410306','孟津区',3,'410300','河南省洛阳市孟津区','410300'),
	 ('410311','洛龙区',3,'410300','河南省洛阳市洛龙区','410300'),
	 ('410323','新安县',3,'410300','河南省洛阳市新安县','410300'),
	 ('410324','栾川县',3,'410300','河南省洛阳市栾川县','410300'),
	 ('410325','嵩县',3,'410300','河南省洛阳市嵩县','410300'),
	 ('410326','汝阳县',3,'410300','河南省洛阳市汝阳县','410300'),
	 ('410327','宜阳县',3,'410300','河南省洛阳市宜阳县','410300'),
	 ('410328','洛宁县',3,'410300','河南省洛阳市洛宁县','410300'),
	 ('410329','伊川县',3,'410300','河南省洛阳市伊川县','410300'),
	 ('410381','偃师区',3,'410300','河南省洛阳市偃师区','410300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('410400','平顶山市',2,'410000','河南省平顶山市','410400'),
	 ('410402','新华区',3,'410400','河南省平顶山市新华区','410400'),
	 ('410403','卫东区',3,'410400','河南省平顶山市卫东区','410400'),
	 ('410404','石龙区',3,'410400','河南省平顶山市石龙区','410400'),
	 ('410411','湛河区',3,'410400','河南省平顶山市湛河区','410400'),
	 ('410421','宝丰县',3,'410400','河南省平顶山市宝丰县','410400'),
	 ('410422','叶县',3,'410400','河南省平顶山市叶县','410400'),
	 ('410423','鲁山县',3,'410400','河南省平顶山市鲁山县','410400'),
	 ('410425','郏县',3,'410400','河南省平顶山市郏县','410400'),
	 ('410481','舞钢市',3,'410400','河南省平顶山市舞钢市','410400');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('410482','汝州市',3,'410400','河南省平顶山市汝州市','410400'),
	 ('410500','安阳市',2,'410000','河南省安阳市','410500'),
	 ('410502','文峰区',3,'410500','河南省安阳市文峰区','410500'),
	 ('410503','北关区',3,'410500','河南省安阳市北关区','410500'),
	 ('410505','殷都区',3,'410500','河南省安阳市殷都区','410500'),
	 ('410506','龙安区',3,'410500','河南省安阳市龙安区','410500'),
	 ('410522','安阳县',3,'410500','河南省安阳市安阳县','410500'),
	 ('410523','汤阴县',3,'410500','河南省安阳市汤阴县','410500'),
	 ('410526','滑县',3,'410500','河南省安阳市滑县','410500'),
	 ('410527','内黄县',3,'410500','河南省安阳市内黄县','410500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('410581','林州市',3,'410500','河南省安阳市林州市','410500'),
	 ('410600','鹤壁市',2,'410000','河南省鹤壁市','410600'),
	 ('410602','鹤山区',3,'410600','河南省鹤壁市鹤山区','410600'),
	 ('410603','山城区',3,'410600','河南省鹤壁市山城区','410600'),
	 ('410611','淇滨区',3,'410600','河南省鹤壁市淇滨区','410600'),
	 ('410621','浚县',3,'410600','河南省鹤壁市浚县','410600'),
	 ('410622','淇县',3,'410600','河南省鹤壁市淇县','410600'),
	 ('410700','新乡市',2,'410000','河南省新乡市','410700'),
	 ('410702','红旗区',3,'410700','河南省新乡市红旗区','410700'),
	 ('410703','卫滨区',3,'410700','河南省新乡市卫滨区','410700');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('410704','凤泉区',3,'410700','河南省新乡市凤泉区','410700'),
	 ('410711','牧野区',3,'410700','河南省新乡市牧野区','410700'),
	 ('410721','新乡县',3,'410700','河南省新乡市新乡县','410700'),
	 ('410724','获嘉县',3,'410700','河南省新乡市获嘉县','410700'),
	 ('410725','原阳县',3,'410700','河南省新乡市原阳县','410700'),
	 ('410726','延津县',3,'410700','河南省新乡市延津县','410700'),
	 ('410727','封丘县',3,'410700','河南省新乡市封丘县','410700'),
	 ('410781','卫辉市',3,'410700','河南省新乡市卫辉市','410700'),
	 ('410782','辉县市',3,'410700','河南省新乡市辉县市','410700'),
	 ('410783','长垣市',3,'410700','河南省新乡市长垣市','410700');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('410800','焦作市',2,'410000','河南省焦作市','410800'),
	 ('410802','解放区',3,'410800','河南省焦作市解放区','410800'),
	 ('410803','中站区',3,'410800','河南省焦作市中站区','410800'),
	 ('410804','马村区',3,'410800','河南省焦作市马村区','410800'),
	 ('410811','山阳区',3,'410800','河南省焦作市山阳区','410800'),
	 ('410821','修武县',3,'410800','河南省焦作市修武县','410800'),
	 ('410822','博爱县',3,'410800','河南省焦作市博爱县','410800'),
	 ('410823','武陟县',3,'410800','河南省焦作市武陟县','410800'),
	 ('410825','温县',3,'410800','河南省焦作市温县','410800'),
	 ('410882','沁阳市',3,'410800','河南省焦作市沁阳市','410800');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('410883','孟州市',3,'410800','河南省焦作市孟州市','410800'),
	 ('410900','濮阳市',2,'410000','河南省濮阳市','410900'),
	 ('410902','华龙区',3,'410900','河南省濮阳市华龙区','410900'),
	 ('410922','清丰县',3,'410900','河南省濮阳市清丰县','410900'),
	 ('410923','南乐县',3,'410900','河南省濮阳市南乐县','410900'),
	 ('410926','范县',3,'410900','河南省濮阳市范县','410900'),
	 ('410927','台前县',3,'410900','河南省濮阳市台前县','410900'),
	 ('410928','濮阳县',3,'410900','河南省濮阳市濮阳县','410900'),
	 ('411000','许昌市',2,'410000','河南省许昌市','411000'),
	 ('411002','魏都区',3,'411000','河南省许昌市魏都区','411000');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('411003','建安区',3,'411000','河南省许昌市建安区','411000'),
	 ('411024','鄢陵县',3,'411000','河南省许昌市鄢陵县','411000'),
	 ('411025','襄城县',3,'411000','河南省许昌市襄城县','411000'),
	 ('411081','禹州市',3,'411000','河南省许昌市禹州市','411000'),
	 ('411082','长葛市',3,'411000','河南省许昌市长葛市','411000'),
	 ('411100','漯河市',2,'410000','河南省漯河市','411100'),
	 ('411102','源汇区',3,'411100','河南省漯河市源汇区','411100'),
	 ('411103','郾城区',3,'411100','河南省漯河市郾城区','411100'),
	 ('411104','召陵区',3,'411100','河南省漯河市召陵区','411100'),
	 ('411121','舞阳县',3,'411100','河南省漯河市舞阳县','411100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('411122','临颍县',3,'411100','河南省漯河市临颍县','411100'),
	 ('411200','三门峡市',2,'410000','河南省三门峡市','411200'),
	 ('411202','湖滨区',3,'411200','河南省三门峡市湖滨区','411200'),
	 ('411203','陕州区',3,'411200','河南省三门峡市陕州区','411200'),
	 ('411221','渑池县',3,'411200','河南省三门峡市渑池县','411200'),
	 ('411224','卢氏县',3,'411200','河南省三门峡市卢氏县','411200'),
	 ('411281','义马市',3,'411200','河南省三门峡市义马市','411200'),
	 ('411282','灵宝市',3,'411200','河南省三门峡市灵宝市','411200'),
	 ('411300','南阳市',2,'410000','河南省南阳市','411300'),
	 ('411302','宛城区',3,'411300','河南省南阳市宛城区','411300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('411303','卧龙区',3,'411300','河南省南阳市卧龙区','411300'),
	 ('411321','南召县',3,'411300','河南省南阳市南召县','411300'),
	 ('411322','方城县',3,'411300','河南省南阳市方城县','411300'),
	 ('411323','西峡县',3,'411300','河南省南阳市西峡县','411300'),
	 ('411324','镇平县',3,'411300','河南省南阳市镇平县','411300'),
	 ('411325','内乡县',3,'411300','河南省南阳市内乡县','411300'),
	 ('411326','淅川县',3,'411300','河南省南阳市淅川县','411300'),
	 ('411327','社旗县',3,'411300','河南省南阳市社旗县','411300'),
	 ('411328','唐河县',3,'411300','河南省南阳市唐河县','411300'),
	 ('411329','新野县',3,'411300','河南省南阳市新野县','411300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('411330','桐柏县',3,'411300','河南省南阳市桐柏县','411300'),
	 ('411381','邓州市',3,'411300','河南省南阳市邓州市','411300'),
	 ('411400','商丘市',2,'410000','河南省商丘市','411400'),
	 ('411402','梁园区',3,'411400','河南省商丘市梁园区','411400'),
	 ('411403','睢阳区',3,'411400','河南省商丘市睢阳区','411400'),
	 ('411421','民权县',3,'411400','河南省商丘市民权县','411400'),
	 ('411422','睢县',3,'411400','河南省商丘市睢县','411400'),
	 ('411423','宁陵县',3,'411400','河南省商丘市宁陵县','411400'),
	 ('411424','柘城县',3,'411400','河南省商丘市柘城县','411400'),
	 ('411425','虞城县',3,'411400','河南省商丘市虞城县','411400');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('411426','夏邑县',3,'411400','河南省商丘市夏邑县','411400'),
	 ('411481','永城市',3,'411400','河南省商丘市永城市','411400'),
	 ('411500','信阳市',2,'410000','河南省信阳市','411500'),
	 ('411502','浉河区',3,'411500','河南省信阳市浉河区','411500'),
	 ('411503','平桥区',3,'411500','河南省信阳市平桥区','411500'),
	 ('411521','罗山县',3,'411500','河南省信阳市罗山县','411500'),
	 ('411522','光山县',3,'411500','河南省信阳市光山县','411500'),
	 ('411523','新县',3,'411500','河南省信阳市新县','411500'),
	 ('411524','商城县',3,'411500','河南省信阳市商城县','411500'),
	 ('411525','固始县',3,'411500','河南省信阳市固始县','411500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('411526','潢川县',3,'411500','河南省信阳市潢川县','411500'),
	 ('411527','淮滨县',3,'411500','河南省信阳市淮滨县','411500'),
	 ('411528','息县',3,'411500','河南省信阳市息县','411500'),
	 ('411600','周口市',2,'410000','河南省周口市','411600'),
	 ('411602','川汇区',3,'411600','河南省周口市川汇区','411600'),
	 ('411603','淮阳区',3,'411600','河南省周口市淮阳区','411600'),
	 ('411621','扶沟县',3,'411600','河南省周口市扶沟县','411600'),
	 ('411622','西华县',3,'411600','河南省周口市西华县','411600'),
	 ('411623','商水县',3,'411600','河南省周口市商水县','411600'),
	 ('411624','沈丘县',3,'411600','河南省周口市沈丘县','411600');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('411625','郸城县',3,'411600','河南省周口市郸城县','411600'),
	 ('411627','太康县',3,'411600','河南省周口市太康县','411600'),
	 ('411628','鹿邑县',3,'411600','河南省周口市鹿邑县','411600'),
	 ('411681','项城市',3,'411600','河南省周口市项城市','411600'),
	 ('411700','驻马店市',2,'410000','河南省驻马店市','411700'),
	 ('411702','驿城区',3,'411700','河南省驻马店市驿城区','411700'),
	 ('411721','西平县',3,'411700','河南省驻马店市西平县','411700'),
	 ('411722','上蔡县',3,'411700','河南省驻马店市上蔡县','411700'),
	 ('411723','平舆县',3,'411700','河南省驻马店市平舆县','411700'),
	 ('411724','正阳县',3,'411700','河南省驻马店市正阳县','411700');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('411725','确山县',3,'411700','河南省驻马店市确山县','411700'),
	 ('411726','泌阳县',3,'411700','河南省驻马店市泌阳县','411700'),
	 ('411727','汝南县',3,'411700','河南省驻马店市汝南县','411700'),
	 ('411728','遂平县',3,'411700','河南省驻马店市遂平县','411700'),
	 ('411729','新蔡县',3,'411700','河南省驻马店市新蔡县','411700'),
	 ('419001','济源市',2,'410000','河南省济源市','419001'),
	 ('420000','湖北省',1,NULL,'湖北省','420000'),
	 ('420100','武汉市',2,'420000','湖北省武汉市','420100'),
	 ('420102','江岸区',3,'420100','湖北省武汉市江岸区','420100'),
	 ('420103','江汉区',3,'420100','湖北省武汉市江汉区','420100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('420104','硚口区',3,'420100','湖北省武汉市硚口区','420100'),
	 ('420105','汉阳区',3,'420100','湖北省武汉市汉阳区','420100'),
	 ('420106','武昌区',3,'420100','湖北省武汉市武昌区','420100'),
	 ('420107','青山区',3,'420100','湖北省武汉市青山区','420100'),
	 ('420111','洪山区',3,'420100','湖北省武汉市洪山区','420100'),
	 ('420112','东西湖区',3,'420100','湖北省武汉市东西湖区','420100'),
	 ('420113','汉南区',3,'420100','湖北省武汉市汉南区','420100'),
	 ('420114','蔡甸区',3,'420100','湖北省武汉市蔡甸区','420100'),
	 ('420115','江夏区',3,'420100','湖北省武汉市江夏区','420100'),
	 ('420116','黄陂区',3,'420100','湖北省武汉市黄陂区','420100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('420117','新洲区',3,'420100','湖北省武汉市新洲区','420100'),
	 ('420200','黄石市',2,'420000','湖北省黄石市','420200'),
	 ('420202','黄石港区',3,'420200','湖北省黄石市黄石港区','420200'),
	 ('420203','西塞山区',3,'420200','湖北省黄石市西塞山区','420200'),
	 ('420204','下陆区',3,'420200','湖北省黄石市下陆区','420200'),
	 ('420205','铁山区',3,'420200','湖北省黄石市铁山区','420200'),
	 ('420222','阳新县',3,'420200','湖北省黄石市阳新县','420200'),
	 ('420281','大冶市',3,'420200','湖北省黄石市大冶市','420200'),
	 ('420300','十堰市',2,'420000','湖北省十堰市','420300'),
	 ('420302','茅箭区',3,'420300','湖北省十堰市茅箭区','420300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('420303','张湾区',3,'420300','湖北省十堰市张湾区','420300'),
	 ('420304','郧阳区',3,'420300','湖北省十堰市郧阳区','420300'),
	 ('420322','郧西县',3,'420300','湖北省十堰市郧西县','420300'),
	 ('420323','竹山县',3,'420300','湖北省十堰市竹山县','420300'),
	 ('420324','竹溪县',3,'420300','湖北省十堰市竹溪县','420300'),
	 ('420325','房县',3,'420300','湖北省十堰市房县','420300'),
	 ('420381','丹江口市',3,'420300','湖北省十堰市丹江口市','420300'),
	 ('420500','宜昌市',2,'420000','湖北省宜昌市','420500'),
	 ('420502','西陵区',3,'420500','湖北省宜昌市西陵区','420500'),
	 ('420503','伍家岗区',3,'420500','湖北省宜昌市伍家岗区','420500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('420504','点军区',3,'420500','湖北省宜昌市点军区','420500'),
	 ('420505','猇亭区',3,'420500','湖北省宜昌市猇亭区','420500'),
	 ('420506','夷陵区',3,'420500','湖北省宜昌市夷陵区','420500'),
	 ('420525','远安县',3,'420500','湖北省宜昌市远安县','420500'),
	 ('420526','兴山县',3,'420500','湖北省宜昌市兴山县','420500'),
	 ('420527','秭归县',3,'420500','湖北省宜昌市秭归县','420500'),
	 ('420528','长阳土家族自治县',3,'420500','湖北省宜昌市长阳土家族自治县','420500'),
	 ('420529','五峰土家族自治县',3,'420500','湖北省宜昌市五峰土家族自治县','420500'),
	 ('420581','宜都市',3,'420500','湖北省宜昌市宜都市','420500'),
	 ('420582','当阳市',3,'420500','湖北省宜昌市当阳市','420500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('420583','枝江市',3,'420500','湖北省宜昌市枝江市','420500'),
	 ('420600','襄阳市',2,'420000','湖北省襄阳市','420600'),
	 ('420602','襄城区',3,'420600','湖北省襄阳市襄城区','420600'),
	 ('420606','樊城区',3,'420600','湖北省襄阳市樊城区','420600'),
	 ('420607','襄州区',3,'420600','湖北省襄阳市襄州区','420600'),
	 ('420624','南漳县',3,'420600','湖北省襄阳市南漳县','420600'),
	 ('420625','谷城县',3,'420600','湖北省襄阳市谷城县','420600'),
	 ('420626','保康县',3,'420600','湖北省襄阳市保康县','420600'),
	 ('420682','老河口市',3,'420600','湖北省襄阳市老河口市','420600'),
	 ('420683','枣阳市',3,'420600','湖北省襄阳市枣阳市','420600');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('420684','宜城市',3,'420600','湖北省襄阳市宜城市','420600'),
	 ('420700','鄂州市',2,'420000','湖北省鄂州市','420700'),
	 ('420702','梁子湖区',3,'420700','湖北省鄂州市梁子湖区','420700'),
	 ('420703','华容区',3,'420700','湖北省鄂州市华容区','420700'),
	 ('420704','鄂城区',3,'420700','湖北省鄂州市鄂城区','420700'),
	 ('420800','荆门市',2,'420000','湖北省荆门市','420800'),
	 ('420802','东宝区',3,'420800','湖北省荆门市东宝区','420800'),
	 ('420804','掇刀区',3,'420800','湖北省荆门市掇刀区','420800'),
	 ('420822','沙洋县',3,'420800','湖北省荆门市沙洋县','420800'),
	 ('420881','钟祥市',3,'420800','湖北省荆门市钟祥市','420800');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('420882','京山市',3,'420800','湖北省荆门市京山市','420800'),
	 ('420900','孝感市',2,'420000','湖北省孝感市','420900'),
	 ('420902','孝南区',3,'420900','湖北省孝感市孝南区','420900'),
	 ('420921','孝昌县',3,'420900','湖北省孝感市孝昌县','420900'),
	 ('420922','大悟县',3,'420900','湖北省孝感市大悟县','420900'),
	 ('420923','云梦县',3,'420900','湖北省孝感市云梦县','420900'),
	 ('420981','应城市',3,'420900','湖北省孝感市应城市','420900'),
	 ('420982','安陆市',3,'420900','湖北省孝感市安陆市','420900'),
	 ('420984','汉川市',3,'420900','湖北省孝感市汉川市','420900'),
	 ('421000','荆州市',2,'420000','湖北省荆州市','421000');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('421002','沙市区',3,'421000','湖北省荆州市沙市区','421000'),
	 ('421003','荆州区',3,'421000','湖北省荆州市荆州区','421000'),
	 ('421022','公安县',3,'421000','湖北省荆州市公安县','421000'),
	 ('421023','监利市',3,'421000','湖北省荆州市监利市','421000'),
	 ('421024','江陵县',3,'421000','湖北省荆州市江陵县','421000'),
	 ('421081','石首市',3,'421000','湖北省荆州市石首市','421000'),
	 ('421083','洪湖市',3,'421000','湖北省荆州市洪湖市','421000'),
	 ('421087','松滋市',3,'421000','湖北省荆州市松滋市','421000'),
	 ('421100','黄冈市',2,'420000','湖北省黄冈市','421100'),
	 ('421102','黄州区',3,'421100','湖北省黄冈市黄州区','421100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('421121','团风县',3,'421100','湖北省黄冈市团风县','421100'),
	 ('421122','红安县',3,'421100','湖北省黄冈市红安县','421100'),
	 ('421123','罗田县',3,'421100','湖北省黄冈市罗田县','421100'),
	 ('421124','英山县',3,'421100','湖北省黄冈市英山县','421100'),
	 ('421125','浠水县',3,'421100','湖北省黄冈市浠水县','421100'),
	 ('421126','蕲春县',3,'421100','湖北省黄冈市蕲春县','421100'),
	 ('421127','黄梅县',3,'421100','湖北省黄冈市黄梅县','421100'),
	 ('421181','麻城市',3,'421100','湖北省黄冈市麻城市','421100'),
	 ('421182','武穴市',3,'421100','湖北省黄冈市武穴市','421100'),
	 ('421200','咸宁市',2,'420000','湖北省咸宁市','421200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('421202','咸安区',3,'421200','湖北省咸宁市咸安区','421200'),
	 ('421221','嘉鱼县',3,'421200','湖北省咸宁市嘉鱼县','421200'),
	 ('421222','通城县',3,'421200','湖北省咸宁市通城县','421200'),
	 ('421223','崇阳县',3,'421200','湖北省咸宁市崇阳县','421200'),
	 ('421224','通山县',3,'421200','湖北省咸宁市通山县','421200'),
	 ('421281','赤壁市',3,'421200','湖北省咸宁市赤壁市','421200'),
	 ('421300','随州市',2,'420000','湖北省随州市','421300'),
	 ('421303','曾都区',3,'421300','湖北省随州市曾都区','421300'),
	 ('421321','随县',3,'421300','湖北省随州市随县','421300'),
	 ('421381','广水市',3,'421300','湖北省随州市广水市','421300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('422800','恩施土家族苗族自治州',2,'420000','湖北省恩施土家族苗族自治州','422800'),
	 ('422801','恩施市',3,'422800','湖北省恩施土家族苗族自治州恩施市','422800'),
	 ('422802','利川市',3,'422800','湖北省恩施土家族苗族自治州利川市','422800'),
	 ('422822','建始县',3,'422800','湖北省恩施土家族苗族自治州建始县','422800'),
	 ('422823','巴东县',3,'422800','湖北省恩施土家族苗族自治州巴东县','422800'),
	 ('422825','宣恩县',3,'422800','湖北省恩施土家族苗族自治州宣恩县','422800'),
	 ('422826','咸丰县',3,'422800','湖北省恩施土家族苗族自治州咸丰县','422800'),
	 ('422827','来凤县',3,'422800','湖北省恩施土家族苗族自治州来凤县','422800'),
	 ('422828','鹤峰县',3,'422800','湖北省恩施土家族苗族自治州鹤峰县','422800'),
	 ('429004','仙桃市',2,'420000','湖北省仙桃市','429004');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('429005','潜江市',2,'420000','湖北省潜江市','429005'),
	 ('429006','天门市',2,'420000','湖北省天门市','429006'),
	 ('429021','神农架林区',2,'420000','湖北省神农架林区','429021'),
	 ('430000','湖南省',1,NULL,'湖南省','430000'),
	 ('430100','长沙市',2,'430000','湖南省长沙市','430100'),
	 ('430102','芙蓉区',3,'430100','湖南省长沙市芙蓉区','430100'),
	 ('430103','天心区',3,'430100','湖南省长沙市天心区','430100'),
	 ('430104','岳麓区',3,'430100','湖南省长沙市岳麓区','430100'),
	 ('430105','开福区',3,'430100','湖南省长沙市开福区','430100'),
	 ('430111','雨花区',3,'430100','湖南省长沙市雨花区','430100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('430112','望城区',3,'430100','湖南省长沙市望城区','430100'),
	 ('430121','长沙县',3,'430100','湖南省长沙市长沙县','430100'),
	 ('430181','浏阳市',3,'430100','湖南省长沙市浏阳市','430100'),
	 ('430182','宁乡市',3,'430100','湖南省长沙市宁乡市','430100'),
	 ('430200','株洲市',2,'430000','湖南省株洲市','430200'),
	 ('430202','荷塘区',3,'430200','湖南省株洲市荷塘区','430200'),
	 ('430203','芦淞区',3,'430200','湖南省株洲市芦淞区','430200'),
	 ('430204','石峰区',3,'430200','湖南省株洲市石峰区','430200'),
	 ('430211','天元区',3,'430200','湖南省株洲市天元区','430200'),
	 ('430212','渌口区',3,'430200','湖南省株洲市渌口区','430200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('430223','攸县',3,'430200','湖南省株洲市攸县','430200'),
	 ('430224','茶陵县',3,'430200','湖南省株洲市茶陵县','430200'),
	 ('430225','炎陵县',3,'430200','湖南省株洲市炎陵县','430200'),
	 ('430281','醴陵市',3,'430200','湖南省株洲市醴陵市','430200'),
	 ('430300','湘潭市',2,'430000','湖南省湘潭市','430300'),
	 ('430302','雨湖区',3,'430300','湖南省湘潭市雨湖区','430300'),
	 ('430304','岳塘区',3,'430300','湖南省湘潭市岳塘区','430300'),
	 ('430321','湘潭县',3,'430300','湖南省湘潭市湘潭县','430300'),
	 ('430381','湘乡市',3,'430300','湖南省湘潭市湘乡市','430300'),
	 ('430382','韶山市',3,'430300','湖南省湘潭市韶山市','430300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('430400','衡阳市',2,'430000','湖南省衡阳市','430400'),
	 ('430405','珠晖区',3,'430400','湖南省衡阳市珠晖区','430400'),
	 ('430406','雁峰区',3,'430400','湖南省衡阳市雁峰区','430400'),
	 ('430407','石鼓区',3,'430400','湖南省衡阳市石鼓区','430400'),
	 ('430408','蒸湘区',3,'430400','湖南省衡阳市蒸湘区','430400'),
	 ('430412','南岳区',3,'430400','湖南省衡阳市南岳区','430400'),
	 ('430421','衡阳县',3,'430400','湖南省衡阳市衡阳县','430400'),
	 ('430422','衡南县',3,'430400','湖南省衡阳市衡南县','430400'),
	 ('430423','衡山县',3,'430400','湖南省衡阳市衡山县','430400'),
	 ('430424','衡东县',3,'430400','湖南省衡阳市衡东县','430400');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('430426','祁东县',3,'430400','湖南省衡阳市祁东县','430400'),
	 ('430481','耒阳市',3,'430400','湖南省衡阳市耒阳市','430400'),
	 ('430482','常宁市',3,'430400','湖南省衡阳市常宁市','430400'),
	 ('430500','邵阳市',2,'430000','湖南省邵阳市','430500'),
	 ('430502','双清区',3,'430500','湖南省邵阳市双清区','430500'),
	 ('430503','大祥区',3,'430500','湖南省邵阳市大祥区','430500'),
	 ('430511','北塔区',3,'430500','湖南省邵阳市北塔区','430500'),
	 ('430522','新邵县',3,'430500','湖南省邵阳市新邵县','430500'),
	 ('430523','邵阳县',3,'430500','湖南省邵阳市邵阳县','430500'),
	 ('430524','隆回县',3,'430500','湖南省邵阳市隆回县','430500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('430525','洞口县',3,'430500','湖南省邵阳市洞口县','430500'),
	 ('430527','绥宁县',3,'430500','湖南省邵阳市绥宁县','430500'),
	 ('430528','新宁县',3,'430500','湖南省邵阳市新宁县','430500'),
	 ('430529','城步苗族自治县',3,'430500','湖南省邵阳市城步苗族自治县','430500'),
	 ('430581','武冈市',3,'430500','湖南省邵阳市武冈市','430500'),
	 ('430582','邵东市',3,'430500','湖南省邵阳市邵东市','430500'),
	 ('430600','岳阳市',2,'430000','湖南省岳阳市','430600'),
	 ('430602','岳阳楼区',3,'430600','湖南省岳阳市岳阳楼区','430600'),
	 ('430603','云溪区',3,'430600','湖南省岳阳市云溪区','430600'),
	 ('430611','君山区',3,'430600','湖南省岳阳市君山区','430600');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('430621','岳阳县',3,'430600','湖南省岳阳市岳阳县','430600'),
	 ('430623','华容县',3,'430600','湖南省岳阳市华容县','430600'),
	 ('430624','湘阴县',3,'430600','湖南省岳阳市湘阴县','430600'),
	 ('430626','平江县',3,'430600','湖南省岳阳市平江县','430600'),
	 ('430681','汨罗市',3,'430600','湖南省岳阳市汨罗市','430600'),
	 ('430682','临湘市',3,'430600','湖南省岳阳市临湘市','430600'),
	 ('430700','常德市',2,'430000','湖南省常德市','430700'),
	 ('430702','武陵区',3,'430700','湖南省常德市武陵区','430700'),
	 ('430703','鼎城区',3,'430700','湖南省常德市鼎城区','430700'),
	 ('430721','安乡县',3,'430700','湖南省常德市安乡县','430700');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('430722','汉寿县',3,'430700','湖南省常德市汉寿县','430700'),
	 ('430723','澧县',3,'430700','湖南省常德市澧县','430700'),
	 ('430724','临澧县',3,'430700','湖南省常德市临澧县','430700'),
	 ('430725','桃源县',3,'430700','湖南省常德市桃源县','430700'),
	 ('430726','石门县',3,'430700','湖南省常德市石门县','430700'),
	 ('430781','津市市',3,'430700','湖南省常德市津市市','430700'),
	 ('430800','张家界市',2,'430000','湖南省张家界市','430800'),
	 ('430802','永定区',3,'430800','湖南省张家界市永定区','430800'),
	 ('430811','武陵源区',3,'430800','湖南省张家界市武陵源区','430800'),
	 ('430821','慈利县',3,'430800','湖南省张家界市慈利县','430800');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('430822','桑植县',3,'430800','湖南省张家界市桑植县','430800'),
	 ('430900','益阳市',2,'430000','湖南省益阳市','430900'),
	 ('430902','资阳区',3,'430900','湖南省益阳市资阳区','430900'),
	 ('430903','赫山区',3,'430900','湖南省益阳市赫山区','430900'),
	 ('430921','南县',3,'430900','湖南省益阳市南县','430900'),
	 ('430922','桃江县',3,'430900','湖南省益阳市桃江县','430900'),
	 ('430923','安化县',3,'430900','湖南省益阳市安化县','430900'),
	 ('430981','沅江市',3,'430900','湖南省益阳市沅江市','430900'),
	 ('431000','郴州市',2,'430000','湖南省郴州市','431000'),
	 ('431002','北湖区',3,'431000','湖南省郴州市北湖区','431000');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('431003','苏仙区',3,'431000','湖南省郴州市苏仙区','431000'),
	 ('431021','桂阳县',3,'431000','湖南省郴州市桂阳县','431000'),
	 ('431022','宜章县',3,'431000','湖南省郴州市宜章县','431000'),
	 ('431023','永兴县',3,'431000','湖南省郴州市永兴县','431000'),
	 ('431024','嘉禾县',3,'431000','湖南省郴州市嘉禾县','431000'),
	 ('431025','临武县',3,'431000','湖南省郴州市临武县','431000'),
	 ('431026','汝城县',3,'431000','湖南省郴州市汝城县','431000'),
	 ('431027','桂东县',3,'431000','湖南省郴州市桂东县','431000'),
	 ('431028','安仁县',3,'431000','湖南省郴州市安仁县','431000'),
	 ('431081','资兴市',3,'431000','湖南省郴州市资兴市','431000');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('431100','永州市',2,'430000','湖南省永州市','431100'),
	 ('431102','零陵区',3,'431100','湖南省永州市零陵区','431100'),
	 ('431103','冷水滩区',3,'431100','湖南省永州市冷水滩区','431100'),
	 ('431121','祁阳市',3,'431100','湖南省永州市祁阳市','431100'),
	 ('431122','东安县',3,'431100','湖南省永州市东安县','431100'),
	 ('431123','双牌县',3,'431100','湖南省永州市双牌县','431100'),
	 ('431124','道县',3,'431100','湖南省永州市道县','431100'),
	 ('431125','江永县',3,'431100','湖南省永州市江永县','431100'),
	 ('431126','宁远县',3,'431100','湖南省永州市宁远县','431100'),
	 ('431127','蓝山县',3,'431100','湖南省永州市蓝山县','431100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('431128','新田县',3,'431100','湖南省永州市新田县','431100'),
	 ('431129','江华瑶族自治县',3,'431100','湖南省永州市江华瑶族自治县','431100'),
	 ('431200','怀化市',2,'430000','湖南省怀化市','431200'),
	 ('431202','鹤城区',3,'431200','湖南省怀化市鹤城区','431200'),
	 ('431221','中方县',3,'431200','湖南省怀化市中方县','431200'),
	 ('431222','沅陵县',3,'431200','湖南省怀化市沅陵县','431200'),
	 ('431223','辰溪县',3,'431200','湖南省怀化市辰溪县','431200'),
	 ('431224','溆浦县',3,'431200','湖南省怀化市溆浦县','431200'),
	 ('431225','会同县',3,'431200','湖南省怀化市会同县','431200'),
	 ('431226','麻阳苗族自治县',3,'431200','湖南省怀化市麻阳苗族自治县','431200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('431227','新晃侗族自治县',3,'431200','湖南省怀化市新晃侗族自治县','431200'),
	 ('431228','芷江侗族自治县',3,'431200','湖南省怀化市芷江侗族自治县','431200'),
	 ('431229','靖州苗族侗族自治县',3,'431200','湖南省怀化市靖州苗族侗族自治县','431200'),
	 ('431230','通道侗族自治县',3,'431200','湖南省怀化市通道侗族自治县','431200'),
	 ('431281','洪江市',3,'431200','湖南省怀化市洪江市','431200'),
	 ('431300','娄底市',2,'430000','湖南省娄底市','431300'),
	 ('431302','娄星区',3,'431300','湖南省娄底市娄星区','431300'),
	 ('431321','双峰县',3,'431300','湖南省娄底市双峰县','431300'),
	 ('431322','新化县',3,'431300','湖南省娄底市新化县','431300'),
	 ('431381','冷水江市',3,'431300','湖南省娄底市冷水江市','431300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('431382','涟源市',3,'431300','湖南省娄底市涟源市','431300'),
	 ('433100','湘西土家族苗族自治州',2,'430000','湖南省湘西土家族苗族自治州','433100'),
	 ('433101','吉首市',3,'433100','湖南省湘西土家族苗族自治州吉首市','433100'),
	 ('433122','泸溪县',3,'433100','湖南省湘西土家族苗族自治州泸溪县','433100'),
	 ('433123','凤凰县',3,'433100','湖南省湘西土家族苗族自治州凤凰县','433100'),
	 ('433124','花垣县',3,'433100','湖南省湘西土家族苗族自治州花垣县','433100'),
	 ('433125','保靖县',3,'433100','湖南省湘西土家族苗族自治州保靖县','433100'),
	 ('433126','古丈县',3,'433100','湖南省湘西土家族苗族自治州古丈县','433100'),
	 ('433127','永顺县',3,'433100','湖南省湘西土家族苗族自治州永顺县','433100'),
	 ('433130','龙山县',3,'433100','湖南省湘西土家族苗族自治州龙山县','433100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('440000','广东省',1,NULL,'广东省','440000'),
	 ('440100','广州市',2,'440000','广东省广州市','440100'),
	 ('440103','荔湾区',3,'440100','广东省广州市荔湾区','440100'),
	 ('440104','越秀区',3,'440100','广东省广州市越秀区','440100'),
	 ('440105','海珠区',3,'440100','广东省广州市海珠区','440100'),
	 ('440106','天河区',3,'440100','广东省广州市天河区','440100'),
	 ('440111','白云区',3,'440100','广东省广州市白云区','440100'),
	 ('440112','黄埔区',3,'440100','广东省广州市黄埔区','440100'),
	 ('440113','番禺区',3,'440100','广东省广州市番禺区','440100'),
	 ('440114','花都区',3,'440100','广东省广州市花都区','440100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('440115','南沙区',3,'440100','广东省广州市南沙区','440100'),
	 ('440117','从化区',3,'440100','广东省广州市从化区','440100'),
	 ('440118','增城区',3,'440100','广东省广州市增城区','440100'),
	 ('440200','韶关市',2,'440000','广东省韶关市','440200'),
	 ('440203','武江区',3,'440200','广东省韶关市武江区','440200'),
	 ('440204','浈江区',3,'440200','广东省韶关市浈江区','440200'),
	 ('440205','曲江区',3,'440200','广东省韶关市曲江区','440200'),
	 ('440222','始兴县',3,'440200','广东省韶关市始兴县','440200'),
	 ('440224','仁化县',3,'440200','广东省韶关市仁化县','440200'),
	 ('440229','翁源县',3,'440200','广东省韶关市翁源县','440200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('440232','乳源瑶族自治县',3,'440200','广东省韶关市乳源瑶族自治县','440200'),
	 ('440233','新丰县',3,'440200','广东省韶关市新丰县','440200'),
	 ('440281','乐昌市',3,'440200','广东省韶关市乐昌市','440200'),
	 ('440282','南雄市',3,'440200','广东省韶关市南雄市','440200'),
	 ('440300','深圳市',2,'440000','广东省深圳市','440300'),
	 ('440303','罗湖区',3,'440300','广东省深圳市罗湖区','440300'),
	 ('440304','福田区',3,'440300','广东省深圳市福田区','440300'),
	 ('440305','南山区',3,'440300','广东省深圳市南山区','440300'),
	 ('440306','宝安区',3,'440300','广东省深圳市宝安区','440300'),
	 ('440307','龙岗区',3,'440300','广东省深圳市龙岗区','440300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('440308','盐田区',3,'440300','广东省深圳市盐田区','440300'),
	 ('440309','龙华区',3,'440300','广东省深圳市龙华区','440300'),
	 ('440310','坪山区',3,'440300','广东省深圳市坪山区','440300'),
	 ('440311','光明区',3,'440300','广东省深圳市光明区','440300'),
	 ('440400','珠海市',2,'440000','广东省珠海市','440400'),
	 ('440402','香洲区',3,'440400','广东省珠海市香洲区','440400'),
	 ('440403','斗门区',3,'440400','广东省珠海市斗门区','440400'),
	 ('440404','金湾区',3,'440400','广东省珠海市金湾区','440400'),
	 ('440500','汕头市',2,'440000','广东省汕头市','440500'),
	 ('440507','龙湖区',3,'440500','广东省汕头市龙湖区','440500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('440511','金平区',3,'440500','广东省汕头市金平区','440500'),
	 ('440512','濠江区',3,'440500','广东省汕头市濠江区','440500'),
	 ('440513','潮阳区',3,'440500','广东省汕头市潮阳区','440500'),
	 ('440514','潮南区',3,'440500','广东省汕头市潮南区','440500'),
	 ('440515','澄海区',3,'440500','广东省汕头市澄海区','440500'),
	 ('440523','南澳县',3,'440500','广东省汕头市南澳县','440500'),
	 ('440600','佛山市',2,'440000','广东省佛山市','440600'),
	 ('440604','禅城区',3,'440600','广东省佛山市禅城区','440600'),
	 ('440605','南海区',3,'440600','广东省佛山市南海区','440600'),
	 ('440606','顺德区',3,'440600','广东省佛山市顺德区','440600');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('440607','三水区',3,'440600','广东省佛山市三水区','440600'),
	 ('440608','高明区',3,'440600','广东省佛山市高明区','440600'),
	 ('440700','江门市',2,'440000','广东省江门市','440700'),
	 ('440703','蓬江区',3,'440700','广东省江门市蓬江区','440700'),
	 ('440704','江海区',3,'440700','广东省江门市江海区','440700'),
	 ('440705','新会区',3,'440700','广东省江门市新会区','440700'),
	 ('440781','台山市',3,'440700','广东省江门市台山市','440700'),
	 ('440783','开平市',3,'440700','广东省江门市开平市','440700'),
	 ('440784','鹤山市',3,'440700','广东省江门市鹤山市','440700'),
	 ('440785','恩平市',3,'440700','广东省江门市恩平市','440700');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('440800','湛江市',2,'440000','广东省湛江市','440800'),
	 ('440802','赤坎区',3,'440800','广东省湛江市赤坎区','440800'),
	 ('440803','霞山区',3,'440800','广东省湛江市霞山区','440800'),
	 ('440804','坡头区',3,'440800','广东省湛江市坡头区','440800'),
	 ('440811','麻章区',3,'440800','广东省湛江市麻章区','440800'),
	 ('440823','遂溪县',3,'440800','广东省湛江市遂溪县','440800'),
	 ('440825','徐闻县',3,'440800','广东省湛江市徐闻县','440800'),
	 ('440881','廉江市',3,'440800','广东省湛江市廉江市','440800'),
	 ('440882','雷州市',3,'440800','广东省湛江市雷州市','440800'),
	 ('440883','吴川市',3,'440800','广东省湛江市吴川市','440800');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('440900','茂名市',2,'440000','广东省茂名市','440900'),
	 ('440902','茂南区',3,'440900','广东省茂名市茂南区','440900'),
	 ('440904','电白区',3,'440900','广东省茂名市电白区','440900'),
	 ('440981','高州市',3,'440900','广东省茂名市高州市','440900'),
	 ('440982','化州市',3,'440900','广东省茂名市化州市','440900'),
	 ('440983','信宜市',3,'440900','广东省茂名市信宜市','440900'),
	 ('441200','肇庆市',2,'440000','广东省肇庆市','441200'),
	 ('441202','端州区',3,'441200','广东省肇庆市端州区','441200'),
	 ('441203','鼎湖区',3,'441200','广东省肇庆市鼎湖区','441200'),
	 ('441204','高要区',3,'441200','广东省肇庆市高要区','441200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('441223','广宁县',3,'441200','广东省肇庆市广宁县','441200'),
	 ('441224','怀集县',3,'441200','广东省肇庆市怀集县','441200'),
	 ('441225','封开县',3,'441200','广东省肇庆市封开县','441200'),
	 ('441226','德庆县',3,'441200','广东省肇庆市德庆县','441200'),
	 ('441284','四会市',3,'441200','广东省肇庆市四会市','441200'),
	 ('441300','惠州市',2,'440000','广东省惠州市','441300'),
	 ('441302','惠城区',3,'441300','广东省惠州市惠城区','441300'),
	 ('441303','惠阳区',3,'441300','广东省惠州市惠阳区','441300'),
	 ('441322','博罗县',3,'441300','广东省惠州市博罗县','441300'),
	 ('441323','惠东县',3,'441300','广东省惠州市惠东县','441300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('441324','龙门县',3,'441300','广东省惠州市龙门县','441300'),
	 ('441400','梅州市',2,'440000','广东省梅州市','441400'),
	 ('441402','梅江区',3,'441400','广东省梅州市梅江区','441400'),
	 ('441403','梅县区',3,'441400','广东省梅州市梅县区','441400'),
	 ('441422','大埔县',3,'441400','广东省梅州市大埔县','441400'),
	 ('441423','丰顺县',3,'441400','广东省梅州市丰顺县','441400'),
	 ('441424','五华县',3,'441400','广东省梅州市五华县','441400'),
	 ('441426','平远县',3,'441400','广东省梅州市平远县','441400'),
	 ('441427','蕉岭县',3,'441400','广东省梅州市蕉岭县','441400'),
	 ('441481','兴宁市',3,'441400','广东省梅州市兴宁市','441400');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('441500','汕尾市',2,'440000','广东省汕尾市','441500'),
	 ('441502','城区',3,'441500','广东省汕尾市城区','441500'),
	 ('441521','海丰县',3,'441500','广东省汕尾市海丰县','441500'),
	 ('441523','陆河县',3,'441500','广东省汕尾市陆河县','441500'),
	 ('441581','陆丰市',3,'441500','广东省汕尾市陆丰市','441500'),
	 ('441600','河源市',2,'440000','广东省河源市','441600'),
	 ('441602','源城区',3,'441600','广东省河源市源城区','441600'),
	 ('441621','紫金县',3,'441600','广东省河源市紫金县','441600'),
	 ('441622','龙川县',3,'441600','广东省河源市龙川县','441600'),
	 ('441623','连平县',3,'441600','广东省河源市连平县','441600');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('441624','和平县',3,'441600','广东省河源市和平县','441600'),
	 ('441625','东源县',3,'441600','广东省河源市东源县','441600'),
	 ('441700','阳江市',2,'440000','广东省阳江市','441700'),
	 ('441702','江城区',3,'441700','广东省阳江市江城区','441700'),
	 ('441704','阳东区',3,'441700','广东省阳江市阳东区','441700'),
	 ('441721','阳西县',3,'441700','广东省阳江市阳西县','441700'),
	 ('441781','阳春市',3,'441700','广东省阳江市阳春市','441700'),
	 ('441800','清远市',2,'440000','广东省清远市','441800'),
	 ('441802','清城区',3,'441800','广东省清远市清城区','441800'),
	 ('441803','清新区',3,'441800','广东省清远市清新区','441800');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('441821','佛冈县',3,'441800','广东省清远市佛冈县','441800'),
	 ('441823','阳山县',3,'441800','广东省清远市阳山县','441800'),
	 ('441825','连山壮族瑶族自治县',3,'441800','广东省清远市连山壮族瑶族自治县','441800'),
	 ('441826','连南瑶族自治县',3,'441800','广东省清远市连南瑶族自治县','441800'),
	 ('441881','英德市',3,'441800','广东省清远市英德市','441800'),
	 ('441882','连州市',3,'441800','广东省清远市连州市','441800'),
	 ('441900','东莞市',2,'440000','广东省东莞市','441900'),
	 ('442000','中山市',2,'440000','广东省中山市','442000'),
	 ('445100','潮州市',2,'440000','广东省潮州市','445100'),
	 ('445102','湘桥区',3,'445100','广东省潮州市湘桥区','445100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('445103','潮安区',3,'445100','广东省潮州市潮安区','445100'),
	 ('445122','饶平县',3,'445100','广东省潮州市饶平县','445100'),
	 ('445200','揭阳市',2,'440000','广东省揭阳市','445200'),
	 ('445202','榕城区',3,'445200','广东省揭阳市榕城区','445200'),
	 ('445203','揭东区',3,'445200','广东省揭阳市揭东区','445200'),
	 ('445222','揭西县',3,'445200','广东省揭阳市揭西县','445200'),
	 ('445224','惠来县',3,'445200','广东省揭阳市惠来县','445200'),
	 ('445281','普宁市',3,'445200','广东省揭阳市普宁市','445200'),
	 ('445300','云浮市',2,'440000','广东省云浮市','445300'),
	 ('445302','云城区',3,'445300','广东省云浮市云城区','445300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('445303','云安区',3,'445300','广东省云浮市云安区','445300'),
	 ('445321','新兴县',3,'445300','广东省云浮市新兴县','445300'),
	 ('445322','郁南县',3,'445300','广东省云浮市郁南县','445300'),
	 ('445381','罗定市',3,'445300','广东省云浮市罗定市','445300'),
	 ('450000','广西壮族自治区',1,NULL,'广西壮族自治区','450000'),
	 ('450100','南宁市',2,'450000','广西壮族自治区南宁市','450100'),
	 ('450102','兴宁区',3,'450100','广西壮族自治区南宁市兴宁区','450100'),
	 ('450103','青秀区',3,'450100','广西壮族自治区南宁市青秀区','450100'),
	 ('450105','江南区',3,'450100','广西壮族自治区南宁市江南区','450100'),
	 ('450107','西乡塘区',3,'450100','广西壮族自治区南宁市西乡塘区','450100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('450108','良庆区',3,'450100','广西壮族自治区南宁市良庆区','450100'),
	 ('450109','邕宁区',3,'450100','广西壮族自治区南宁市邕宁区','450100'),
	 ('450110','武鸣区',3,'450100','广西壮族自治区南宁市武鸣区','450100'),
	 ('450123','隆安县',3,'450100','广西壮族自治区南宁市隆安县','450100'),
	 ('450124','马山县',3,'450100','广西壮族自治区南宁市马山县','450100'),
	 ('450125','上林县',3,'450100','广西壮族自治区南宁市上林县','450100'),
	 ('450126','宾阳县',3,'450100','广西壮族自治区南宁市宾阳县','450100'),
	 ('450127','横州市',3,'450100','广西壮族自治区南宁市横州市','450100'),
	 ('450200','柳州市',2,'450000','广西壮族自治区柳州市','450200'),
	 ('450202','城中区',3,'450200','广西壮族自治区柳州市城中区','450200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('450203','鱼峰区',3,'450200','广西壮族自治区柳州市鱼峰区','450200'),
	 ('450204','柳南区',3,'450200','广西壮族自治区柳州市柳南区','450200'),
	 ('450205','柳北区',3,'450200','广西壮族自治区柳州市柳北区','450200'),
	 ('450206','柳江区',3,'450200','广西壮族自治区柳州市柳江区','450200'),
	 ('450222','柳城县',3,'450200','广西壮族自治区柳州市柳城县','450200'),
	 ('450223','鹿寨县',3,'450200','广西壮族自治区柳州市鹿寨县','450200'),
	 ('450224','融安县',3,'450200','广西壮族自治区柳州市融安县','450200'),
	 ('450225','融水苗族自治县',3,'450200','广西壮族自治区柳州市融水苗族自治县','450200'),
	 ('450226','三江侗族自治县',3,'450200','广西壮族自治区柳州市三江侗族自治县','450200'),
	 ('450300','桂林市',2,'450000','广西壮族自治区桂林市','450300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('450302','秀峰区',3,'450300','广西壮族自治区桂林市秀峰区','450300'),
	 ('450303','叠彩区',3,'450300','广西壮族自治区桂林市叠彩区','450300'),
	 ('450304','象山区',3,'450300','广西壮族自治区桂林市象山区','450300'),
	 ('450305','七星区',3,'450300','广西壮族自治区桂林市七星区','450300'),
	 ('450311','雁山区',3,'450300','广西壮族自治区桂林市雁山区','450300'),
	 ('450312','临桂区',3,'450300','广西壮族自治区桂林市临桂区','450300'),
	 ('450321','阳朔县',3,'450300','广西壮族自治区桂林市阳朔县','450300'),
	 ('450323','灵川县',3,'450300','广西壮族自治区桂林市灵川县','450300'),
	 ('450324','全州县',3,'450300','广西壮族自治区桂林市全州县','450300'),
	 ('450325','兴安县',3,'450300','广西壮族自治区桂林市兴安县','450300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('450326','永福县',3,'450300','广西壮族自治区桂林市永福县','450300'),
	 ('450327','灌阳县',3,'450300','广西壮族自治区桂林市灌阳县','450300'),
	 ('450328','龙胜各族自治县',3,'450300','广西壮族自治区桂林市龙胜各族自治县','450300'),
	 ('450329','资源县',3,'450300','广西壮族自治区桂林市资源县','450300'),
	 ('450330','平乐县',3,'450300','广西壮族自治区桂林市平乐县','450300'),
	 ('450332','恭城瑶族自治县',3,'450300','广西壮族自治区桂林市恭城瑶族自治县','450300'),
	 ('450381','荔浦市',3,'450300','广西壮族自治区桂林市荔浦市','450300'),
	 ('450400','梧州市',2,'450000','广西壮族自治区梧州市','450400'),
	 ('450403','万秀区',3,'450400','广西壮族自治区梧州市万秀区','450400'),
	 ('450405','长洲区',3,'450400','广西壮族自治区梧州市长洲区','450400');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('450406','龙圩区',3,'450400','广西壮族自治区梧州市龙圩区','450400'),
	 ('450421','苍梧县',3,'450400','广西壮族自治区梧州市苍梧县','450400'),
	 ('450422','藤县',3,'450400','广西壮族自治区梧州市藤县','450400'),
	 ('450423','蒙山县',3,'450400','广西壮族自治区梧州市蒙山县','450400'),
	 ('450481','岑溪市',3,'450400','广西壮族自治区梧州市岑溪市','450400'),
	 ('450500','北海市',2,'450000','广西壮族自治区北海市','450500'),
	 ('450502','海城区',3,'450500','广西壮族自治区北海市海城区','450500'),
	 ('450503','银海区',3,'450500','广西壮族自治区北海市银海区','450500'),
	 ('450512','铁山港区',3,'450500','广西壮族自治区北海市铁山港区','450500'),
	 ('450521','合浦县',3,'450500','广西壮族自治区北海市合浦县','450500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('450600','防城港市',2,'450000','广西壮族自治区防城港市','450600'),
	 ('450602','港口区',3,'450600','广西壮族自治区防城港市港口区','450600'),
	 ('450603','防城区',3,'450600','广西壮族自治区防城港市防城区','450600'),
	 ('450621','上思县',3,'450600','广西壮族自治区防城港市上思县','450600'),
	 ('450681','东兴市',3,'450600','广西壮族自治区防城港市东兴市','450600'),
	 ('450700','钦州市',2,'450000','广西壮族自治区钦州市','450700'),
	 ('450702','钦南区',3,'450700','广西壮族自治区钦州市钦南区','450700'),
	 ('450703','钦北区',3,'450700','广西壮族自治区钦州市钦北区','450700'),
	 ('450721','灵山县',3,'450700','广西壮族自治区钦州市灵山县','450700'),
	 ('450722','浦北县',3,'450700','广西壮族自治区钦州市浦北县','450700');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('450800','贵港市',2,'450000','广西壮族自治区贵港市','450800'),
	 ('450802','港北区',3,'450800','广西壮族自治区贵港市港北区','450800'),
	 ('450803','港南区',3,'450800','广西壮族自治区贵港市港南区','450800'),
	 ('450804','覃塘区',3,'450800','广西壮族自治区贵港市覃塘区','450800'),
	 ('450821','平南县',3,'450800','广西壮族自治区贵港市平南县','450800'),
	 ('450881','桂平市',3,'450800','广西壮族自治区贵港市桂平市','450800'),
	 ('450900','玉林市',2,'450000','广西壮族自治区玉林市','450900'),
	 ('450902','玉州区',3,'450900','广西壮族自治区玉林市玉州区','450900'),
	 ('450903','福绵区',3,'450900','广西壮族自治区玉林市福绵区','450900'),
	 ('450921','容县',3,'450900','广西壮族自治区玉林市容县','450900');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('450922','陆川县',3,'450900','广西壮族自治区玉林市陆川县','450900'),
	 ('450923','博白县',3,'450900','广西壮族自治区玉林市博白县','450900'),
	 ('450924','兴业县',3,'450900','广西壮族自治区玉林市兴业县','450900'),
	 ('450981','北流市',3,'450900','广西壮族自治区玉林市北流市','450900'),
	 ('451000','百色市',2,'450000','广西壮族自治区百色市','451000'),
	 ('451002','右江区',3,'451000','广西壮族自治区百色市右江区','451000'),
	 ('451003','田阳区',3,'451000','广西壮族自治区百色市田阳区','451000'),
	 ('451022','田东县',3,'451000','广西壮族自治区百色市田东县','451000'),
	 ('451024','德保县',3,'451000','广西壮族自治区百色市德保县','451000'),
	 ('451026','那坡县',3,'451000','广西壮族自治区百色市那坡县','451000');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('451027','凌云县',3,'451000','广西壮族自治区百色市凌云县','451000'),
	 ('451028','乐业县',3,'451000','广西壮族自治区百色市乐业县','451000'),
	 ('451029','田林县',3,'451000','广西壮族自治区百色市田林县','451000'),
	 ('451030','西林县',3,'451000','广西壮族自治区百色市西林县','451000'),
	 ('451031','隆林各族自治县',3,'451000','广西壮族自治区百色市隆林各族自治县','451000'),
	 ('451081','靖西市',3,'451000','广西壮族自治区百色市靖西市','451000'),
	 ('451082','平果市',3,'451000','广西壮族自治区百色市平果市','451000'),
	 ('451100','贺州市',2,'450000','广西壮族自治区贺州市','451100'),
	 ('451102','八步区',3,'451100','广西壮族自治区贺州市八步区','451100'),
	 ('451103','平桂区',3,'451100','广西壮族自治区贺州市平桂区','451100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('451121','昭平县',3,'451100','广西壮族自治区贺州市昭平县','451100'),
	 ('451122','钟山县',3,'451100','广西壮族自治区贺州市钟山县','451100'),
	 ('451123','富川瑶族自治县',3,'451100','广西壮族自治区贺州市富川瑶族自治县','451100'),
	 ('451200','河池市',2,'450000','广西壮族自治区河池市','451200'),
	 ('451202','金城江区',3,'451200','广西壮族自治区河池市金城江区','451200'),
	 ('451203','宜州区',3,'451200','广西壮族自治区河池市宜州区','451200'),
	 ('451221','南丹县',3,'451200','广西壮族自治区河池市南丹县','451200'),
	 ('451222','天峨县',3,'451200','广西壮族自治区河池市天峨县','451200'),
	 ('451223','凤山县',3,'451200','广西壮族自治区河池市凤山县','451200'),
	 ('451224','东兰县',3,'451200','广西壮族自治区河池市东兰县','451200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('451225','罗城仫佬族自治县',3,'451200','广西壮族自治区河池市罗城仫佬族自治县','451200'),
	 ('451226','环江毛南族自治县',3,'451200','广西壮族自治区河池市环江毛南族自治县','451200'),
	 ('451227','巴马瑶族自治县',3,'451200','广西壮族自治区河池市巴马瑶族自治县','451200'),
	 ('451228','都安瑶族自治县',3,'451200','广西壮族自治区河池市都安瑶族自治县','451200'),
	 ('451229','大化瑶族自治县',3,'451200','广西壮族自治区河池市大化瑶族自治县','451200'),
	 ('451300','来宾市',2,'450000','广西壮族自治区来宾市','451300'),
	 ('451302','兴宾区',3,'451300','广西壮族自治区来宾市兴宾区','451300'),
	 ('451321','忻城县',3,'451300','广西壮族自治区来宾市忻城县','451300'),
	 ('451322','象州县',3,'451300','广西壮族自治区来宾市象州县','451300'),
	 ('451323','武宣县',3,'451300','广西壮族自治区来宾市武宣县','451300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('451324','金秀瑶族自治县',3,'451300','广西壮族自治区来宾市金秀瑶族自治县','451300'),
	 ('451381','合山市',3,'451300','广西壮族自治区来宾市合山市','451300'),
	 ('451400','崇左市',2,'450000','广西壮族自治区崇左市','451400'),
	 ('451402','江州区',3,'451400','广西壮族自治区崇左市江州区','451400'),
	 ('451421','扶绥县',3,'451400','广西壮族自治区崇左市扶绥县','451400'),
	 ('451422','宁明县',3,'451400','广西壮族自治区崇左市宁明县','451400'),
	 ('451423','龙州县',3,'451400','广西壮族自治区崇左市龙州县','451400'),
	 ('451424','大新县',3,'451400','广西壮族自治区崇左市大新县','451400'),
	 ('451425','天等县',3,'451400','广西壮族自治区崇左市天等县','451400'),
	 ('451481','凭祥市',3,'451400','广西壮族自治区崇左市凭祥市','451400');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('460000','海南省',1,NULL,'海南省','460000'),
	 ('460100','海口市',2,'460000','海南省海口市','460100'),
	 ('460105','秀英区',3,'460100','海南省海口市秀英区','460100'),
	 ('460106','龙华区',3,'460100','海南省海口市龙华区','460100'),
	 ('460107','琼山区',3,'460100','海南省海口市琼山区','460100'),
	 ('460108','美兰区',3,'460100','海南省海口市美兰区','460100'),
	 ('460200','三亚市',2,'460000','海南省三亚市','460200'),
	 ('460202','海棠区',3,'460200','海南省三亚市海棠区','460200'),
	 ('460203','吉阳区',3,'460200','海南省三亚市吉阳区','460200'),
	 ('460204','天涯区',3,'460200','海南省三亚市天涯区','460200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('460205','崖州区',3,'460200','海南省三亚市崖州区','460200'),
	 ('460300','三沙市',2,'460000','海南省三沙市','460300'),
	 ('460301','西沙区',3,'460300','海南省三沙市西沙区','460300'),
	 ('460302','南沙区',3,'460300','海南省三沙市南沙区','460300'),
	 ('460400','儋州市',2,'460000','海南省儋州市','460400'),
	 ('469001','五指山市',2,'460000','海南省五指山市','469001'),
	 ('469002','琼海市',2,'460000','海南省琼海市','469002'),
	 ('469005','文昌市',2,'460000','海南省文昌市','469005'),
	 ('469006','万宁市',2,'460000','海南省万宁市','469006'),
	 ('469007','东方市',2,'460000','海南省东方市','469007');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('469021','定安县',2,'460000','海南省定安县','469021'),
	 ('469022','屯昌县',2,'460000','海南省屯昌县','469022'),
	 ('469023','澄迈县',2,'460000','海南省澄迈县','469023'),
	 ('469024','临高县',2,'460000','海南省临高县','469024'),
	 ('469025','白沙黎族自治县',2,'460000','海南省白沙黎族自治县','469025'),
	 ('469026','昌江黎族自治县',2,'460000','海南省昌江黎族自治县','469026'),
	 ('469027','乐东黎族自治县',2,'460000','海南省乐东黎族自治县','469027'),
	 ('469028','陵水黎族自治县',2,'460000','海南省陵水黎族自治县','469028'),
	 ('469029','保亭黎族苗族自治县',2,'460000','海南省保亭黎族苗族自治县','469029'),
	 ('469030','琼中黎族苗族自治县',2,'460000','海南省琼中黎族苗族自治县','469030');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('500000','重庆市',1,NULL,'重庆市','500000'),
	 ('500101','万州区',2,'500000','重庆市万州区','500101'),
	 ('500102','涪陵区',2,'500000','重庆市涪陵区','500102'),
	 ('500103','渝中区',2,'500000','重庆市渝中区','500103'),
	 ('500104','大渡口区',2,'500000','重庆市大渡口区','500104'),
	 ('500105','江北区',2,'500000','重庆市江北区','500105'),
	 ('500106','沙坪坝区',2,'500000','重庆市沙坪坝区','500106'),
	 ('500107','九龙坡区',2,'500000','重庆市九龙坡区','500107'),
	 ('500108','南岸区',2,'500000','重庆市南岸区','500108'),
	 ('500109','北碚区',2,'500000','重庆市北碚区','500109');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('500110','綦江区',2,'500000','重庆市綦江区','500110'),
	 ('500111','大足区',2,'500000','重庆市大足区','500111'),
	 ('500112','渝北区',2,'500000','重庆市渝北区','500112'),
	 ('500113','巴南区',2,'500000','重庆市巴南区','500113'),
	 ('500114','黔江区',2,'500000','重庆市黔江区','500114'),
	 ('500115','长寿区',2,'500000','重庆市长寿区','500115'),
	 ('500116','江津区',2,'500000','重庆市江津区','500116'),
	 ('500117','合川区',2,'500000','重庆市合川区','500117'),
	 ('500118','永川区',2,'500000','重庆市永川区','500118'),
	 ('500119','南川区',2,'500000','重庆市南川区','500119');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('500120','璧山区',2,'500000','重庆市璧山区','500120'),
	 ('500151','铜梁区',2,'500000','重庆市铜梁区','500151'),
	 ('500152','潼南区',2,'500000','重庆市潼南区','500152'),
	 ('500153','荣昌区',2,'500000','重庆市荣昌区','500153'),
	 ('500154','开州区',2,'500000','重庆市开州区','500154'),
	 ('500155','梁平区',2,'500000','重庆市梁平区','500155'),
	 ('500156','武隆区',2,'500000','重庆市武隆区','500156'),
	 ('500229','城口县',2,'500000','重庆市城口县','500229'),
	 ('500230','丰都县',2,'500000','重庆市丰都县','500230'),
	 ('500231','垫江县',2,'500000','重庆市垫江县','500231');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('500233','忠县',2,'500000','重庆市忠县','500233'),
	 ('500235','云阳县',2,'500000','重庆市云阳县','500235'),
	 ('500236','奉节县',2,'500000','重庆市奉节县','500236'),
	 ('500237','巫山县',2,'500000','重庆市巫山县','500237'),
	 ('500238','巫溪县',2,'500000','重庆市巫溪县','500238'),
	 ('500240','石柱土家族自治县',2,'500000','重庆市石柱土家族自治县','500240'),
	 ('500241','秀山土家族苗族自治县',2,'500000','重庆市秀山土家族苗族自治县','500241'),
	 ('500242','酉阳土家族苗族自治县',2,'500000','重庆市酉阳土家族苗族自治县','500242'),
	 ('500243','彭水苗族土家族自治县',2,'500000','重庆市彭水苗族土家族自治县','500243'),
	 ('510000','四川省',1,NULL,'四川省','510000');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('510100','成都市',2,'510000','四川省成都市','510100'),
	 ('510104','锦江区',3,'510100','四川省成都市锦江区','510100'),
	 ('510105','青羊区',3,'510100','四川省成都市青羊区','510100'),
	 ('510106','金牛区',3,'510100','四川省成都市金牛区','510100'),
	 ('510107','武侯区',3,'510100','四川省成都市武侯区','510100'),
	 ('510108','成华区',3,'510100','四川省成都市成华区','510100'),
	 ('510112','龙泉驿区',3,'510100','四川省成都市龙泉驿区','510100'),
	 ('510113','青白江区',3,'510100','四川省成都市青白江区','510100'),
	 ('510114','新都区',3,'510100','四川省成都市新都区','510100'),
	 ('510115','温江区',3,'510100','四川省成都市温江区','510100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('510116','双流区',3,'510100','四川省成都市双流区','510100'),
	 ('510117','郫都区',3,'510100','四川省成都市郫都区','510100'),
	 ('510118','新津区',3,'510100','四川省成都市新津区','510100'),
	 ('510121','金堂县',3,'510100','四川省成都市金堂县','510100'),
	 ('510129','大邑县',3,'510100','四川省成都市大邑县','510100'),
	 ('510131','蒲江县',3,'510100','四川省成都市蒲江县','510100'),
	 ('510181','都江堰市',3,'510100','四川省成都市都江堰市','510100'),
	 ('510182','彭州市',3,'510100','四川省成都市彭州市','510100'),
	 ('510183','邛崃市',3,'510100','四川省成都市邛崃市','510100'),
	 ('510184','崇州市',3,'510100','四川省成都市崇州市','510100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('510185','简阳市',3,'510100','四川省成都市简阳市','510100'),
	 ('510300','自贡市',2,'510000','四川省自贡市','510300'),
	 ('510302','自流井区',3,'510300','四川省自贡市自流井区','510300'),
	 ('510303','贡井区',3,'510300','四川省自贡市贡井区','510300'),
	 ('510304','大安区',3,'510300','四川省自贡市大安区','510300'),
	 ('510311','沿滩区',3,'510300','四川省自贡市沿滩区','510300'),
	 ('510321','荣县',3,'510300','四川省自贡市荣县','510300'),
	 ('510322','富顺县',3,'510300','四川省自贡市富顺县','510300'),
	 ('510400','攀枝花市',2,'510000','四川省攀枝花市','510400'),
	 ('510402','东区',3,'510400','四川省攀枝花市东区','510400');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('510403','西区',3,'510400','四川省攀枝花市西区','510400'),
	 ('510411','仁和区',3,'510400','四川省攀枝花市仁和区','510400'),
	 ('510421','米易县',3,'510400','四川省攀枝花市米易县','510400'),
	 ('510422','盐边县',3,'510400','四川省攀枝花市盐边县','510400'),
	 ('510500','泸州市',2,'510000','四川省泸州市','510500'),
	 ('510502','江阳区',3,'510500','四川省泸州市江阳区','510500'),
	 ('510503','纳溪区',3,'510500','四川省泸州市纳溪区','510500'),
	 ('510504','龙马潭区',3,'510500','四川省泸州市龙马潭区','510500'),
	 ('510521','泸县',3,'510500','四川省泸州市泸县','510500'),
	 ('510522','合江县',3,'510500','四川省泸州市合江县','510500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('510524','叙永县',3,'510500','四川省泸州市叙永县','510500'),
	 ('510525','古蔺县',3,'510500','四川省泸州市古蔺县','510500'),
	 ('510600','德阳市',2,'510000','四川省德阳市','510600'),
	 ('510603','旌阳区',3,'510600','四川省德阳市旌阳区','510600'),
	 ('510604','罗江区',3,'510600','四川省德阳市罗江区','510600'),
	 ('510623','中江县',3,'510600','四川省德阳市中江县','510600'),
	 ('510681','广汉市',3,'510600','四川省德阳市广汉市','510600'),
	 ('510682','什邡市',3,'510600','四川省德阳市什邡市','510600'),
	 ('510683','绵竹市',3,'510600','四川省德阳市绵竹市','510600'),
	 ('510700','绵阳市',2,'510000','四川省绵阳市','510700');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('510703','涪城区',3,'510700','四川省绵阳市涪城区','510700'),
	 ('510704','游仙区',3,'510700','四川省绵阳市游仙区','510700'),
	 ('510705','安州区',3,'510700','四川省绵阳市安州区','510700'),
	 ('510722','三台县',3,'510700','四川省绵阳市三台县','510700'),
	 ('510723','盐亭县',3,'510700','四川省绵阳市盐亭县','510700'),
	 ('510725','梓潼县',3,'510700','四川省绵阳市梓潼县','510700'),
	 ('510726','北川羌族自治县',3,'510700','四川省绵阳市北川羌族自治县','510700'),
	 ('510727','平武县',3,'510700','四川省绵阳市平武县','510700'),
	 ('510781','江油市',3,'510700','四川省绵阳市江油市','510700'),
	 ('510800','广元市',2,'510000','四川省广元市','510800');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('510802','利州区',3,'510800','四川省广元市利州区','510800'),
	 ('510811','昭化区',3,'510800','四川省广元市昭化区','510800'),
	 ('510812','朝天区',3,'510800','四川省广元市朝天区','510800'),
	 ('510821','旺苍县',3,'510800','四川省广元市旺苍县','510800'),
	 ('510822','青川县',3,'510800','四川省广元市青川县','510800'),
	 ('510823','剑阁县',3,'510800','四川省广元市剑阁县','510800'),
	 ('510824','苍溪县',3,'510800','四川省广元市苍溪县','510800'),
	 ('510900','遂宁市',2,'510000','四川省遂宁市','510900'),
	 ('510903','船山区',3,'510900','四川省遂宁市船山区','510900'),
	 ('510904','安居区',3,'510900','四川省遂宁市安居区','510900');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('510921','蓬溪县',3,'510900','四川省遂宁市蓬溪县','510900'),
	 ('510923','大英县',3,'510900','四川省遂宁市大英县','510900'),
	 ('510981','射洪市',3,'510900','四川省遂宁市射洪市','510900'),
	 ('511000','内江市',2,'510000','四川省内江市','511000'),
	 ('511002','市中区',3,'511000','四川省内江市市中区','511000'),
	 ('511011','东兴区',3,'511000','四川省内江市东兴区','511000'),
	 ('511024','威远县',3,'511000','四川省内江市威远县','511000'),
	 ('511025','资中县',3,'511000','四川省内江市资中县','511000'),
	 ('511083','隆昌市',3,'511000','四川省内江市隆昌市','511000'),
	 ('511100','乐山市',2,'510000','四川省乐山市','511100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('511102','市中区',3,'511100','四川省乐山市市中区','511100'),
	 ('511111','沙湾区',3,'511100','四川省乐山市沙湾区','511100'),
	 ('511112','五通桥区',3,'511100','四川省乐山市五通桥区','511100'),
	 ('511113','金口河区',3,'511100','四川省乐山市金口河区','511100'),
	 ('511123','犍为县',3,'511100','四川省乐山市犍为县','511100'),
	 ('511124','井研县',3,'511100','四川省乐山市井研县','511100'),
	 ('511126','夹江县',3,'511100','四川省乐山市夹江县','511100'),
	 ('511129','沐川县',3,'511100','四川省乐山市沐川县','511100'),
	 ('511132','峨边彝族自治县',3,'511100','四川省乐山市峨边彝族自治县','511100'),
	 ('511133','马边彝族自治县',3,'511100','四川省乐山市马边彝族自治县','511100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('511181','峨眉山市',3,'511100','四川省乐山市峨眉山市','511100'),
	 ('511300','南充市',2,'510000','四川省南充市','511300'),
	 ('511302','顺庆区',3,'511300','四川省南充市顺庆区','511300'),
	 ('511303','高坪区',3,'511300','四川省南充市高坪区','511300'),
	 ('511304','嘉陵区',3,'511300','四川省南充市嘉陵区','511300'),
	 ('511321','南部县',3,'511300','四川省南充市南部县','511300'),
	 ('511322','营山县',3,'511300','四川省南充市营山县','511300'),
	 ('511323','蓬安县',3,'511300','四川省南充市蓬安县','511300'),
	 ('511324','仪陇县',3,'511300','四川省南充市仪陇县','511300'),
	 ('511325','西充县',3,'511300','四川省南充市西充县','511300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('511381','阆中市',3,'511300','四川省南充市阆中市','511300'),
	 ('511400','眉山市',2,'510000','四川省眉山市','511400'),
	 ('511402','东坡区',3,'511400','四川省眉山市东坡区','511400'),
	 ('511403','彭山区',3,'511400','四川省眉山市彭山区','511400'),
	 ('511421','仁寿县',3,'511400','四川省眉山市仁寿县','511400'),
	 ('511423','洪雅县',3,'511400','四川省眉山市洪雅县','511400'),
	 ('511424','丹棱县',3,'511400','四川省眉山市丹棱县','511400'),
	 ('511425','青神县',3,'511400','四川省眉山市青神县','511400'),
	 ('511500','宜宾市',2,'510000','四川省宜宾市','511500'),
	 ('511502','翠屏区',3,'511500','四川省宜宾市翠屏区','511500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('511503','南溪区',3,'511500','四川省宜宾市南溪区','511500'),
	 ('511504','叙州区',3,'511500','四川省宜宾市叙州区','511500'),
	 ('511523','江安县',3,'511500','四川省宜宾市江安县','511500'),
	 ('511524','长宁县',3,'511500','四川省宜宾市长宁县','511500'),
	 ('511525','高县',3,'511500','四川省宜宾市高县','511500'),
	 ('511526','珙县',3,'511500','四川省宜宾市珙县','511500'),
	 ('511527','筠连县',3,'511500','四川省宜宾市筠连县','511500'),
	 ('511528','兴文县',3,'511500','四川省宜宾市兴文县','511500'),
	 ('511529','屏山县',3,'511500','四川省宜宾市屏山县','511500'),
	 ('511600','广安市',2,'510000','四川省广安市','511600');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('511602','广安区',3,'511600','四川省广安市广安区','511600'),
	 ('511603','前锋区',3,'511600','四川省广安市前锋区','511600'),
	 ('511621','岳池县',3,'511600','四川省广安市岳池县','511600'),
	 ('511622','武胜县',3,'511600','四川省广安市武胜县','511600'),
	 ('511623','邻水县',3,'511600','四川省广安市邻水县','511600'),
	 ('511681','华蓥市',3,'511600','四川省广安市华蓥市','511600'),
	 ('511700','达州市',2,'510000','四川省达州市','511700'),
	 ('511702','通川区',3,'511700','四川省达州市通川区','511700'),
	 ('511703','达川区',3,'511700','四川省达州市达川区','511700'),
	 ('511722','宣汉县',3,'511700','四川省达州市宣汉县','511700');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('511723','开江县',3,'511700','四川省达州市开江县','511700'),
	 ('511724','大竹县',3,'511700','四川省达州市大竹县','511700'),
	 ('511725','渠县',3,'511700','四川省达州市渠县','511700'),
	 ('511781','万源市',3,'511700','四川省达州市万源市','511700'),
	 ('511800','雅安市',2,'510000','四川省雅安市','511800'),
	 ('511802','雨城区',3,'511800','四川省雅安市雨城区','511800'),
	 ('511803','名山区',3,'511800','四川省雅安市名山区','511800'),
	 ('511822','荥经县',3,'511800','四川省雅安市荥经县','511800'),
	 ('511823','汉源县',3,'511800','四川省雅安市汉源县','511800'),
	 ('511824','石棉县',3,'511800','四川省雅安市石棉县','511800');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('511825','天全县',3,'511800','四川省雅安市天全县','511800'),
	 ('511826','芦山县',3,'511800','四川省雅安市芦山县','511800'),
	 ('511827','宝兴县',3,'511800','四川省雅安市宝兴县','511800'),
	 ('511900','巴中市',2,'510000','四川省巴中市','511900'),
	 ('511902','巴州区',3,'511900','四川省巴中市巴州区','511900'),
	 ('511903','恩阳区',3,'511900','四川省巴中市恩阳区','511900'),
	 ('511921','通江县',3,'511900','四川省巴中市通江县','511900'),
	 ('511922','南江县',3,'511900','四川省巴中市南江县','511900'),
	 ('511923','平昌县',3,'511900','四川省巴中市平昌县','511900'),
	 ('512000','资阳市',2,'510000','四川省资阳市','512000');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('512002','雁江区',3,'512000','四川省资阳市雁江区','512000'),
	 ('512021','安岳县',3,'512000','四川省资阳市安岳县','512000'),
	 ('512022','乐至县',3,'512000','四川省资阳市乐至县','512000'),
	 ('513200','阿坝藏族羌族自治州',2,'510000','四川省阿坝藏族羌族自治州','513200'),
	 ('513201','马尔康市',3,'513200','四川省阿坝藏族羌族自治州马尔康市','513200'),
	 ('513221','汶川县',3,'513200','四川省阿坝藏族羌族自治州汶川县','513200'),
	 ('513222','理县',3,'513200','四川省阿坝藏族羌族自治州理县','513200'),
	 ('513223','茂县',3,'513200','四川省阿坝藏族羌族自治州茂县','513200'),
	 ('513224','松潘县',3,'513200','四川省阿坝藏族羌族自治州松潘县','513200'),
	 ('513225','九寨沟县',3,'513200','四川省阿坝藏族羌族自治州九寨沟县','513200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('513226','金川县',3,'513200','四川省阿坝藏族羌族自治州金川县','513200'),
	 ('513227','小金县',3,'513200','四川省阿坝藏族羌族自治州小金县','513200'),
	 ('513228','黑水县',3,'513200','四川省阿坝藏族羌族自治州黑水县','513200'),
	 ('513230','壤塘县',3,'513200','四川省阿坝藏族羌族自治州壤塘县','513200'),
	 ('513231','阿坝县',3,'513200','四川省阿坝藏族羌族自治州阿坝县','513200'),
	 ('513232','若尔盖县',3,'513200','四川省阿坝藏族羌族自治州若尔盖县','513200'),
	 ('513233','红原县',3,'513200','四川省阿坝藏族羌族自治州红原县','513200'),
	 ('513300','甘孜藏族自治州',2,'510000','四川省甘孜藏族自治州','513300'),
	 ('513301','康定市',3,'513300','四川省甘孜藏族自治州康定市','513300'),
	 ('513322','泸定县',3,'513300','四川省甘孜藏族自治州泸定县','513300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('513323','丹巴县',3,'513300','四川省甘孜藏族自治州丹巴县','513300'),
	 ('513324','九龙县',3,'513300','四川省甘孜藏族自治州九龙县','513300'),
	 ('513325','雅江县',3,'513300','四川省甘孜藏族自治州雅江县','513300'),
	 ('513326','道孚县',3,'513300','四川省甘孜藏族自治州道孚县','513300'),
	 ('513327','炉霍县',3,'513300','四川省甘孜藏族自治州炉霍县','513300'),
	 ('513328','甘孜县',3,'513300','四川省甘孜藏族自治州甘孜县','513300'),
	 ('513329','新龙县',3,'513300','四川省甘孜藏族自治州新龙县','513300'),
	 ('513330','德格县',3,'513300','四川省甘孜藏族自治州德格县','513300'),
	 ('513331','白玉县',3,'513300','四川省甘孜藏族自治州白玉县','513300'),
	 ('513332','石渠县',3,'513300','四川省甘孜藏族自治州石渠县','513300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('513333','色达县',3,'513300','四川省甘孜藏族自治州色达县','513300'),
	 ('513334','理塘县',3,'513300','四川省甘孜藏族自治州理塘县','513300'),
	 ('513335','巴塘县',3,'513300','四川省甘孜藏族自治州巴塘县','513300'),
	 ('513336','乡城县',3,'513300','四川省甘孜藏族自治州乡城县','513300'),
	 ('513337','稻城县',3,'513300','四川省甘孜藏族自治州稻城县','513300'),
	 ('513338','得荣县',3,'513300','四川省甘孜藏族自治州得荣县','513300'),
	 ('513400','凉山彝族自治州',2,'510000','四川省凉山彝族自治州','513400'),
	 ('513401','西昌市',3,'513400','四川省凉山彝族自治州西昌市','513400'),
	 ('513422','木里藏族自治县',3,'513400','四川省凉山彝族自治州木里藏族自治县','513400'),
	 ('513423','盐源县',3,'513400','四川省凉山彝族自治州盐源县','513400');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('513424','德昌县',3,'513400','四川省凉山彝族自治州德昌县','513400'),
	 ('513425','会理市',3,'513400','四川省凉山彝族自治州会理市','513400'),
	 ('513426','会东县',3,'513400','四川省凉山彝族自治州会东县','513400'),
	 ('513427','宁南县',3,'513400','四川省凉山彝族自治州宁南县','513400'),
	 ('513428','普格县',3,'513400','四川省凉山彝族自治州普格县','513400'),
	 ('513429','布拖县',3,'513400','四川省凉山彝族自治州布拖县','513400'),
	 ('513430','金阳县',3,'513400','四川省凉山彝族自治州金阳县','513400'),
	 ('513431','昭觉县',3,'513400','四川省凉山彝族自治州昭觉县','513400'),
	 ('513432','喜德县',3,'513400','四川省凉山彝族自治州喜德县','513400'),
	 ('513433','冕宁县',3,'513400','四川省凉山彝族自治州冕宁县','513400');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('513434','越西县',3,'513400','四川省凉山彝族自治州越西县','513400'),
	 ('513435','甘洛县',3,'513400','四川省凉山彝族自治州甘洛县','513400'),
	 ('513436','美姑县',3,'513400','四川省凉山彝族自治州美姑县','513400'),
	 ('513437','雷波县',3,'513400','四川省凉山彝族自治州雷波县','513400'),
	 ('520000','贵州省',1,NULL,'贵州省','520000'),
	 ('520100','贵阳市',2,'520000','贵州省贵阳市','520100'),
	 ('520102','南明区',3,'520100','贵州省贵阳市南明区','520100'),
	 ('520103','云岩区',3,'520100','贵州省贵阳市云岩区','520100'),
	 ('520111','花溪区',3,'520100','贵州省贵阳市花溪区','520100'),
	 ('520112','乌当区',3,'520100','贵州省贵阳市乌当区','520100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('520113','白云区',3,'520100','贵州省贵阳市白云区','520100'),
	 ('520115','观山湖区',3,'520100','贵州省贵阳市观山湖区','520100'),
	 ('520121','开阳县',3,'520100','贵州省贵阳市开阳县','520100'),
	 ('520122','息烽县',3,'520100','贵州省贵阳市息烽县','520100'),
	 ('520123','修文县',3,'520100','贵州省贵阳市修文县','520100'),
	 ('520181','清镇市',3,'520100','贵州省贵阳市清镇市','520100'),
	 ('520200','六盘水市',2,'520000','贵州省六盘水市','520200'),
	 ('520201','钟山区',3,'520200','贵州省六盘水市钟山区','520200'),
	 ('520203','六枝特区',3,'520200','贵州省六盘水市六枝特区','520200'),
	 ('520221','水城区',3,'520200','贵州省六盘水市水城区','520200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('520281','盘州市',3,'520200','贵州省六盘水市盘州市','520200'),
	 ('520300','遵义市',2,'520000','贵州省遵义市','520300'),
	 ('520302','红花岗区',3,'520300','贵州省遵义市红花岗区','520300'),
	 ('520303','汇川区',3,'520300','贵州省遵义市汇川区','520300'),
	 ('520304','播州区',3,'520300','贵州省遵义市播州区','520300'),
	 ('520322','桐梓县',3,'520300','贵州省遵义市桐梓县','520300'),
	 ('520323','绥阳县',3,'520300','贵州省遵义市绥阳县','520300'),
	 ('520324','正安县',3,'520300','贵州省遵义市正安县','520300'),
	 ('520325','道真仡佬族苗族自治县',3,'520300','贵州省遵义市道真仡佬族苗族自治县','520300'),
	 ('520326','务川仡佬族苗族自治县',3,'520300','贵州省遵义市务川仡佬族苗族自治县','520300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('520327','凤冈县',3,'520300','贵州省遵义市凤冈县','520300'),
	 ('520328','湄潭县',3,'520300','贵州省遵义市湄潭县','520300'),
	 ('520329','余庆县',3,'520300','贵州省遵义市余庆县','520300'),
	 ('520330','习水县',3,'520300','贵州省遵义市习水县','520300'),
	 ('520381','赤水市',3,'520300','贵州省遵义市赤水市','520300'),
	 ('520382','仁怀市',3,'520300','贵州省遵义市仁怀市','520300'),
	 ('520400','安顺市',2,'520000','贵州省安顺市','520400'),
	 ('520402','西秀区',3,'520400','贵州省安顺市西秀区','520400'),
	 ('520403','平坝区',3,'520400','贵州省安顺市平坝区','520400'),
	 ('520422','普定县',3,'520400','贵州省安顺市普定县','520400');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('520423','镇宁布依族苗族自治县',3,'520400','贵州省安顺市镇宁布依族苗族自治县','520400'),
	 ('520424','关岭布依族苗族自治县',3,'520400','贵州省安顺市关岭布依族苗族自治县','520400'),
	 ('520425','紫云苗族布依族自治县',3,'520400','贵州省安顺市紫云苗族布依族自治县','520400'),
	 ('520500','毕节市',2,'520000','贵州省毕节市','520500'),
	 ('520502','七星关区',3,'520500','贵州省毕节市七星关区','520500'),
	 ('520521','大方县',3,'520500','贵州省毕节市大方县','520500'),
	 ('520522','黔西市',3,'520500','贵州省毕节市黔西市','520500'),
	 ('520523','金沙县',3,'520500','贵州省毕节市金沙县','520500'),
	 ('520524','织金县',3,'520500','贵州省毕节市织金县','520500'),
	 ('520525','纳雍县',3,'520500','贵州省毕节市纳雍县','520500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('520526','威宁彝族回族苗族自治县',3,'520500','贵州省毕节市威宁彝族回族苗族自治县','520500'),
	 ('520527','赫章县',3,'520500','贵州省毕节市赫章县','520500'),
	 ('520600','铜仁市',2,'520000','贵州省铜仁市','520600'),
	 ('520602','碧江区',3,'520600','贵州省铜仁市碧江区','520600'),
	 ('520603','万山区',3,'520600','贵州省铜仁市万山区','520600'),
	 ('520621','江口县',3,'520600','贵州省铜仁市江口县','520600'),
	 ('520622','玉屏侗族自治县',3,'520600','贵州省铜仁市玉屏侗族自治县','520600'),
	 ('520623','石阡县',3,'520600','贵州省铜仁市石阡县','520600'),
	 ('520624','思南县',3,'520600','贵州省铜仁市思南县','520600'),
	 ('520625','印江土家族苗族自治县',3,'520600','贵州省铜仁市印江土家族苗族自治县','520600');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('520626','德江县',3,'520600','贵州省铜仁市德江县','520600'),
	 ('520627','沿河土家族自治县',3,'520600','贵州省铜仁市沿河土家族自治县','520600'),
	 ('520628','松桃苗族自治县',3,'520600','贵州省铜仁市松桃苗族自治县','520600'),
	 ('522300','黔西南布依族苗族自治州',2,'520000','贵州省黔西南布依族苗族自治州','522300'),
	 ('522301','兴义市',3,'522300','贵州省黔西南布依族苗族自治州兴义市','522300'),
	 ('522302','兴仁市',3,'522300','贵州省黔西南布依族苗族自治州兴仁市','522300'),
	 ('522323','普安县',3,'522300','贵州省黔西南布依族苗族自治州普安县','522300'),
	 ('522324','晴隆县',3,'522300','贵州省黔西南布依族苗族自治州晴隆县','522300'),
	 ('522325','贞丰县',3,'522300','贵州省黔西南布依族苗族自治州贞丰县','522300'),
	 ('522326','望谟县',3,'522300','贵州省黔西南布依族苗族自治州望谟县','522300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('522327','册亨县',3,'522300','贵州省黔西南布依族苗族自治州册亨县','522300'),
	 ('522328','安龙县',3,'522300','贵州省黔西南布依族苗族自治州安龙县','522300'),
	 ('522600','黔东南苗族侗族自治州',2,'520000','贵州省黔东南苗族侗族自治州','522600'),
	 ('522601','凯里市',3,'522600','贵州省黔东南苗族侗族自治州凯里市','522600'),
	 ('522622','黄平县',3,'522600','贵州省黔东南苗族侗族自治州黄平县','522600'),
	 ('522623','施秉县',3,'522600','贵州省黔东南苗族侗族自治州施秉县','522600'),
	 ('522624','三穗县',3,'522600','贵州省黔东南苗族侗族自治州三穗县','522600'),
	 ('522625','镇远县',3,'522600','贵州省黔东南苗族侗族自治州镇远县','522600'),
	 ('522626','岑巩县',3,'522600','贵州省黔东南苗族侗族自治州岑巩县','522600'),
	 ('522627','天柱县',3,'522600','贵州省黔东南苗族侗族自治州天柱县','522600');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('522628','锦屏县',3,'522600','贵州省黔东南苗族侗族自治州锦屏县','522600'),
	 ('522629','剑河县',3,'522600','贵州省黔东南苗族侗族自治州剑河县','522600'),
	 ('522630','台江县',3,'522600','贵州省黔东南苗族侗族自治州台江县','522600'),
	 ('522631','黎平县',3,'522600','贵州省黔东南苗族侗族自治州黎平县','522600'),
	 ('522632','榕江县',3,'522600','贵州省黔东南苗族侗族自治州榕江县','522600'),
	 ('522633','从江县',3,'522600','贵州省黔东南苗族侗族自治州从江县','522600'),
	 ('522634','雷山县',3,'522600','贵州省黔东南苗族侗族自治州雷山县','522600'),
	 ('522635','麻江县',3,'522600','贵州省黔东南苗族侗族自治州麻江县','522600'),
	 ('522636','丹寨县',3,'522600','贵州省黔东南苗族侗族自治州丹寨县','522600'),
	 ('522700','黔南布依族苗族自治州',2,'520000','贵州省黔南布依族苗族自治州','522700');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('522701','都匀市',3,'522700','贵州省黔南布依族苗族自治州都匀市','522700'),
	 ('522702','福泉市',3,'522700','贵州省黔南布依族苗族自治州福泉市','522700'),
	 ('522722','荔波县',3,'522700','贵州省黔南布依族苗族自治州荔波县','522700'),
	 ('522723','贵定县',3,'522700','贵州省黔南布依族苗族自治州贵定县','522700'),
	 ('522725','瓮安县',3,'522700','贵州省黔南布依族苗族自治州瓮安县','522700'),
	 ('522726','独山县',3,'522700','贵州省黔南布依族苗族自治州独山县','522700'),
	 ('522727','平塘县',3,'522700','贵州省黔南布依族苗族自治州平塘县','522700'),
	 ('522728','罗甸县',3,'522700','贵州省黔南布依族苗族自治州罗甸县','522700'),
	 ('522729','长顺县',3,'522700','贵州省黔南布依族苗族自治州长顺县','522700'),
	 ('522730','龙里县',3,'522700','贵州省黔南布依族苗族自治州龙里县','522700');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('522731','惠水县',3,'522700','贵州省黔南布依族苗族自治州惠水县','522700'),
	 ('522732','三都水族自治县',3,'522700','贵州省黔南布依族苗族自治州三都水族自治县','522700'),
	 ('530000','云南省',1,NULL,'云南省','530000'),
	 ('530100','昆明市',2,'530000','云南省昆明市','530100'),
	 ('530102','五华区',3,'530100','云南省昆明市五华区','530100'),
	 ('530103','盘龙区',3,'530100','云南省昆明市盘龙区','530100'),
	 ('530111','官渡区',3,'530100','云南省昆明市官渡区','530100'),
	 ('530112','西山区',3,'530100','云南省昆明市西山区','530100'),
	 ('530113','东川区',3,'530100','云南省昆明市东川区','530100'),
	 ('530114','呈贡区',3,'530100','云南省昆明市呈贡区','530100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('530115','晋宁区',3,'530100','云南省昆明市晋宁区','530100'),
	 ('530124','富民县',3,'530100','云南省昆明市富民县','530100'),
	 ('530125','宜良县',3,'530100','云南省昆明市宜良县','530100'),
	 ('530126','石林彝族自治县',3,'530100','云南省昆明市石林彝族自治县','530100'),
	 ('530127','嵩明县',3,'530100','云南省昆明市嵩明县','530100'),
	 ('530128','禄劝彝族苗族自治县',3,'530100','云南省昆明市禄劝彝族苗族自治县','530100'),
	 ('530129','寻甸回族彝族自治县',3,'530100','云南省昆明市寻甸回族彝族自治县','530100'),
	 ('530181','安宁市',3,'530100','云南省昆明市安宁市','530100'),
	 ('530300','曲靖市',2,'530000','云南省曲靖市','530300'),
	 ('530302','麒麟区',3,'530300','云南省曲靖市麒麟区','530300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('530303','沾益区',3,'530300','云南省曲靖市沾益区','530300'),
	 ('530304','马龙区',3,'530300','云南省曲靖市马龙区','530300'),
	 ('530322','陆良县',3,'530300','云南省曲靖市陆良县','530300'),
	 ('530323','师宗县',3,'530300','云南省曲靖市师宗县','530300'),
	 ('530324','罗平县',3,'530300','云南省曲靖市罗平县','530300'),
	 ('530325','富源县',3,'530300','云南省曲靖市富源县','530300'),
	 ('530326','会泽县',3,'530300','云南省曲靖市会泽县','530300'),
	 ('530381','宣威市',3,'530300','云南省曲靖市宣威市','530300'),
	 ('530400','玉溪市',2,'530000','云南省玉溪市','530400'),
	 ('530402','红塔区',3,'530400','云南省玉溪市红塔区','530400');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('530403','江川区',3,'530400','云南省玉溪市江川区','530400'),
	 ('530423','通海县',3,'530400','云南省玉溪市通海县','530400'),
	 ('530424','华宁县',3,'530400','云南省玉溪市华宁县','530400'),
	 ('530425','易门县',3,'530400','云南省玉溪市易门县','530400'),
	 ('530426','峨山彝族自治县',3,'530400','云南省玉溪市峨山彝族自治县','530400'),
	 ('530427','新平彝族傣族自治县',3,'530400','云南省玉溪市新平彝族傣族自治县','530400'),
	 ('530428','元江哈尼族彝族傣族自治县',3,'530400','云南省玉溪市元江哈尼族彝族傣族自治县','530400'),
	 ('530481','澄江市',3,'530400','云南省玉溪市澄江市','530400'),
	 ('530500','保山市',2,'530000','云南省保山市','530500'),
	 ('530502','隆阳区',3,'530500','云南省保山市隆阳区','530500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('530521','施甸县',3,'530500','云南省保山市施甸县','530500'),
	 ('530523','龙陵县',3,'530500','云南省保山市龙陵县','530500'),
	 ('530524','昌宁县',3,'530500','云南省保山市昌宁县','530500'),
	 ('530581','腾冲市',3,'530500','云南省保山市腾冲市','530500'),
	 ('530600','昭通市',2,'530000','云南省昭通市','530600'),
	 ('530602','昭阳区',3,'530600','云南省昭通市昭阳区','530600'),
	 ('530621','鲁甸县',3,'530600','云南省昭通市鲁甸县','530600'),
	 ('530622','巧家县',3,'530600','云南省昭通市巧家县','530600'),
	 ('530623','盐津县',3,'530600','云南省昭通市盐津县','530600'),
	 ('530624','大关县',3,'530600','云南省昭通市大关县','530600');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('530625','永善县',3,'530600','云南省昭通市永善县','530600'),
	 ('530626','绥江县',3,'530600','云南省昭通市绥江县','530600'),
	 ('530627','镇雄县',3,'530600','云南省昭通市镇雄县','530600'),
	 ('530628','彝良县',3,'530600','云南省昭通市彝良县','530600'),
	 ('530629','威信县',3,'530600','云南省昭通市威信县','530600'),
	 ('530681','水富市',3,'530600','云南省昭通市水富市','530600'),
	 ('530700','丽江市',2,'530000','云南省丽江市','530700'),
	 ('530702','古城区',3,'530700','云南省丽江市古城区','530700'),
	 ('530721','玉龙纳西族自治县',3,'530700','云南省丽江市玉龙纳西族自治县','530700'),
	 ('530722','永胜县',3,'530700','云南省丽江市永胜县','530700');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('530723','华坪县',3,'530700','云南省丽江市华坪县','530700'),
	 ('530724','宁蒗彝族自治县',3,'530700','云南省丽江市宁蒗彝族自治县','530700'),
	 ('530800','普洱市',2,'530000','云南省普洱市','530800'),
	 ('530802','思茅区',3,'530800','云南省普洱市思茅区','530800'),
	 ('530821','宁洱哈尼族彝族自治县',3,'530800','云南省普洱市宁洱哈尼族彝族自治县','530800'),
	 ('530822','墨江哈尼族自治县',3,'530800','云南省普洱市墨江哈尼族自治县','530800'),
	 ('530823','景东彝族自治县',3,'530800','云南省普洱市景东彝族自治县','530800'),
	 ('530824','景谷傣族彝族自治县',3,'530800','云南省普洱市景谷傣族彝族自治县','530800'),
	 ('530825','镇沅彝族哈尼族拉祜族自治县',3,'530800','云南省普洱市镇沅彝族哈尼族拉祜族自治县','530800'),
	 ('530826','江城哈尼族彝族自治县',3,'530800','云南省普洱市江城哈尼族彝族自治县','530800');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('530827','孟连傣族拉祜族佤族自治县',3,'530800','云南省普洱市孟连傣族拉祜族佤族自治县','530800'),
	 ('530828','澜沧拉祜族自治县',3,'530800','云南省普洱市澜沧拉祜族自治县','530800'),
	 ('530829','西盟佤族自治县',3,'530800','云南省普洱市西盟佤族自治县','530800'),
	 ('530900','临沧市',2,'530000','云南省临沧市','530900'),
	 ('530902','临翔区',3,'530900','云南省临沧市临翔区','530900'),
	 ('530921','凤庆县',3,'530900','云南省临沧市凤庆县','530900'),
	 ('530922','云县',3,'530900','云南省临沧市云县','530900'),
	 ('530923','永德县',3,'530900','云南省临沧市永德县','530900'),
	 ('530924','镇康县',3,'530900','云南省临沧市镇康县','530900'),
	 ('530925','双江拉祜族佤族布朗族傣族自治县',3,'530900','云南省临沧市双江拉祜族佤族布朗族傣族自治县','530900');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('530926','耿马傣族佤族自治县',3,'530900','云南省临沧市耿马傣族佤族自治县','530900'),
	 ('530927','沧源佤族自治县',3,'530900','云南省临沧市沧源佤族自治县','530900'),
	 ('532300','楚雄彝族自治州',2,'530000','云南省楚雄彝族自治州','532300'),
	 ('532301','楚雄市',3,'532300','云南省楚雄彝族自治州楚雄市','532300'),
	 ('532322','双柏县',3,'532300','云南省楚雄彝族自治州双柏县','532300'),
	 ('532323','牟定县',3,'532300','云南省楚雄彝族自治州牟定县','532300'),
	 ('532324','南华县',3,'532300','云南省楚雄彝族自治州南华县','532300'),
	 ('532325','姚安县',3,'532300','云南省楚雄彝族自治州姚安县','532300'),
	 ('532326','大姚县',3,'532300','云南省楚雄彝族自治州大姚县','532300'),
	 ('532327','永仁县',3,'532300','云南省楚雄彝族自治州永仁县','532300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('532328','元谋县',3,'532300','云南省楚雄彝族自治州元谋县','532300'),
	 ('532329','武定县',3,'532300','云南省楚雄彝族自治州武定县','532300'),
	 ('532331','禄丰市',3,'532300','云南省楚雄彝族自治州禄丰市','532300'),
	 ('532500','红河哈尼族彝族自治州',2,'530000','云南省红河哈尼族彝族自治州','532500'),
	 ('532501','个旧市',3,'532500','云南省红河哈尼族彝族自治州个旧市','532500'),
	 ('532502','开远市',3,'532500','云南省红河哈尼族彝族自治州开远市','532500'),
	 ('532503','蒙自市',3,'532500','云南省红河哈尼族彝族自治州蒙自市','532500'),
	 ('532504','弥勒市',3,'532500','云南省红河哈尼族彝族自治州弥勒市','532500'),
	 ('532523','屏边苗族自治县',3,'532500','云南省红河哈尼族彝族自治州屏边苗族自治县','532500'),
	 ('532524','建水县',3,'532500','云南省红河哈尼族彝族自治州建水县','532500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('532525','石屏县',3,'532500','云南省红河哈尼族彝族自治州石屏县','532500'),
	 ('532527','泸西县',3,'532500','云南省红河哈尼族彝族自治州泸西县','532500'),
	 ('532528','元阳县',3,'532500','云南省红河哈尼族彝族自治州元阳县','532500'),
	 ('532529','红河县',3,'532500','云南省红河哈尼族彝族自治州红河县','532500'),
	 ('532530','金平苗族瑶族傣族自治县',3,'532500','云南省红河哈尼族彝族自治州金平苗族瑶族傣族自治县','532500'),
	 ('532531','绿春县',3,'532500','云南省红河哈尼族彝族自治州绿春县','532500'),
	 ('532532','河口瑶族自治县',3,'532500','云南省红河哈尼族彝族自治州河口瑶族自治县','532500'),
	 ('532600','文山壮族苗族自治州',2,'530000','云南省文山壮族苗族自治州','532600'),
	 ('532601','文山市',3,'532600','云南省文山壮族苗族自治州文山市','532600'),
	 ('532622','砚山县',3,'532600','云南省文山壮族苗族自治州砚山县','532600');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('532623','西畴县',3,'532600','云南省文山壮族苗族自治州西畴县','532600'),
	 ('532624','麻栗坡县',3,'532600','云南省文山壮族苗族自治州麻栗坡县','532600'),
	 ('532625','马关县',3,'532600','云南省文山壮族苗族自治州马关县','532600'),
	 ('532626','丘北县',3,'532600','云南省文山壮族苗族自治州丘北县','532600'),
	 ('532627','广南县',3,'532600','云南省文山壮族苗族自治州广南县','532600'),
	 ('532628','富宁县',3,'532600','云南省文山壮族苗族自治州富宁县','532600'),
	 ('532800','西双版纳傣族自治州',2,'530000','云南省西双版纳傣族自治州','532800'),
	 ('532801','景洪市',3,'532800','云南省西双版纳傣族自治州景洪市','532800'),
	 ('532822','勐海县',3,'532800','云南省西双版纳傣族自治州勐海县','532800'),
	 ('532823','勐腊县',3,'532800','云南省西双版纳傣族自治州勐腊县','532800');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('532900','大理白族自治州',2,'530000','云南省大理白族自治州','532900'),
	 ('532901','大理市',3,'532900','云南省大理白族自治州大理市','532900'),
	 ('532922','漾濞彝族自治县',3,'532900','云南省大理白族自治州漾濞彝族自治县','532900'),
	 ('532923','祥云县',3,'532900','云南省大理白族自治州祥云县','532900'),
	 ('532924','宾川县',3,'532900','云南省大理白族自治州宾川县','532900'),
	 ('532925','弥渡县',3,'532900','云南省大理白族自治州弥渡县','532900'),
	 ('532926','南涧彝族自治县',3,'532900','云南省大理白族自治州南涧彝族自治县','532900'),
	 ('532927','巍山彝族回族自治县',3,'532900','云南省大理白族自治州巍山彝族回族自治县','532900'),
	 ('532928','永平县',3,'532900','云南省大理白族自治州永平县','532900'),
	 ('532929','云龙县',3,'532900','云南省大理白族自治州云龙县','532900');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('532930','洱源县',3,'532900','云南省大理白族自治州洱源县','532900'),
	 ('532931','剑川县',3,'532900','云南省大理白族自治州剑川县','532900'),
	 ('532932','鹤庆县',3,'532900','云南省大理白族自治州鹤庆县','532900'),
	 ('533100','德宏傣族景颇族自治州',2,'530000','云南省德宏傣族景颇族自治州','533100'),
	 ('533102','瑞丽市',3,'533100','云南省德宏傣族景颇族自治州瑞丽市','533100'),
	 ('533103','芒市',3,'533100','云南省德宏傣族景颇族自治州芒市','533100'),
	 ('533122','梁河县',3,'533100','云南省德宏傣族景颇族自治州梁河县','533100'),
	 ('533123','盈江县',3,'533100','云南省德宏傣族景颇族自治州盈江县','533100'),
	 ('533124','陇川县',3,'533100','云南省德宏傣族景颇族自治州陇川县','533100'),
	 ('533300','怒江傈僳族自治州',2,'530000','云南省怒江傈僳族自治州','533300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('533301','泸水市',3,'533300','云南省怒江傈僳族自治州泸水市','533300'),
	 ('533323','福贡县',3,'533300','云南省怒江傈僳族自治州福贡县','533300'),
	 ('533324','贡山独龙族怒族自治县',3,'533300','云南省怒江傈僳族自治州贡山独龙族怒族自治县','533300'),
	 ('533325','兰坪白族普米族自治县',3,'533300','云南省怒江傈僳族自治州兰坪白族普米族自治县','533300'),
	 ('533400','迪庆藏族自治州',2,'530000','云南省迪庆藏族自治州','533400'),
	 ('533401','香格里拉市',3,'533400','云南省迪庆藏族自治州香格里拉市','533400'),
	 ('533422','德钦县',3,'533400','云南省迪庆藏族自治州德钦县','533400'),
	 ('533423','维西傈僳族自治县',3,'533400','云南省迪庆藏族自治州维西傈僳族自治县','533400'),
	 ('540000','西藏自治区',1,NULL,'西藏自治区','540000'),
	 ('540100','拉萨市',2,'540000','西藏自治区拉萨市','540100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('540102','城关区',3,'540100','西藏自治区拉萨市城关区','540100'),
	 ('540103','堆龙德庆区',3,'540100','西藏自治区拉萨市堆龙德庆区','540100'),
	 ('540104','达孜区',3,'540100','西藏自治区拉萨市达孜区','540100'),
	 ('540121','林周县',3,'540100','西藏自治区拉萨市林周县','540100'),
	 ('540122','当雄县',3,'540100','西藏自治区拉萨市当雄县','540100'),
	 ('540123','尼木县',3,'540100','西藏自治区拉萨市尼木县','540100'),
	 ('540124','曲水县',3,'540100','西藏自治区拉萨市曲水县','540100'),
	 ('540127','墨竹工卡县',3,'540100','西藏自治区拉萨市墨竹工卡县','540100'),
	 ('540200','日喀则市',2,'540000','西藏自治区日喀则市','540200'),
	 ('540202','桑珠孜区',3,'540200','西藏自治区日喀则市桑珠孜区','540200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('540221','南木林县',3,'540200','西藏自治区日喀则市南木林县','540200'),
	 ('540222','江孜县',3,'540200','西藏自治区日喀则市江孜县','540200'),
	 ('540223','定日县',3,'540200','西藏自治区日喀则市定日县','540200'),
	 ('540224','萨迦县',3,'540200','西藏自治区日喀则市萨迦县','540200'),
	 ('540225','拉孜县',3,'540200','西藏自治区日喀则市拉孜县','540200'),
	 ('540226','昂仁县',3,'540200','西藏自治区日喀则市昂仁县','540200'),
	 ('540227','谢通门县',3,'540200','西藏自治区日喀则市谢通门县','540200'),
	 ('540228','白朗县',3,'540200','西藏自治区日喀则市白朗县','540200'),
	 ('540229','仁布县',3,'540200','西藏自治区日喀则市仁布县','540200'),
	 ('540230','康马县',3,'540200','西藏自治区日喀则市康马县','540200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('540231','定结县',3,'540200','西藏自治区日喀则市定结县','540200'),
	 ('540232','仲巴县',3,'540200','西藏自治区日喀则市仲巴县','540200'),
	 ('540233','亚东县',3,'540200','西藏自治区日喀则市亚东县','540200'),
	 ('540234','吉隆县',3,'540200','西藏自治区日喀则市吉隆县','540200'),
	 ('540235','聂拉木县',3,'540200','西藏自治区日喀则市聂拉木县','540200'),
	 ('540236','萨嘎县',3,'540200','西藏自治区日喀则市萨嘎县','540200'),
	 ('540237','岗巴县',3,'540200','西藏自治区日喀则市岗巴县','540200'),
	 ('540300','昌都市',2,'540000','西藏自治区昌都市','540300'),
	 ('540302','卡若区',3,'540300','西藏自治区昌都市卡若区','540300'),
	 ('540321','江达县',3,'540300','西藏自治区昌都市江达县','540300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('540322','贡觉县',3,'540300','西藏自治区昌都市贡觉县','540300'),
	 ('540323','类乌齐县',3,'540300','西藏自治区昌都市类乌齐县','540300'),
	 ('540324','丁青县',3,'540300','西藏自治区昌都市丁青县','540300'),
	 ('540325','察雅县',3,'540300','西藏自治区昌都市察雅县','540300'),
	 ('540326','八宿县',3,'540300','西藏自治区昌都市八宿县','540300'),
	 ('540327','左贡县',3,'540300','西藏自治区昌都市左贡县','540300'),
	 ('540328','芒康县',3,'540300','西藏自治区昌都市芒康县','540300'),
	 ('540329','洛隆县',3,'540300','西藏自治区昌都市洛隆县','540300'),
	 ('540330','边坝县',3,'540300','西藏自治区昌都市边坝县','540300'),
	 ('540400','林芝市',2,'540000','西藏自治区林芝市','540400');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('540402','巴宜区',3,'540400','西藏自治区林芝市巴宜区','540400'),
	 ('540421','工布江达县',3,'540400','西藏自治区林芝市工布江达县','540400'),
	 ('540422','米林县',3,'540400','西藏自治区林芝市米林县','540400'),
	 ('540423','墨脱县',3,'540400','西藏自治区林芝市墨脱县','540400'),
	 ('540424','波密县',3,'540400','西藏自治区林芝市波密县','540400'),
	 ('540425','察隅县',3,'540400','西藏自治区林芝市察隅县','540400'),
	 ('540426','朗县',3,'540400','西藏自治区林芝市朗县','540400'),
	 ('540500','山南市',2,'540000','西藏自治区山南市','540500'),
	 ('540502','乃东区',3,'540500','西藏自治区山南市乃东区','540500'),
	 ('540521','扎囊县',3,'540500','西藏自治区山南市扎囊县','540500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('540522','贡嘎县',3,'540500','西藏自治区山南市贡嘎县','540500'),
	 ('540523','桑日县',3,'540500','西藏自治区山南市桑日县','540500'),
	 ('540524','琼结县',3,'540500','西藏自治区山南市琼结县','540500'),
	 ('540525','曲松县',3,'540500','西藏自治区山南市曲松县','540500'),
	 ('540526','措美县',3,'540500','西藏自治区山南市措美县','540500'),
	 ('540527','洛扎县',3,'540500','西藏自治区山南市洛扎县','540500'),
	 ('540528','加查县',3,'540500','西藏自治区山南市加查县','540500'),
	 ('540529','隆子县',3,'540500','西藏自治区山南市隆子县','540500'),
	 ('540530','错那县',3,'540500','西藏自治区山南市错那县','540500'),
	 ('540531','浪卡子县',3,'540500','西藏自治区山南市浪卡子县','540500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('540600','那曲市',2,'540000','西藏自治区那曲市','540600'),
	 ('540602','色尼区',3,'540600','西藏自治区那曲市色尼区','540600'),
	 ('540621','嘉黎县',3,'540600','西藏自治区那曲市嘉黎县','540600'),
	 ('540622','比如县',3,'540600','西藏自治区那曲市比如县','540600'),
	 ('540623','聂荣县',3,'540600','西藏自治区那曲市聂荣县','540600'),
	 ('540624','安多县',3,'540600','西藏自治区那曲市安多县','540600'),
	 ('540625','申扎县',3,'540600','西藏自治区那曲市申扎县','540600'),
	 ('540626','索县',3,'540600','西藏自治区那曲市索县','540600'),
	 ('540627','班戈县',3,'540600','西藏自治区那曲市班戈县','540600'),
	 ('540628','巴青县',3,'540600','西藏自治区那曲市巴青县','540600');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('540629','尼玛县',3,'540600','西藏自治区那曲市尼玛县','540600'),
	 ('540630','双湖县',3,'540600','西藏自治区那曲市双湖县','540600'),
	 ('542500','阿里地区',2,'540000','西藏自治区阿里地区','542500'),
	 ('542521','普兰县',3,'542500','西藏自治区阿里地区普兰县','542500'),
	 ('542522','札达县',3,'542500','西藏自治区阿里地区札达县','542500'),
	 ('542523','噶尔县',3,'542500','西藏自治区阿里地区噶尔县','542500'),
	 ('542524','日土县',3,'542500','西藏自治区阿里地区日土县','542500'),
	 ('542525','革吉县',3,'542500','西藏自治区阿里地区革吉县','542500'),
	 ('542526','改则县',3,'542500','西藏自治区阿里地区改则县','542500'),
	 ('542527','措勤县',3,'542500','西藏自治区阿里地区措勤县','542500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('610000','陕西省',1,NULL,'陕西省','610000'),
	 ('610100','西安市',2,'610000','陕西省西安市','610100'),
	 ('610102','新城区',3,'610100','陕西省西安市新城区','610100'),
	 ('610103','碑林区',3,'610100','陕西省西安市碑林区','610100'),
	 ('610104','莲湖区',3,'610100','陕西省西安市莲湖区','610100'),
	 ('610111','灞桥区',3,'610100','陕西省西安市灞桥区','610100'),
	 ('610112','未央区',3,'610100','陕西省西安市未央区','610100'),
	 ('610113','雁塔区',3,'610100','陕西省西安市雁塔区','610100'),
	 ('610114','阎良区',3,'610100','陕西省西安市阎良区','610100'),
	 ('610115','临潼区',3,'610100','陕西省西安市临潼区','610100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('610116','长安区',3,'610100','陕西省西安市长安区','610100'),
	 ('610117','高陵区',3,'610100','陕西省西安市高陵区','610100'),
	 ('610118','鄠邑区',3,'610100','陕西省西安市鄠邑区','610100'),
	 ('610122','蓝田县',3,'610100','陕西省西安市蓝田县','610100'),
	 ('610124','周至县',3,'610100','陕西省西安市周至县','610100'),
	 ('610200','铜川市',2,'610000','陕西省铜川市','610200'),
	 ('610202','王益区',3,'610200','陕西省铜川市王益区','610200'),
	 ('610203','印台区',3,'610200','陕西省铜川市印台区','610200'),
	 ('610204','耀州区',3,'610200','陕西省铜川市耀州区','610200'),
	 ('610222','宜君县',3,'610200','陕西省铜川市宜君县','610200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('610300','宝鸡市',2,'610000','陕西省宝鸡市','610300'),
	 ('610302','渭滨区',3,'610300','陕西省宝鸡市渭滨区','610300'),
	 ('610303','金台区',3,'610300','陕西省宝鸡市金台区','610300'),
	 ('610304','陈仓区',3,'610300','陕西省宝鸡市陈仓区','610300'),
	 ('610322','凤翔区',3,'610300','陕西省宝鸡市凤翔区','610300'),
	 ('610323','岐山县',3,'610300','陕西省宝鸡市岐山县','610300'),
	 ('610324','扶风县',3,'610300','陕西省宝鸡市扶风县','610300'),
	 ('610326','眉县',3,'610300','陕西省宝鸡市眉县','610300'),
	 ('610327','陇县',3,'610300','陕西省宝鸡市陇县','610300'),
	 ('610328','千阳县',3,'610300','陕西省宝鸡市千阳县','610300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('610329','麟游县',3,'610300','陕西省宝鸡市麟游县','610300'),
	 ('610330','凤县',3,'610300','陕西省宝鸡市凤县','610300'),
	 ('610331','太白县',3,'610300','陕西省宝鸡市太白县','610300'),
	 ('610400','咸阳市',2,'610000','陕西省咸阳市','610400'),
	 ('610402','秦都区',3,'610400','陕西省咸阳市秦都区','610400'),
	 ('610403','杨陵区',3,'610400','陕西省咸阳市杨陵区','610400'),
	 ('610404','渭城区',3,'610400','陕西省咸阳市渭城区','610400'),
	 ('610422','三原县',3,'610400','陕西省咸阳市三原县','610400'),
	 ('610423','泾阳县',3,'610400','陕西省咸阳市泾阳县','610400'),
	 ('610424','乾县',3,'610400','陕西省咸阳市乾县','610400');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('610425','礼泉县',3,'610400','陕西省咸阳市礼泉县','610400'),
	 ('610426','永寿县',3,'610400','陕西省咸阳市永寿县','610400'),
	 ('610428','长武县',3,'610400','陕西省咸阳市长武县','610400'),
	 ('610429','旬邑县',3,'610400','陕西省咸阳市旬邑县','610400'),
	 ('610430','淳化县',3,'610400','陕西省咸阳市淳化县','610400'),
	 ('610431','武功县',3,'610400','陕西省咸阳市武功县','610400'),
	 ('610481','兴平市',3,'610400','陕西省咸阳市兴平市','610400'),
	 ('610482','彬州市',3,'610400','陕西省咸阳市彬州市','610400'),
	 ('610500','渭南市',2,'610000','陕西省渭南市','610500'),
	 ('610502','临渭区',3,'610500','陕西省渭南市临渭区','610500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('610503','华州区',3,'610500','陕西省渭南市华州区','610500'),
	 ('610522','潼关县',3,'610500','陕西省渭南市潼关县','610500'),
	 ('610523','大荔县',3,'610500','陕西省渭南市大荔县','610500'),
	 ('610524','合阳县',3,'610500','陕西省渭南市合阳县','610500'),
	 ('610525','澄城县',3,'610500','陕西省渭南市澄城县','610500'),
	 ('610526','蒲城县',3,'610500','陕西省渭南市蒲城县','610500'),
	 ('610527','白水县',3,'610500','陕西省渭南市白水县','610500'),
	 ('610528','富平县',3,'610500','陕西省渭南市富平县','610500'),
	 ('610581','韩城市',3,'610500','陕西省渭南市韩城市','610500'),
	 ('610582','华阴市',3,'610500','陕西省渭南市华阴市','610500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('610600','延安市',2,'610000','陕西省延安市','610600'),
	 ('610602','宝塔区',3,'610600','陕西省延安市宝塔区','610600'),
	 ('610603','安塞区',3,'610600','陕西省延安市安塞区','610600'),
	 ('610621','延长县',3,'610600','陕西省延安市延长县','610600'),
	 ('610622','延川县',3,'610600','陕西省延安市延川县','610600'),
	 ('610625','志丹县',3,'610600','陕西省延安市志丹县','610600'),
	 ('610626','吴起县',3,'610600','陕西省延安市吴起县','610600'),
	 ('610627','甘泉县',3,'610600','陕西省延安市甘泉县','610600'),
	 ('610628','富县',3,'610600','陕西省延安市富县','610600'),
	 ('610629','洛川县',3,'610600','陕西省延安市洛川县','610600');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('610630','宜川县',3,'610600','陕西省延安市宜川县','610600'),
	 ('610631','黄龙县',3,'610600','陕西省延安市黄龙县','610600'),
	 ('610632','黄陵县',3,'610600','陕西省延安市黄陵县','610600'),
	 ('610681','子长市',3,'610600','陕西省延安市子长市','610600'),
	 ('610700','汉中市',2,'610000','陕西省汉中市','610700'),
	 ('610702','汉台区',3,'610700','陕西省汉中市汉台区','610700'),
	 ('610703','南郑区',3,'610700','陕西省汉中市南郑区','610700'),
	 ('610722','城固县',3,'610700','陕西省汉中市城固县','610700'),
	 ('610723','洋县',3,'610700','陕西省汉中市洋县','610700'),
	 ('610724','西乡县',3,'610700','陕西省汉中市西乡县','610700');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('610725','勉县',3,'610700','陕西省汉中市勉县','610700'),
	 ('610726','宁强县',3,'610700','陕西省汉中市宁强县','610700'),
	 ('610727','略阳县',3,'610700','陕西省汉中市略阳县','610700'),
	 ('610728','镇巴县',3,'610700','陕西省汉中市镇巴县','610700'),
	 ('610729','留坝县',3,'610700','陕西省汉中市留坝县','610700'),
	 ('610730','佛坪县',3,'610700','陕西省汉中市佛坪县','610700'),
	 ('610800','榆林市',2,'610000','陕西省榆林市','610800'),
	 ('610802','榆阳区',3,'610800','陕西省榆林市榆阳区','610800'),
	 ('610803','横山区',3,'610800','陕西省榆林市横山区','610800'),
	 ('610822','府谷县',3,'610800','陕西省榆林市府谷县','610800');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('610824','靖边县',3,'610800','陕西省榆林市靖边县','610800'),
	 ('610825','定边县',3,'610800','陕西省榆林市定边县','610800'),
	 ('610826','绥德县',3,'610800','陕西省榆林市绥德县','610800'),
	 ('610827','米脂县',3,'610800','陕西省榆林市米脂县','610800'),
	 ('610828','佳县',3,'610800','陕西省榆林市佳县','610800'),
	 ('610829','吴堡县',3,'610800','陕西省榆林市吴堡县','610800'),
	 ('610830','清涧县',3,'610800','陕西省榆林市清涧县','610800'),
	 ('610831','子洲县',3,'610800','陕西省榆林市子洲县','610800'),
	 ('610881','神木市',3,'610800','陕西省榆林市神木市','610800'),
	 ('610900','安康市',2,'610000','陕西省安康市','610900');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('610902','汉滨区',3,'610900','陕西省安康市汉滨区','610900'),
	 ('610921','汉阴县',3,'610900','陕西省安康市汉阴县','610900'),
	 ('610922','石泉县',3,'610900','陕西省安康市石泉县','610900'),
	 ('610923','宁陕县',3,'610900','陕西省安康市宁陕县','610900'),
	 ('610924','紫阳县',3,'610900','陕西省安康市紫阳县','610900'),
	 ('610925','岚皋县',3,'610900','陕西省安康市岚皋县','610900'),
	 ('610926','平利县',3,'610900','陕西省安康市平利县','610900'),
	 ('610927','镇坪县',3,'610900','陕西省安康市镇坪县','610900'),
	 ('610928','旬阳市',3,'610900','陕西省安康市旬阳市','610900'),
	 ('610929','白河县',3,'610900','陕西省安康市白河县','610900');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('611000','商洛市',2,'610000','陕西省商洛市','611000'),
	 ('611002','商州区',3,'611000','陕西省商洛市商州区','611000'),
	 ('611021','洛南县',3,'611000','陕西省商洛市洛南县','611000'),
	 ('611022','丹凤县',3,'611000','陕西省商洛市丹凤县','611000'),
	 ('611023','商南县',3,'611000','陕西省商洛市商南县','611000'),
	 ('611024','山阳县',3,'611000','陕西省商洛市山阳县','611000'),
	 ('611025','镇安县',3,'611000','陕西省商洛市镇安县','611000'),
	 ('611026','柞水县',3,'611000','陕西省商洛市柞水县','611000'),
	 ('620000','甘肃省',1,NULL,'甘肃省','620000'),
	 ('620100','兰州市',2,'620000','甘肃省兰州市','620100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('620102','城关区',3,'620100','甘肃省兰州市城关区','620100'),
	 ('620103','七里河区',3,'620100','甘肃省兰州市七里河区','620100'),
	 ('620104','西固区',3,'620100','甘肃省兰州市西固区','620100'),
	 ('620105','安宁区',3,'620100','甘肃省兰州市安宁区','620100'),
	 ('620111','红古区',3,'620100','甘肃省兰州市红古区','620100'),
	 ('620121','永登县',3,'620100','甘肃省兰州市永登县','620100'),
	 ('620122','皋兰县',3,'620100','甘肃省兰州市皋兰县','620100'),
	 ('620123','榆中县',3,'620100','甘肃省兰州市榆中县','620100'),
	 ('620200','嘉峪关市',2,'620000','甘肃省嘉峪关市','620200'),
	 ('620300','金昌市',2,'620000','甘肃省金昌市','620300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('620302','金川区',3,'620300','甘肃省金昌市金川区','620300'),
	 ('620321','永昌县',3,'620300','甘肃省金昌市永昌县','620300'),
	 ('620400','白银市',2,'620000','甘肃省白银市','620400'),
	 ('620402','白银区',3,'620400','甘肃省白银市白银区','620400'),
	 ('620403','平川区',3,'620400','甘肃省白银市平川区','620400'),
	 ('620421','靖远县',3,'620400','甘肃省白银市靖远县','620400'),
	 ('620422','会宁县',3,'620400','甘肃省白银市会宁县','620400'),
	 ('620423','景泰县',3,'620400','甘肃省白银市景泰县','620400'),
	 ('620500','天水市',2,'620000','甘肃省天水市','620500'),
	 ('620502','秦州区',3,'620500','甘肃省天水市秦州区','620500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('620503','麦积区',3,'620500','甘肃省天水市麦积区','620500'),
	 ('620521','清水县',3,'620500','甘肃省天水市清水县','620500'),
	 ('620522','秦安县',3,'620500','甘肃省天水市秦安县','620500'),
	 ('620523','甘谷县',3,'620500','甘肃省天水市甘谷县','620500'),
	 ('620524','武山县',3,'620500','甘肃省天水市武山县','620500'),
	 ('620525','张家川回族自治县',3,'620500','甘肃省天水市张家川回族自治县','620500'),
	 ('620600','武威市',2,'620000','甘肃省武威市','620600'),
	 ('620602','凉州区',3,'620600','甘肃省武威市凉州区','620600'),
	 ('620621','民勤县',3,'620600','甘肃省武威市民勤县','620600'),
	 ('620622','古浪县',3,'620600','甘肃省武威市古浪县','620600');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('620623','天祝藏族自治县',3,'620600','甘肃省武威市天祝藏族自治县','620600'),
	 ('620700','张掖市',2,'620000','甘肃省张掖市','620700'),
	 ('620702','甘州区',3,'620700','甘肃省张掖市甘州区','620700'),
	 ('620721','肃南裕固族自治县',3,'620700','甘肃省张掖市肃南裕固族自治县','620700'),
	 ('620722','民乐县',3,'620700','甘肃省张掖市民乐县','620700'),
	 ('620723','临泽县',3,'620700','甘肃省张掖市临泽县','620700'),
	 ('620724','高台县',3,'620700','甘肃省张掖市高台县','620700'),
	 ('620725','山丹县',3,'620700','甘肃省张掖市山丹县','620700'),
	 ('620800','平凉市',2,'620000','甘肃省平凉市','620800'),
	 ('620802','崆峒区',3,'620800','甘肃省平凉市崆峒区','620800');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('620821','泾川县',3,'620800','甘肃省平凉市泾川县','620800'),
	 ('620822','灵台县',3,'620800','甘肃省平凉市灵台县','620800'),
	 ('620823','崇信县',3,'620800','甘肃省平凉市崇信县','620800'),
	 ('620825','庄浪县',3,'620800','甘肃省平凉市庄浪县','620800'),
	 ('620826','静宁县',3,'620800','甘肃省平凉市静宁县','620800'),
	 ('620881','华亭市',3,'620800','甘肃省平凉市华亭市','620800'),
	 ('620900','酒泉市',2,'620000','甘肃省酒泉市','620900'),
	 ('620902','肃州区',3,'620900','甘肃省酒泉市肃州区','620900'),
	 ('620921','金塔县',3,'620900','甘肃省酒泉市金塔县','620900'),
	 ('620922','瓜州县',3,'620900','甘肃省酒泉市瓜州县','620900');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('620923','肃北蒙古族自治县',3,'620900','甘肃省酒泉市肃北蒙古族自治县','620900'),
	 ('620924','阿克塞哈萨克族自治县',3,'620900','甘肃省酒泉市阿克塞哈萨克族自治县','620900'),
	 ('620981','玉门市',3,'620900','甘肃省酒泉市玉门市','620900'),
	 ('620982','敦煌市',3,'620900','甘肃省酒泉市敦煌市','620900'),
	 ('621000','庆阳市',2,'620000','甘肃省庆阳市','621000'),
	 ('621002','西峰区',3,'621000','甘肃省庆阳市西峰区','621000'),
	 ('621021','庆城县',3,'621000','甘肃省庆阳市庆城县','621000'),
	 ('621022','环县',3,'621000','甘肃省庆阳市环县','621000'),
	 ('621023','华池县',3,'621000','甘肃省庆阳市华池县','621000'),
	 ('621024','合水县',3,'621000','甘肃省庆阳市合水县','621000');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('621025','正宁县',3,'621000','甘肃省庆阳市正宁县','621000'),
	 ('621026','宁县',3,'621000','甘肃省庆阳市宁县','621000'),
	 ('621027','镇原县',3,'621000','甘肃省庆阳市镇原县','621000'),
	 ('621100','定西市',2,'620000','甘肃省定西市','621100'),
	 ('621102','安定区',3,'621100','甘肃省定西市安定区','621100'),
	 ('621121','通渭县',3,'621100','甘肃省定西市通渭县','621100'),
	 ('621122','陇西县',3,'621100','甘肃省定西市陇西县','621100'),
	 ('621123','渭源县',3,'621100','甘肃省定西市渭源县','621100'),
	 ('621124','临洮县',3,'621100','甘肃省定西市临洮县','621100'),
	 ('621125','漳县',3,'621100','甘肃省定西市漳县','621100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('621126','岷县',3,'621100','甘肃省定西市岷县','621100'),
	 ('621200','陇南市',2,'620000','甘肃省陇南市','621200'),
	 ('621202','武都区',3,'621200','甘肃省陇南市武都区','621200'),
	 ('621221','成县',3,'621200','甘肃省陇南市成县','621200'),
	 ('621222','文县',3,'621200','甘肃省陇南市文县','621200'),
	 ('621223','宕昌县',3,'621200','甘肃省陇南市宕昌县','621200'),
	 ('621224','康县',3,'621200','甘肃省陇南市康县','621200'),
	 ('621225','西和县',3,'621200','甘肃省陇南市西和县','621200'),
	 ('621226','礼县',3,'621200','甘肃省陇南市礼县','621200'),
	 ('621227','徽县',3,'621200','甘肃省陇南市徽县','621200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('621228','两当县',3,'621200','甘肃省陇南市两当县','621200'),
	 ('622900','临夏回族自治州',2,'620000','甘肃省临夏回族自治州','622900'),
	 ('622901','临夏市',3,'622900','甘肃省临夏回族自治州临夏市','622900'),
	 ('622921','临夏县',3,'622900','甘肃省临夏回族自治州临夏县','622900'),
	 ('622922','康乐县',3,'622900','甘肃省临夏回族自治州康乐县','622900'),
	 ('622923','永靖县',3,'622900','甘肃省临夏回族自治州永靖县','622900'),
	 ('622924','广河县',3,'622900','甘肃省临夏回族自治州广河县','622900'),
	 ('622925','和政县',3,'622900','甘肃省临夏回族自治州和政县','622900'),
	 ('622926','东乡族自治县',3,'622900','甘肃省临夏回族自治州东乡族自治县','622900'),
	 ('622927','积石山保安族东乡族撒拉族自治县',3,'622900','甘肃省临夏回族自治州积石山保安族东乡族撒拉族自治县','622900');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('623000','甘南藏族自治州',2,'620000','甘肃省甘南藏族自治州','623000'),
	 ('623001','合作市',3,'623000','甘肃省甘南藏族自治州合作市','623000'),
	 ('623021','临潭县',3,'623000','甘肃省甘南藏族自治州临潭县','623000'),
	 ('623022','卓尼县',3,'623000','甘肃省甘南藏族自治州卓尼县','623000'),
	 ('623023','舟曲县',3,'623000','甘肃省甘南藏族自治州舟曲县','623000'),
	 ('623024','迭部县',3,'623000','甘肃省甘南藏族自治州迭部县','623000'),
	 ('623025','玛曲县',3,'623000','甘肃省甘南藏族自治州玛曲县','623000'),
	 ('623026','碌曲县',3,'623000','甘肃省甘南藏族自治州碌曲县','623000'),
	 ('623027','夏河县',3,'623000','甘肃省甘南藏族自治州夏河县','623000'),
	 ('630000','青海省',1,NULL,'青海省','630000');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('630100','西宁市',2,'630000','青海省西宁市','630100'),
	 ('630102','城东区',3,'630100','青海省西宁市城东区','630100'),
	 ('630103','城中区',3,'630100','青海省西宁市城中区','630100'),
	 ('630104','城西区',3,'630100','青海省西宁市城西区','630100'),
	 ('630105','城北区',3,'630100','青海省西宁市城北区','630100'),
	 ('630106','湟中区',3,'630100','青海省西宁市湟中区','630100'),
	 ('630121','大通回族土族自治县',3,'630100','青海省西宁市大通回族土族自治县','630100'),
	 ('630123','湟源县',3,'630100','青海省西宁市湟源县','630100'),
	 ('630200','海东市',2,'630000','青海省海东市','630200'),
	 ('630202','乐都区',3,'630200','青海省海东市乐都区','630200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('630203','平安区',3,'630200','青海省海东市平安区','630200'),
	 ('630222','民和回族土族自治县',3,'630200','青海省海东市民和回族土族自治县','630200'),
	 ('630223','互助土族自治县',3,'630200','青海省海东市互助土族自治县','630200'),
	 ('630224','化隆回族自治县',3,'630200','青海省海东市化隆回族自治县','630200'),
	 ('630225','循化撒拉族自治县',3,'630200','青海省海东市循化撒拉族自治县','630200'),
	 ('632200','海北藏族自治州',2,'630000','青海省海北藏族自治州','632200'),
	 ('632221','门源回族自治县',3,'632200','青海省海北藏族自治州门源回族自治县','632200'),
	 ('632222','祁连县',3,'632200','青海省海北藏族自治州祁连县','632200'),
	 ('632223','海晏县',3,'632200','青海省海北藏族自治州海晏县','632200'),
	 ('632224','刚察县',3,'632200','青海省海北藏族自治州刚察县','632200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('632300','黄南藏族自治州',2,'630000','青海省黄南藏族自治州','632300'),
	 ('632301','同仁市',3,'632300','青海省黄南藏族自治州同仁市','632300'),
	 ('632322','尖扎县',3,'632300','青海省黄南藏族自治州尖扎县','632300'),
	 ('632323','泽库县',3,'632300','青海省黄南藏族自治州泽库县','632300'),
	 ('632324','河南蒙古族自治县',3,'632300','青海省黄南藏族自治州河南蒙古族自治县','632300'),
	 ('632500','海南藏族自治州',2,'630000','青海省海南藏族自治州','632500'),
	 ('632521','共和县',3,'632500','青海省海南藏族自治州共和县','632500'),
	 ('632522','同德县',3,'632500','青海省海南藏族自治州同德县','632500'),
	 ('632523','贵德县',3,'632500','青海省海南藏族自治州贵德县','632500'),
	 ('632524','兴海县',3,'632500','青海省海南藏族自治州兴海县','632500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('632525','贵南县',3,'632500','青海省海南藏族自治州贵南县','632500'),
	 ('632600','果洛藏族自治州',2,'630000','青海省果洛藏族自治州','632600'),
	 ('632621','玛沁县',3,'632600','青海省果洛藏族自治州玛沁县','632600'),
	 ('632622','班玛县',3,'632600','青海省果洛藏族自治州班玛县','632600'),
	 ('632623','甘德县',3,'632600','青海省果洛藏族自治州甘德县','632600'),
	 ('632624','达日县',3,'632600','青海省果洛藏族自治州达日县','632600'),
	 ('632625','久治县',3,'632600','青海省果洛藏族自治州久治县','632600'),
	 ('632626','玛多县',3,'632600','青海省果洛藏族自治州玛多县','632600'),
	 ('632700','玉树藏族自治州',2,'630000','青海省玉树藏族自治州','632700'),
	 ('632701','玉树市',3,'632700','青海省玉树藏族自治州玉树市','632700');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('632722','杂多县',3,'632700','青海省玉树藏族自治州杂多县','632700'),
	 ('632723','称多县',3,'632700','青海省玉树藏族自治州称多县','632700'),
	 ('632724','治多县',3,'632700','青海省玉树藏族自治州治多县','632700'),
	 ('632725','囊谦县',3,'632700','青海省玉树藏族自治州囊谦县','632700'),
	 ('632726','曲麻莱县',3,'632700','青海省玉树藏族自治州曲麻莱县','632700'),
	 ('632800','海西蒙古族藏族自治州',2,'630000','青海省海西蒙古族藏族自治州','632800'),
	 ('632801','格尔木市',3,'632800','青海省海西蒙古族藏族自治州格尔木市','632800'),
	 ('632802','德令哈市',3,'632800','青海省海西蒙古族藏族自治州德令哈市','632800'),
	 ('632803','茫崖市',3,'632800','青海省海西蒙古族藏族自治州茫崖市','632800'),
	 ('632821','乌兰县',3,'632800','青海省海西蒙古族藏族自治州乌兰县','632800');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('632822','都兰县',3,'632800','青海省海西蒙古族藏族自治州都兰县','632800'),
	 ('632823','天峻县',3,'632800','青海省海西蒙古族藏族自治州天峻县','632800'),
	 ('632825','海西蒙古族藏族自治州直辖',3,'632800','青海省海西蒙古族藏族自治州海西蒙古族藏族自治州直辖','632800'),
	 ('640000','宁夏回族自治区',1,NULL,'宁夏回族自治区','640000'),
	 ('640100','银川市',2,'640000','宁夏回族自治区银川市','640100'),
	 ('640104','兴庆区',3,'640100','宁夏回族自治区银川市兴庆区','640100'),
	 ('640105','西夏区',3,'640100','宁夏回族自治区银川市西夏区','640100'),
	 ('640106','金凤区',3,'640100','宁夏回族自治区银川市金凤区','640100'),
	 ('640121','永宁县',3,'640100','宁夏回族自治区银川市永宁县','640100'),
	 ('640122','贺兰县',3,'640100','宁夏回族自治区银川市贺兰县','640100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('640181','灵武市',3,'640100','宁夏回族自治区银川市灵武市','640100'),
	 ('640200','石嘴山市',2,'640000','宁夏回族自治区石嘴山市','640200'),
	 ('640202','大武口区',3,'640200','宁夏回族自治区石嘴山市大武口区','640200'),
	 ('640205','惠农区',3,'640200','宁夏回族自治区石嘴山市惠农区','640200'),
	 ('640221','平罗县',3,'640200','宁夏回族自治区石嘴山市平罗县','640200'),
	 ('640300','吴忠市',2,'640000','宁夏回族自治区吴忠市','640300'),
	 ('640302','利通区',3,'640300','宁夏回族自治区吴忠市利通区','640300'),
	 ('640303','红寺堡区',3,'640300','宁夏回族自治区吴忠市红寺堡区','640300'),
	 ('640323','盐池县',3,'640300','宁夏回族自治区吴忠市盐池县','640300'),
	 ('640324','同心县',3,'640300','宁夏回族自治区吴忠市同心县','640300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('640381','青铜峡市',3,'640300','宁夏回族自治区吴忠市青铜峡市','640300'),
	 ('640400','固原市',2,'640000','宁夏回族自治区固原市','640400'),
	 ('640402','原州区',3,'640400','宁夏回族自治区固原市原州区','640400'),
	 ('640422','西吉县',3,'640400','宁夏回族自治区固原市西吉县','640400'),
	 ('640423','隆德县',3,'640400','宁夏回族自治区固原市隆德县','640400'),
	 ('640424','泾源县',3,'640400','宁夏回族自治区固原市泾源县','640400'),
	 ('640425','彭阳县',3,'640400','宁夏回族自治区固原市彭阳县','640400'),
	 ('640500','中卫市',2,'640000','宁夏回族自治区中卫市','640500'),
	 ('640502','沙坡头区',3,'640500','宁夏回族自治区中卫市沙坡头区','640500'),
	 ('640521','中宁县',3,'640500','宁夏回族自治区中卫市中宁县','640500');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('640522','海原县',3,'640500','宁夏回族自治区中卫市海原县','640500'),
	 ('650000','新疆维吾尔自治区',1,NULL,'新疆维吾尔自治区','650000'),
	 ('650100','乌鲁木齐市',2,'650000','新疆维吾尔自治区乌鲁木齐市','650100'),
	 ('650102','天山区',3,'650100','新疆维吾尔自治区乌鲁木齐市天山区','650100'),
	 ('650103','沙依巴克区',3,'650100','新疆维吾尔自治区乌鲁木齐市沙依巴克区','650100'),
	 ('650104','新市区',3,'650100','新疆维吾尔自治区乌鲁木齐市新市区','650100'),
	 ('650105','水磨沟区',3,'650100','新疆维吾尔自治区乌鲁木齐市水磨沟区','650100'),
	 ('650106','头屯河区',3,'650100','新疆维吾尔自治区乌鲁木齐市头屯河区','650100'),
	 ('650107','达坂城区',3,'650100','新疆维吾尔自治区乌鲁木齐市达坂城区','650100'),
	 ('650109','米东区',3,'650100','新疆维吾尔自治区乌鲁木齐市米东区','650100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('650121','乌鲁木齐县',3,'650100','新疆维吾尔自治区乌鲁木齐市乌鲁木齐县','650100'),
	 ('650200','克拉玛依市',2,'650000','新疆维吾尔自治区克拉玛依市','650200'),
	 ('650202','独山子区',3,'650200','新疆维吾尔自治区克拉玛依市独山子区','650200'),
	 ('650203','克拉玛依区',3,'650200','新疆维吾尔自治区克拉玛依市克拉玛依区','650200'),
	 ('650204','白碱滩区',3,'650200','新疆维吾尔自治区克拉玛依市白碱滩区','650200'),
	 ('650205','乌尔禾区',3,'650200','新疆维吾尔自治区克拉玛依市乌尔禾区','650200'),
	 ('650400','吐鲁番市',2,'650000','新疆维吾尔自治区吐鲁番市','650400'),
	 ('650402','高昌区',3,'650400','新疆维吾尔自治区吐鲁番市高昌区','650400'),
	 ('650421','鄯善县',3,'650400','新疆维吾尔自治区吐鲁番市鄯善县','650400'),
	 ('650422','托克逊县',3,'650400','新疆维吾尔自治区吐鲁番市托克逊县','650400');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('650500','哈密市',2,'650000','新疆维吾尔自治区哈密市','650500'),
	 ('650502','伊州区',3,'650500','新疆维吾尔自治区哈密市伊州区','650500'),
	 ('650521','巴里坤哈萨克自治县',3,'650500','新疆维吾尔自治区哈密市巴里坤哈萨克自治县','650500'),
	 ('650522','伊吾县',3,'650500','新疆维吾尔自治区哈密市伊吾县','650500'),
	 ('652300','昌吉回族自治州',2,'650000','新疆维吾尔自治区昌吉回族自治州','652300'),
	 ('652301','昌吉市',3,'652300','新疆维吾尔自治区昌吉回族自治州昌吉市','652300'),
	 ('652302','阜康市',3,'652300','新疆维吾尔自治区昌吉回族自治州阜康市','652300'),
	 ('652323','呼图壁县',3,'652300','新疆维吾尔自治区昌吉回族自治州呼图壁县','652300'),
	 ('652324','玛纳斯县',3,'652300','新疆维吾尔自治区昌吉回族自治州玛纳斯县','652300'),
	 ('652325','奇台县',3,'652300','新疆维吾尔自治区昌吉回族自治州奇台县','652300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('652327','吉木萨尔县',3,'652300','新疆维吾尔自治区昌吉回族自治州吉木萨尔县','652300'),
	 ('652328','木垒哈萨克自治县',3,'652300','新疆维吾尔自治区昌吉回族自治州木垒哈萨克自治县','652300'),
	 ('652700','博尔塔拉蒙古自治州',2,'650000','新疆维吾尔自治区博尔塔拉蒙古自治州','652700'),
	 ('652701','博乐市',3,'652700','新疆维吾尔自治区博尔塔拉蒙古自治州博乐市','652700'),
	 ('652702','阿拉山口市',3,'652700','新疆维吾尔自治区博尔塔拉蒙古自治州阿拉山口市','652700'),
	 ('652722','精河县',3,'652700','新疆维吾尔自治区博尔塔拉蒙古自治州精河县','652700'),
	 ('652723','温泉县',3,'652700','新疆维吾尔自治区博尔塔拉蒙古自治州温泉县','652700'),
	 ('652800','巴音郭楞蒙古自治州',2,'650000','新疆维吾尔自治区巴音郭楞蒙古自治州','652800'),
	 ('652801','库尔勒市',3,'652800','新疆维吾尔自治区巴音郭楞蒙古自治州库尔勒市','652800'),
	 ('652822','轮台县',3,'652800','新疆维吾尔自治区巴音郭楞蒙古自治州轮台县','652800');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('652823','尉犁县',3,'652800','新疆维吾尔自治区巴音郭楞蒙古自治州尉犁县','652800'),
	 ('652824','若羌县',3,'652800','新疆维吾尔自治区巴音郭楞蒙古自治州若羌县','652800'),
	 ('652825','且末县',3,'652800','新疆维吾尔自治区巴音郭楞蒙古自治州且末县','652800'),
	 ('652826','焉耆回族自治县',3,'652800','新疆维吾尔自治区巴音郭楞蒙古自治州焉耆回族自治县','652800'),
	 ('652827','和静县',3,'652800','新疆维吾尔自治区巴音郭楞蒙古自治州和静县','652800'),
	 ('652828','和硕县',3,'652800','新疆维吾尔自治区巴音郭楞蒙古自治州和硕县','652800'),
	 ('652829','博湖县',3,'652800','新疆维吾尔自治区巴音郭楞蒙古自治州博湖县','652800'),
	 ('652900','阿克苏地区',2,'650000','新疆维吾尔自治区阿克苏地区','652900'),
	 ('652901','阿克苏市',3,'652900','新疆维吾尔自治区阿克苏地区阿克苏市','652900'),
	 ('652902','库车市',3,'652900','新疆维吾尔自治区阿克苏地区库车市','652900');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('652922','温宿县',3,'652900','新疆维吾尔自治区阿克苏地区温宿县','652900'),
	 ('652924','沙雅县',3,'652900','新疆维吾尔自治区阿克苏地区沙雅县','652900'),
	 ('652925','新和县',3,'652900','新疆维吾尔自治区阿克苏地区新和县','652900'),
	 ('652926','拜城县',3,'652900','新疆维吾尔自治区阿克苏地区拜城县','652900'),
	 ('652927','乌什县',3,'652900','新疆维吾尔自治区阿克苏地区乌什县','652900'),
	 ('652928','阿瓦提县',3,'652900','新疆维吾尔自治区阿克苏地区阿瓦提县','652900'),
	 ('652929','柯坪县',3,'652900','新疆维吾尔自治区阿克苏地区柯坪县','652900'),
	 ('653000','克孜勒苏柯尔克孜自治州',2,'650000','新疆维吾尔自治区克孜勒苏柯尔克孜自治州','653000'),
	 ('653001','阿图什市',3,'653000','新疆维吾尔自治区克孜勒苏柯尔克孜自治州阿图什市','653000'),
	 ('653022','阿克陶县',3,'653000','新疆维吾尔自治区克孜勒苏柯尔克孜自治州阿克陶县','653000');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('653023','阿合奇县',3,'653000','新疆维吾尔自治区克孜勒苏柯尔克孜自治州阿合奇县','653000'),
	 ('653024','乌恰县',3,'653000','新疆维吾尔自治区克孜勒苏柯尔克孜自治州乌恰县','653000'),
	 ('653100','喀什地区',2,'650000','新疆维吾尔自治区喀什地区','653100'),
	 ('653101','喀什市',3,'653100','新疆维吾尔自治区喀什地区喀什市','653100'),
	 ('653121','疏附县',3,'653100','新疆维吾尔自治区喀什地区疏附县','653100'),
	 ('653122','疏勒县',3,'653100','新疆维吾尔自治区喀什地区疏勒县','653100'),
	 ('653123','英吉沙县',3,'653100','新疆维吾尔自治区喀什地区英吉沙县','653100'),
	 ('653124','泽普县',3,'653100','新疆维吾尔自治区喀什地区泽普县','653100'),
	 ('653125','莎车县',3,'653100','新疆维吾尔自治区喀什地区莎车县','653100'),
	 ('653126','叶城县',3,'653100','新疆维吾尔自治区喀什地区叶城县','653100');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('653127','麦盖提县',3,'653100','新疆维吾尔自治区喀什地区麦盖提县','653100'),
	 ('653128','岳普湖县',3,'653100','新疆维吾尔自治区喀什地区岳普湖县','653100'),
	 ('653129','伽师县',3,'653100','新疆维吾尔自治区喀什地区伽师县','653100'),
	 ('653130','巴楚县',3,'653100','新疆维吾尔自治区喀什地区巴楚县','653100'),
	 ('653131','塔什库尔干塔吉克自治县',3,'653100','新疆维吾尔自治区喀什地区塔什库尔干塔吉克自治县','653100'),
	 ('653200','和田地区',2,'650000','新疆维吾尔自治区和田地区','653200'),
	 ('653201','和田市',3,'653200','新疆维吾尔自治区和田地区和田市','653200'),
	 ('653221','和田县',3,'653200','新疆维吾尔自治区和田地区和田县','653200'),
	 ('653222','墨玉县',3,'653200','新疆维吾尔自治区和田地区墨玉县','653200'),
	 ('653223','皮山县',3,'653200','新疆维吾尔自治区和田地区皮山县','653200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('653224','洛浦县',3,'653200','新疆维吾尔自治区和田地区洛浦县','653200'),
	 ('653225','策勒县',3,'653200','新疆维吾尔自治区和田地区策勒县','653200'),
	 ('653226','于田县',3,'653200','新疆维吾尔自治区和田地区于田县','653200'),
	 ('653227','民丰县',3,'653200','新疆维吾尔自治区和田地区民丰县','653200'),
	 ('654000','伊犁哈萨克自治州',2,'650000','新疆维吾尔自治区伊犁哈萨克自治州','654000'),
	 ('654002','伊宁市',3,'654000','新疆维吾尔自治区伊犁哈萨克自治州伊宁市','654000'),
	 ('654003','奎屯市',3,'654000','新疆维吾尔自治区伊犁哈萨克自治州奎屯市','654000'),
	 ('654004','霍尔果斯市',3,'654000','新疆维吾尔自治区伊犁哈萨克自治州霍尔果斯市','654000'),
	 ('654021','伊宁县',3,'654000','新疆维吾尔自治区伊犁哈萨克自治州伊宁县','654000'),
	 ('654022','察布查尔锡伯自治县',3,'654000','新疆维吾尔自治区伊犁哈萨克自治州察布查尔锡伯自治县','654000');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('654023','霍城县',3,'654000','新疆维吾尔自治区伊犁哈萨克自治州霍城县','654000'),
	 ('654024','巩留县',3,'654000','新疆维吾尔自治区伊犁哈萨克自治州巩留县','654000'),
	 ('654025','新源县',3,'654000','新疆维吾尔自治区伊犁哈萨克自治州新源县','654000'),
	 ('654026','昭苏县',3,'654000','新疆维吾尔自治区伊犁哈萨克自治州昭苏县','654000'),
	 ('654027','特克斯县',3,'654000','新疆维吾尔自治区伊犁哈萨克自治州特克斯县','654000'),
	 ('654028','尼勒克县',3,'654000','新疆维吾尔自治区伊犁哈萨克自治州尼勒克县','654000'),
	 ('654200','塔城地区',2,'650000','新疆维吾尔自治区塔城地区','654200'),
	 ('654201','塔城市',3,'654200','新疆维吾尔自治区塔城地区塔城市','654200'),
	 ('654202','乌苏市',3,'654200','新疆维吾尔自治区塔城地区乌苏市','654200'),
	 ('654221','额敏县',3,'654200','新疆维吾尔自治区塔城地区额敏县','654200');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('654223','沙湾市',3,'654200','新疆维吾尔自治区塔城地区沙湾市','654200'),
	 ('654224','托里县',3,'654200','新疆维吾尔自治区塔城地区托里县','654200'),
	 ('654225','裕民县',3,'654200','新疆维吾尔自治区塔城地区裕民县','654200'),
	 ('654226','和布克赛尔蒙古自治县',3,'654200','新疆维吾尔自治区塔城地区和布克赛尔蒙古自治县','654200'),
	 ('654300','阿勒泰地区',2,'650000','新疆维吾尔自治区阿勒泰地区','654300'),
	 ('654301','阿勒泰市',3,'654300','新疆维吾尔自治区阿勒泰地区阿勒泰市','654300'),
	 ('654321','布尔津县',3,'654300','新疆维吾尔自治区阿勒泰地区布尔津县','654300'),
	 ('654322','富蕴县',3,'654300','新疆维吾尔自治区阿勒泰地区富蕴县','654300'),
	 ('654323','福海县',3,'654300','新疆维吾尔自治区阿勒泰地区福海县','654300'),
	 ('654324','哈巴河县',3,'654300','新疆维吾尔自治区阿勒泰地区哈巴河县','654300');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('654325','青河县',3,'654300','新疆维吾尔自治区阿勒泰地区青河县','654300'),
	 ('654326','吉木乃县',3,'654300','新疆维吾尔自治区阿勒泰地区吉木乃县','654300'),
	 ('659001','石河子市',2,'650000','新疆维吾尔自治区石河子市','659001'),
	 ('659002','阿拉尔市',2,'650000','新疆维吾尔自治区阿拉尔市','659002'),
	 ('659003','图木舒克市',2,'650000','新疆维吾尔自治区图木舒克市','659003'),
	 ('659004','五家渠市',2,'650000','新疆维吾尔自治区五家渠市','659004'),
	 ('659005','北屯市',2,'650000','新疆维吾尔自治区北屯市','659005'),
	 ('659006','铁门关市',2,'650000','新疆维吾尔自治区铁门关市','659006'),
	 ('659007','双河市',2,'650000','新疆维吾尔自治区双河市','659007'),
	 ('659008','可克达拉市',2,'650000','新疆维吾尔自治区可克达拉市','659008');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('659009','昆玉市',2,'650000','新疆维吾尔自治区昆玉市','659009'),
	 ('659010','胡杨河市',2,'650000','新疆维吾尔自治区胡杨河市','659010'),
	 ('710000','台湾省',1,NULL,'台湾省','710000'),
	 ('810000','香港特别行政区',1,NULL,'香港特别行政区','810000'),
	 ('810001','中西区',2,'810000','香港特别行政区中西区','810001'),
	 ('810002','湾仔区',2,'810000','香港特别行政区湾仔区','810002'),
	 ('810003','东区',2,'810000','香港特别行政区东区','810003'),
	 ('810004','南区',2,'810000','香港特别行政区南区','810004'),
	 ('810005','油尖旺区',2,'810000','香港特别行政区油尖旺区','810005'),
	 ('810006','深水埗区',2,'810000','香港特别行政区深水埗区','810006');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('810007','九龙城区',2,'810000','香港特别行政区九龙城区','810007'),
	 ('810008','黄大仙区',2,'810000','香港特别行政区黄大仙区','810008'),
	 ('810009','观塘区',2,'810000','香港特别行政区观塘区','810009'),
	 ('810010','荃湾区',2,'810000','香港特别行政区荃湾区','810010'),
	 ('810011','屯门区',2,'810000','香港特别行政区屯门区','810011'),
	 ('810012','元朗区',2,'810000','香港特别行政区元朗区','810012'),
	 ('810013','北区',2,'810000','香港特别行政区北区','810013'),
	 ('810014','大埔区',2,'810000','香港特别行政区大埔区','810014'),
	 ('810015','西贡区',2,'810000','香港特别行政区西贡区','810015'),
	 ('810016','沙田区',2,'810000','香港特别行政区沙田区','810016');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('810017','葵青区',2,'810000','香港特别行政区葵青区','810017'),
	 ('810018','离岛区',2,'810000','香港特别行政区离岛区','810018'),
	 ('820000','澳门特别行政区',1,NULL,'澳门特别行政区','820000'),
	 ('820001','花地玛堂区',2,'820000','澳门特别行政区花地玛堂区','820001'),
	 ('820002','花王堂区',2,'820000','澳门特别行政区花王堂区','820002'),
	 ('820003','望德堂区',2,'820000','澳门特别行政区望德堂区','820003'),
	 ('820004','大堂区',2,'820000','澳门特别行政区大堂区','820004'),
	 ('820005','风顺堂区',2,'820000','澳门特别行政区风顺堂区','820005'),
	 ('820006','嘉模堂区',2,'820000','澳门特别行政区嘉模堂区','820006'),
	 ('820007','路凼填海区',2,'820000','澳门特别行政区路凼填海区','820007');
INSERT INTO sys_region_code (code,name,`level`,parent_code,full_name,city_code) VALUES
	 ('820008','圣方济各堂区',2,'820000','澳门特别行政区圣方济各堂区','820008');
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (1,1,'平台管理',0,1,6,'/platform','',0,0,'','_self',0,1,1,NULL,'local-image:icon01.png',NULL,NULL,0,1,NULL,'系统管理目录',NULL,'2025-11-12 17:41:18',1,'2026-06-07 09:36:04',NULL,'pc',2,0),
	 (2,1,'用户管理',1,2,1,'/system/user','system/user',0,0,'','_self',0,1,1,'system:user:list','ionicons5:AccessibilityOutline',NULL,NULL,1,0,NULL,'用户管理菜单',NULL,'2025-11-12 17:41:18',1,'2026-06-10 15:40:05',NULL,'pc',2,0),
	 (3,1,'用户查询',2,3,1,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:user:query','ionicons5:AddCircleSharp',NULL,NULL,0,0,NULL,'用户查询按钮',NULL,'2025-11-12 17:41:18',1,'2026-07-15 15:31:19',NULL,'pc',2,0),
	 (4,1,'用户新增',2,3,2,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:user:add',NULL,NULL,NULL,0,0,NULL,'用户新增按钮',NULL,'2025-11-12 17:41:18',NULL,'2026-06-10 15:40:05',NULL,'pc',2,0),
	 (5,1,'用户修改',2,3,3,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:user:edit',NULL,NULL,NULL,0,0,NULL,'用户修改按钮',NULL,'2025-11-12 17:41:18',NULL,'2026-06-10 15:40:05',NULL,'pc',2,0),
	 (6,1,'用户删除',2,3,4,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:user:remove',NULL,NULL,NULL,0,0,NULL,'用户删除按钮',NULL,'2025-11-12 17:41:18',NULL,'2026-06-10 15:40:05',NULL,'pc',2,0),
	 (7,1,'用户分页查询API',2,4,1,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:user:page',NULL,'GET','/system/user/page',0,0,NULL,'用户分页查询接口',NULL,'2025-11-12 17:41:18',NULL,'2026-06-10 15:40:05',NULL,'pc',2,0),
	 (8,1,'用户新增API',2,4,2,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:user:add',NULL,'POST','/system/user/add',0,0,NULL,'用户新增接口',NULL,'2025-11-12 17:41:18',1,'2026-06-26 17:19:30',NULL,'pc',2,0),
	 (9,1,'角色管理',1,2,2,'/system/role','system/role',0,0,NULL,'_self',0,1,1,'system:role:list','local:ai-icon:layers',NULL,NULL,1,0,NULL,'角色管理菜单',NULL,'2025-11-12 17:41:18',NULL,'2026-06-10 15:40:05',NULL,'pc',2,0),
	 (10,1,'角色查询',9,3,1,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:role:query',NULL,NULL,NULL,0,0,NULL,'角色查询按钮',NULL,'2025-11-12 17:41:18',NULL,'2026-06-10 15:40:05',NULL,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (11,1,'角色新增',9,3,2,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:role:add',NULL,NULL,NULL,0,0,NULL,'角色新增按钮',NULL,'2025-11-12 17:41:18',NULL,'2026-06-10 15:40:05',NULL,'pc',2,0),
	 (12,1,'角色修改',9,3,3,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:role:edit',NULL,NULL,NULL,0,0,NULL,'角色修改按钮',NULL,'2025-11-12 17:41:18',NULL,'2026-06-10 15:40:05',NULL,'pc',2,0),
	 (13,1,'角色删除',9,3,4,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:role:remove',NULL,NULL,NULL,0,0,NULL,'角色删除按钮',NULL,'2025-11-12 17:41:18',NULL,'2026-06-10 15:40:05',NULL,'pc',2,0),
	 (14,1,'组织管理',1,2,3,'/system/org','system/org',0,0,NULL,'_self',0,1,1,'system:org:list','local:ai-icon:briefcase',NULL,NULL,1,0,NULL,'组织管理菜单',NULL,'2025-11-12 17:41:18',NULL,'2026-06-10 15:40:05',NULL,'pc',2,0),
	 (15,1,'组织查询',14,3,1,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:org:query',NULL,NULL,NULL,0,0,NULL,'组织查询按钮',NULL,'2025-11-12 17:41:18',1,'2026-06-10 15:40:05',NULL,'pc',2,0),
	 (16,1,'组织新增',14,3,2,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:org:add',NULL,NULL,NULL,0,0,NULL,'组织新增按钮',NULL,'2025-11-12 17:41:18',NULL,'2026-06-10 15:40:05',NULL,'pc',2,0),
	 (17,1,'岗位管理',1,2,4,'/system/post','system/post',0,0,NULL,'_self',0,1,1,'system:post:list','ionicons5:Albums',NULL,NULL,1,0,NULL,'岗位管理菜单',NULL,'2025-11-12 17:41:18',NULL,'2026-06-10 15:22:06',NULL,'pc',1,0),
	 (18,1,'岗位查询',17,3,1,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:post:query',NULL,NULL,NULL,0,0,NULL,'岗位查询按钮',NULL,'2025-11-12 17:41:18',NULL,'2026-06-10 15:22:06',NULL,'pc',1,0),
	 (20,1,'查询案件信息',19,3,1,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:legalCase:query',NULL,NULL,NULL,0,0,NULL,'查询案件信息按钮',NULL,'2025-12-05 14:37:17',NULL,'2025-12-09 10:28:31',NULL,'pc',2,0),
	 (21,1,'新增案件信息',19,3,2,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:legalCase:add',NULL,NULL,NULL,0,0,NULL,'新增案件信息按钮',NULL,'2025-12-05 14:37:17',NULL,'2025-12-08 16:04:23',NULL,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (22,1,'修改案件信息',19,3,3,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:legalCase:edit',NULL,NULL,NULL,0,0,NULL,'修改案件信息按钮',NULL,'2025-12-05 14:37:17',NULL,'2025-12-08 16:04:24',NULL,'pc',2,0),
	 (23,1,'删除案件信息',19,3,4,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:legalCase:remove',NULL,NULL,NULL,0,0,NULL,'删除案件信息按钮',NULL,'2025-12-05 14:37:17',NULL,'2025-12-08 16:04:24',NULL,'pc',2,0),
	 (24,1,'菜单管理',1,2,5,'/system/menu','system/menu',0,0,NULL,'_self',0,1,1,'system:menu:list','ionicons5:Menu',NULL,NULL,1,0,NULL,'菜单管理',NULL,'2025-11-12 17:41:18',NULL,'2026-06-10 15:22:06',NULL,'pc',0,0),
	 (26,1,'租户管理',1,2,6,'/system/tenant','system/tenant',0,0,NULL,'_self',0,1,1,NULL,'ionicons5:MapOutline','GET',NULL,0,0,NULL,NULL,NULL,'2025-12-09 17:32:06',NULL,'2026-06-10 15:22:06',NULL,'pc',0,0),
	 (27,1,'字典管理',44,2,1,'/system/dictType','system/dictType',0,0,NULL,'_self',0,1,1,NULL,'ionicons5:MedicalOutline','GET',NULL,0,0,NULL,NULL,NULL,'2025-12-11 16:01:06',1,'2026-06-10 15:22:06',NULL,'pc',0,0),
	 (28,1,'字典数据',44,2,2,'/system/dictData','system/dictData',0,0,NULL,'_self',0,0,0,NULL,'ionicons5:Dice','GET',NULL,0,0,NULL,NULL,NULL,'2025-12-11 16:06:09',1,'2026-06-10 15:22:06',NULL,'pc',0,0),
	 (29,1,'参数设置',44,2,3,'/system/config','system/config',0,0,NULL,'_self',0,1,1,NULL,'ionicons5:Apps','GET',NULL,0,0,NULL,NULL,NULL,'2025-12-11 17:21:09',1,'2026-06-10 15:22:06',NULL,'pc',0,0),
	 (30,1,'通知公告',43,2,10,'/system/notice','system/notice',0,0,NULL,'_self',0,1,1,NULL,'ionicons5:NotificationsOutline','GET',NULL,0,0,NULL,NULL,NULL,'2025-12-12 09:34:50',1,'2026-06-07 09:27:16',NULL,'pc',2,0),
	 (31,1,'数据权限配置',1,2,8,'/system/dataScopeConfig','system/dataScopeConfig',0,0,NULL,'_self',0,1,1,'system:dataScope:list','ionicons5:HandRight',NULL,NULL,0,0,NULL,'数据权限配置管理',1,'2026-01-20 15:06:29',1,'2026-06-10 15:22:06',NULL,'pc',0,0),
	 (39,1,'旧低代码配置',0,1,900,'/generator','',0,0,'','_self',0,0,0,NULL,'local-image:icon02.png','GET',NULL,0,1,NULL,'AI低代码目录',1,'2026-01-20 16:50:19',1,'2026-06-14 21:38:25',1,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (40,1,'应用数据源',9194,2,2,'/generator/datasource','generator/datasource',0,0,NULL,'_self',0,1,1,NULL,'ionicons5:GridSharp','GET',NULL,0,0,NULL,'代码生成数据源管理',1,'2026-01-20 16:51:58',1,'2026-06-07 20:39:16',1,'pc',2,0),
	 (41,1,'轻表设计',9194,2,2,'/generator/table','generator/table',0,0,'','_self',0,1,1,NULL,'ionicons5:TabletLandscape','GET',NULL,0,0,NULL,'；统一低代码代码生成后隐藏旧入口；已收敛到开发者工具或隐藏普通菜单入口；已收敛到开发者工具或隐藏普通菜单入口',1,'2026-01-20 16:53:04',1,'2026-06-14 21:45:49',1,'pc',2,0),
	 (43,1,'工作台',0,1,0,'/workbench','',0,0,'','_self',0,1,1,NULL,'local-image:icon04.png','GET',NULL,0,0,NULL,NULL,1,'2026-01-21 15:55:31',1,'2026-06-13 08:48:50',1,'pc',2,0),
	 (44,1,'基础配置',1,1,50,'/platform/config',NULL,0,0,NULL,'_self',0,1,1,NULL,'ionicons5:OptionsOutline','GET',NULL,0,0,NULL,NULL,1,'2026-01-21 16:24:25',1,'2026-06-07 09:27:17',1,'pc',2,0),
	 (45,1,'存储配置',44,2,20,'/system/storage-config','system/storage-config',0,0,NULL,'_self',0,1,1,NULL,'ionicons5:FileTray','GET',NULL,0,0,NULL,NULL,1,'2026-01-21 16:24:55',1,'2026-06-07 09:27:17',1,'pc',2,0),
	 (46,1,'文件管理',44,2,21,'/system/file-list','system/file-list',0,0,NULL,'_self',0,1,1,NULL,'ionicons5:FileTrayStackedSharp','GET',NULL,0,0,NULL,NULL,1,'2026-01-21 16:25:41',1,'2026-06-07 09:27:17',1,'pc',2,0),
	 (47,1,'Excel导入导出配置',44,2,23,'/system/excel-export-config','system/excel-export-config',0,0,NULL,'_self',0,1,1,NULL,'ionicons5:ExitSharp','GET',NULL,0,0,NULL,NULL,1,'2026-01-21 22:34:08',1,'2026-06-07 09:27:17',2,'pc',2,0),
	 (48,1,'消息中心',1,1,60,'/platform/message',NULL,0,0,NULL,'_self',0,1,1,NULL,'ionicons5:ChatboxEllipsesOutline','GET',NULL,0,0,NULL,NULL,1,'2026-01-31 20:19:24',1,'2026-06-07 09:27:17',2,'pc',2,0),
	 (49,1,'模板配置',48,2,1,'/message/template-list','message/template-list',0,0,NULL,'_self',0,1,1,NULL,'ionicons5:AddCircleSharp','GET',NULL,0,0,NULL,NULL,1,'2026-01-31 20:19:59',1,'2026-06-07 09:27:17',2,'pc',2,0),
	 (50,1,'消息列表',48,2,2,'/message/message-list','message/message-list',0,0,NULL,'_self',0,1,1,NULL,'ionicons5:TabletLandscape','GET',NULL,0,0,NULL,NULL,1,'2026-01-31 20:20:39',1,'2026-06-07 09:27:17',2,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (53,1,'测试222',52,2,0,'/test/template','test/template',0,0,NULL,'_self',0,0,0,NULL,NULL,'GET',NULL,0,0,NULL,NULL,1,'2026-02-03 17:25:57',1,'2026-06-07 09:27:17',2,'pc',2,0),
	 (54,1,'运维监控',1,1,70,'/platform/monitor',NULL,0,0,NULL,'_self',0,1,1,NULL,'ionicons5:PulseOutline','GET',NULL,0,0,NULL,NULL,1,'2026-02-04 11:52:09',1,'2026-06-07 09:27:17',2,'pc',2,0),
	 (55,1,'定时任务配置',54,2,6,'/system/job-config','system/job-config',0,0,NULL,'_self',0,1,1,NULL,'ionicons5:TimeSharp','GET',NULL,0,0,NULL,NULL,1,'2026-02-04 11:52:50',1,'2026-06-07 09:27:17',2,'pc',2,0),
	 (56,1,'登录日志',54,2,4,'/system/login-log','system/login-log',0,0,NULL,'_self',0,1,1,NULL,'ionicons5:LogoFirebase','GET',NULL,0,0,NULL,NULL,1,'2026-02-04 14:40:48',1,'2026-06-10 15:22:06',2,'pc',0,0),
	 (57,1,'页面操作审计',54,2,5,'/system/operation-log','system/operation-log',0,0,NULL,'_self',0,1,1,'system:operationLog:list','ionicons5:LogoInstagram','GET',NULL,0,0,NULL,'页面操作审计日志',1,'2026-02-04 14:41:26',1,'2026-07-08 08:56:39',2,'pc',0,0),
	 (58,1,'缓存监控',54,2,2,'/system/cache','system/cache',0,0,NULL,'_self',0,1,1,'system:cache:list','ionicons5:LogoGooglePlaystore',NULL,NULL,1,0,NULL,'缓存管理菜单',1,'2026-02-04 15:25:05',1,'2026-06-10 15:22:06',NULL,'pc',0,0),
	 (59,1,'缓存查询',58,3,1,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:cache:query',NULL,NULL,NULL,0,0,NULL,'查询缓存按钮',1,'2026-02-04 15:25:05',1,'2026-06-10 15:22:06',NULL,'pc',0,0),
	 (60,1,'缓存删除',58,3,2,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:cache:remove',NULL,NULL,NULL,0,0,NULL,'删除缓存按钮',1,'2026-02-04 15:25:06',1,'2026-06-10 15:22:06',NULL,'pc',0,0),
	 (61,1,'清空缓存',58,3,3,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:cache:clear',NULL,NULL,NULL,0,0,NULL,'清空缓存按钮',1,'2026-02-04 15:25:06',1,'2026-06-10 15:22:06',NULL,'pc',0,0),
	 (62,1,'缓存-分页查询',58,4,1,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:cache:page',NULL,'GET','/system/cache/page',0,0,NULL,'分页查询缓存列表',1,'2026-02-04 15:25:06',1,'2026-06-10 15:22:06',NULL,'pc',0,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (63,1,'缓存-详情查询',58,4,2,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:cache:getInfo',NULL,'POST','/system/cache/getInfo',0,0,NULL,'查询缓存详细信息',1,'2026-02-04 15:25:06',1,'2026-06-10 15:22:06',NULL,'pc',0,0),
	 (64,1,'缓存-删除',58,4,3,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:cache:remove',NULL,'POST','/system/cache/remove',0,0,NULL,'删除单个缓存',1,'2026-02-04 15:25:06',1,'2026-06-10 15:22:06',NULL,'pc',0,0),
	 (65,1,'缓存-批量删除',58,4,4,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:cache:removeBatch',NULL,'POST','/system/cache/removeBatch',0,0,NULL,'批量删除缓存',1,'2026-02-04 15:25:06',1,'2026-06-10 15:22:06',NULL,'pc',0,0),
	 (66,1,'缓存-清空',58,4,5,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:cache:clear',NULL,'POST','/system/cache/clear',0,0,NULL,'清空所有缓存',1,'2026-02-04 15:25:06',1,'2026-06-10 15:22:06',NULL,'pc',0,0),
	 (67,1,'在线用户',54,2,3,'/system/online','system/online',0,0,NULL,'_self',0,1,1,NULL,'ionicons5:ApertureOutline','GET',NULL,0,0,NULL,NULL,1,'2026-02-05 12:02:09',1,'2026-06-10 15:22:06',2,'pc',0,0),
	 (68,1,'API配置管理',44,2,24,'/system/apiConfig','system/apiConfig',0,0,NULL,'_self',0,1,1,'system:apiConfig:list','ionicons5:BuildSharp',NULL,NULL,0,0,NULL,'API接口配置管理',1,'2026-02-09 16:29:33',1,'2026-06-07 09:27:17',NULL,'pc',2,0),
	 (69,1,'API配置管理-分页查询',68,4,1,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:apiConfig:page',NULL,'GET','/system/apiConfig/page',0,0,NULL,'分页查询API配置列表',1,'2026-02-09 16:29:33',1,'2026-02-09 16:31:21',NULL,'pc',2,0),
	 (70,1,'API配置管理-列表查询',68,4,2,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:apiConfig:list',NULL,'GET','/system/apiConfig/list',0,0,NULL,'查询API配置列表',1,'2026-02-09 16:29:33',1,'2026-02-09 16:31:21',NULL,'pc',2,0),
	 (71,1,'API配置管理-查询详情',68,4,3,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:apiConfig:query',NULL,'GET','/system/apiConfig/getById',0,0,NULL,'根据ID查询API配置详情',1,'2026-02-09 16:29:33',1,'2026-02-09 16:31:21',NULL,'pc',2,0),
	 (72,1,'API配置管理-新增',68,4,4,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:apiConfig:add',NULL,'POST','/system/apiConfig/add',0,0,NULL,'新增API配置',1,'2026-02-09 16:29:33',1,'2026-02-09 16:31:21',NULL,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (73,1,'API配置管理-修改',68,4,5,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:apiConfig:edit',NULL,'POST','/system/apiConfig/edit',0,0,NULL,'修改API配置',1,'2026-02-09 16:29:33',1,'2026-02-09 16:31:21',NULL,'pc',2,0),
	 (74,1,'API配置管理-删除',68,4,6,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:apiConfig:remove',NULL,'POST','/system/apiConfig/remove',0,0,NULL,'删除API配置',1,'2026-02-09 16:29:33',1,'2026-02-09 16:31:21',NULL,'pc',2,0),
	 (76,1,'API配置管理-刷新缓存',68,4,8,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:apiConfig:refresh',NULL,'POST','/apiConfig/refresh',0,0,NULL,'刷新API配置缓存',1,'2026-02-09 16:29:57',1,'2026-02-09 16:31:21',NULL,'pc',2,0),
	 (77,1,'系统配置',44,2,4,'/system/config-center','system/config-center',0,0,NULL,'_self',0,1,1,NULL,'ionicons5:ConstructOutline','GET',NULL,0,0,NULL,NULL,1,'2026-02-26 09:49:55',1,'2026-06-10 15:22:06',2,'pc',0,0),
	 (78,1,'服务监控',54,2,1,'/system/monitor','system/monitor',0,0,NULL,'_self',0,1,1,NULL,'ionicons5:Earth','GET',NULL,0,0,NULL,NULL,1,'2026-03-02 14:28:26',1,'2026-06-10 15:22:06',2,'pc',0,0),
	 (79,1,'流程中心',0,1,3,'/flow','',0,0,'','_self',0,1,1,NULL,'local-image:icon08.png',NULL,NULL,0,0,NULL,'流程管理目录',NULL,'2026-03-15 18:32:12',1,'2026-06-07 09:35:01',NULL,'pc',2,0),
	 (80,1,'我的待办',43,2,1,'/flow/todo','/flow/todo',0,0,NULL,'_self',0,1,1,NULL,'ionicons5:LogoMastodon',NULL,NULL,0,0,NULL,'我的待办任务',NULL,'2026-03-15 18:32:12',1,'2026-06-07 09:27:16',NULL,'pc',2,0),
	 (81,1,'我的已办',43,2,2,'/flow/done','/flow/done',0,0,NULL,'_self',0,1,1,NULL,'ionicons5:CheckmarkDoneCircle',NULL,NULL,0,0,NULL,'我的已办任务',NULL,'2026-03-15 18:32:12',1,'2026-06-25 17:27:12',NULL,'pc',2,0),
	 (82,1,'我发起的',43,2,3,'/flow/started','/flow/started',0,0,NULL,'_self',0,1,1,NULL,'ionicons5:Send',NULL,NULL,0,0,NULL,'我发起的流程',NULL,'2026-03-15 18:32:12',1,'2026-06-07 09:27:16',NULL,'pc',2,0),
	 (83,1,'我的抄送',43,2,4,'/flow/cc','/flow/cc',0,0,NULL,'_self',0,1,1,NULL,'ionicons5:SendOutline',NULL,NULL,0,0,NULL,'我的抄送',NULL,'2026-03-15 18:32:13',1,'2026-06-21 16:33:14',NULL,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (85,1,'流程模型',79,2,1,'/flow/model','flow/model',0,0,NULL,'_self',0,1,1,NULL,'ionicons5:Create',NULL,NULL,0,0,NULL,'流程模型管理',NULL,'2026-03-15 18:32:13',1,'2026-06-07 09:27:16',NULL,'pc',2,0),
	 (86,1,'流程分类',79,2,2,'/flow/category','/flow/category',0,0,NULL,'_self',0,1,0,NULL,'ionicons5:DuplicateOutline',NULL,NULL,0,0,NULL,'流程分类管理',NULL,'2026-03-15 18:32:13',1,'2026-07-08 18:13:19',NULL,'pc',2,0),
	 (87,1,'创建模型',85,3,1,NULL,NULL,0,0,NULL,'_self',0,1,1,'flow:model:create',NULL,NULL,NULL,0,0,NULL,'创建流程模型',NULL,'2026-03-15 18:32:13',NULL,'2026-03-15 18:32:39',NULL,'pc',2,0),
	 (88,1,'编辑模型',85,3,2,NULL,NULL,0,0,NULL,'_self',0,1,1,'flow:model:update',NULL,NULL,NULL,0,0,NULL,'编辑流程模型',NULL,'2026-03-15 18:32:13',NULL,'2026-03-15 18:32:39',NULL,'pc',2,0),
	 (89,1,'删除模型',85,3,3,NULL,NULL,0,0,NULL,'_self',0,1,1,'flow:model:delete',NULL,NULL,NULL,0,0,NULL,'删除流程模型',NULL,'2026-03-15 18:32:13',NULL,'2026-03-15 18:32:39',NULL,'pc',2,0),
	 (90,1,'部署模型',85,3,4,NULL,NULL,0,0,NULL,'_self',0,1,1,'flow:model:deploy',NULL,NULL,NULL,0,0,NULL,'部署流程模型',NULL,'2026-03-15 18:32:13',NULL,'2026-03-15 18:32:39',NULL,'pc',2,0),
	 (91,1,'创建分类',86,3,1,NULL,NULL,0,0,NULL,'_self',0,1,1,'flow:category:create',NULL,NULL,NULL,0,0,NULL,'创建流程分类',NULL,'2026-03-15 18:32:13',NULL,'2026-03-15 18:32:39',NULL,'pc',2,0),
	 (92,1,'编辑分类',86,3,2,NULL,NULL,0,0,NULL,'_self',0,1,1,'flow:category:update',NULL,NULL,NULL,0,0,NULL,'编辑流程分类',NULL,'2026-03-15 18:32:13',NULL,'2026-03-15 18:32:39',NULL,'pc',2,0),
	 (93,1,'删除分类',86,3,3,NULL,NULL,0,0,NULL,'_self',0,1,1,'flow:category:delete',NULL,NULL,NULL,0,0,NULL,'删除流程分类',NULL,'2026-03-15 18:32:13',NULL,'2026-03-15 18:32:39',NULL,'pc',2,0),
	 (94,1,'待办任务查询',80,4,10,NULL,NULL,0,0,NULL,'_self',0,1,1,'flow:task:todo',NULL,'GET','/api/flow/task/todo',0,0,NULL,'查询待办任务',NULL,'2026-03-15 18:32:13',NULL,'2026-07-12 11:12:59',NULL,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (95,1,'已办任务查询',81,4,11,NULL,NULL,0,0,NULL,'_self',0,1,1,'flow:task:done',NULL,'GET','/api/flow/task/done',0,0,NULL,'查询已办任务',NULL,'2026-03-15 18:32:13',NULL,'2026-07-12 11:12:59',NULL,'pc',2,0),
	 (96,1,'发起的流程查询',82,4,12,NULL,NULL,0,0,NULL,'_self',0,1,1,'flow:task:started',NULL,'GET','/api/flow/task/started',0,0,NULL,'查询发起的流程',NULL,'2026-03-15 18:32:13',NULL,'2026-07-12 11:12:59',NULL,'pc',2,0),
	 (97,1,'任务签收',85,4,13,NULL,NULL,0,0,NULL,'_self',0,1,1,'flow:task:claim',NULL,'POST','/api/flow/task/claim',0,0,NULL,'签收任务',NULL,'2026-03-15 18:32:13',NULL,'2026-03-15 18:32:39',NULL,'pc',2,0),
	 (98,1,'任务审批',85,4,14,NULL,NULL,0,0,NULL,'_self',0,1,1,'flow:task:approve',NULL,'POST','/api/flow/task/approve',0,0,NULL,'审批任务',NULL,'2026-03-15 18:32:13',NULL,'2026-03-15 18:32:39',NULL,'pc',2,0),
	 (99,1,'任务驳回',85,4,15,NULL,NULL,0,0,NULL,'_self',0,1,1,'flow:task:reject',NULL,'POST','/api/flow/task/reject',0,0,NULL,'驳回任务',NULL,'2026-03-15 18:32:13',NULL,'2026-03-15 18:32:39',NULL,'pc',2,0),
	 (100,1,'任务催办',85,4,16,NULL,NULL,0,0,NULL,'_self',0,1,1,'flow:task:remind',NULL,'POST','/api/flow/task/remind',0,0,NULL,'催办任务',NULL,'2026-03-15 18:32:13',NULL,'2026-03-15 18:32:40',NULL,'pc',2,0),
	 (101,1,'流程设计',79,2,90,'/flow/design','/flow/design',0,0,NULL,'_self',0,1,0,NULL,'mdi:pencil-ruler',NULL,NULL,0,0,NULL,'流程设计器',NULL,'2026-03-16 16:45:04',NULL,'2026-06-07 09:27:16',NULL,'pc',2,0),
	 (900,1,'请假管理',43,1,90,'/leave',NULL,0,0,NULL,'_self',0,1,1,NULL,'ionicons5:BarChartSharp',NULL,NULL,0,0,NULL,'请假管理目录',NULL,'2026-03-22 17:58:08',1,'2026-06-07 09:27:16',NULL,'pc',2,0),
	 (901,1,'请假申请',900,2,1,'/leave/apply','/leave/apply',0,0,NULL,'_self',0,1,1,'leave:apply:view','ionicons5:CogSharp',NULL,NULL,0,0,NULL,'请假申请页面',NULL,'2026-03-22 17:58:08',1,'2026-03-22 18:02:20',NULL,'pc',2,0),
	 (902,1,'我的请假',900,2,2,'/leave/list','/leave/list',0,0,NULL,'_self',0,1,1,'leave:list:view','ionicons5:BarbellSharp',NULL,NULL,0,0,NULL,'我的请假列表页面',NULL,'2026-03-22 17:58:08',1,'2026-03-22 18:02:05',NULL,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9011,1,'提交申请',901,3,1,NULL,NULL,0,0,NULL,'_self',0,1,1,'leave:apply:submit',NULL,NULL,NULL,0,0,NULL,'',NULL,'2026-03-22 17:58:08',1,'2026-07-06 13:59:04',NULL,'pc',2,0),
	 (9012,1,'保存草稿',901,3,2,NULL,NULL,0,0,NULL,'_self',0,1,1,'leave:apply:draft',NULL,NULL,NULL,0,0,NULL,'',NULL,'2026-03-22 17:58:08',NULL,'2026-03-22 17:58:53',NULL,'pc',2,0),
	 (9021,1,'查看详情',902,3,1,NULL,NULL,0,0,NULL,'_self',0,1,1,'leave:list:detail',NULL,NULL,NULL,0,0,NULL,'',NULL,'2026-03-22 17:58:08',NULL,'2026-03-22 17:58:53',NULL,'pc',2,0),
	 (9022,1,'撤销申请',902,3,2,NULL,NULL,0,0,NULL,'_self',0,1,1,'leave:list:cancel',NULL,NULL,NULL,0,0,NULL,'',NULL,'2026-03-22 17:58:08',NULL,'2026-03-22 17:58:53',NULL,'pc',2,0),
	 (9023,1,'删除申请',902,3,3,NULL,NULL,0,0,NULL,'_self',0,1,1,'leave:list:delete',NULL,NULL,NULL,0,0,NULL,'',NULL,'2026-03-22 17:58:08',NULL,'2026-03-22 17:58:53',NULL,'pc',2,0),
	 (9031,1,'提交请假申请',901,4,1,NULL,NULL,0,0,NULL,'_self',0,1,1,'leave:apply:submit',NULL,'POST','/leave/submit',0,0,NULL,'',NULL,'2026-03-22 17:58:08',1,'2026-06-16 10:27:43',NULL,'pc',2,0),
	 (9032,1,'保存草稿',901,4,2,NULL,NULL,0,0,NULL,'_self',0,1,1,'leave:apply:draft',NULL,'POST','/leave/draft',0,0,NULL,'',NULL,'2026-03-22 17:58:08',NULL,'2026-03-22 17:58:54',NULL,'pc',2,0),
	 (9041,1,'获取请假详情',902,4,1,NULL,NULL,0,0,NULL,'_self',0,1,1,'leave:list:detail',NULL,'GET','/leave/detail/*',0,0,NULL,'',NULL,'2026-03-22 17:58:24',NULL,'2026-03-22 17:58:54',NULL,'pc',2,0),
	 (9042,1,'分页查询请假列表',902,4,2,NULL,NULL,0,0,NULL,'_self',0,1,1,'leave:list:view',NULL,'GET','/leave/page',0,0,NULL,'',NULL,'2026-03-22 17:58:24',NULL,'2026-03-22 17:58:54',NULL,'pc',2,0),
	 (9043,1,'撤销申请',902,4,3,NULL,NULL,0,0,NULL,'_self',0,1,1,'leave:list:cancel',NULL,'POST','/leave/cancel/*',0,0,NULL,'',NULL,'2026-03-22 17:58:24',NULL,'2026-03-22 17:58:54',NULL,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9044,1,'删除申请',902,4,4,NULL,NULL,0,0,NULL,'_self',0,1,1,'leave:list:delete',NULL,'DELETE','/leave/*',0,0,NULL,'',NULL,'2026-03-22 17:58:25',NULL,'2026-03-22 17:58:54',NULL,'pc',2,0),
	 (9045,1,'个人中心(已归档)',0,1,910,'/system/usercenter',NULL,0,0,NULL,'_self',0,0,0,NULL,'local-image:icon07.png','GET',NULL,0,0,NULL,NULL,1,'2026-03-26 17:23:58',1,'2026-06-07 09:27:16',2,'pc',2,0),
	 (9046,1,'流程监控',79,2,4,'/flow/monitor','flow/monitor',0,0,'','_self',0,1,1,NULL,'local:ai-icon:monitor','GET',NULL,0,0,NULL,NULL,1,'2026-03-26 20:19:24',1,'2026-07-10 15:04:56',2,'pc',2,0),
	 (9047,1,'三方登录配置',44,2,6,'/system/socialConfig','system/socialConfig',0,0,NULL,'_self',0,1,1,NULL,'ionicons5:LogoWechat','GET',NULL,0,0,NULL,NULL,1,'2026-04-01 14:56:15',1,'2026-06-07 09:27:17',2,'pc',2,0),
	 (9048,1,'渠道配置',48,2,3,'/message/messageConfig','message/messageConfig',0,0,NULL,'_self',0,1,1,NULL,'ionicons5:ChatboxEllipses','GET',NULL,0,0,NULL,NULL,1,'2026-04-02 10:31:27',1,'2026-06-07 09:27:17',2,'pc',2,0),
	 (9049,1,'消息监控',48,2,5,'/message/manage','/message/manage',0,0,NULL,'_self',0,1,1,NULL,'ionicons5:FlameSharp','GET',NULL,0,0,NULL,NULL,1,'2026-04-02 11:20:54',1,'2026-06-07 09:27:17',2,'pc',2,0),
	 (9050,1,'业务配置',48,2,4,'/message/biz-type','/message/biz-type',0,0,NULL,'_self',0,1,1,NULL,'ionicons5:PrismSharp','GET',NULL,0,0,NULL,NULL,1,'2026-04-02 16:28:51',1,'2026-06-07 09:27:17',2,'pc',2,0),
	 (9051,1,'客户端管理',1,2,7,'/system/client','system/client',0,0,NULL,'_self',0,1,1,NULL,'ionicons5:LogoTableau',NULL,NULL,0,0,NULL,NULL,1,'2026-04-07 10:43:18',1,'2026-06-10 15:22:06',NULL,'pc',0,0),
	 (9067,1,'AI能力',0,1,5,'/ai','',0,0,'','_self',0,1,1,NULL,'local-image:icon09.png',NULL,NULL,0,1,NULL,'AI管理目录',NULL,'2026-04-17 16:19:34',1,'2026-06-26 17:19:56',NULL,'pc',2,0),
	 (9068,1,'供应商管理',9067,2,1,'/ai/provider-model','/ai/provider-model',0,0,NULL,'_self',0,1,1,NULL,'ionicons5:ExtensionPuzzle',NULL,NULL,0,0,NULL,'AI供应商管理',NULL,'2026-04-17 16:19:34',1,'2026-06-07 09:27:17',NULL,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9070,1,'供应商新增',9068,3,1,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:provider:add',NULL,NULL,NULL,0,0,NULL,'新增AI供应商',NULL,'2026-04-17 16:19:34',1,'2026-06-26 17:20:04',NULL,'pc',2,0),
	 (9071,1,'供应商编辑',9068,3,2,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:provider:edit',NULL,NULL,NULL,0,0,NULL,'编辑AI供应商',NULL,'2026-04-17 16:19:34',NULL,'2026-04-17 16:20:46',NULL,'pc',2,0),
	 (9072,1,'供应商删除',9068,3,3,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:provider:delete',NULL,NULL,NULL,0,0,NULL,'删除AI供应商',NULL,'2026-04-17 16:19:34',NULL,'2026-04-17 16:20:46',NULL,'pc',2,0),
	 (9073,1,'供应商测试',9068,3,4,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:provider:test',NULL,NULL,NULL,0,0,NULL,'测试供应商连接',NULL,'2026-04-17 16:19:34',NULL,'2026-04-17 16:20:46',NULL,'pc',2,0),
	 (9074,1,'模型新增',9069,3,1,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:model:add',NULL,NULL,NULL,0,0,NULL,'新增AI模型',NULL,'2026-04-17 16:19:34',NULL,'2026-04-17 16:20:46',NULL,'pc',2,0),
	 (9075,1,'模型编辑',9069,3,2,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:model:edit',NULL,NULL,NULL,0,0,NULL,'编辑AI模型',NULL,'2026-04-17 16:19:34',NULL,'2026-04-17 16:20:46',NULL,'pc',2,0),
	 (9076,1,'模型删除',9069,3,3,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:model:delete',NULL,NULL,NULL,0,0,NULL,'删除AI模型',NULL,'2026-04-17 16:19:34',NULL,'2026-04-17 16:20:46',NULL,'pc',2,0),
	 (9077,1,'会话管理',9067,2,3,'/ai/session','/ai/session',0,0,NULL,'_self',0,1,1,NULL,'ionicons5:ChatboxEllipses',NULL,NULL,0,0,NULL,'AI会话管理',NULL,'2026-04-19 19:05:28',1,'2026-06-07 09:27:17',NULL,'pc',2,0),
	 (9078,1,'会话查看',9077,3,1,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:session:query',NULL,NULL,NULL,0,0,NULL,'查看AI会话',NULL,'2026-04-19 19:05:28',NULL,'2026-04-19 19:05:28',NULL,'pc',2,0),
	 (9079,1,'会话删除',9077,3,2,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:session:delete',NULL,NULL,NULL,0,0,NULL,'删除AI会话',NULL,'2026-04-19 19:05:28',NULL,'2026-04-19 19:05:28',NULL,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9080,1,'AI表单生编辑',39,2,900,'/ai/crud-generator','ai/crud-generator',0,0,NULL,'_self',0,0,0,'ai:crud-gen:list','ionicons5:BonfireSharp',NULL,NULL,0,0,NULL,'CRUD配置管理；统一低代码代码生成后隐藏旧入口；已收敛到开发者工具或隐藏普通菜单入口；已收敛到开发者工具或隐藏普通菜单入口',NULL,'2026-04-20 17:46:45',1,'2026-06-14 21:18:18',NULL,'pc',2,0),
	 (9081,1,'通用表单',39,2,900,'/ai/crud-page','ai/crud-page',0,0,NULL,'_self',1,0,0,NULL,NULL,NULL,NULL,0,0,NULL,'通用表单',NULL,'2026-04-20 17:46:45',1,'2026-06-07 09:27:16',NULL,'pc',2,0),
	 (9085,1,'AI表单生成',39,2,900,'/ai/crud-config','ai/crud-config',0,0,NULL,'_self',0,0,1,'ai:crud-config:list','ionicons5:ColorWand',NULL,NULL,0,0,NULL,'CRUD配置管理；统一低代码代码生成后隐藏旧入口；已收敛到开发者工具或隐藏普通菜单入口；已收敛到开发者工具或隐藏普通菜单入口',NULL,'2026-04-20 17:46:45',1,'2026-06-14 21:37:11',NULL,'pc',2,0),
	 (9088,1,'模版市场',39,2,900,'/ai/page-template','ai/page-template',0,0,NULL,'_self',0,0,0,'ai:page-template:list','mdi:puzzle-outline',NULL,NULL,0,0,NULL,'AI页面模板管理；统一低代码代码生成后隐藏旧入口；已收敛到开发者工具或隐藏普通菜单入口；已收敛到开发者工具或隐藏普通菜单入口',NULL,'2026-04-26 17:09:26',1,'2026-06-14 21:18:18',NULL,'pc',2,0),
	 (9091,1,'行政区划',44,2,5,'/system/region','system/region',0,0,NULL,'_self',0,1,1,NULL,'ionicons5:TriangleSharp','GET',NULL,0,0,NULL,NULL,1,'2026-04-28 13:06:30',1,'2026-06-07 09:27:17',2,'pc',2,0),
	 (9092,1,'APP测试',0,1,0,'/app',NULL,0,0,NULL,'_self',0,1,1,NULL,NULL,'GET',NULL,0,1,NULL,NULL,1,'2026-04-29 16:25:30',1,'2026-04-29 17:10:21',2,'app',2,0),
	 (9093,1,'测试APP菜单',9092,2,2,'/cac','/cac',0,0,NULL,'_self',0,1,1,NULL,NULL,'GET',NULL,0,0,NULL,NULL,1,'2026-04-29 16:33:05',1,'2026-04-29 16:33:05',2,'app',2,0),
	 (9094,1,'流程表达式',79,2,3,'/flow/spelTemplate','flow/spelTemplate',0,0,NULL,'_self',0,1,1,NULL,'ionicons5:GitCompare','GET',NULL,0,1,NULL,NULL,1,'2026-05-05 17:43:05',1,'2026-06-07 09:27:16',2,'pc',2,0),
	 (9095,1,'版本历史',79,2,91,'/flow/version','flow/version',0,0,NULL,'_self',0,1,0,NULL,NULL,'GET',NULL,0,0,NULL,NULL,1,'2026-05-09 11:18:18',1,'2026-06-07 09:27:16',2,'pc',2,0),
	 (9097,1,'服务接口代理',44,2,22,'/external/system','external/system',0,0,NULL,'_self',0,1,1,NULL,'ionicons5:LogoElectron','GET',NULL,0,0,NULL,NULL,1,'2026-05-10 20:53:38',1,'2026-06-07 09:27:17',2,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9100,1,'数据与报表',0,1,4,'/data-report','',0,0,'','_self',0,1,1,NULL,'local-image:icon06.png',NULL,NULL,0,1,NULL,'数据资产管理目录',1,'2026-05-12 10:03:57',1,'2026-06-13 09:26:09',1,'pc',2,0),
	 (9101,1,'数据连接',9100,2,1,'/data/connection','data/connection',0,0,NULL,'_self',0,1,1,'data:connection:list','ionicons5:ContractSharp',NULL,NULL,1,0,NULL,'数据连接管理菜单',1,'2026-05-12 10:03:57',1,'2026-06-07 09:27:16',1,'pc',2,0),
	 (9102,1,'数据集管理',9100,2,2,'/data/dataset','data/dataset',0,0,NULL,'_self',0,1,1,'data:dataset:list','ionicons5:FilterCircle',NULL,NULL,1,0,NULL,'数据集管理菜单',1,'2026-05-12 10:03:57',1,'2026-06-07 09:27:17',1,'pc',2,0),
	 (9103,1,'查询数据连接列表',9101,3,1,NULL,NULL,0,0,NULL,'_self',0,1,1,'data:connection:query',NULL,NULL,NULL,0,0,NULL,'数据连接查询按钮',1,'2026-05-12 10:03:57',1,'2026-05-12 10:03:57',1,'pc',2,0),
	 (9104,1,'新增数据连接',9101,3,2,NULL,NULL,0,0,NULL,'_self',0,1,1,'data:connection:add',NULL,NULL,NULL,0,0,NULL,'数据连接新增按钮',1,'2026-05-12 10:03:57',1,'2026-05-12 10:03:57',1,'pc',2,0),
	 (9105,1,'修改数据连接',9101,3,3,NULL,NULL,0,0,NULL,'_self',0,1,1,'data:connection:edit',NULL,NULL,NULL,0,0,NULL,'数据连接修改按钮',1,'2026-05-12 10:03:57',1,'2026-05-12 10:03:57',1,'pc',2,0),
	 (9106,1,'删除数据连接',9101,3,4,NULL,NULL,0,0,NULL,'_self',0,1,1,'data:connection:remove',NULL,NULL,NULL,0,0,NULL,'数据连接删除按钮',1,'2026-05-12 10:03:57',1,'2026-05-12 10:03:57',1,'pc',2,0),
	 (9107,1,'测试数据连接',9101,3,5,NULL,NULL,0,0,NULL,'_self',0,1,1,'data:connection:test',NULL,NULL,NULL,0,0,NULL,'数据连接测试按钮',1,'2026-05-12 10:03:57',1,'2026-05-12 10:03:57',1,'pc',2,0),
	 (9108,1,'查询数据集列表',9102,3,1,NULL,NULL,0,0,NULL,'_self',0,1,1,'data:dataset:query',NULL,NULL,NULL,0,0,NULL,'数据集查询按钮',1,'2026-05-12 10:03:57',1,'2026-05-12 10:03:57',1,'pc',2,0),
	 (9109,1,'新增数据集',9102,3,2,NULL,NULL,0,0,NULL,'_self',0,1,1,'data:dataset:add',NULL,NULL,NULL,0,0,NULL,'数据集新增按钮',1,'2026-05-12 10:03:57',1,'2026-05-12 10:03:57',1,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9110,1,'修改数据集',9102,3,3,NULL,NULL,0,0,NULL,'_self',0,1,1,'data:dataset:edit',NULL,NULL,NULL,0,0,NULL,'数据集修改按钮',1,'2026-05-12 10:03:57',1,'2026-05-12 10:03:57',1,'pc',2,0),
	 (9111,1,'删除数据集',9102,3,4,NULL,NULL,0,0,NULL,'_self',0,1,1,'data:dataset:remove',NULL,NULL,NULL,0,0,NULL,'数据集删除按钮',1,'2026-05-12 10:03:57',1,'2026-05-12 10:03:57',1,'pc',2,0),
	 (9112,1,'预览数据集',9102,3,5,NULL,NULL,0,0,NULL,'_self',0,1,1,'data:dataset:preview',NULL,NULL,NULL,0,0,NULL,'数据集预览按钮',1,'2026-05-12 10:03:57',1,'2026-05-12 10:03:57',1,'pc',2,0),
	 (9113,1,'AI大屏',9100,2,10,'/project/items','',0,1,'forge_report','_blank',0,1,1,NULL,'ionicons5:ScaleSharp','GET',NULL,0,0,NULL,NULL,1,'2026-05-13 08:02:38',1,'2026-06-07 09:27:17',2,'pc',2,0),
	 (9136,1,'数据集分类管理',9100,2,3,'/data/dataset-category','data/dataset-category',0,0,NULL,'_self',0,1,1,'data:dataset:category:list','AppstoreOutlined',NULL,NULL,1,0,NULL,'数据集分类管理菜单',1,'2026-05-14 16:17:13',1,'2026-06-07 09:27:17',1,'pc',2,0),
	 (9147,1,'数据维度',9100,2,4,'/data/dimension','data/dimension',0,0,NULL,'_self',0,1,1,'data:dimension:list','PartitionOutlined',NULL,NULL,1,0,NULL,'数据维度管理菜单',1,'2026-05-14 20:50:47',1,'2026-06-07 09:27:17',1,'pc',2,0),
	 (9148,1,'AI大屏目录',0,1,0,'',NULL,0,0,'','_self',0,1,1,NULL,'local:i-streamline-plump-color:ai-generate-variation-spark','GET',NULL,0,0,NULL,NULL,1,'2026-05-14 22:41:33',1,'2026-05-18 11:25:27',2,'forge_report',2,0),
	 (9149,1,'全部项目',9148,2,0,'/project/items','project/items/index',0,0,'','_self',0,1,1,NULL,'ionicons5:AlbumsOutline','GET',NULL,0,0,NULL,NULL,1,'2026-05-14 22:55:51',1,'2026-05-14 22:55:51',2,'forge_report',2,0),
	 (9150,1,'我的模版',9148,2,1,'/project/my-template','project/mtTemplate/index',0,0,'','_self',0,1,1,NULL,'ionicons5:Apps','GET',NULL,0,0,NULL,NULL,1,'2026-05-14 22:57:20',1,'2026-05-14 22:57:20',2,'forge_report',2,0),
	 (9151,1,'素材库',9148,2,2,'/project/materials','project/materials/index',0,0,'','_self',0,1,1,NULL,'ionicons5:AirplaneSharp','GET',NULL,0,0,NULL,NULL,1,'2026-05-14 22:57:55',1,'2026-05-14 22:57:55',2,'forge_report',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9152,1,'AI供应商',9148,2,3,'/project/ai-provider','project/settings/AiSettings',0,0,'','_self',0,1,0,NULL,'ionicons5:AlertCircleSharp','GET',NULL,0,0,NULL,NULL,1,'2026-05-14 22:58:57',1,'2026-07-23 01:00:25',2,'forge_report',2,0),
	 (9153,1,'目录管理',9148,2,4,'/project/directories','project/directories/index',0,0,'','_self',0,1,1,NULL,'ionicons5:ApertureSharp','GET',NULL,0,0,NULL,NULL,1,'2026-05-14 22:59:35',1,'2026-05-14 22:59:35',2,'forge_report',2,0),
	 (9154,1,'模版市场',9148,2,5,'/project/template-market','project/templateMarket/index',0,0,'','_self',0,1,1,NULL,'ionicons5:Archive','GET',NULL,0,0,NULL,NULL,1,'2026-05-14 23:00:28',1,'2026-05-14 23:00:28',2,'forge_report',2,0),
	 (9156,1,'删除',9151,3,0,NULL,NULL,0,0,'','_self',0,1,1,'report:user:del',NULL,'GET',NULL,0,0,NULL,NULL,1,'2026-05-14 23:14:24',1,'2026-05-14 23:14:24',2,'forge_report',2,0),
	 (9157,1,'下载',9151,3,1,NULL,NULL,0,0,'','_self',0,1,1,'report:user:down',NULL,'GET',NULL,0,0,NULL,NULL,1,'2026-05-14 23:14:52',1,'2026-05-14 23:14:52',2,'forge_report',2,0),
	 (9158,1,'修改名字',9151,3,2,NULL,NULL,0,0,'','_self',0,1,1,'report:user:editName',NULL,'GET',NULL,0,0,NULL,NULL,1,'2026-05-14 23:15:26',1,'2026-05-14 23:15:26',2,'forge_report',2,0),
	 (9160,1,'智能体管理',9067,2,2,'/ai/agent','/ai/agent',0,0,NULL,'_self',0,1,1,NULL,'ionicons5:HardwareChipOutline',NULL,NULL,0,0,NULL,'AI智能体管理',NULL,'2026-05-15 15:32:07',NULL,'2026-06-07 09:27:17',NULL,'pc',2,0),
	 (9162,1,'业务定义',9100,2,5,'/data/business','data/business',0,0,NULL,'_self',0,1,1,'data:business:list','ProfileOutlined',NULL,NULL,1,0,NULL,'业务定义管理菜单',1,'2026-05-17 12:19:20',1,'2026-06-07 09:27:17',1,'pc',2,0),
	 (9163,1,'大屏生成记录',9067,2,5,'/ai/dashboard-generate-record','ai/dashboard-generate-record',0,0,NULL,'_self',0,1,1,'ai:dashboard-record:list','ionicons5:ReceiptOutline',NULL,NULL,1,0,NULL,'AI大屏生成记录稽核',1,'2026-05-19 11:57:10',1,'2026-06-07 09:27:17',1,'pc',2,0),
	 (9167,1,'提示词模板库',9067,2,4,'/ai/prompt-template','ai/prompt-template',0,0,NULL,'_self',0,1,1,'ai:prompt-template:list','ionicons5:DocumentTextOutline',NULL,NULL,1,0,NULL,'AI提示词模板库',1,'2026-05-19 15:22:05',1,'2026-06-07 09:27:17',1,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9169,1,'应用搭建器',9174,2,900,'/ai/lowcode-builder','ai/lowcode-builder',0,0,'','_self',0,0,0,'ai:lowcode:edit','ionicons5:BuildOutline',NULL,NULL,1,0,NULL,'低代码应用搭建器隐藏入口',1,'2026-05-20 06:20:27',1,'2026-06-14 21:18:18',1,'pc',2,0),
	 (9170,1,'发布低代码应用',9174,3,1,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:lowcode:publish',NULL,NULL,NULL,0,0,NULL,'发布低代码应用按钮权限',1,'2026-05-20 06:20:27',1,'2026-05-28 18:00:38',1,'pc',2,0),
	 (9171,1,'回滚低代码应用',9174,3,2,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:lowcode:rollback',NULL,NULL,NULL,0,0,NULL,'回滚低代码应用按钮权限',1,'2026-05-20 06:20:27',1,'2026-05-28 18:00:38',1,'pc',2,0),
	 (9172,1,'在线建表发布',9174,3,3,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:lowcode:deploy-ddl',NULL,NULL,NULL,0,0,NULL,'低代码在线建表发布按钮权限',1,'2026-05-20 06:20:27',1,'2026-05-28 18:00:38',1,'pc',2,0),
	 (9174,1,'应用开发',9218,2,900,'/ai/lowcode-apps','ai/lowcode-apps',0,0,'','_self',0,1,1,'ai:lowcode:domain:list','ionicons5:ConstructOutline',NULL,NULL,1,0,NULL,'低代码应用设计、AI创建、发布运行和代码输出主入口',1,'2026-05-20 20:00:39',1,'2026-06-09 20:43:36',1,'pc',2,0),
	 (9176,1,'新增业务领域',9174,3,1,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:lowcode:domain:add',NULL,NULL,NULL,0,0,NULL,'新增业务领域按钮权限',1,'2026-05-20 20:00:39',1,'2026-05-28 18:00:38',1,'pc',2,0),
	 (9177,1,'编辑业务领域',9174,3,2,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:lowcode:domain:edit',NULL,NULL,NULL,0,0,NULL,'编辑业务领域按钮权限',1,'2026-05-20 20:00:39',1,'2026-05-28 18:00:38',1,'pc',2,0),
	 (9178,1,'启停业务领域',9174,3,3,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:lowcode:domain:status',NULL,NULL,NULL,0,0,NULL,'启停业务领域按钮权限',1,'2026-05-20 20:00:39',1,'2026-05-28 18:00:38',1,'pc',2,0),
	 (9179,1,'迁移业务领域',9174,3,4,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:lowcode:domain:move',NULL,NULL,NULL,0,0,NULL,'迁移低代码应用业务领域按钮权限',1,'2026-05-20 20:00:39',1,'2026-05-28 18:00:38',1,'pc',2,0),
	 (9180,1,'模型设计',9218,2,900,'/ai/lowcode-models','ai/lowcode-models',0,0,'','_self',0,1,0,'ai:lowcode:model:list','ionicons5:GitBranchOutline',NULL,NULL,1,0,NULL,'高级模型资产兼容入口；简单表导入请从应用中心的新建应用或数据对象进入',1,'2026-05-20 22:01:15',1,'2026-07-15 06:21:06',1,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9181,1,'新增数据模型',9180,3,1,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:lowcode:model:add',NULL,NULL,NULL,0,0,NULL,'新增低代码数据模型按钮权限',1,'2026-05-20 22:01:15',1,'2026-05-28 18:00:38',1,'pc',2,0),
	 (9182,1,'编辑数据模型',9180,3,2,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:lowcode:model:edit',NULL,NULL,NULL,0,0,NULL,'编辑低代码数据模型按钮权限',1,'2026-05-20 22:01:15',1,'2026-05-28 18:00:38',1,'pc',2,0),
	 (9183,1,'启停数据模型',9180,3,3,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:lowcode:model:status',NULL,NULL,NULL,0,0,NULL,'启停低代码数据模型按钮权限',1,'2026-05-20 22:01:15',1,'2026-05-28 18:00:38',1,'pc',2,0),
	 (9184,1,'客户管理',39,2,900,'/ai/crud-page/custmanage','ai/crud-page',0,0,NULL,'_self',0,0,0,'ai:crud:custmanage',NULL,NULL,NULL,0,0,NULL,NULL,1,'2026-05-21 20:24:17',1,'2026-06-07 09:27:16',2,'pc',2,0),
	 (9185,1,'订单管理',39,2,900,'/ai/crud-page/order_manage','ai/crud-page',0,0,NULL,'_self',0,0,0,'ai:crud:order_manage',NULL,NULL,NULL,0,0,NULL,NULL,1,'2026-05-22 11:27:14',1,'2026-06-07 09:27:16',2,'pc',2,0),
	 (9186,1,'订单左树',39,2,900,'/ai/crud-page/order_class','ai/crud-page',0,0,NULL,'_self',0,0,0,'ai:crud:order_class',NULL,NULL,NULL,0,0,NULL,NULL,1,'2026-05-22 16:25:00',1,'2026-06-07 09:27:16',2,'pc',2,0),
	 (9187,1,'订单分类树',39,2,900,'/ai/crud-page/order_class_manage','ai/crud-page',0,0,NULL,'_self',0,0,0,'ai:crud:order_class_manage',NULL,NULL,NULL,0,0,NULL,NULL,1,'2026-05-24 18:00:32',1,'2026-06-07 09:27:16',2,'pc',2,0),
	 (9188,1,'AI创建应用',9174,3,11,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:lowcode:ai-generate',NULL,NULL,NULL,0,0,NULL,'AI生成低代码模型和应用草稿按钮权限',1,'2026-05-25 11:26:48',1,'2026-05-28 18:00:38',1,'pc',2,0),
	 (9189,1,'代码预览',9174,3,12,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:lowcode:code-preview',NULL,NULL,NULL,0,0,NULL,'低代码应用代码预览按钮权限',1,'2026-05-25 11:26:48',1,'2026-05-28 18:00:38',1,'pc',2,0),
	 (9190,1,'下载代码',9174,3,13,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:lowcode:code-download',NULL,NULL,NULL,0,0,NULL,'低代码应用代码下载按钮权限',1,'2026-05-25 11:26:48',1,'2026-05-28 18:00:38',1,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9191,1,'导入数据源表',9180,3,11,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:lowcode:model:import-table',NULL,NULL,NULL,0,0,NULL,'从数据源表导入低代码模型按钮权限',1,'2026-05-25 11:26:48',1,'2026-05-28 18:00:38',1,'pc',2,0),
	 (9192,1,'AI生成模型',9180,3,12,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:lowcode:model:ai-generate',NULL,NULL,NULL,0,0,NULL,'AI生成低代码模型草稿按钮权限',1,'2026-05-25 11:26:48',1,'2026-05-28 18:00:38',1,'pc',2,0),
	 (9193,1,'商品管理',39,2,900,'/ai/crud-page/scm_product','ai/crud-page',0,0,'','_self',0,0,0,'ai:crud:scm_product','local:ai-icon:shopping-bag',NULL,NULL,0,0,NULL,NULL,1,'2026-05-25 16:01:31',1,'2026-06-07 09:27:16',2,'pc',2,0),
	 (9194,1,'应用中心',0,1,2,'/app-center','',0,0,'','_self',0,1,1,NULL,'local-image:icon02.png',NULL,NULL,1,1,NULL,'企业应用装配平台主入口',1,'2026-05-27 13:01:02',1,'2026-06-13 09:26:30',1,'pc',2,0),
	 (9195,1,'查看业务域',9217,2,1,'/app-center/suite/:suiteCode','app-center/suite.[suiteCode]',0,0,'','_self',0,1,0,'ai:businessSuite:list','ionicons5:AlbumsOutline',NULL,NULL,0,0,NULL,'业务套件详情隐藏路由',1,'2026-05-27 13:01:02',1,'2026-06-15 13:06:10',1,'pc',2,0),
	 (9196,1,'查看业务单元',9217,2,2,'/app-center/object/:objectCode','app-center/object.[objectCode]',0,0,'','_self',0,1,0,'ai:businessObject:list','ionicons5:CubeOutline',NULL,NULL,0,0,NULL,'业务对象详情隐藏路由',1,'2026-05-27 13:01:02',1,'2026-06-25 15:27:58',1,'pc',2,0),
	 (9197,1,'引擎中心',9194,2,3,'/app-center/engines','app-center/engines',0,0,NULL,'_self',0,1,1,'ai:businessEngine:list','ionicons5:HardwareChipOutline',NULL,NULL,1,0,NULL,'流程、审批、报表、权限和消息引擎统一入口',1,'2026-05-27 13:01:02',1,'2026-06-07 20:39:16',1,'pc',2,0),
	 (9198,1,'移动端中心',9194,2,4,'/app-center/mobile','app-center/mobile',0,0,'','_self',0,1,1,'ai:businessMobile:list','ionicons5:PhonePortraitOutline',NULL,NULL,1,0,NULL,'移动H5和移动业务入口',1,'2026-05-27 13:01:02',1,'2026-06-07 20:39:16',1,'pc',2,0),
	 (9199,1,'集成中心',9194,2,5,'/app-center/integration','app-center/integration',0,0,'','_self',0,1,1,'ai:businessIntegration:list','ionicons5:GitNetworkOutline',NULL,NULL,1,0,NULL,'开放接口、Webhook和第三方平台连接入口',1,'2026-05-27 13:01:02',1,'2026-06-07 20:39:16',1,'pc',2,0),
	 (9202,1,'查看业务域',9217,3,11,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessSuite:list',NULL,NULL,NULL,0,0,NULL,'查询业务套件按钮权限',1,'2026-05-27 13:01:02',1,'2026-06-15 13:06:10',1,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9203,1,'维护业务域',9217,3,12,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessSuite:edit',NULL,NULL,NULL,0,0,NULL,'新增或编辑业务套件按钮权限',1,'2026-05-27 13:01:02',1,'2026-06-14 21:18:18',1,'pc',2,0),
	 (9204,1,'查看业务单元',9217,3,21,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessObject:list',NULL,NULL,NULL,0,0,NULL,'查询业务对象按钮权限',1,'2026-05-27 13:01:02',1,'2026-06-15 13:06:10',1,'pc',2,0),
	 (9205,1,'新增业务单元',9217,3,22,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessObject:add',NULL,NULL,NULL,0,0,NULL,'新增业务对象按钮权限',1,'2026-05-27 13:01:02',1,'2026-06-14 21:18:18',1,'pc',2,0),
	 (9206,1,'编辑业务单元',9217,3,23,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessObject:edit',NULL,NULL,NULL,0,0,NULL,'编辑业务对象按钮权限',1,'2026-05-27 13:01:02',1,'2026-06-14 21:18:18',1,'pc',2,0),
	 (9207,1,'删除业务单元',9217,3,24,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessObject:delete',NULL,NULL,NULL,0,0,NULL,'删除业务对象按钮权限',1,'2026-05-27 13:01:02',1,'2026-06-14 21:18:18',1,'pc',2,0),
	 (9208,1,'配置业务单元关系',9217,3,25,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessObject:relation',NULL,NULL,NULL,0,0,NULL,'配置业务对象关系按钮权限',1,'2026-05-27 13:01:02',1,'2026-06-15 13:06:10',1,'pc',2,0),
	 (9209,1,'配置接入能力',9217,3,31,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessBinding:config',NULL,NULL,NULL,0,0,NULL,'配置对象或应用能力挂接按钮权限',1,'2026-05-27 13:01:02',1,'2026-06-15 13:06:10',1,'pc',2,0),
	 (9210,1,'新增访问入口',9217,3,41,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessApp:add',NULL,NULL,NULL,0,0,NULL,'新增应用入口按钮权限',1,'2026-05-27 13:01:02',1,'2026-06-14 21:18:18',1,'pc',2,0),
	 (9211,1,'编辑访问入口',9217,3,42,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessApp:edit',NULL,NULL,NULL,0,0,NULL,'编辑应用入口按钮权限',1,'2026-05-27 13:01:02',1,'2026-06-14 21:18:18',1,'pc',2,0),
	 (9212,1,'删除访问入口',9217,3,43,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessApp:delete',NULL,NULL,NULL,0,0,NULL,'删除应用入口按钮权限',1,'2026-05-27 13:01:02',1,'2026-06-14 21:18:18',1,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9213,1,'启停访问入口',9217,3,44,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessApp:status',NULL,NULL,NULL,0,0,NULL,'启停应用入口按钮权限',1,'2026-05-27 13:01:02',1,'2026-06-14 21:18:18',1,'pc',2,0),
	 (9214,1,'打开访问入口',9217,3,45,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessApp:open',NULL,NULL,NULL,0,0,NULL,'打开应用入口按钮权限',1,'2026-05-27 13:01:02',1,'2026-06-15 13:06:10',1,'pc',2,0),
	 (9217,1,'应用总览',9194,2,1,'/app-center','app-center/index',0,0,NULL,'_self',0,1,1,'ai:businessApp:list','ionicons5:GridOutline',NULL,NULL,1,0,NULL,'企业应用装配平台主入口',1,'2026-05-27 15:04:46',1,'2026-06-14 21:18:18',1,'pc',2,0),
	 (9218,1,'开发者工具(已归档)',0,1,930,'','',0,0,'','_self',0,0,1,NULL,'ionicons5:CodeSlashOutline',NULL,NULL,0,1,NULL,'低代码、模型设计、数据源和代码输出等开发者入口',1,'2026-05-28 13:00:54',1,'2026-06-14 21:36:52',1,'pc',2,0),
	 (9220,1,'客户管理',9219,2,900,'/ai/crud-page/crm_crm_customer','ai/crud-page',0,0,NULL,'_self',0,0,0,'ai:crud:crm_crm_customer',NULL,NULL,NULL,0,0,NULL,NULL,1,'2026-05-29 08:59:17',1,'2026-06-07 09:27:16',2,'pc',2,0),
	 (9221,1,'业务对象设计器',9217,2,6,'/app-center/object/:objectCode/designer','app-center/object-designer.[objectCode]',0,0,'','_self',0,1,0,'ai:businessObject:design','ionicons5:BuildOutline',NULL,NULL,0,0,NULL,'业务对象设计器隐藏路由',1,'2026-05-29 14:39:56',1,'2026-06-15 13:06:10',1,'pc',2,0),
	 (9222,1,'设计业务对象',9221,3,26,NULL,NULL,0,0,NULL,'_self',0,1,0,'ai:businessObject:design',NULL,NULL,NULL,0,0,NULL,'进入和保存业务对象设计器',1,'2026-05-29 14:39:56',1,'2026-07-14 08:01:05',1,'pc',2,0),
	 (9223,1,'维护业务字段',9221,3,27,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessObject:field',NULL,NULL,NULL,0,0,NULL,'新增、编辑、删除和排序业务字段',1,'2026-05-29 14:39:56',1,'2026-05-29 14:39:56',1,'pc',2,0),
	 (9224,1,'维护对象布局',9221,3,28,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessObject:layout',NULL,NULL,NULL,0,0,NULL,'维护表单、列表和详情布局',1,'2026-05-29 14:39:56',1,'2026-05-29 14:39:56',1,'pc',2,0),
	 (9225,1,'配置对象操作',9221,3,29,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessObject:action',NULL,NULL,NULL,0,0,NULL,'配置工具栏、行和详情自定义操作',1,'2026-05-29 14:39:56',1,'2026-05-29 14:39:56',1,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9226,1,'发布业务对象',9221,3,30,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessObject:publish',NULL,NULL,NULL,0,0,NULL,'执行发布检查并发布业务对象',1,'2026-05-29 14:39:56',1,'2026-05-29 14:39:56',1,'pc',2,0),
	 (9227,1,'高级对象配置',9221,3,31,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessObject:advanced',NULL,NULL,NULL,0,0,NULL,'查看模型、表名、Schema、DDL和开发者入口',1,'2026-05-29 14:39:56',1,'2026-05-29 14:39:56',1,'pc',2,0),
	 (9230,1,'跟进记录',9219,2,900,'/ai/crud-page/crm_follow_record','ai/crud-page',0,0,NULL,'_self',0,0,0,'ai:crud:crm_follow_record',NULL,NULL,NULL,0,0,NULL,NULL,1,'2026-05-30 19:54:53',1,'2026-06-07 09:27:16',2,'pc',2,0),
	 (9239,1,'打开访问入口',9238,2,910,'/app-center/app/1910000000000000301','app-center/app-entry',0,0,NULL,'_self',0,0,0,'ai:businessApp:open','ionicons5:BusinessOutline',NULL,NULL,0,0,NULL,NULL,1,'2026-05-31 07:21:43',1,'2026-06-14 21:18:18',2,'pc',2,0),
	 (9243,1,'联系人管理',9238,2,910,'/app-center/app/1910000000000000302','app-center/app-entry',0,0,NULL,'_self',0,0,0,'ai:businessApp:open:crm_contact_manage','ionicons5:PersonOutline',NULL,NULL,0,0,NULL,NULL,1,'2026-05-31 08:30:22',1,'2026-06-07 09:27:16',2,'pc',2,0),
	 (9244,1,'联系人',9219,2,900,'/ai/crud-page/crm_contact','ai/crud-page',0,0,NULL,'_self',0,0,0,'ai:crud:crm_contact',NULL,NULL,NULL,0,0,NULL,NULL,1,'2026-05-31 10:55:01',1,'2026-06-07 09:27:16',2,'pc',2,0),
	 (9245,1,'触发器管理',9217,3,20,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessTrigger:list',NULL,NULL,NULL,0,0,NULL,'业务触发器列表权限',1,'2026-06-01 14:27:12',1,'2026-06-15 13:06:10',1,'pc',2,0),
	 (9246,1,'新增触发器',9217,3,21,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessTrigger:add',NULL,NULL,NULL,0,0,NULL,'新增业务触发器按钮权限',1,'2026-06-01 14:27:12',1,'2026-06-01 14:27:12',1,'pc',2,0),
	 (9247,1,'编辑触发器',9217,3,22,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessTrigger:edit',NULL,NULL,NULL,0,0,NULL,'编辑业务触发器按钮权限',1,'2026-06-01 14:27:12',1,'2026-06-01 14:27:12',1,'pc',2,0),
	 (9248,1,'删除触发器',9217,3,23,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessTrigger:delete',NULL,NULL,NULL,0,0,NULL,'删除业务触发器按钮权限',1,'2026-06-01 14:27:12',1,'2026-06-01 14:27:12',1,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9249,1,'流程发起',9217,3,24,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessFlow:start',NULL,NULL,NULL,0,0,NULL,'从业务记录发起流程权限',1,'2026-06-01 14:27:12',1,'2026-06-01 14:27:12',1,'pc',2,0),
	 (9250,1,'配置单据',9217,3,25,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessDocument:config',NULL,NULL,NULL,0,0,NULL,'配置业务对象单据模式权限',1,'2026-06-01 15:56:24',1,'2026-06-01 15:56:24',1,'pc',2,0),
	 (9251,1,'查看单据运行态',9217,3,26,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessDocument:view',NULL,NULL,NULL,0,0,NULL,'查看业务单据运行态权限',1,'2026-06-01 15:56:24',1,'2026-06-01 15:56:24',1,'pc',2,0),
	 (9252,1,'配置业务流程',9217,3,27,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessFlow:config',NULL,NULL,NULL,0,0,NULL,'配置业务对象流程绑定权限',1,'2026-06-01 15:56:24',1,'2026-06-01 15:56:24',1,'pc',2,0),
	 (9253,1,'查看业务流程',9217,3,28,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessFlow:view',NULL,NULL,NULL,0,0,NULL,'查看业务记录流程状态权限',1,'2026-06-01 15:56:24',1,'2026-06-01 15:56:24',1,'pc',2,0),
	 (9258,1,'流程回调',9217,3,29,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessFlow:callback',NULL,NULL,NULL,0,0,NULL,'处理业务流程回调权限',1,'2026-06-02 06:41:19',1,'2026-06-02 06:41:19',1,'pc',2,0),
	 (9259,1,'审批兼容发起',9217,3,30,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessApproval:start',NULL,NULL,NULL,0,0,NULL,'旧业务审批入口兼容转发权限',1,'2026-06-02 06:41:19',1,'2026-06-02 06:41:19',1,'pc',2,0),
	 (9261,1,'保存单据',9217,3,31,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessDocument:save',NULL,NULL,NULL,0,0,NULL,'保存业务单据权限',1,'2026-06-02 06:41:19',1,'2026-06-02 06:41:19',1,'pc',2,0),
	 (9262,1,'提交单据',9217,3,32,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessDocument:submit',NULL,NULL,NULL,0,0,NULL,'提交业务单据权限',1,'2026-06-02 06:41:19',1,'2026-06-02 06:41:19',1,'pc',2,0),
	 (9263,1,'撤回单据',9217,3,33,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessDocument:withdraw',NULL,NULL,NULL,0,0,NULL,'撤回业务单据权限',1,'2026-06-02 06:41:19',1,'2026-06-02 06:41:19',1,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9264,1,'执行触发器',9217,3,34,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessTrigger:execute',NULL,NULL,NULL,0,0,NULL,'执行业务触发器权限',1,'2026-06-02 06:41:19',1,'2026-06-02 06:41:19',1,'pc',2,0),
	 (9265,1,'查看业务报表',9217,3,35,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessStats:view',NULL,NULL,NULL,0,0,NULL,'查看业务对象报表权限',1,'2026-06-02 06:41:19',1,'2026-06-15 13:06:10',1,'pc',2,0),
	 (9270,1,'离职申请',9269,2,0,'/ai/crud-page/hr_leave_application','ai/crud-page',0,0,NULL,'_self',0,0,0,'ai:crud:hr_leave_application',NULL,NULL,NULL,0,0,NULL,NULL,1,'2026-06-02 16:53:23',1,'2026-06-14 14:25:06',2,'pc',2,0),
	 (9276,1,'发布流程表单版本',85,3,41,NULL,NULL,0,0,NULL,'_self',0,1,1,'flow:form:publish',NULL,NULL,NULL,0,0,NULL,'发布流程表单不可变版本',1,'2026-06-06 16:36:36',1,'2026-06-06 16:36:36',1,'pc',2,0),
	 (9277,1,'维护模型入口配置',85,3,42,NULL,NULL,0,0,NULL,'_self',0,1,1,'flow:entry:edit',NULL,NULL,NULL,0,0,NULL,'在流程模型内新增或编辑入口配置',1,'2026-06-06 16:36:36',1,'2026-06-06 18:07:09',1,'pc',2,0),
	 (9278,1,'打开模型入口',85,3,43,NULL,NULL,0,0,NULL,'_self',0,1,1,'flow:entry:open',NULL,NULL,NULL,0,0,NULL,'按模型入口填报并发起流程',1,'2026-06-06 16:36:36',1,'2026-06-06 18:07:09',1,'pc',2,0),
	 (9279,1,'查看流程表单实例',85,3,44,NULL,NULL,0,0,NULL,'_self',0,1,1,'flow:runtime:instance',NULL,NULL,NULL,0,0,NULL,'查看流程表单实例快照',1,'2026-06-06 16:36:36',1,'2026-06-06 16:36:36',1,'pc',2,0),
	 (9283,1,'首页',43,2,0,'/home','home/index',0,0,NULL,'_self',0,1,1,NULL,'ionicons5:HomeOutline',NULL,NULL,0,0,NULL,NULL,NULL,'2026-06-07 09:27:16',NULL,'2026-06-07 09:33:29',NULL,'pc',2,0),
	 (9288,1,'InOutHistory',9269,2,0,'/ai/crud-page/in_out_history','ai/crud-page',0,0,NULL,'_self',0,1,1,'ai:crud:in_out_history',NULL,NULL,NULL,0,0,NULL,NULL,1,'2026-06-08 17:19:53',1,'2026-06-14 11:36:18',2,'pc',2,0),
	 (9291,1,'合同',9219,2,0,'/ai/crud-page/crm_contract','ai/crud-page',0,0,NULL,'_self',0,1,1,'ai:crud:crm_contract',NULL,NULL,NULL,0,0,NULL,NULL,1,'2026-06-08 18:16:42',1,'2026-06-08 18:16:57',2,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9296,1,'测试',9269,2,0,'/ai/crud-page/in_out_crm_follow_record','ai/crud-page',0,0,NULL,'_self',0,1,1,'ai:crud:in_out_crm_follow_record',NULL,NULL,NULL,0,0,NULL,NULL,1,'2026-06-10 10:26:52',1,'2026-06-20 21:56:42',2,'pc',2,0),
	 (9299,0,'待办',0,2,0,'/pages/todo','',0,0,'','_self',0,1,1,NULL,'ionicons5:Alarm',NULL,NULL,0,0,NULL,NULL,1,'2026-06-11 10:41:51',1,'2026-06-11 10:41:51',2,'h5',2,0),
	 (9300,0,'协同办公',0,1,0,'','',0,0,'','_self',0,1,1,NULL,NULL,NULL,NULL,0,0,NULL,NULL,1,'2026-06-11 10:57:16',1,'2026-06-11 10:57:16',2,'h5',2,0),
	 (9301,0,'联系方式',9300,2,0,'/pages/test','',0,0,'','_self',0,1,1,NULL,'ionicons5:Airplane',NULL,NULL,0,0,NULL,NULL,1,'2026-06-11 10:57:58',1,'2026-07-14 10:10:25',2,'h5',2,0),
	 (9371,1,'出入管理',9372,1,0,'/app-center/suite-menu/IN_OUT',NULL,0,0,NULL,'_self',0,1,1,'ai:business:suite-menu:IN_OUT','ionicons5:Accessibility',NULL,NULL,0,1,NULL,NULL,1,'2026-06-14 11:36:10',1,'2026-06-23 10:51:05',2,'pc',2,0),
	 (9372,1,'InOutHistory',9371,2,0,'/ai/crud-page/in_out_history?appId=2063913897860308994&runtimeOpenMode=LIST','ai/crud-page',0,0,NULL,'_self',0,1,1,'ai:businessApp:open:in_out_in_out_history_runtime','ionicons5:AppsOutline',NULL,NULL,0,0,NULL,NULL,1,'2026-06-14 11:36:10',1,'2026-06-22 12:43:21',2,'pc',2,0),
	 (9374,1,'设置功能代码',9217,3,46,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessApp:code',NULL,NULL,NULL,0,0,NULL,'维护访问入口代码包设置',1,'2026-06-14 21:18:18',1,'2026-06-14 21:18:18',1,'pc',2,0),
	 (9375,1,'预览功能代码',9217,3,47,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessApp:codePreview',NULL,NULL,NULL,0,0,NULL,'预览访问入口完整功能代码',1,'2026-06-14 21:18:18',1,'2026-06-14 21:18:18',1,'pc',2,0),
	 (9376,1,'下载功能代码',9217,3,48,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessApp:codeDownload',NULL,NULL,NULL,0,0,NULL,'下载访问入口完整功能代码',1,'2026-06-14 21:18:18',1,'2026-06-14 21:18:18',1,'pc',2,0),
	 (9377,1,'业务报表看板',9197,2,31,'/app-center/stats','app-center/stats-dashboard',0,0,NULL,'_self',0,1,0,'ai:businessStats:view','ionicons5:BarChartOutline',NULL,NULL,0,0,NULL,'应用中心报表看板隐藏路由',1,'2026-06-15 13:04:07',1,'2026-06-15 13:06:10',1,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9378,1,'触发器配置',9197,2,32,'/app-center/trigger','app-center/trigger',0,0,NULL,'_self',0,1,0,'ai:businessTrigger:list','ionicons5:FlashOutline',NULL,NULL,0,0,NULL,'应用中心触发器配置隐藏路由',1,'2026-06-15 13:04:07',1,'2026-06-15 13:06:10',1,'pc',2,0),
	 (9379,1,'业务运行态页面',9217,2,33,'/ai/crud-page/:configKey','ai/crud-page',0,0,NULL,'_self',0,1,0,'ai:businessRuntime:route','ionicons5:PlayCircleOutline',NULL,NULL,0,0,NULL,'应用中心通用运行态隐藏路由',1,'2026-06-15 13:04:07',1,'2026-06-15 13:06:10',1,'pc',2,0),
	 (9380,1,'访问入口跳转',9217,2,34,'/app-center/app/:appId','app-center/app-entry',0,0,NULL,'_self',0,1,0,'ai:businessApp:entryRoute','ionicons5:OpenOutline',NULL,NULL,0,0,NULL,'应用中心访问入口跳转隐藏路由',1,'2026-06-15 13:04:07',1,'2026-06-15 13:06:10',1,'pc',2,0),
	 (9381,1,'消息模板配置',48,2,35,'/message/template','message/template-list',0,0,NULL,'_self',0,1,0,'message:template:route','ionicons5:MailOutline',NULL,NULL,0,0,NULL,'引擎中心消息模板隐藏路由',1,'2026-06-15 13:04:07',1,'2026-06-15 13:06:10',1,'pc',2,0),
	 (9384,1,'查看业务就绪度',9217,3,36,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessReadiness:view',NULL,NULL,NULL,0,0,NULL,'查看业务单元就绪度权限',1,'2026-06-15 13:04:07',1,'2026-06-15 13:06:10',1,'pc',2,0),
	 (9385,1,'查看关系运行入口',9217,3,37,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessRelation:runtime',NULL,NULL,NULL,0,0,NULL,'查看业务对象关系运行入口权限',1,'2026-06-15 13:04:07',1,'2026-06-15 13:06:10',1,'pc',2,0),
	 (9386,1,'查看业务域验收',9217,3,38,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessAcceptance:view',NULL,NULL,NULL,0,0,NULL,'查看业务域验收检查权限',1,'2026-06-15 13:04:07',1,'2026-06-15 13:06:10',1,'pc',2,0),
	 (9387,1,'查看引擎运行状态',9217,3,39,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessEngine:runtime',NULL,NULL,NULL,0,0,NULL,'查看引擎运行状态汇总权限',1,'2026-06-15 13:04:07',1,'2026-06-15 13:06:10',1,'pc',2,0),
	 (9392,1,'CRM',0,1,0,'/app-center/suite-menu/CRM','',0,0,'','_self',0,1,1,'ai:business:suite-menu:CRM','ionicons5:PeopleOutline',NULL,NULL,0,1,NULL,NULL,1,'2026-06-15 17:23:04',1,'2026-07-23 09:24:06',2,'pc',2,9392),
	 (9393,1,'商机管理',9392,2,4,'/ai/crud-page/crm_opportunity?appId=1910000000000000304&runtimeOpenMode=LIST','ai/crud-page',0,0,NULL,'_self',0,1,1,'ai:businessApp:open:crm_opportunity_manage','ionicons5:TrendingUpOutline',NULL,NULL,0,0,NULL,NULL,1,'2026-06-15 17:23:04',1,'2026-07-23 09:24:06',2,'pc',2,9393);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9394,1,'客户',9392,2,0,'/ai/crud-page/crm_customer?appId=1910000000000000301&runtimeOpenMode=LIST','ai/crud-page',0,0,NULL,'_self',0,1,1,'ai:businessApp:open:crm_customer_manage','ionicons5:BusinessOutline',NULL,NULL,0,0,NULL,NULL,1,'2026-06-21 11:57:39',1,'2026-07-23 09:24:06',2,'pc',2,9394),
	 (9395,1,'订单管理',9269,2,0,'/ai/crud-page/test_datasource_order_management','ai/crud-page',0,0,NULL,'_self',0,1,1,'ai:crud:test_datasource_order_management',NULL,NULL,NULL,0,0,NULL,NULL,1,'2026-06-21 21:36:01',1,'2026-06-26 07:37:31',2,'pc',2,0),
	 (9396,1,'业务数据源调试',9194,2,6,'/business/datasource-debug','business/datasource-debug',0,0,NULL,'_self',0,1,1,'business:datasourceDebug:view','ionicons5:GitCompareOutline',NULL,NULL,0,0,NULL,'租户业务数据源路由调试页面',1,'2026-06-22 15:44:02',1,'2026-06-22 15:44:02',1,'pc',2,0),
	 (9397,1,'业务数据源调试-当前路由',9396,4,1,NULL,NULL,0,0,NULL,'_self',0,1,0,'business:datasourceDebug:current',NULL,'GET','/business/datasource-demo/current',0,0,NULL,'查询当前租户业务数据源路由',1,'2026-06-22 15:44:02',1,'2026-06-22 15:44:02',1,'pc',2,0),
	 (9398,1,'业务数据源调试-写入记录',9396,4,2,NULL,NULL,0,0,NULL,'_self',0,1,0,'business:datasourceDebug:prepare',NULL,'POST','/business/datasource-demo/prepare',0,0,NULL,'写入业务数据源调试记录',1,'2026-06-22 15:44:02',1,'2026-06-22 15:44:02',1,'pc',2,0),
	 (9399,1,'业务数据源调试-记录列表',9396,4,3,NULL,NULL,0,0,NULL,'_self',0,1,0,'business:datasourceDebug:list',NULL,'GET','/business/datasource-demo/list',0,0,NULL,'查询业务数据源调试记录',1,'2026-06-22 15:44:02',1,'2026-06-22 15:44:02',1,'pc',2,0),
	 (9400,1,'出入库历史',9371,2,0,'/app-center/app/2069251419888988161','app-center/app-entry',0,0,NULL,'_self',0,1,1,'ai:businessApp:open:inouthistory','ionicons5:AppsOutline',NULL,NULL,0,0,NULL,NULL,1,'2026-06-23 10:51:06',1,'2026-06-23 10:56:39',2,'pc',2,0),
	 (9401,1,'测试数据源',9403,1,0,'/app-center/suite-menu/TEST_DATASOURCE',NULL,0,0,NULL,'_self',0,1,1,'ai:business:suite-menu:TEST_DATASOURCE','ionicons5:AlbumsOutline',NULL,NULL,0,1,NULL,NULL,1,'2026-06-23 11:26:26',1,'2026-07-23 09:24:06',2,'pc',2,9401),
	 (9402,1,'订单管理',9403,2,0,'/ai/crud-page/test_datasource_order_management?appId=2068689395142389762&runtimeOpenMode=LIST','ai/crud-page',0,0,NULL,'_self',0,1,1,'ai:businessApp:open:test_datasource_order_management_runtime','ionicons5:AppsOutline',NULL,NULL,0,0,NULL,NULL,1,'2026-06-23 11:26:26',1,'2026-07-23 09:24:06',2,'pc',2,9402),
	 (9403,1,'资产设备系统',0,1,0,'/app-center/suite-menu/ASSET_DEVICE_SYSTEM',NULL,0,0,NULL,'_self',0,1,1,'ai:business:suite-menu:ASSET_DEVICE_SYSTEM','ionicons5:Desktop',NULL,NULL,0,1,NULL,NULL,1,'2026-06-25 16:53:37',1,'2026-07-23 09:24:06',6,'pc',2,9403);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9404,1,'资产设备维护',9403,2,0,'/ai/crud-page/asset_device_system_asset_device_create?appId=2070067878156353538&runtimeOpenMode=LIST','ai/crud-page',0,0,NULL,'_self',0,1,1,'ai:businessApp:open:tpm_asset_create','ionicons5:Build',NULL,NULL,0,0,NULL,NULL,1,'2026-06-25 16:53:37',1,'2026-07-23 09:24:06',6,'pc',2,9404),
	 (9405,1,'线索入口',9392,2,0,'/ai/crud-page/crm_lead?appId=2071021662462324737&runtimeOpenMode=LIST','ai/crud-page',0,0,NULL,'_self',0,1,1,'ai:businessApp:open:crm_xians','ionicons5:AppsOutline',NULL,NULL,0,0,NULL,NULL,1,'2026-06-28 08:03:37',1,'2026-07-23 09:24:06',6,'pc',2,9405),
	 (9406,1,'合同入口',9392,2,0,'/ai/crud-page/crm_contract?appId=2071021763234672641&runtimeOpenMode=LIST','ai/crud-page',0,0,NULL,'_self',0,1,1,'ai:businessApp:open:crm_hetong1','ionicons5:AppsOutline',NULL,NULL,0,0,NULL,NULL,1,'2026-06-28 08:04:01',1,'2026-07-23 09:24:06',6,'pc',2,9406),
	 (9409,1,'线索',9392,2,3,'/ai/crud-page/crm_lead?appId=1910000000000000303&runtimeOpenMode=LIST','ai/crud-page',0,0,NULL,'_self',0,1,1,'ai:businessApp:open:crm_lead_manage','ionicons5:SparklesOutline',NULL,NULL,0,0,NULL,NULL,1,'2026-06-28 08:05:44',1,'2026-07-23 09:24:06',6,'pc',2,9409),
	 (9410,1,'业务测试',9194,1,30,'/business',NULL,0,0,NULL,'_self',0,1,1,'business:test','ionicons5:BriefcaseOutline',NULL,NULL,0,1,NULL,'业务测试目录',1,'2026-06-28 15:07:29',1,'2026-06-28 15:07:29',1,'pc',2,0),
	 (9411,1,'采购单审批测试',9410,2,10,'/business/purchase-order-test','business/purchase-order-test',0,0,NULL,'_self',0,1,1,'business:purchaseOrderTest:list','ionicons5:CartOutline',NULL,NULL,0,0,NULL,'采购单审批流程联动测试页面',1,'2026-06-28 15:07:29',1,'2026-06-28 15:07:29',1,'pc',2,0),
	 (9412,1,'采购单查询',9411,3,1,NULL,NULL,0,0,NULL,'_self',0,1,1,'business:purchaseOrderTest:query',NULL,NULL,NULL,0,0,NULL,'采购单查询按钮权限',1,'2026-06-28 15:07:29',1,'2026-06-28 15:07:29',1,'pc',2,0),
	 (9413,1,'采购单新增',9411,3,2,NULL,NULL,0,0,NULL,'_self',0,1,1,'business:purchaseOrderTest:add',NULL,NULL,NULL,0,0,NULL,'采购单新增按钮权限',1,'2026-06-28 15:07:29',1,'2026-06-28 15:07:29',1,'pc',2,0),
	 (9414,1,'采购单修改',9411,3,3,NULL,NULL,0,0,NULL,'_self',0,1,1,'business:purchaseOrderTest:edit',NULL,NULL,NULL,0,0,NULL,'采购单修改按钮权限',1,'2026-06-28 15:07:29',1,'2026-06-28 15:07:29',1,'pc',2,0),
	 (9415,1,'采购单删除',9411,3,4,NULL,NULL,0,0,NULL,'_self',0,1,1,'business:purchaseOrderTest:remove',NULL,NULL,NULL,0,0,NULL,'采购单删除按钮权限',1,'2026-06-28 15:07:29',1,'2026-06-28 15:07:29',1,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9416,1,'采购单提交审批',9411,3,5,NULL,NULL,0,0,NULL,'_self',0,1,1,'business:purchaseOrderTest:submit',NULL,NULL,NULL,0,0,NULL,'采购单提交审批按钮权限',1,'2026-06-28 15:07:29',1,'2026-06-28 15:07:29',1,'pc',2,0),
	 (9417,1,'采购单初始化流程',9411,3,6,NULL,NULL,0,0,NULL,'_self',0,1,1,'business:purchaseOrderTest:initFlow',NULL,NULL,NULL,0,0,NULL,'采购单测试流程初始化按钮权限',1,'2026-06-28 15:07:29',1,'2026-06-28 15:07:29',1,'pc',2,0),
	 (9419,1,'连锁经营',0,1,0,'/app-center/suite-menu/LSJY',NULL,0,0,NULL,'_self',0,1,1,'ai:business:suite-menu:LSJY','ionicons5:FlowerSharp',NULL,NULL,0,1,NULL,NULL,1,'2026-06-28 19:12:39',1,'2026-07-23 09:24:06',6,'pc',2,9419),
	 (9422,1,'桌台管理',9419,2,0,'/ai/crud-page/lsjy_desk?appId=2071516567991267330&runtimeOpenMode=LIST','ai/crud-page',0,0,NULL,'_self',0,1,1,'ai:businessApp:open:desk','ionicons5:AddCircleSharp',NULL,NULL,0,0,NULL,NULL,1,'2026-06-29 16:50:11',1,'2026-07-23 09:24:06',6,'pc',2,9422),
	 (9423,1,'工单管理',0,1,0,'/app-center/suite-menu/MANAGEMENT',NULL,0,0,NULL,'_self',0,1,1,'ai:business:suite-menu:MANAGEMENT','ionicons5:AccessibilitySharp',NULL,NULL,0,1,NULL,NULL,1,'2026-06-30 08:55:11',1,'2026-07-23 09:24:06',6,'pc',2,9423),
	 (9425,1,'仓储管理',0,1,0,'/app-center/suite-menu/PURCHASE',NULL,0,0,NULL,'_self',0,0,0,'ai:business:suite-menu:PURCHASE','ionicons5:CartOutline',NULL,NULL,0,1,NULL,NULL,1,'2026-07-01 15:27:33',1,'2026-07-23 09:24:06',6,'pc',2,9425),
	 (9426,1,'物料管理',9425,2,0,'/ai/crud-page/purchase_material?appId=2072219603706064897&runtimeOpenMode=LIST','ai/crud-page',0,0,NULL,'_self',0,1,1,'ai:businessApp:open:purchase_material_runtime','ionicons5:AppsOutline',NULL,NULL,0,0,NULL,NULL,1,'2026-07-01 15:27:33',1,'2026-07-23 09:24:06',6,'pc',2,9426),
	 (9427,1,'通用业务域',9067,1,0,'/ai/lowcode-domain/general',NULL,0,0,NULL,'_self',0,1,1,'ai:lowcode:domain-menu:general','ionicons5:FolderOpenOutline',NULL,NULL,0,1,NULL,NULL,1,'2026-07-01 15:31:39',1,'2026-07-23 09:24:06',6,'pc',2,9427),
	 (9428,1,'仓库管理',9427,2,0,'/ai/crud-page/purchase_warehouse_management','ai/crud-page',0,0,NULL,'_self',0,1,1,'ai:crud:purchase_warehouse_management',NULL,NULL,NULL,0,0,NULL,NULL,1,'2026-07-01 17:04:51',1,'2026-07-23 09:24:06',6,'pc',2,9428),
	 (9429,1,'仓库管理',9425,2,0,'/ai/crud-page/purchase_warehouse_management?appId=2072245036191330305&runtimeOpenMode=LIST','ai/crud-page',0,0,NULL,'_self',0,1,1,'ai:businessApp:open:purchase_warehouse_management_runtime','ionicons5:AppsOutline',NULL,NULL,0,0,NULL,NULL,1,'2026-07-01 17:08:49',1,'2026-07-23 09:24:06',6,'pc',2,9429);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9432,1,'编码规则',9194,2,8,'/app-center/code-rules','app-center/code-rules',0,0,NULL,'_self',0,1,1,'system:codeRule:list','ionicons5:BarcodeOutline',NULL,NULL,0,0,NULL,'平台级结构化编码规则维护页面',1,'2026-07-02 16:13:52',1,'2026-07-16 20:51:34',1,'pc',2,0),
	 (9433,1,'执行业务动作',9217,3,210,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessAction:execute',NULL,NULL,NULL,0,0,NULL,'通用业务动作执行权限',1,'2026-07-03 09:10:32',1,'2026-07-03 09:10:32',1,'pc',2,0),
	 (9434,1,'查看业务动作日志',9217,3,211,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessAction:log',NULL,NULL,NULL,0,0,NULL,'通用业务动作执行日志查看权限',1,'2026-07-03 09:10:32',1,'2026-07-03 09:10:32',1,'pc',2,0),
	 (9435,1,'执行数量台账动作',9217,3,212,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessQuantity:operate',NULL,NULL,NULL,0,0,NULL,'通用数量台账动作权限',1,'2026-07-03 09:10:34',1,'2026-07-03 09:10:34',1,'pc',2,0),
	 (9436,1,'查看数量台账',9217,3,213,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessQuantity:view',NULL,NULL,NULL,0,0,NULL,'通用数量台账只读查询权限',1,'2026-07-03 11:14:56',1,'2026-07-03 11:14:56',1,'pc',2,0),
	 (9437,1,'采购仓储',9067,1,30,'/ai/lowcode-domain/PROCUREMENT_WAREHOUSE',NULL,0,0,NULL,'_self',0,1,1,'ai:lowcode:domain-menu:PROCUREMENT_WAREHOUSE','ionicons5:FolderOpenOutline',NULL,NULL,0,1,NULL,NULL,1,'2026-07-03 22:17:51',1,'2026-07-23 09:24:06',6,'pc',2,9437),
	 (9438,1,'物料',9437,2,10,'/ai/crud-page/pw_material','ai/crud-page',0,0,NULL,'_self',0,1,1,'ai:crud:pw_material',NULL,NULL,NULL,0,0,NULL,NULL,1,'2026-07-03 22:17:51',1,'2026-07-23 09:24:06',6,'pc',2,9438),
	 (9439,1,'仓库',9437,2,30,'/ai/crud-page/pw_warehouse','ai/crud-page',0,0,NULL,'_self',0,1,1,'ai:crud:pw_warehouse',NULL,NULL,NULL,0,0,NULL,NULL,1,'2026-07-03 22:19:31',1,'2026-07-23 09:24:06',6,'pc',2,9439),
	 (9440,1,'采购仓储',0,1,0,'/app-center/suite-menu/PROCUREMENT_WAREHOUSE',NULL,0,0,NULL,'_self',0,1,1,'ai:business:suite-menu:PROCUREMENT_WAREHOUSE','ionicons5:CubeOutline',NULL,NULL,0,1,NULL,NULL,1,'2026-07-03 22:25:06',1,'2026-07-23 09:24:06',6,'pc',2,9440),
	 (9441,1,'出库管理',9440,2,50,'/ai/crud-page/pw_outbound_order?appId=1910000000000000945&runtimeOpenMode=LIST','ai/crud-page',0,0,NULL,'_self',0,1,1,'ai:businessApp:open:pw_outbound_manage','ionicons5:ExitOutline',NULL,NULL,0,0,NULL,NULL,1,'2026-07-03 22:25:06',1,'2026-07-23 09:24:06',6,'pc',2,9441);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9442,1,'采购单',9440,2,40,'/ai/crud-page/pw_purchase_order?appId=1910000000000000944&runtimeOpenMode=LIST','ai/crud-page',0,0,NULL,'_self',0,1,1,'ai:businessApp:open:pw_purchase_manage','ionicons5:CartOutline',NULL,NULL,0,0,NULL,NULL,1,'2026-07-03 22:25:14',1,'2026-07-23 09:24:06',6,'pc',2,9442),
	 (9443,1,'仓库',9440,2,30,'/ai/crud-page/pw_warehouse?appId=1910000000000000943&runtimeOpenMode=LIST','ai/crud-page',0,0,NULL,'_self',0,1,1,'ai:businessApp:open:pw_warehouse_manage','ionicons5:HomeOutline',NULL,NULL,0,0,NULL,NULL,1,'2026-07-03 22:25:23',1,'2026-07-23 09:24:06',6,'pc',2,9443),
	 (9444,1,'供应商管理',9440,2,20,'/ai/crud-page/pw_supplier?appId=1910000000000000942&runtimeOpenMode=LIST','ai/crud-page',0,0,NULL,'_self',0,1,1,'ai:businessApp:open:pw_supplier_manage','ionicons5:PeopleOutline',NULL,NULL,0,0,NULL,NULL,1,'2026-07-03 22:25:29',1,'2026-07-23 09:24:06',6,'pc',2,9444),
	 (9445,1,'物料',9440,2,10,'/ai/crud-page/pw_material?appId=1910000000000000941&runtimeOpenMode=LIST','ai/crud-page',0,1,'app','_self',0,1,1,'ai:businessApp:open:pw_material_manage','ionicons5:CubeOutline',NULL,NULL,0,0,NULL,NULL,1,'2026-07-03 22:25:34',1,'2026-07-23 09:24:06',6,'pc',2,9445),
	 (9446,1,'出库明细',9437,2,80,'/ai/crud-page/pw_outbound_order_item','ai/crud-page',0,0,NULL,'_self',0,1,1,'ai:crud:pw_outbound_order_item',NULL,NULL,NULL,0,0,NULL,NULL,1,'2026-07-03 22:33:43',1,'2026-07-23 09:24:06',6,'pc',2,9446),
	 (9447,1,'供应商',9437,2,20,'/ai/crud-page/pw_supplier','ai/crud-page',0,0,NULL,'_self',0,1,1,'ai:crud:pw_supplier',NULL,NULL,NULL,0,0,NULL,NULL,1,'2026-07-03 23:03:41',1,'2026-07-23 09:24:06',6,'pc',2,9447),
	 (9448,1,'采购单',9437,2,40,'/ai/crud-page/pw_purchase_order','ai/crud-page',0,0,NULL,'_self',0,1,1,'ai:crud:pw_purchase_order',NULL,NULL,NULL,0,0,NULL,NULL,1,'2026-07-03 23:04:35',1,'2026-07-23 09:24:06',6,'pc',2,9448),
	 (9449,1,'供应商报价',9437,2,25,'/ai/crud-page/pw_supplier_material','ai/crud-page',0,0,NULL,'_self',0,1,1,'ai:crud:pw_supplier_material',NULL,NULL,NULL,0,0,NULL,NULL,1,'2026-07-04 09:21:02',1,'2026-07-23 09:24:06',6,'pc',2,9449),
	 (9450,1,'采购明细',9437,2,60,'/ai/crud-page/pw_purchase_order_item','ai/crud-page',0,0,NULL,'_self',0,1,1,'ai:crud:pw_purchase_order_item',NULL,NULL,NULL,0,0,NULL,NULL,1,'2026-07-04 14:00:26',1,'2026-07-23 09:24:06',6,'pc',2,9450),
	 (9451,1,'审计日志查询',57,3,1,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:operationLog:query',NULL,NULL,NULL,0,0,NULL,'查询页面操作审计日志',1,'2026-07-08 08:56:39',1,'2026-07-08 08:56:39',1,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9452,1,'审计日志详情',57,3,2,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:operationLog:detail',NULL,NULL,NULL,0,0,NULL,'查看页面操作审计详情',1,'2026-07-08 08:56:39',1,'2026-07-08 08:56:39',1,'pc',2,0),
	 (9453,1,'审计日志导出',57,3,3,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:operationLog:export',NULL,NULL,NULL,0,0,NULL,'导出页面操作审计日志',1,'2026-07-08 08:56:39',1,'2026-07-08 08:56:39',1,'pc',2,0),
	 (9454,1,'审计日志分页接口',57,4,11,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:operationLog:api:page',NULL,'GET','/system/operationLog/page',0,0,NULL,'页面操作审计日志分页接口',1,'2026-07-08 08:56:39',1,'2026-07-08 08:56:39',1,'pc',2,0),
	 (9455,1,'审计日志详情接口',57,4,12,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:operationLog:api:detail',NULL,'GET','/system/operationLog/{id}',0,0,NULL,'页面操作审计日志详情接口',1,'2026-07-08 08:56:39',1,'2026-07-08 08:56:39',1,'pc',2,0),
	 (9456,1,'审计日志导出接口',57,4,13,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:operationLog:api:export',NULL,'POST','/api/excel/export/sys_operation_log_export',0,0,NULL,'页面操作审计日志导出接口',1,'2026-07-08 08:56:39',1,'2026-07-08 08:56:39',1,'pc',2,0),
	 (9457,1,'模型治理',9067,2,3,'/ai/model-routing','ai/model-routing',0,0,NULL,'_self',0,1,1,'ai:model-routing:list','ionicons5:GitNetworkOutline',NULL,NULL,1,0,NULL,'AI模型路由策略与调用记录',1,'2026-07-11 19:21:23',1,'2026-07-11 19:21:23',1,'pc',2,0),
	 (9458,1,'维护路由策略',9457,3,1,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:model-routing:edit',NULL,NULL,NULL,0,0,NULL,'新增、编辑和删除路由策略',1,'2026-07-11 19:21:23',1,'2026-07-11 19:21:23',1,'pc',2,0),
	 (9459,1,'预览模型路由',9457,3,2,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:model-routing:preview',NULL,NULL,NULL,0,0,NULL,'只读预览路由决策',1,'2026-07-11 19:21:23',1,'2026-07-11 19:21:23',1,'pc',2,0),
	 (9460,1,'查看调用记录',9457,3,3,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:model-invocation:list',NULL,NULL,NULL,0,0,NULL,'查看安全调用治理记录',1,'2026-07-11 19:21:23',1,'2026-07-11 19:21:23',1,'pc',2,0),
	 (9461,1,'测试模型连接',9457,3,4,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:model:test',NULL,NULL,NULL,0,0,NULL,'手动测试单个模型连接',1,'2026-07-11 19:21:23',1,'2026-07-11 19:21:23',1,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9465,1,'查询能力目录',9067,3,20,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:capability:query',NULL,NULL,NULL,0,0,NULL,'查询AI中枢能力目录',1,'2026-07-12 11:13:01',1,'2026-07-12 11:13:01',1,'pc',2,0),
	 (9466,1,'发布停用能力',9067,3,21,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:capability:publish',NULL,NULL,NULL,0,0,NULL,'发布或停用AI中枢能力',1,'2026-07-12 11:13:01',1,'2026-07-12 11:13:01',1,'pc',2,0),
	 (9467,1,'查询机器客户端',9067,3,22,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:capability:client:query',NULL,NULL,NULL,0,0,NULL,'查询AI中枢机器客户端',1,'2026-07-12 11:13:01',1,'2026-07-12 11:13:01',1,'pc',2,0),
	 (9468,1,'创建机器客户端',9067,3,23,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:capability:client:add',NULL,NULL,NULL,0,0,NULL,'创建机器客户端并一次性展示密钥',1,'2026-07-12 11:13:01',1,'2026-07-12 11:13:01',1,'pc',2,0),
	 (9469,1,'轮换客户端密钥',9067,3,24,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:capability:client:rotate',NULL,NULL,NULL,0,0,NULL,'轮换机器客户端密钥',1,'2026-07-12 11:13:01',1,'2026-07-12 11:13:01',1,'pc',2,0),
	 (9470,1,'吊销机器客户端',9067,3,25,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:capability:client:revoke',NULL,NULL,NULL,0,0,NULL,'吊销机器客户端',1,'2026-07-12 11:13:01',1,'2026-07-12 11:13:01',1,'pc',2,0),
	 (9471,1,'查询能力授权',9067,3,26,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:capability:grant:query',NULL,NULL,NULL,0,0,NULL,'查询客户端能力授权',1,'2026-07-12 11:13:01',1,'2026-07-12 11:13:01',1,'pc',2,0),
	 (9472,1,'新增能力授权',9067,3,27,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:capability:grant:add',NULL,NULL,NULL,0,0,NULL,'新增客户端能力授权',1,'2026-07-12 11:13:01',1,'2026-07-12 11:13:01',1,'pc',2,0),
	 (9473,1,'撤销能力授权',9067,3,28,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:capability:grant:revoke',NULL,NULL,NULL,0,0,NULL,'撤销客户端能力授权',1,'2026-07-12 11:13:01',1,'2026-07-12 11:13:01',1,'pc',2,0),
	 (9474,1,'查询能力调用日志',9067,3,29,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:capability:invocation:query',NULL,NULL,NULL,0,0,NULL,'查询安全能力调用日志',1,'2026-07-12 11:13:01',1,'2026-07-12 11:13:01',1,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9480,1,'发布受控业务动作',9067,3,30,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:capability:business-action:publish',NULL,NULL,NULL,0,0,NULL,'从业务对象发布快照创建受控AI动作能力',1,'2026-07-12 20:13:49',1,'2026-07-12 20:13:49',1,'pc',2,0),
	 (9481,1,'调用受控业务动作',9067,3,31,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:capability:business-action:invoke',NULL,NULL,NULL,0,0,NULL,'允许具体用户通过MCP确认后调用受控业务动作',1,'2026-07-12 20:13:49',1,'2026-07-12 20:13:49',1,'pc',2,0),
	 (9483,1,'发布受控流程动作',9067,3,32,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:capability:flow-action:publish',NULL,NULL,NULL,0,0,NULL,'从已发布业务对象与真实流程绑定创建受控流程能力',1,'2026-07-12 20:13:49',1,'2026-07-12 20:13:49',1,'pc',2,0),
	 (9484,1,'调用受控流程动作',9067,3,33,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:capability:flow-action:invoke',NULL,NULL,NULL,0,0,NULL,'允许具体用户通过MCP确认后发起或办理流程',1,'2026-07-12 20:13:49',1,'2026-07-12 20:13:49',1,'pc',2,0),
	 (9486,1,'发布高风险动作',9067,3,34,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:capability:high-risk:publish',NULL,NULL,NULL,0,0,NULL,'发布HIGH业务动作与人工审批策略',1,'2026-07-12 20:13:51',1,'2026-07-12 20:13:51',1,'pc',2,0),
	 (9487,1,'提交高风险审批',9067,3,35,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:capability:approval:submit',NULL,NULL,NULL,0,0,NULL,'可信USER提交高风险人工审批',1,'2026-07-12 20:13:51',1,'2026-07-12 20:13:51',1,'pc',2,0),
	 (9488,1,'查询高风险审批',9067,3,36,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:capability:approval:query',NULL,NULL,NULL,0,0,NULL,'查询本人客户端审批状态',1,'2026-07-12 20:13:51',1,'2026-07-12 20:13:51',1,'pc',2,0),
	 (9489,1,'采购仓储',9067,1,30,'/ai/lowcode-domain/PROCUREMENT_WAREHOUSE',NULL,0,0,NULL,'_self',0,1,1,'ai:lowcode:domain-menu:PROCUREMENT_WAREHOUSE','ionicons5:FolderOpenOutline',NULL,NULL,0,1,NULL,NULL,1,'2026-07-13 18:08:35',1,'2026-07-23 09:24:06',6,'pc',2,9489),
	 (9490,1,'产品',9489,2,0,'/ai/crud-page/procurement_warehouse_product','ai/crud-page',0,0,NULL,'_self',0,1,1,'ai:crud:procurement_warehouse_product',NULL,NULL,NULL,0,0,NULL,NULL,1,'2026-07-13 18:08:35',1,'2026-07-23 09:24:06',6,'pc',2,9490),
	 (9491,1,'查看业务应用',9217,3,51,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessApplication:list',NULL,NULL,NULL,0,0,NULL,'查询业务应用聚合',1,'2026-07-14 06:48:55',1,'2026-07-14 06:48:55',1,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9492,1,'新增业务应用',9217,3,52,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessApplication:add',NULL,NULL,NULL,0,0,NULL,'新增业务应用聚合',1,'2026-07-14 06:48:55',1,'2026-07-14 06:48:55',1,'pc',2,0),
	 (9493,1,'编辑业务应用',9217,3,53,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessApplication:edit',NULL,NULL,NULL,0,0,NULL,'编辑业务应用和对象编排',1,'2026-07-14 06:48:55',1,'2026-07-14 06:48:55',1,'pc',2,0),
	 (9494,1,'启停业务应用',9217,3,54,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessApplication:status',NULL,NULL,NULL,0,0,NULL,'启停业务应用聚合',1,'2026-07-14 06:48:55',1,'2026-07-14 06:48:55',1,'pc',2,0),
	 (9495,1,'删除业务应用',9217,3,55,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessApplication:delete',NULL,NULL,NULL,0,0,NULL,'逻辑删除业务应用聚合',1,'2026-07-14 06:48:55',1,'2026-07-14 06:48:55',1,'pc',2,0),
	 (9498,1,'查看业务扩展',9217,3,61,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessExtension:list',NULL,NULL,NULL,0,0,NULL,'查询业务扩展和白名单目录',1,'2026-07-14 06:48:57',1,'2026-07-14 06:48:57',1,'pc',2,0),
	 (9499,1,'新增业务扩展',9217,3,62,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessExtension:add',NULL,NULL,NULL,0,0,NULL,'新增业务扩展草稿',1,'2026-07-14 06:48:57',1,'2026-07-14 06:48:57',1,'pc',2,0),
	 (9500,1,'编辑业务扩展',9217,3,63,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessExtension:edit',NULL,NULL,NULL,0,0,NULL,'编辑扩展元数据、内容和编辑锁',1,'2026-07-14 06:48:57',1,'2026-07-14 06:48:57',1,'pc',2,0),
	 (9501,1,'删除业务扩展',9217,3,64,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessExtension:delete',NULL,NULL,NULL,0,0,NULL,'逻辑删除停用扩展',1,'2026-07-14 06:48:57',1,'2026-07-14 06:48:57',1,'pc',2,0),
	 (9502,1,'校验业务扩展',9217,3,65,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessExtension:validate',NULL,NULL,NULL,0,0,NULL,'执行扩展安全校验',1,'2026-07-14 06:48:57',1,'2026-07-14 06:48:57',1,'pc',2,0),
	 (9503,1,'测试业务扩展',9217,3,66,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessExtension:test',NULL,NULL,NULL,0,0,NULL,'执行受限扩展测试',1,'2026-07-14 06:48:57',1,'2026-07-14 06:48:57',1,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9504,1,'启停业务扩展',9217,3,67,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessExtension:status',NULL,NULL,NULL,0,0,NULL,'启用已测试版本或停用扩展',1,'2026-07-14 06:48:57',1,'2026-07-14 06:48:57',1,'pc',2,0),
	 (9505,1,'回滚业务扩展',9217,3,68,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessExtension:rollback',NULL,NULL,NULL,0,0,NULL,'从历史版本生成新草稿',1,'2026-07-14 06:48:57',1,'2026-07-14 06:48:57',1,'pc',2,0),
	 (9513,1,'发布业务应用',9217,3,71,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessApplication:publish',NULL,NULL,NULL,0,0,NULL,'执行应用就绪度检查和协调发布',1,'2026-07-14 06:48:57',1,'2026-07-14 06:48:57',1,'pc',2,0),
	 (9514,1,'恢复应用发布',9217,3,72,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessApplication:recover',NULL,NULL,NULL,0,0,NULL,'恢复部分失败的协调发布运行单',1,'2026-07-14 06:48:57',1,'2026-07-14 06:48:57',1,'pc',2,0),
	 (9515,1,'回滚业务应用',9217,3,73,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessApplication:rollback',NULL,NULL,NULL,0,0,NULL,'从历史应用快照生成回滚发布版本',1,'2026-07-14 06:48:57',1,'2026-07-14 06:48:57',1,'pc',2,0),
	 (9516,1,'应用工作台',9194,2,90,'/app-center/application/:applicationCode','app-center/application.[applicationCode]',0,0,NULL,'_self',0,1,0,'ai:businessApplication:list','ionicons5:AppsOutline',NULL,NULL,0,0,NULL,'业务应用聚合工作台隐藏路由，仅进入授权树，不显示在侧边菜单',1,'2026-07-14 07:07:16',1,'2026-07-14 07:07:16',1,'pc',2,0),
	 (9517,1,'通用业务域',9067,1,0,'/ai/lowcode-domain/general',NULL,0,0,NULL,'_self',0,1,1,'ai:lowcode:domain-menu:general','ionicons5:FolderOpenOutline',NULL,NULL,0,1,NULL,NULL,1,'2026-07-14 11:08:11',1,'2026-07-23 09:24:06',6,'pc',2,9517),
	 (9518,1,'采购管理',9517,2,0,'/ai/crud-page/purchase_procurement','ai/crud-page',0,0,NULL,'_self',0,1,1,'ai:crud:purchase_procurement',NULL,NULL,NULL,0,0,NULL,NULL,1,'2026-07-14 11:08:11',1,'2026-07-23 09:24:06',6,'pc',2,9518),
	 (9519,1,'订单基本信息',9489,2,0,'/ai/crud-page/procurement_warehouse_tf_f_order','ai/crud-page',0,0,NULL,'_self',0,1,1,'ai:crud:procurement_warehouse_tf_f_order',NULL,NULL,NULL,0,0,NULL,NULL,1,'2026-07-14 20:23:47',1,'2026-07-23 09:24:06',6,'pc',2,9519),
	 (9520,1,'测试订单管理',9440,2,0,'/ai/crud-page/procurement_warehouse_tf_f_order?appId=2077006427452342274&runtimeOpenMode=LIST','ai/crud-page',0,0,NULL,'_self',0,1,1,'ai:businessApp:open:ordertest','ionicons5:AlbumsOutline',NULL,NULL,0,0,NULL,NULL,1,'2026-07-14 20:24:56',1,'2026-07-23 09:24:06',6,'pc',2,9520);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9521,1,'销售订单',9489,2,0,'/ai/crud-page/procurement_warehouse_tf_f_order_sales_order','ai/crud-page',0,0,NULL,'_self',0,1,1,'ai:crud:procurement_warehouse_tf_f_order_sales_order',NULL,NULL,NULL,0,0,NULL,NULL,1,'2026-07-14 22:03:58',1,'2026-07-23 09:24:06',6,'pc',2,9521),
	 (9523,1,'应用草稿预览',9516,2,91,'/app-center/application/:applicationCode/preview','app-center/application-preview.[applicationCode]',0,0,NULL,'_self',0,1,0,'ai:businessApplication:preview','ionicons5:EyeOutline',NULL,NULL,0,0,NULL,'不依赖页面入口的应用对象草稿预览隐藏路由',1,'2026-07-15 06:25:10',1,'2026-07-15 06:25:10',1,'pc',2,0),
	 (9524,1,'配置应用代码包',9217,3,81,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessApplication:code',NULL,NULL,NULL,0,0,NULL,'查看和保存应用代码包设置',1,'2026-07-15 14:52:06',1,'2026-07-15 14:52:06',1,'pc',2,0),
	 (9525,1,'预览应用代码',9217,3,82,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessApplication:codePreview',NULL,NULL,NULL,0,0,NULL,'预览应用完整前后端代码',1,'2026-07-15 14:52:06',1,'2026-07-15 14:52:06',1,'pc',2,0),
	 (9526,1,'下载应用代码',9217,3,83,NULL,NULL,0,0,NULL,'_self',0,1,1,'ai:businessApplication:codeDownload',NULL,NULL,NULL,0,0,NULL,'批量下载应用完整代码包',1,'2026-07-15 14:52:06',1,'2026-07-15 14:52:06',1,'pc',2,0),
	 (9527,1,'新增编码规则',9432,3,1,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:codeRule:add',NULL,NULL,NULL,0,0,NULL,'新增结构化编码规则',1,'2026-07-16 20:51:34',1,'2026-07-16 20:51:34',1,'pc',2,0),
	 (9528,1,'修改编码规则',9432,3,2,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:codeRule:edit',NULL,NULL,NULL,0,0,NULL,'修改结构化编码规则及分段',1,'2026-07-16 20:51:34',1,'2026-07-16 20:51:34',1,'pc',2,0),
	 (9529,1,'删除编码规则',9432,3,3,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:codeRule:remove',NULL,NULL,NULL,0,0,NULL,'逻辑删除自定义编码规则',1,'2026-07-16 20:51:34',1,'2026-07-16 20:51:34',1,'pc',2,0),
	 (9530,1,'使用编码规则',9432,3,4,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:codeRule:use',NULL,NULL,NULL,0,0,NULL,'预览和生成业务编码',1,'2026-07-16 20:51:34',1,'2026-07-16 20:51:34',1,'pc',2,0),
	 (9534,1,'编码规则分页接口',9432,4,11,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:codeRule:api:page',NULL,'GET','/system/code-rule/page',0,0,NULL,'分页查询编码规则',1,'2026-07-16 20:51:34',1,'2026-07-16 20:51:34',1,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9535,1,'编码规则列表接口',9432,4,12,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:codeRule:api:list',NULL,'GET','/system/code-rule/list',0,0,NULL,'查询可选择编码规则',1,'2026-07-16 20:51:34',1,'2026-07-16 20:51:34',1,'pc',2,0),
	 (9536,1,'编码规则详情接口',9432,4,13,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:codeRule:api:detail',NULL,'POST','/system/code-rule/getById',0,0,NULL,'查询编码规则及结构化分段',1,'2026-07-16 20:51:34',1,'2026-07-16 20:51:34',1,'pc',2,0),
	 (9537,1,'编码规则新增接口',9432,4,14,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:codeRule:api:add',NULL,'POST','/system/code-rule/add',0,0,NULL,'新增编码规则及分段',1,'2026-07-16 20:51:34',1,'2026-07-16 20:51:34',1,'pc',2,0),
	 (9538,1,'编码规则修改接口',9432,4,15,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:codeRule:api:edit',NULL,'POST','/system/code-rule/edit',0,0,NULL,'修改编码规则及分段',1,'2026-07-16 20:51:34',1,'2026-07-16 20:51:34',1,'pc',2,0),
	 (9539,1,'编码规则删除接口',9432,4,16,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:codeRule:api:remove',NULL,'POST','/system/code-rule/remove/*',0,0,NULL,'逻辑删除自定义编码规则',1,'2026-07-16 20:51:34',1,'2026-07-16 20:51:34',1,'pc',2,0),
	 (9540,1,'编码规则状态接口',9432,4,17,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:codeRule:api:status',NULL,'POST','/system/code-rule/status',0,0,NULL,'启停编码规则',1,'2026-07-16 20:51:34',1,'2026-07-16 20:51:34',1,'pc',2,0),
	 (9541,1,'编码规则预览接口',9432,4,18,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:codeRule:api:preview',NULL,'POST','/system/code-rule/preview',0,0,NULL,'无副作用预览编码规则',1,'2026-07-16 20:51:34',1,'2026-07-16 20:51:34',1,'pc',2,0),
	 (9542,1,'编码规则生成接口',9432,4,19,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:codeRule:api:generate',NULL,'POST','/system/code-rule/generate',0,0,NULL,'生成真实业务编码',1,'2026-07-16 20:51:34',1,'2026-07-16 20:51:34',1,'pc',2,0),
	 (9543,1,'编码规则能力接口',9432,4,20,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:codeRule:api:capabilities',NULL,'GET','/system/code-rule/capabilities',0,0,NULL,'查询结构化规则可执行能力',1,'2026-07-16 20:51:34',1,'2026-07-16 20:51:34',1,'pc',2,0),
	 (9549,1,'演示系统',0,1,0,'/app-center/suite-menu/TTT',NULL,0,0,NULL,'_self',0,1,1,'ai:business:suite-menu:TTT','ionicons5:ArrowUp',NULL,NULL,0,1,NULL,NULL,1,'2026-07-17 14:35:07',1,'2026-07-23 09:24:06',6,'pc',2,9549);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9550,1,'采购仓储',0,1,0,'/app-center/suite-menu/PROCUREMENT_WAREHOUSE',NULL,0,0,NULL,'_self',0,1,1,'ai:business:suite-menu:PROCUREMENT_WAREHOUSE','ionicons5:CubeOutline',NULL,NULL,0,1,NULL,NULL,1,'2026-07-21 13:12:24',1,'2026-07-21 13:12:24',6,'pc',2,0),
	 (9551,1,'资产设备系统',0,1,0,'/app-center/suite-menu/ASSET_DEVICE_SYSTEM',NULL,0,0,NULL,'_self',0,1,1,'ai:business:suite-menu:ASSET_DEVICE_SYSTEM','ionicons5:Desktop',NULL,NULL,0,1,NULL,NULL,1,'2026-07-21 13:13:55',1,'2026-07-21 13:13:55',6,'pc',2,0),
	 (9552,1,'新建定时任务',55,2,90,'/system/job-config/editor','system/job-config.editor',0,0,NULL,'_self',0,1,0,NULL,'ionicons5:AddCircleOutline',NULL,NULL,0,0,NULL,'定时任务全屏新建工作台隐藏路由',1,'2026-07-21 16:02:55',1,'2026-07-21 16:02:55',1,'pc',2,0),
	 (9553,1,'编辑定时任务',55,2,91,'/system/job-config/editor/:id','system/job-config.editor.[id]',0,0,NULL,'_self',0,1,0,NULL,'ionicons5:CreateOutline',NULL,NULL,0,0,NULL,'定时任务全屏编辑工作台隐藏路由',1,'2026-07-21 16:02:56',1,'2026-07-21 16:02:56',1,'pc',2,0),
	 (9554,1,'查看任务配置',55,3,1,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobConfig:list',NULL,NULL,NULL,0,0,NULL,'查询任务配置、执行目录和监控摘要',1,'2026-07-21 16:03:02',1,'2026-07-21 16:03:02',1,'pc',2,0),
	 (9555,1,'新增任务配置',55,3,2,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobConfig:add',NULL,NULL,NULL,0,0,NULL,'新增定时任务配置',1,'2026-07-21 16:03:02',1,'2026-07-21 16:03:02',1,'pc',2,0),
	 (9556,1,'编辑任务配置',55,3,3,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobConfig:edit',NULL,NULL,NULL,0,0,NULL,'编辑任务配置和执行计划',1,'2026-07-21 16:03:02',1,'2026-07-21 16:03:02',1,'pc',2,0),
	 (9557,1,'删除任务配置',55,3,4,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobConfig:remove',NULL,NULL,NULL,0,0,NULL,'逻辑删除定时任务配置',1,'2026-07-21 16:03:02',1,'2026-07-21 16:03:02',1,'pc',2,0),
	 (9558,1,'启用任务',55,3,5,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobConfig:start',NULL,NULL,NULL,0,0,NULL,'启用并同步定时任务',1,'2026-07-21 16:03:02',1,'2026-07-21 16:03:02',1,'pc',2,0),
	 (9559,1,'停用任务',55,3,6,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobConfig:stop',NULL,NULL,NULL,0,0,NULL,'停用并同步定时任务',1,'2026-07-21 16:03:02',1,'2026-07-21 16:03:02',1,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9560,1,'立即运行任务',55,3,7,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobConfig:trigger',NULL,NULL,NULL,0,0,NULL,'立即触发一次任务执行',1,'2026-07-21 16:03:02',1,'2026-07-21 16:03:02',1,'pc',2,0),
	 (9561,1,'重新同步任务',55,3,8,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobConfig:sync',NULL,NULL,NULL,0,0,NULL,'重试数据库与调度器同步',1,'2026-07-21 16:03:02',1,'2026-07-21 16:03:02',1,'pc',2,0),
	 (9562,1,'管理危险执行目标',55,3,9,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobConfig:dangerous',NULL,NULL,NULL,0,0,NULL,'管理BEAN、RPC和受保护任务',1,'2026-07-21 16:03:02',1,'2026-07-21 16:03:02',1,'pc',2,0),
	 (9563,1,'查看任务日志',55,3,20,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobLog:list',NULL,NULL,NULL,0,0,NULL,'查询任务运行日志',1,'2026-07-21 16:03:02',1,'2026-07-21 16:03:02',1,'pc',2,0),
	 (9564,1,'查看敏感日志详情',55,3,21,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobLog:detail',NULL,NULL,NULL,0,0,NULL,'查看经过裁剪的结果和异常摘要',1,'2026-07-21 16:03:02',1,'2026-07-21 16:03:02',1,'pc',2,0),
	 (9565,1,'导出任务日志',55,3,22,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobLog:export',NULL,NULL,NULL,0,0,NULL,'导出任务运行日志安全字段',1,'2026-07-21 16:03:02',1,'2026-07-21 16:03:02',1,'pc',2,0),
	 (9566,1,'清理任务日志',55,3,23,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobLog:clean',NULL,NULL,NULL,0,0,NULL,'物理清理超期终态日志',1,'2026-07-21 16:03:02',1,'2026-07-21 16:03:02',1,'pc',2,0),
	 (9569,1,'任务配置查询接口',55,4,101,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobConfig:api:read',NULL,'GET','/job/config/**',0,0,NULL,'任务配置、目录、计划和概览查询接口',1,'2026-07-21 16:03:02',1,'2026-07-21 16:03:02',1,'pc',2,0),
	 (9570,1,'任务配置新增接口',55,4,102,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobConfig:api:add',NULL,'POST','/job/config',0,0,NULL,'新增任务配置接口',1,'2026-07-21 16:03:02',1,'2026-07-21 16:03:02',1,'pc',2,0),
	 (9571,1,'任务配置编辑接口',55,4,103,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobConfig:api:edit',NULL,'PUT','/job/config',0,0,NULL,'编辑任务配置接口',1,'2026-07-21 16:03:02',1,'2026-07-21 16:03:02',1,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9572,1,'任务配置删除接口',55,4,104,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobConfig:api:remove',NULL,'DELETE','/job/config/*',0,0,NULL,'删除任务配置接口',1,'2026-07-21 16:03:02',1,'2026-07-21 16:03:02',1,'pc',2,0),
	 (9573,1,'任务启用接口',55,4,105,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobConfig:api:start',NULL,'POST','/job/config/*/start',0,0,NULL,'启用任务接口',1,'2026-07-21 16:03:02',1,'2026-07-21 16:03:02',1,'pc',2,0),
	 (9574,1,'任务停用接口',55,4,106,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobConfig:api:stop',NULL,'POST','/job/config/*/stop',0,0,NULL,'停用任务接口',1,'2026-07-21 16:03:02',1,'2026-07-21 16:03:02',1,'pc',2,0),
	 (9575,1,'任务立即运行接口',55,4,107,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobConfig:api:trigger',NULL,'POST','/job/config/*/trigger',0,0,NULL,'立即运行任务接口',1,'2026-07-21 16:03:02',1,'2026-07-21 16:03:02',1,'pc',2,0),
	 (9576,1,'任务同步接口',55,4,108,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobConfig:api:sync',NULL,'POST','/job/config/*/sync',0,0,NULL,'重新同步任务接口',1,'2026-07-21 16:03:02',1,'2026-07-21 16:03:02',1,'pc',2,0),
	 (9577,1,'任务执行计划编辑接口',55,4,109,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobConfig:api:cron',NULL,'POST','/job/config/*/cron',0,0,NULL,'编辑任务Cron接口',1,'2026-07-21 16:03:02',1,'2026-07-21 16:03:02',1,'pc',2,0),
	 (9578,1,'任务Cron预览接口',55,4,110,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobConfig:api:preview',NULL,'POST','/job/config/cron/preview',0,0,NULL,'Cron计划预览接口',1,'2026-07-21 16:03:02',1,'2026-07-21 16:03:02',1,'pc',2,0),
	 (9579,1,'任务监控摘要接口',55,4,111,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobConfig:api:monitor',NULL,'GET','/job/monitor/summary',0,0,NULL,'任务监控摘要接口',1,'2026-07-21 16:03:02',1,'2026-07-21 16:03:02',1,'pc',2,0),
	 (9580,1,'任务日志查询接口',55,4,120,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobLog:api:list',NULL,'GET','/job/log/page',0,0,NULL,'任务日志分页接口',1,'2026-07-21 16:03:02',1,'2026-07-21 16:03:02',1,'pc',2,0),
	 (9581,1,'任务日志详情接口',55,4,121,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobLog:api:detail',NULL,'GET','/job/log/*',0,0,NULL,'任务日志敏感详情接口',1,'2026-07-21 16:03:02',1,'2026-07-21 16:03:02',1,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9582,1,'任务日志导出接口',55,4,122,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobLog:api:export',NULL,'POST','/job/log/export',0,0,NULL,'任务日志安全导出接口',1,'2026-07-21 16:03:02',1,'2026-07-21 16:03:02',1,'pc',2,0),
	 (9583,1,'任务日志清理接口',55,4,123,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobLog:api:clean',NULL,'DELETE','/job/log/clean',0,0,NULL,'任务日志留存清理接口',1,'2026-07-21 16:03:02',1,'2026-07-21 16:03:02',1,'pc',2,0),
	 (9584,1,'开放API服务账号',55,2,92,'/system/job-api-token','system/job-api-token',0,0,NULL,'_self',0,1,0,NULL,'ionicons5:KeyOutline',NULL,NULL,0,0,NULL,'定时任务开放API服务账号隐藏路由',1,'2026-07-21 16:03:03',1,'2026-07-21 16:03:03',1,'pc',2,0),
	 (9585,1,'查看开放API服务账号',9584,3,1,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobApiToken:list',NULL,NULL,NULL,0,0,NULL,'查看服务账号Token列表和资源选项',1,'2026-07-21 16:03:03',1,'2026-07-21 16:03:03',1,'pc',2,0),
	 (9586,1,'创建开放API服务账号',9584,3,2,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobApiToken:add',NULL,NULL,NULL,0,0,NULL,'创建服务账号Token',1,'2026-07-21 16:03:03',1,'2026-07-21 16:03:03',1,'pc',2,0),
	 (9587,1,'吊销开放API服务账号',9584,3,3,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobApiToken:revoke',NULL,NULL,NULL,0,0,NULL,'吊销服务账号Token',1,'2026-07-21 16:03:03',1,'2026-07-21 16:03:03',1,'pc',2,0),
	 (9588,1,'轮换开放API服务账号',9584,3,4,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobApiToken:rotate',NULL,NULL,NULL,0,0,NULL,'轮换服务账号Token',1,'2026-07-21 16:03:03',1,'2026-07-21 16:03:03',1,'pc',2,0),
	 (9589,1,'开放API服务账号查询接口',9584,4,101,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobApiToken:api:list',NULL,'GET','/job/api-token/**',0,0,NULL,'查询服务账号Token接口',1,'2026-07-21 16:03:03',1,'2026-07-21 16:03:03',1,'pc',2,0),
	 (9590,1,'开放API服务账号创建接口',9584,4,102,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobApiToken:api:add',NULL,'POST','/job/api-token',0,0,NULL,'创建服务账号Token接口',1,'2026-07-21 16:03:03',1,'2026-07-21 16:03:03',1,'pc',2,0),
	 (9591,1,'开放API服务账号吊销接口',9584,4,103,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobApiToken:api:revoke',NULL,'POST','/job/api-token/*/revoke',0,0,NULL,'吊销服务账号Token接口',1,'2026-07-21 16:03:03',1,'2026-07-21 16:03:03',1,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9592,1,'开放API服务账号轮换接口',9584,4,104,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:jobApiToken:api:rotate',NULL,'POST','/job/api-token/*/rotate',0,0,NULL,'轮换服务账号Token接口',1,'2026-07-21 16:03:03',1,'2026-07-21 16:03:03',1,'pc',2,0),
	 (9600,1,'查看出站白名单',44,3,91,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:outboundWhitelist:list',NULL,NULL,NULL,0,0,NULL,'查看平台出站白名单',1,'2026-07-21 16:03:04',1,'2026-07-21 16:03:04',1,'pc',2,0),
	 (9601,1,'新增出站白名单',44,3,92,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:outboundWhitelist:add',NULL,NULL,NULL,0,0,NULL,'新增平台出站白名单',1,'2026-07-21 16:03:04',1,'2026-07-21 16:03:04',1,'pc',2,0),
	 (9602,1,'修改出站白名单',44,3,93,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:outboundWhitelist:edit',NULL,NULL,NULL,0,0,NULL,'修改平台出站白名单',1,'2026-07-21 16:03:04',1,'2026-07-21 16:03:04',1,'pc',2,0),
	 (9603,1,'删除出站白名单',44,3,94,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:outboundWhitelist:remove',NULL,NULL,NULL,0,0,NULL,'逻辑删除平台出站白名单',1,'2026-07-21 16:03:04',1,'2026-07-21 16:03:04',1,'pc',2,0),
	 (9604,1,'出站白名单查询接口',44,4,191,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:outboundWhitelist:api:list',NULL,'GET','/system/outbound-whitelist/page',0,0,NULL,'查询出站白名单接口',1,'2026-07-21 16:03:04',1,'2026-07-21 16:03:04',1,'pc',2,0),
	 (9605,1,'出站白名单详情接口',44,4,192,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:outboundWhitelist:api:detail',NULL,'GET','/system/outbound-whitelist/*',0,0,NULL,'查询出站白名单详情接口',1,'2026-07-21 16:03:04',1,'2026-07-21 16:03:04',1,'pc',2,0),
	 (9606,1,'出站白名单新增接口',44,4,193,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:outboundWhitelist:api:add',NULL,'POST','/system/outbound-whitelist',0,0,NULL,'新增出站白名单接口',1,'2026-07-21 16:03:04',1,'2026-07-21 16:03:04',1,'pc',2,0),
	 (9607,1,'出站白名单修改接口',44,4,194,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:outboundWhitelist:api:edit',NULL,'PUT','/system/outbound-whitelist',0,0,NULL,'修改出站白名单接口',1,'2026-07-21 16:03:04',1,'2026-07-21 16:03:04',1,'pc',2,0),
	 (9608,1,'出站白名单删除接口',44,4,195,NULL,NULL,0,0,NULL,'_self',0,1,1,'system:outboundWhitelist:api:remove',NULL,'DELETE','/system/outbound-whitelist/*',0,0,NULL,'删除出站白名单接口',1,'2026-07-21 16:03:04',1,'2026-07-21 16:03:04',1,'pc',2,0);
INSERT INTO sys_resource (id,tenant_id,resource_name,parent_id,resource_type,sort,`path`,component,is_external,sso_enabled,sso_target_client,open_target,is_public,menu_status,visible,perms,icon,api_method,api_url,keep_alive,always_show,redirect,remark,create_by,create_time,update_by,update_time,create_dept,client_code,min_user_type,del_flag) VALUES
	 (9616,1,'运行应用',9516,2,92,'/app-center/application/:applicationCode/runtime','app-center/application-runtime.[applicationCode]',0,0,NULL,'_self',0,1,0,'ai:businessApplication:runtime','ionicons5:AppsOutline',NULL,NULL,0,0,NULL,'业务应用运行页隐藏路由，仅进入授权树，不显示在侧边菜单',1,'2026-07-21 22:20:00',1,'2026-07-21 22:20:00',1,'pc',2,0);

-- ============================================================
-- 企业协同（V1.0.59）：字典、菜单权限、出站场景与 Job 配置
-- 全部内置数据 tenant_id=1，具备 NOT EXISTS 防重复保护；不写入任何 Secret。
-- ============================================================

INSERT INTO sys_dict_type (
  tenant_id, dict_name, dict_type, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_name, seed.dict_type, 1, seed.remark,
       1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, '企业协同平台' dict_name, 'sys_collab_platform' dict_type, '企业协同连接平台类型' remark
  UNION ALL SELECT 1, '企业协同能力', 'sys_collab_capability', '连接下可绑定的业务能力'
  UNION ALL SELECT 1, '协同身份匹配策略', 'sys_collab_identity_policy', '目录同步身份匹配策略'
  UNION ALL SELECT 1, '协同目录权威来源', 'sys_collab_directory_authority', '组织目录权威来源'
  UNION ALL SELECT 1, '协同同步状态', 'sys_collab_sync_status', '目录同步批次状态'
  UNION ALL SELECT 1, '协同投递状态', 'sys_collab_delivery_status', '消息/待办外部投递状态'
  UNION ALL SELECT 1, '协同回调状态', 'sys_collab_callback_status', '回调事件处理状态'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_type data
  WHERE data.tenant_id = seed.tenant_id
    AND data.dict_type = seed.dict_type
);

INSERT INTO sys_dict_data (
  tenant_id, dict_sort, dict_label, dict_value, dict_type,
  css_class, list_class, is_default, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type,
       NULL, seed.list_class, seed.is_default, 1, seed.remark,
       1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 1 dict_sort, '企业微信' dict_label, 'WECHAT_ENTERPRISE' dict_value,
         'sys_collab_platform' dict_type, 'info' list_class, 'Y' is_default, '企业微信自建应用' remark
  UNION ALL SELECT 1, 1, '扫码登录', 'LOGIN', 'sys_collab_capability', 'info', 'N', '企业成员OAuth登录'
  UNION ALL SELECT 1, 2, '通讯录同步', 'DIRECTORY', 'sys_collab_capability', 'success', 'N', '组织/成员/标签同步'
  UNION ALL SELECT 1, 3, '消息推送', 'MESSAGE', 'sys_collab_capability', 'warning', 'N', '应用消息与模板卡片'
  UNION ALL SELECT 1, 4, '待办联动', 'TODO', 'sys_collab_capability', 'error', 'N', 'Flowable待办投影'
  UNION ALL SELECT 1, 1, '仅绑定已有用户', 'BIND_ONLY', 'sys_collab_identity_policy', 'info', 'Y', '不自动创建Forge用户'
  UNION ALL SELECT 1, 2, '自动创建用户', 'AUTO_CREATE', 'sys_collab_identity_policy', 'warning', 'N', '未匹配时自动创建Forge用户'
  UNION ALL SELECT 1, 3, '人工处理', 'MANUAL', 'sys_collab_identity_policy', 'default', 'N', '未匹配进入问题单'
  UNION ALL SELECT 1, 1, '外部权威', 'EXTERNAL', 'sys_collab_directory_authority', 'success', 'Y', '外部平台为组织目录权威来源'
  UNION ALL SELECT 1, 2, '本地权威', 'LOCAL', 'sys_collab_directory_authority', 'info', 'N', 'Forge为权威来源，外部只读'
  UNION ALL SELECT 1, 3, '不同步', 'NONE', 'sys_collab_directory_authority', 'default', 'N', '不进行目录同步'
  UNION ALL SELECT 1, 1, '运行中', 'RUNNING', 'sys_collab_sync_status', 'info', 'N', '同步批次执行中'
  UNION ALL SELECT 1, 2, '成功', 'SUCCESS', 'sys_collab_sync_status', 'success', 'N', '同步批次成功'
  UNION ALL SELECT 1, 3, '部分成功', 'PARTIAL', 'sys_collab_sync_status', 'warning', 'N', '存在问题单的批次'
  UNION ALL SELECT 1, 4, '失败', 'FAILED', 'sys_collab_sync_status', 'error', 'N', '同步批次失败'
  UNION ALL SELECT 1, 1, '待投递', 'PENDING', 'sys_collab_delivery_status', 'info', 'N', '等待外部投递'
  UNION ALL SELECT 1, 2, '已投递', 'SENT', 'sys_collab_delivery_status', 'success', 'N', '外部投递成功'
  UNION ALL SELECT 1, 3, '投递失败', 'FAILED', 'sys_collab_delivery_status', 'error', 'N', '外部投递失败待补偿'
  UNION ALL SELECT 1, 4, '已跳过', 'SKIPPED', 'sys_collab_delivery_status', 'default', 'N', '无映射等原因跳过'
  UNION ALL SELECT 1, 5, '已关闭', 'CLOSED', 'sys_collab_delivery_status', 'default', 'N', '投影已关闭'
  UNION ALL SELECT 1, 1, '待处理', 'PENDING', 'sys_collab_callback_status', 'info', 'N', '回调事件待处理'
  UNION ALL SELECT 1, 2, '处理中', 'PROCESSING', 'sys_collab_callback_status', 'warning', 'N', '回调事件处理中'
  UNION ALL SELECT 1, 3, '已处理', 'PROCESSED', 'sys_collab_callback_status', 'success', 'N', '回调事件处理成功'
  UNION ALL SELECT 1, 4, '处理失败', 'FAILED', 'sys_collab_callback_status', 'error', 'N', '回调事件处理失败待重试'
  UNION ALL SELECT 1, 5, '已丢弃', 'DISCARDED', 'sys_collab_callback_status', 'default', 'N', '超过重试上限或人工丢弃'
  UNION ALL SELECT 1, 5, '企业协同', 'COLLABORATION', 'sys_message_channel', 'info', 'N', '企业微信等企业协同渠道'
  UNION ALL SELECT 1, 3, '企业协同供应商', 'COLLABORATION_PROVIDER', 'sys_outbound_scene', 'info', 'N', '企业协同供应商API出站场景，永不允许私网'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data data
  WHERE data.tenant_id = seed.tenant_id
    AND data.dict_type = seed.dict_type
    AND data.dict_value = seed.dict_value
);

SET @system_dir_id = (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND resource_type = 1 AND path = '/system' AND del_flag = 0
  ORDER BY id LIMIT 1
);

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, open_target, is_public, menu_status, visible,
  perms, icon, keep_alive, always_show, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, '企业协同', COALESCE(@system_dir_id, 0), 1, 95,
       '/system/collaboration', NULL, 0, '_self', 0, 1, 1,
       NULL, 'ionicons5:GitNetworkOutline', 0, 1, '企业协同集成管理目录',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource r
  WHERE r.tenant_id = 1 AND r.resource_type = 1 AND r.path = '/system/collaboration' AND r.del_flag = 0
);

SET @collab_dir_id = (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND resource_type = 1 AND path = '/system/collaboration' AND del_flag = 0
  ORDER BY id LIMIT 1
);

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, open_target, is_public, menu_status, visible,
  perms, icon, keep_alive, always_show, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, seed.resource_name, @collab_dir_id, 2, seed.sort,
       seed.path, seed.component, 0, '_self', 0, 1, 1,
       seed.perms, seed.icon, 1, 0, seed.remark,
       1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT '连接管理' resource_name, 1 sort, '/system/collaboration/connections' path,
         'system/collaboration/connections' component, 'system:collaboration:connection:list' perms,
         'ionicons5:LinkOutline' icon, '企业协同连接与应用管理' remark
  UNION ALL SELECT '同步批次', 2, '/system/collaboration/sync', 'system/collaboration/sync',
         'system:collaboration:sync:view', 'ionicons5:SyncOutline', '目录同步批次与阶段统计'
  UNION ALL SELECT '问题单', 3, '/system/collaboration/issues', 'system/collaboration/issues',
         'system:collaboration:issue:view', 'ionicons5:AlertCircleOutline', '同步冲突与人工处理队列'
  UNION ALL SELECT '映射查询', 4, '/system/collaboration/mappings', 'system/collaboration/mappings',
         'system:collaboration:mapping:view', 'ionicons5:GitCompareOutline', '部门/用户/岗位/标签映射检索'
  UNION ALL SELECT '投递记录', 5, '/system/collaboration/deliveries', 'system/collaboration/deliveries',
         'system:collaboration:delivery:view', 'ionicons5:PaperPlaneOutline', '消息与待办投递状态'
  UNION ALL SELECT '回调事件', 6, '/system/collaboration/callback-events', 'system/collaboration/callback-events',
         'system:collaboration:callback:view', 'ionicons5:DownloadOutline', '回调收件箱元数据与处理状态'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource r
  WHERE r.tenant_id = 1 AND r.resource_type = 2 AND r.perms = seed.perms AND r.del_flag = 0
);

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  is_external, open_target, is_public, menu_status, visible,
  perms, keep_alive, always_show, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, seed.resource_name, menu.id, 3, seed.sort, 0, '_self', 0, 1, 1,
       seed.perms, 0, 0, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT '新建连接' resource_name, 1 sort, 'system:collaboration:connection:create' perms,
         'system:collaboration:connection:list' menu_perms, '创建企业协同连接与应用' remark
  UNION ALL SELECT '修改连接', 2, 'system:collaboration:connection:update', 'system:collaboration:connection:list', '修改连接、应用与Secret轮换'
  UNION ALL SELECT '删除连接', 3, 'system:collaboration:connection:delete', 'system:collaboration:connection:list', '逻辑删除未被引用的连接'
  UNION ALL SELECT '连通测试', 4, 'system:collaboration:connection:test', 'system:collaboration:connection:list', '按能力执行连通验证'
  UNION ALL SELECT '旧凭据迁移', 5, 'system:collaboration:credential:migrate', 'system:collaboration:connection:list', '旧明文凭据与身份兼容迁移'
  UNION ALL SELECT '触发同步', 1, 'system:collaboration:sync:execute', 'system:collaboration:sync:view', '触发全量/部门/成员/标签同步'
  UNION ALL SELECT '处理问题单', 1, 'system:collaboration:sync:resolve', 'system:collaboration:issue:view', '绑定、忽略、重试问题单'
  UNION ALL SELECT '重试投递', 1, 'system:collaboration:delivery:retry', 'system:collaboration:delivery:view', '人工重试失败投递'
  UNION ALL SELECT '重试回调', 1, 'system:collaboration:callback:retry', 'system:collaboration:callback:view', '人工重试失败回调事件'
) seed
JOIN sys_resource menu
  ON menu.tenant_id = 1 AND menu.resource_type = 2 AND menu.perms = seed.menu_perms AND menu.del_flag = 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource r
  WHERE r.tenant_id = 1 AND r.resource_type = 3 AND r.perms = seed.perms AND r.del_flag = 0
);

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  is_external, open_target, is_public, menu_status, visible,
  perms, api_method, api_url, keep_alive, always_show, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, seed.resource_name, COALESCE(@collab_dir_id, 0), 4, seed.sort, 0, '_self', 0, 1, 1,
       seed.perms, seed.api_method, seed.api_url, 0, 0, seed.remark,
       1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT '连接分页接口' resource_name, 1 sort, 'system:collaboration:api:connection:page' perms,
         'GET' api_method, '/system/collaboration/connections/page' api_url, '分页查询协同连接' remark
  UNION ALL SELECT '连接详情接口', 2, 'system:collaboration:api:connection:detail', 'GET', '/system/collaboration/connections/*', '查询连接、能力和凭据状态摘要'
  UNION ALL SELECT '连接新增接口', 3, 'system:collaboration:api:connection:add', 'POST', '/system/collaboration/connections', '创建协同连接'
  UNION ALL SELECT '连接修改接口', 4, 'system:collaboration:api:connection:edit', 'PUT', '/system/collaboration/connections', '修改协同连接'
  UNION ALL SELECT '连接删除接口', 5, 'system:collaboration:api:connection:remove', 'DELETE', '/system/collaboration/connections/*', '逻辑删除协同连接'
  UNION ALL SELECT '应用查询接口', 6, 'system:collaboration:api:app:list', 'GET', '/system/collaboration/connections/*/apps', '查询连接下物理应用'
  UNION ALL SELECT '应用新增接口', 7, 'system:collaboration:api:app:add', 'POST', '/system/collaboration/connections/*/apps', '新增物理应用'
  UNION ALL SELECT '应用修改接口', 8, 'system:collaboration:api:app:edit', 'PUT', '/system/collaboration/connections/*/apps', '修改物理应用与Secret轮换'
  UNION ALL SELECT '连通测试接口', 9, 'system:collaboration:api:connection:test', 'POST', '/system/collaboration/connections/*/test', '按能力执行连通验证'
  UNION ALL SELECT '触发同步接口', 10, 'system:collaboration:api:sync:execute', 'POST', '/system/collaboration/connections/*/sync', '触发目录同步'
  UNION ALL SELECT '同步批次分页接口', 11, 'system:collaboration:api:synclog:page', 'GET', '/system/collaboration/sync-logs/page', '查询同步批次'
  UNION ALL SELECT '问题单分页接口', 12, 'system:collaboration:api:issue:page', 'GET', '/system/collaboration/sync-issues/page', '查询同步问题单'
  UNION ALL SELECT '问题单处理接口', 13, 'system:collaboration:api:issue:resolve', 'POST', '/system/collaboration/sync-issues/*/resolve', '处理同步问题单'
  UNION ALL SELECT '映射查询接口', 14, 'system:collaboration:api:mapping:list', 'GET', '/system/collaboration/mappings/*', '查询目录映射'
  UNION ALL SELECT '投递分页接口', 15, 'system:collaboration:api:delivery:page', 'GET', '/system/collaboration/deliveries/page', '查询投递状态'
  UNION ALL SELECT '投递重试接口', 16, 'system:collaboration:api:delivery:retry', 'POST', '/system/collaboration/deliveries/*/retry', '人工重试失败投递'
  UNION ALL SELECT '回调事件分页接口', 17, 'system:collaboration:api:callback:page', 'GET', '/system/collaboration/callback-events/page', '查询回调事件元数据'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource r
  WHERE r.tenant_id = 1 AND r.resource_type = 4 AND r.perms = seed.perms AND r.del_flag = 0
);



-- Public demonstration identities, newly generated for this distribution.
-- Local demonstration only: admin / change-me-on-first-login. Change before network use.
INSERT INTO sys_tenant (id,tenant_name,tenant_status) VALUES (1,'Default tenant',1);
INSERT INTO sys_org (id,tenant_id,org_code,org_name,parent_id,org_status) VALUES (1,1,'ROOT','Demo organization',0,1);
INSERT INTO sys_role (id,tenant_id,role_name,role_key,role_status,is_system) VALUES (1,1,'Administrator','admin',1,1);
INSERT INTO sys_user (id,tenant_id,username,real_name,user_type,password,user_status,force_password_change) VALUES (1,1,'admin','Administrator',0,'$2b$10$wTLUbvMIn9NFST71nauWsObL.qUoBMk9CxfyVZtw/.AQwzO/3AYsu',1,1);
INSERT INTO sys_user_role (id,tenant_id,user_id,role_id) VALUES (1,1,1,1);
INSERT INTO sys_user_tenant (id,tenant_id,user_id,member_type,is_default,status) VALUES (1,1,1,1,1,1);
INSERT INTO sys_role_resource (tenant_id,role_id,resource_id) SELECT 1,1,id FROM sys_resource WHERE del_flag=0;
