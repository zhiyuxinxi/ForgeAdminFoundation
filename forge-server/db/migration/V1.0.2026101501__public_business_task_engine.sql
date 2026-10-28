-- Public ordinary business tasks. No customer data or workflow-engine tables.
CREATE TABLE IF NOT EXISTS sys_business_task_definition (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  code VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  definition_version INT NOT NULL,
  title VARCHAR(200) NOT NULL,
  source_type VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  create_by BIGINT NOT NULL,
  create_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  create_dept BIGINT DEFAULT NULL,
  update_by BIGINT NOT NULL,
  update_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  del_flag BIGINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_business_task_definition (tenant_id,source_type,code,definition_version,del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_business_task (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  definition_id BIGINT NOT NULL,
  source_type VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  source_key VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  occurrence_key VARCHAR(96) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  assignee_id BIGINT NOT NULL,
  assignee_name VARCHAR(200) NOT NULL,
  title VARCHAR(200) NOT NULL,
  state VARCHAR(24) NOT NULL,
  due_at DATETIME(6) DEFAULT NULL,
  completed_at DATETIME(6) DEFAULT NULL,
  result_json TEXT,
  completion_key VARCHAR(96) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  cancellation_reason VARCHAR(500) DEFAULT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  create_by BIGINT NOT NULL,
  create_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  create_dept BIGINT DEFAULT NULL,
  update_by BIGINT NOT NULL,
  update_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  del_flag BIGINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_business_task_obligation (tenant_id,definition_id,source_key,occurrence_key,assignee_id,del_flag),
  KEY idx_business_task_assignee (tenant_id,assignee_id,state,del_flag,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
