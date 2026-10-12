# SQL Templates

Use this as the starting point for a code-first business workflow migration. Replace all `xxx_*` placeholders and fixed seed IDs before committing.

## Complete Flyway Template

```sql
-- Business approval workflow: xxx_order.

CREATE TABLE IF NOT EXISTS `xxx_order` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `order_no` varchar(64) NOT NULL COMMENT '单据编号',
  `title` varchar(128) NOT NULL COMMENT '单据标题',
  `amount_cent` bigint NOT NULL DEFAULT 0 COMMENT '金额，单位分',
  `status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '业务状态',
  `applicant_id` bigint DEFAULT NULL COMMENT '申请人ID',
  `applicant_name` varchar(64) DEFAULT NULL COMMENT '申请人名称',
  `applicant_dept_id` bigint DEFAULT NULL COMMENT '申请部门ID',
  `applicant_dept_name` varchar(128) DEFAULT NULL COMMENT '申请部门名称',
  `business_key` varchar(128) DEFAULT NULL COMMENT '流程业务Key',
  `process_instance_id` varchar(128) DEFAULT NULL COMMENT '流程实例ID',
  `approver_id` bigint DEFAULT NULL COMMENT '审批人ID',
  `approver_remark` varchar(500) DEFAULT NULL COMMENT '审批意见',
  `reject_reason` varchar(500) DEFAULT NULL COMMENT '驳回/取消原因',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_xxx_order_no` (`tenant_id`, `order_no`),
  UNIQUE KEY `uk_xxx_order_business_key` (`tenant_id`, `business_key`),
  KEY `idx_xxx_order_status` (`tenant_id`, `status`, `update_time`),
  KEY `idx_xxx_order_process` (`tenant_id`, `process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='xxx审批业务表';

INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_time, update_time)
SELECT seed.tenant_id, seed.dict_name, seed.dict_type, seed.dict_status, seed.remark, NOW(), NOW()
FROM (
  SELECT 1 tenant_id, 'xxx审批状态' dict_name, 'xxx_order_status' dict_type, 1 dict_status, 'xxx审批业务状态' remark
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_type t
  WHERE t.tenant_id = seed.tenant_id AND t.dict_type = seed.dict_type
);

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class,
                           is_default, dict_status, remark, create_time, update_time)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type, NULL,
       seed.list_class, seed.is_default, 1, seed.remark, NOW(), NOW()
FROM (
  SELECT 1 tenant_id, 1 dict_sort, '草稿' dict_label, 'DRAFT' dict_value, 'xxx_order_status' dict_type, 'default' list_class, 'Y' is_default, '草稿' remark
  UNION ALL SELECT 1, 2, '审批中', 'IN_PROCESS', 'xxx_order_status', 'info', 'N', '审批中'
  UNION ALL SELECT 1, 3, '待修改', 'NEED_MODIFY', 'xxx_order_status', 'warning', 'N', '被驳回后等待申请人修改'
  UNION ALL SELECT 1, 4, '已通过', 'APPROVED', 'xxx_order_status', 'success', 'N', '审批通过'
  UNION ALL SELECT 1, 5, '已拒绝', 'REJECTED', 'xxx_order_status', 'error', 'N', '流程拒绝结束'
  UNION ALL SELECT 1, 6, '已取消', 'CANCELED', 'xxx_order_status', 'default', 'N', '流程取消'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data d
  WHERE d.tenant_id = seed.tenant_id
    AND d.dict_type = seed.dict_type
    AND d.dict_value = seed.dict_value
);

SET @app_center_root_id := (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND client_code = 'pc' AND resource_type = 1
    AND parent_id = 0 AND resource_name = '应用中心'
  ORDER BY id LIMIT 1
);

