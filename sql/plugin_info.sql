-- 插件信息表
-- 用于持久化已安装的插件信息，重启后可恢复

CREATE TABLE IF NOT EXISTS `sys_plugin` (
  `plugin_id` varchar(64) NOT NULL COMMENT '插件ID',
  `plugin_name` varchar(100) NOT NULL COMMENT '插件名称',
  `process_key` varchar(100) DEFAULT NULL COMMENT '流程标识',
  `version` varchar(20) DEFAULT NULL COMMENT '版本号',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1禁用）',
  `author` varchar(50) DEFAULT NULL COMMENT '作者',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `manifest_json` text COMMENT 'manifest.json内容',
  `install_time` datetime DEFAULT NULL COMMENT '安装时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`plugin_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件信息表';
