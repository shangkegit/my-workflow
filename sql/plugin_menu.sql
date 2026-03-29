-- 插件管理菜单 SQL 脚本
-- 执行完成后重新登录系统

-- 1. 添加 plugin_id 字段
ALTER TABLE sys_menu ADD COLUMN plugin_id VARCHAR(64) DEFAULT NULL COMMENT '插件ID';

-- 2. 添加插件管理主菜单（parent_id=1 是系统管理）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('插件管理', 1, 99, 'plugin', 'plugin/index', 'C', '0', '0', 'plugin:list', 'tool', 'admin', NOW(), '插件管理菜单');

-- 3. 获取插件管理菜单ID
SET @plugin_menu_id = LAST_INSERT_ID();

-- 4. 添加插件导入权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('插件导入', @plugin_menu_id, 1, '', '', 'F', '0', '0', 'plugin:import', '#', 'admin', NOW());

-- 5. 添加插件查询权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('插件查询', @plugin_menu_id, 2, '', '', 'F', '0', '0', 'plugin:query', '#', 'admin', NOW());

-- 6. 添加插件删除权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('插件删除', @plugin_menu_id, 3, '', '', 'F', '0', '0', 'plugin:remove', '#', 'admin', NOW());
