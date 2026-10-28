-- Calendar rules and allocation snapshots. Business checklists stay with source owners.
CREATE TABLE IF NOT EXISTS sys_task_schedule (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  code VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  rule_version INT NOT NULL,
  rule_json TEXT NOT NULL,
  actor_id BIGINT NOT NULL,
  actor_org_id BIGINT NOT NULL,
  active_slot INT DEFAULT NULL,
  next_index INT NOT NULL DEFAULT 0,
  next_generate_at DATETIME(6) DEFAULT NULL,
  next_attempt_at DATETIME(6) DEFAULT NULL,
  last_failure VARCHAR(200) DEFAULT NULL,
  UNIQUE KEY uk_task_schedule_version(tenant_id,code,rule_version),
  UNIQUE KEY uk_task_schedule_active(tenant_id,code,active_slot),
  KEY idx_task_schedule_due(active_slot,next_attempt_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_task_occurrence (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  schedule_id BIGINT NOT NULL,
  occurrence_index INT NOT NULL,
  starts_at DATETIME(6) NOT NULL,
  due_at DATETIME(6) NOT NULL,
  allow_late BOOLEAN NOT NULL,
  assignment_json TEXT NOT NULL,
  state VARCHAR(24) NOT NULL,
  UNIQUE KEY uk_task_occurrence(tenant_id,schedule_id,occurrence_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_task_occurrence_assignment (
  tenant_id BIGINT NOT NULL,
  occurrence_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  assignee_id BIGINT NOT NULL,
  PRIMARY KEY(tenant_id,task_id),
  UNIQUE KEY uk_task_period_assignee(tenant_id,occurrence_id,assignee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
