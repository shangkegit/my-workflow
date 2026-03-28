-- 插件管理菜单（父菜单ID为1，系统管理下）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2000, '插件管理', 1, 10, 'plugin', 'plugin/index', 'C', '0', '0', 'plugin:list', 'tool', 'admin', NOW(), '插件管理菜单');

-- 插件导入权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('插件导入', 2000, 1, '', '', 'F', '0', '0', 'plugin:import', '#', 'admin', NOW());

-- 插件查询权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('插件查询', 2000, 2, '', '', 'F', '0', '0', 'plugin:query', '#', 'admin', NOW());

-- 插件删除权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('插件删除', 2000, 3, '', '', 'F', '0', '0', 'plugin:remove', '#', 'admin', NOW());

-- 添加 plugin_id 字段到 sys_menu 表
ALTER TABLE sys_menu ADD COLUMN plugin_id VARCHAR(64) DEFAULT NULL COMMENT '插件ID';
