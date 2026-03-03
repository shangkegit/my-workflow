-- 流程模块元数据表
-- 用于持久化存储已部署的流程模块信息
-- Author: OpenClaw Agent
-- Date: 2026-03-03

DROP TABLE IF EXISTS `wf_process_module`;
CREATE TABLE `wf_process_module` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `module_id` varchar(64) NOT NULL COMMENT '模块ID（唯一标识）',
  `module_name` varchar(100) NOT NULL COMMENT '模块名称',
  `process_key` varchar(64) NOT NULL COMMENT '流程定义键（对应 BPMN process id）',
  `version` varchar(20) DEFAULT '1.0.0' COMMENT '版本号',
  `author` varchar(50) DEFAULT '' COMMENT '作者',
  `description` varchar(500) DEFAULT '' COMMENT '描述',
  `status` varchar(20) DEFAULT 'active' COMMENT '状态：active-激活, suspended-暂停, disabled-禁用',
  `deploy_source` varchar(20) DEFAULT 'manual' COMMENT '部署来源：import-导入, manual-手动上传, dev-开发环境同步',
  `config_json` text COMMENT '模块配置JSON（包含任务处理器、脚本等）',
  `bpmn_xml` longtext COMMENT 'BPMN流程定义XML',
  `deploy_time` datetime DEFAULT NULL COMMENT '部署时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deploy_by` varchar(64) DEFAULT '' COMMENT '部署人',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_process_key` (`process_key`),
  KEY `idx_status` (`status`),
  KEY `idx_deploy_time` (`deploy_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程模块元数据表';

-- 模块任务处理器配置表
DROP TABLE IF EXISTS `wf_module_task_handler`;
CREATE TABLE `wf_module_task_handler` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `module_id` bigint(20) NOT NULL COMMENT '模块ID（关联 wf_process_module.id）',
  `task_key` varchar(64) NOT NULL COMMENT '任务定义键',
  `task_name` varchar(100) DEFAULT '' COMMENT '任务名称',
  `form_key` varchar(200) DEFAULT '' COMMENT '表单键',
  `handler_type` varchar(20) DEFAULT 'script' COMMENT '处理器类型：script-脚本, class-Java类',
  `handler_content` text COMMENT '处理器内容（脚本代码或类名）',
  `sort_order` int(11) DEFAULT 0 COMMENT '排序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_module_id` (`module_id`),
  KEY `idx_task_key` (`task_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模块任务处理器配置表';

-- 模块部署历史记录表
DROP TABLE IF EXISTS `wf_module_deploy_history`;
CREATE TABLE `wf_module_deploy_history` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `module_id` varchar(64) NOT NULL COMMENT '模块ID',
  `module_name` varchar(100) DEFAULT '' COMMENT '模块名称',
  `version` varchar(20) DEFAULT '' COMMENT '版本号',
  `action` varchar(20) NOT NULL COMMENT '操作：deploy-部署, update-更新, suspend-暂停, activate-激活, remove-删除',
  `status` varchar(20) DEFAULT '' COMMENT '操作前状态',
  `operate_by` varchar(64) DEFAULT '' COMMENT '操作人',
  `operate_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_module_id` (`module_id`),
  KEY `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模块部署历史记录表';

-- 初始化数据：将现有的请假流程转换为模块格式
INSERT INTO `wf_process_module` (`module_id`, `module_name`, `process_key`, `version`, `author`, `description`, `status`, `deploy_source`, `deploy_time`, `update_time`, `remark`)
VALUES ('leave-module', '请假流程模块', 'leave', '1.0.0', 'system', '员工请假审批流程模块', 'active', 'system', NOW(), NOW(), '系统内置模块');

-- 插入请假流程的任务处理器
INSERT INTO `wf_module_task_handler` (`module_id`, `task_key`, `task_name`, `form_key`, `handler_type`, `handler_content`, `sort_order`)
SELECT m.id, 'deptLeaderCheck', '部门领导审批', 'leave/deptLeaderCheck', 'class', 'com.ruoyi.web.controller.activiti.LeaveapplyController', 1
FROM wf_process_module m WHERE m.process_key = 'leave';

INSERT INTO `wf_module_task_handler` (`module_id`, `task_key`, `task_name`, `form_key`, `handler_type`, `handler_content`, `sort_order`)
SELECT m.id, 'hrCheck', '人事审批', 'leave/hrCheck', 'class', 'com.ruoyi.web.controller.activiti.LeaveapplyController', 2
FROM wf_process_module m WHERE m.process_key = 'leave';

INSERT INTO `wf_module_task_handler` (`module_id`, `task_key`, `task_name`, `form_key`, `handler_type`, `handler_content`, `sort_order`)
SELECT m.id, 'modifyApply', '调整申请', 'leave/modifyApply', 'class', 'com.ruoyi.web.controller.activiti.LeaveapplyController', 3
FROM wf_process_module m WHERE m.process_key = 'leave';

INSERT INTO `wf_module_task_handler` (`module_id`, `task_key`, `task_name`, `form_key`, `handler_type`, `handler_content`, `sort_order`)
SELECT m.id, 'destroyApply', '销假', 'leave/destroyApply', 'class', 'com.ruoyi.web.controller.activiti.LeaveapplyController', 4
FROM wf_process_module m WHERE m.process_key = 'leave';
