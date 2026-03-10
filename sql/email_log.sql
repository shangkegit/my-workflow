-- 邮件发送日志表
CREATE TABLE sys_email_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    email_type VARCHAR(50) NOT NULL COMMENT '邮件类型（SUSPEND_NOTIFY）',
    recipient VARCHAR(100) NOT NULL COMMENT '收件人邮箱',
    recipient_name VARCHAR(100) COMMENT '收件人姓名',
    subject VARCHAR(200) NOT NULL COMMENT '邮件主题',
    content TEXT COMMENT '邮件内容',
    process_id VARCHAR(64) COMMENT '流程实例ID',
    process_name VARCHAR(200) COMMENT '流程名称',
    status CHAR(1) DEFAULT '0' COMMENT '状态（0待发送 1已发送 2发送失败）',
    error_msg VARCHAR(500) COMMENT '错误信息',
    send_time DATETIME COMMENT '发送时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_process_id (process_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB COMMENT='邮件发送日志表';
