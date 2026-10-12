-- ROOT-01: only management reads and the management page's send action require these permissions.
-- Personal inbox/read endpoints remain available to authenticated recipients.
INSERT INTO sys_resource (
    tenant_id, resource_name, resource_name_en, parent_id, resource_type, sort,
    is_external, open_target, is_public, menu_status, visible, perms, api_method, api_url,
    keep_alive, always_show, create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT menu.tenant_id, seed.resource_name, seed.resource_name_en, menu.id, 4, seed.sort,
       0, '_self', 0, 1, 1, seed.perms, seed.api_method, seed.api_url,
       0, 0, 1, NOW(), 1, NOW(), 1, menu.client_code
FROM (
    SELECT tenant_id, MIN(id) id, COALESCE(client_code, 'pc') client_code
    FROM sys_resource
    WHERE resource_type = 2 AND path = '/message/manage' AND del_flag = 0
    GROUP BY tenant_id, COALESCE(client_code, 'pc')
) menu
CROSS JOIN (
    SELECT '查询消息管理列表' resource_name, 'View managed messages' resource_name_en, 1 sort,
           'message:manage:query' perms, 'POST' api_method, '/api/message/manage/page' api_url
    UNION ALL SELECT '查看消息管理详情', 'View managed message details', 2,
           'message:manage:query', 'GET', '/api/message/manage/*/detail'
    UNION ALL SELECT '发送管理消息', 'Send managed messages', 3,
           'message:manage:send', 'POST', '/api/message/send'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM sys_resource current_resource
    WHERE current_resource.tenant_id = menu.tenant_id AND current_resource.resource_type = 4
      AND current_resource.perms = seed.perms AND current_resource.api_method = seed.api_method
      AND current_resource.api_url = seed.api_url AND current_resource.client_code = menu.client_code
      AND current_resource.del_flag = 0
);

-- Public default administrators only; custom/customer roles need explicit assignment.
INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT role_row.tenant_id, role_row.id, resource.id, NOW()
FROM sys_role role_row
JOIN sys_resource resource ON resource.tenant_id = role_row.tenant_id AND resource.resource_type = 4
    AND resource.del_flag = 0 AND resource.perms IN ('message:manage:query', 'message:manage:send')
WHERE role_row.role_key = 'admin' AND role_row.del_flag = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_resource existing
      WHERE existing.tenant_id = role_row.tenant_id AND existing.role_id = role_row.id
        AND existing.resource_id = resource.id
  );
