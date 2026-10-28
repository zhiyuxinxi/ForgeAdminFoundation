-- Register generic CC endpoints for each existing PC CC menu.
-- Existing menu holders retain recipient-only access; send/sent require an explicit grant.
-- No customer organization, role, account or business data is introduced.
INSERT INTO sys_resource (
    tenant_id, resource_name, resource_name_en, parent_id, resource_type, sort,
    is_external, sso_enabled, open_target, is_public, menu_status, visible,
    perms, api_method, api_url, keep_alive, always_show, remark,
    create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT menu.tenant_id, endpoint.label_zh, endpoint.label_en, menu.id, 4, endpoint.sort_order,
       0, 0, '_self', 0, 1, 1,
       endpoint.permission_key, endpoint.http_method, endpoint.api_path, 0, 0,
       'Public CC access with session recipient ownership',
       1, NOW(), 1, NOW(), 1, menu.client_code
FROM sys_resource menu
CROSS JOIN (
    SELECT '我的抄送-列表' AS label_zh, 'Received CC list' AS label_en, 10 AS sort_order,
           'flow:cc:my' AS permission_key, 'GET' AS http_method, '/api/flow/cc/my' AS api_path
    UNION ALL SELECT '我的抄送-未读数量', 'Unread CC count', 11, 'flow:cc:my', 'GET', '/api/flow/cc/unread/count'
    UNION ALL SELECT '我的抄送-表单', 'Received CC form', 12, 'flow:cc:form', 'GET', '/api/flow/cc/form/*'
    UNION ALL SELECT '我的抄送-标记已读', 'Mark received CC as read', 13, 'flow:cc:read', 'POST', '/api/flow/cc/read/*'
    UNION ALL SELECT '我的抄送-批量已读', 'Mark received CC batch as read', 14, 'flow:cc:read', 'POST', '/api/flow/cc/read/batch'
    UNION ALL SELECT '我的抄送-业务表单', 'Received CC business form', 15, 'flow:cc:business-form', 'GET', '/ai/business/flow/task-form-context/cc/*'
    UNION ALL SELECT '发送抄送', 'Send CC', 16, 'flow:cc:send', 'POST', '/api/flow/cc/send'
    UNION ALL SELECT '我发送的抄送', 'Sent CC list', 17, 'flow:cc:sent', 'GET', '/api/flow/cc/sent'
) endpoint
WHERE menu.client_code = 'pc' AND menu.resource_type = 2 AND menu.path = '/flow/cc'
  AND menu.del_flag = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_resource existing
      WHERE existing.tenant_id = menu.tenant_id AND existing.client_code = menu.client_code
        AND existing.resource_type = 4 AND existing.perms = endpoint.permission_key
        AND existing.api_method = endpoint.http_method AND existing.api_url = endpoint.api_path
        AND existing.del_flag = 0
  );

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT menu.tenant_id, menu_grant.role_id, endpoint.id, NOW()
FROM sys_resource menu
JOIN sys_role_resource menu_grant
  ON menu_grant.tenant_id = menu.tenant_id AND menu_grant.resource_id = menu.id
JOIN sys_role role_row
  ON role_row.tenant_id = menu.tenant_id AND role_row.id = menu_grant.role_id
 AND role_row.del_flag = 0 AND role_row.role_status = 1
JOIN sys_resource endpoint
  ON endpoint.tenant_id = menu.tenant_id AND endpoint.client_code = menu.client_code
 AND endpoint.parent_id = menu.id AND endpoint.resource_type = 4 AND endpoint.del_flag = 0
 AND endpoint.perms IN ('flow:cc:my', 'flow:cc:form', 'flow:cc:read', 'flow:cc:business-form')
WHERE menu.client_code = 'pc' AND menu.resource_type = 2 AND menu.path = '/flow/cc'
  AND menu.del_flag = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_resource existing
      WHERE existing.tenant_id = menu.tenant_id AND existing.role_id = menu_grant.role_id
        AND existing.resource_id = endpoint.id
  );