SET @business_parent_id := COALESCE(@app_center_root_id, 0);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, 'xxx审批', @business_parent_id, 2, 10, '/business/xxx-order', 'business/xxx-order', 0,
       0, NULL, '_self', 0, 1, 1, 'business:xxxOrder:list', 'ionicons5:DocumentTextOutline',
       NULL, NULL, 0, 0, NULL, 'xxx审批业务页面', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource
  WHERE tenant_id = 1 AND client_code = 'pc' AND resource_type = 2 AND path = '/business/xxx-order'
);

SET @xxx_menu_id := (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1 AND client_code = 'pc' AND resource_type = 2 AND path = '/business/xxx-order'
  ORDER BY id LIMIT 1
);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT seed.tenant_id, seed.resource_name, @xxx_menu_id, 3, seed.sort, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, seed.perms, NULL,
       NULL, NULL, 0, 0, NULL, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT 1 tenant_id, 'xxx查询' resource_name, 1 sort, 'business:xxxOrder:query' perms, 'xxx查询按钮权限' remark
  UNION ALL SELECT 1, 'xxx新增', 2, 'business:xxxOrder:add', 'xxx新增按钮权限'
  UNION ALL SELECT 1, 'xxx修改', 3, 'business:xxxOrder:edit', 'xxx修改按钮权限'
  UNION ALL SELECT 1, 'xxx删除', 4, 'business:xxxOrder:remove', 'xxx删除按钮权限'
  UNION ALL SELECT 1, 'xxx提交审批', 5, 'business:xxxOrder:submit', 'xxx提交审批按钮权限'
  UNION ALL SELECT 1, 'xxx初始化流程', 6, 'business:xxxOrder:initFlow', 'xxx流程初始化按钮权限'
) seed
WHERE @xxx_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_resource r
    WHERE r.tenant_id = seed.tenant_id
      AND r.client_code = 'pc'
      AND r.resource_type = 3
      AND r.perms = seed.perms
  );

SET @admin_role_id := (
  SELECT id FROM sys_role
  WHERE tenant_id = 1 AND role_key = 'admin'
  ORDER BY id LIMIT 1
);

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT 1, @admin_role_id, target.id, NOW()
FROM sys_resource target
WHERE @admin_role_id IS NOT NULL
  AND target.tenant_id = 1
  AND target.client_code = 'pc'
  AND (
    target.path = '/business/xxx-order'
    OR target.perms LIKE 'business:xxxOrder:%'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_resource exists_rr
    WHERE exists_rr.tenant_id = 1
      AND exists_rr.role_id = @admin_role_id
      AND exists_rr.resource_id = target.id
  );

INSERT INTO ai_business_suite (id, tenant_id, parent_id, suite_code, suite_name, icon, description, status,
                               sort_order, options, create_by, create_time, create_dept, update_by, update_time)
SELECT 1910000000000010001, 1, NULL, 'XXX_SUITE', 'xxx业务', 'ionicons5:BriefcaseOutline',
       'xxx审批业务域', 1, 40,
       '{"codeApp":true,"scenario":"xxx_order_flow"}',
       1, NOW(), 1, 1, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM ai_business_suite
  WHERE tenant_id = 1 AND suite_code = 'XXX_SUITE'
);

INSERT INTO ai_business_object (id, tenant_id, suite_code, object_code, object_name, object_type, model_id,
                                model_code, display_field, icon, description, status, sort_order, options,
                                design_status, config_key, last_publish_time, last_publish_version,
                                designer_options, create_by, create_time, create_dept, update_by, update_time)
SELECT 1910000000000010002, 1, 'XXX_SUITE', 'xxx_order', 'xxx申请', 'TRANSACTION', NULL,
       NULL, 'title', 'ionicons5:DocumentTextOutline',
       '代码实现的xxx审批业务，流程设计器维护节点表单和字段权限', 1, 10,
       '{"codeApp":true,"businessType":"xxx_order","flowModelKey":"xxx_order_approval"}',
       'PUBLISHED', NULL, NOW(), 1,
       '{"codeApp":true,"documentManaged":false,"defaultPanel":"flow-app"}',
       1, NOW(), 1, 1, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM ai_business_object
  WHERE tenant_id = 1 AND suite_code = 'XXX_SUITE' AND object_code = 'xxx_order'
);

