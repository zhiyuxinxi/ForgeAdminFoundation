-- =============================================
-- Excel导出配置表（主表）
-- =============================================
CREATE TABLE IF NOT EXISTS sys_excel_export_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键（唯一标识）',
    config_type VARCHAR(16) NOT NULL DEFAULT 'BOTH' COMMENT '配置类型：EXPORT导出 IMPORT导入 BOTH导入导出',
    export_name VARCHAR(200) NOT NULL COMMENT '配置名称',
    sheet_name VARCHAR(100) DEFAULT 'Sheet1' COMMENT 'Sheet名称',
    file_name_template VARCHAR(200) COMMENT '文件名模板（支持占位符：{date}、{time}）',
    data_source_bean VARCHAR(100) COMMENT '数据源Bean名称（如：userService，导入-only可为空）',
    query_method VARCHAR(100) COMMENT '数据查询方法名（如：list、page，导入-only可为空）',
    auto_trans TINYINT(1) DEFAULT 1 COMMENT '是否自动翻译字典（1-是，0-否）',
    pageable TINYINT(1) DEFAULT 0 COMMENT '是否分页查询（1-是，0-否）',
    max_rows INT DEFAULT 10000 COMMENT '最大导出条数',
    sort_field VARCHAR(50) COMMENT '排序字段',
    sort_order VARCHAR(10) COMMENT '排序方向（ASC/DESC）',
    status TINYINT(1) DEFAULT 1 COMMENT '状态（1-启用，0-禁用）',
    include_sample TINYINT(1) DEFAULT 0 COMMENT '是否包含示例数据（用于模板）',
    allow_import TINYINT(1) DEFAULT 1 COMMENT '是否允许导入',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_config_key(config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Excel导出配置表';

-- =============================================
-- Excel列配置表（从表）
-- =============================================
CREATE TABLE IF NOT EXISTS sys_excel_column_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    config_key VARCHAR(100) NOT NULL COMMENT '关联配置键',
    field_name VARCHAR(100) NOT NULL COMMENT '字段名（实体属性名）',
    column_name VARCHAR(200) NOT NULL COMMENT '列名（Excel表头）',
    width INT DEFAULT 20 COMMENT '列宽',
    order_num INT DEFAULT 0 COMMENT '排序（值越小越靠前）',
    export TINYINT(1) DEFAULT 1 COMMENT '是否导出（1-是，0-否）',
    date_format VARCHAR(50) COMMENT '日期格式（如：yyyy-MM-dd）',
    number_format VARCHAR(50) COMMENT '数字格式',
    dict_type VARCHAR(50) COMMENT '字典类型（关联sys_dict_type）',
    importable TINYINT(1) DEFAULT 1 COMMENT '是否可导入（1-是，0-否）',
    required TINYINT(1) DEFAULT 0 COMMENT '是否必填（1-是，0-否）',
    example_value VARCHAR(255) COMMENT '示例值（用于模板）',
    validation_rule VARCHAR(255) COMMENT '校验规则（正则表达式）',
    validation_message VARCHAR(255) COMMENT '校验失败提示信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_config_key(config_key),
    INDEX idx_order(order_num)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Excel列配置表';

-- =============================================
-- 示例数据：用户列表导出配置
-- =============================================
INSERT INTO sys_excel_export_config 
(config_key, config_type, export_name, sheet_name, file_name_template, data_source_bean, query_method, auto_trans, pageable, max_rows, status, allow_import)
VALUES 
('user_list_export', 'EXPORT', '用户列表导出', '用户数据', '用户列表_{date}.xlsx', 'sysUserService', 'list', 1, 0, 50000, 1, 0);

-- 用户列表导出字段配置
INSERT INTO sys_excel_column_config 
(config_key, field_name, column_name, width, order_num, export, dict_type, importable)
VALUES 
('user_list_export', 'userId', '用户ID', 15, 1, 1, NULL, 0),
('user_list_export', 'username', '用户名', 20, 2, 1, NULL, 0),
('user_list_export', 'nickname', '昵称', 20, 3, 1, NULL, 0),
('user_list_export', 'status', '状态', 15, 4, 1, 'user_status', 0),
('user_list_export', 'statusName', '状态说明', 15, 5, 1, NULL, 0),
('user_list_export', 'createTime', '创建时间', 25, 6, 1, NULL, 0);

-- =============================================
-- 示例数据：订单列表导出配置
-- =============================================
INSERT INTO sys_excel_export_config 
(config_key, config_type, export_name, sheet_name, file_name_template, data_source_bean, query_method, auto_trans, pageable, max_rows, status, allow_import)
VALUES 
('order_list_export', 'EXPORT', '订单列表导出', '订单数据', '订单列表_{date}_{time}.xlsx', 'orderService', 'queryList', 1, 1, 100000, 1, 0);

INSERT INTO sys_excel_column_config 
(config_key, field_name, column_name, width, order_num, export, dict_type, importable)
VALUES 
('order_list_export', 'orderId', '订单编号', 25, 1, 1, NULL, 0),
('order_list_export', 'userName', '用户名', 20, 2, 1, NULL, 0),
('order_list_export', 'totalAmount', '订单金额', 15, 3, 1, NULL, 0),
('order_list_export', 'orderStatus', '订单状态', 15, 4, 1, 'order_status', 0),
('order_list_export', 'orderStatusName', '状态说明', 15, 5, 1, NULL, 0),
('order_list_export', 'createTime', '下单时间', 25, 6, 1, NULL, 0);
