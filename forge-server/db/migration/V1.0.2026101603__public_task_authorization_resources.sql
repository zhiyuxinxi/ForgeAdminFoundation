-- Existing resource/role UI owns assignment. No automatic grant to ordinary roles.
INSERT INTO sys_resource (
  tenant_id,resource_name,resource_name_en,parent_id,resource_type,sort,
  is_external,open_target,is_public,menu_status,visible,perms,api_method,api_url,
  keep_alive,always_show,create_by,create_time,update_by,update_time,create_dept,client_code
)
SELECT tenant.id,seed.name_zh,seed.name_en,0,seed.kind,0,
  0,'_self',0,1,1,seed.perms,seed.method,seed.url,
  0,0,1,NOW(),1,NOW(),1,'pc'
FROM sys_tenant tenant
CROSS JOIN (
  SELECT '维护公共任务定义' name_zh,'Manage public task definitions' name_en,3 kind,
    'task:definition:write' perms,NULL method,NULL url
  UNION ALL SELECT '维护公共周期任务','Manage public periodic tasks',3,'task:schedule:write',NULL,NULL
  UNION ALL SELECT '创建公共任务定义','Create public task definition',4,'task:definition:write','POST','/api/task/definition'
  UNION ALL SELECT '查看任务定义列表','List task definitions',4,'task:instance:use','GET','/api/task/definitions'
  UNION ALL SELECT '查看任务定义','View task definition',4,'task:instance:use','GET','/api/task/definition/*'
  UNION ALL SELECT '保存周期规则','Save periodic rule',4,'task:schedule:write','POST','/api/task/schedules'
  UNION ALL SELECT '查看周期规则列表','List periodic rules',4,'task:schedule:write','GET','/api/task/schedules'
  UNION ALL SELECT '查看周期规则','View periodic rule',4,'task:schedule:write','GET','/api/task/schedules/*'
  UNION ALL SELECT '试算周期','Preview periodic dates',4,'task:schedule:write','POST','/api/task/schedules/preview'
  UNION ALL SELECT '启停周期规则','Enable or disable periodic rule',4,'task:schedule:write','POST','/api/task/schedules/*/enabled'
  UNION ALL SELECT '生成或补发周期','Generate or recover periodic tasks',4,'task:schedule:write','POST','/api/task/schedules/*/generate'
  UNION ALL SELECT '查看本人业务待办','List assigned business tasks',4,'task:instance:use','GET','/api/task/assigned'
  UNION ALL SELECT '查看业务任务','View business task',4,'task:instance:use','GET','/api/task/*'
  UNION ALL SELECT '开始本人业务任务','Start assigned business task',4,'task:instance:use','POST','/api/task/*/start'
  UNION ALL SELECT '完成本人业务任务','Complete assigned business task',4,'task:instance:use','POST','/api/task/*/complete'
  UNION ALL SELECT '读取任务结果表单','Read task result form',4,'task:instance:use','GET','/ai/business-tasks/*/form'
  UNION ALL SELECT '读取对象任务动作','Read object task actions',4,'task:instance:use','GET','/ai/crud/*/task-actions'
  UNION ALL SELECT '读取对象任务绑定','Read object task bindings',4,'task:instance:use','GET','/ai/crud/*/tasks'
  UNION ALL SELECT '查询来源关联任务','Read source-linked task',4,'task:instance:use','GET','/ai/crud/*/tasks/*/*'
  UNION ALL SELECT '生成来源任务','Create source task',4,'task:instance:create','POST','/ai/crud/*/tasks/*/*'
  UNION ALL SELECT '生成公共业务任务','Create public business task',4,'task:instance:create','POST','/api/task/create'
  UNION ALL SELECT '派发公共业务任务','Dispatch public business task',4,'task:instance:manage','POST','/api/task/*/dispatch'
  UNION ALL SELECT '取消公共业务任务','Cancel public business task',4,'task:instance:manage','POST','/api/task/*/cancel'
) seed
WHERE tenant.id > 0 AND tenant.del_flag=0
  AND NOT EXISTS (
    SELECT 1 FROM sys_resource existing
    WHERE existing.tenant_id=tenant.id AND existing.resource_type=seed.kind
      AND existing.perms=seed.perms AND existing.client_code='pc' AND existing.del_flag=0
      AND COALESCE(existing.api_method,'')=COALESCE(seed.method,'')
      AND COALESCE(existing.api_url,'')=COALESCE(seed.url,'')
  );

-- Default public administrators only. Custom roles use the assignable resource tree.
INSERT INTO sys_role_resource(tenant_id,role_id,resource_id,create_time)
SELECT role_row.tenant_id,role_row.id,resource.id,NOW()
FROM sys_role role_row
JOIN sys_resource resource ON resource.tenant_id=role_row.tenant_id AND resource.del_flag=0
  AND resource.client_code='pc'
  AND resource.perms IN ('task:definition:write','task:schedule:write',
                        'task:instance:use','task:instance:create','task:instance:manage')
WHERE role_row.role_key='admin' AND role_row.del_flag=0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_resource existing WHERE existing.tenant_id=role_row.tenant_id
      AND existing.role_id=role_row.id AND existing.resource_id=resource.id
  );