SET @xxx_object_id := (
  SELECT id FROM ai_business_object
  WHERE tenant_id = 1 AND suite_code = 'XXX_SUITE' AND object_code = 'xxx_order'
  LIMIT 1
);

INSERT INTO ai_business_app (id, tenant_id, app_code, app_name, app_type, suite_code, object_code, entry_mode,
                             entry_url, config_key, icon, description, status, sort_order, options, create_by,
                             create_time, create_dept, update_by, update_time)
SELECT 1910000000000010003, 1, 'XXX_ORDER_APPROVAL', 'xxx审批', 'BUSINESS',
       'XXX_SUITE', 'xxx_order', 'ROUTE',
       '/business/xxx-order', NULL, 'ionicons5:ClipboardOutline',
       '打开xxx审批业务页面', 1, 10,
       '{"codeApp":true,"flowConfigUrl":"/app-center/object/xxx_order/designer?panel=flow-app&codeApp=1&name=xxx申请"}',
       1, NOW(), 1, 1, NOW()
WHERE @xxx_object_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM ai_business_app
    WHERE tenant_id = 1 AND app_code = 'XXX_ORDER_APPROVAL'
  );

INSERT INTO ai_business_binding (id, tenant_id, target_type, target_id, target_code, binding_type, binding_key,
                                 binding_name, binding_config, description, status, sort_order, create_by,
                                 create_time, create_dept, update_by, update_time)
SELECT 1910000000000010004, 1, 'OBJECT', @xxx_object_id, 'xxx_order', 'FLOW', 'xxx_order_approval',
       'xxx审批流程',
       '{
         "flowModelKey":"xxx_order_approval",
         "flowModelName":"xxx审批流程",
         "titleTemplate":"xxx审批-{orderNo}",
         "startMode":"MANUAL",
         "businessBinding":{
           "mode":"ADAPTER",
           "primaryKeyField":"id",
           "tenantField":"tenant_id",
           "titleField":"title",
           "statusField":"status",
           "ownerField":"applicantId"
         },
         "variableMapping":[
           {"formField":"businessKey","flowVariable":"businessKey","label":"业务Key"},
           {"formField":"orderNo","flowVariable":"orderNo","label":"单据编号"},
           {"formField":"title","flowVariable":"title","label":"单据标题"},
           {"formField":"amountCent","flowVariable":"amountCent","label":"金额分"},
           {"formField":"approverId","flowVariable":"approverId","label":"审批人"}
         ],
         "nodeForms":[],
         "conditionFlows":[],
         "options":{"codeApp":true,"businessKeyPattern":"xxx_order:{recordId}"}
       }',
       'xxx代码业务默认流程绑定。节点表单与字段权限以BPMN节点配置为准，nodeForms仅作兼容兜底。',
       1, 1, 1, NOW(), 1, 1, NOW()
WHERE @xxx_object_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM ai_business_binding
    WHERE tenant_id = 1
      AND target_type = 'OBJECT'
      AND target_code = 'xxx_order'
      AND binding_type = 'FLOW'
      AND binding_key = 'xxx_order_approval'
  );
```

## Script Rules

- Version filename: `V<next_version>__<lower_snake_description>.sql`.
- Do not modify migrations already executed in `forge_schema_history`.
- Use `tenant_id = 1` for built-in business/config data.
- Every insert must have explicit columns and duplicate protection.
- Avoid `${...}` in SQL string literals because Flyway may parse placeholders. Use `{field}` or `CONCAT('$', '{field}')` when a literal `${field}` is unavoidable.
- Keep fixed seed IDs unique and in the project long ID style.
- Add role-resource seed only for required built-in access.
