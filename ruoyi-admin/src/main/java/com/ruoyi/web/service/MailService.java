package com.ruoyi.web.service;

import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;

import java.util.List;

/**
 * 邮件服务接口
 *
 * @author ruoyi
 */
public interface MailService {

    /**
     * 发送流程停用通知给管理员
     *
     * @param processDefinition 流程定义信息
     * @param instances 受影响的流程实例列表
     */
    void sendSuspendNoticeToAdmin(ProcessDefinition processDefinition, List<ProcessInstance> instances);

    /**
     * 发送流程停用通知给申请人
     *
     * @param userName 申请人用户名（显示名）
     * @param userEmail 申请人邮箱
     * @param processDefinitionName 流程定义名称
     * @param instances 该申请人发起的被停用流程实例
     */
    void sendSuspendNoticeToApplicant(String userName, String userEmail, String processDefinitionName, List<ProcessInstance> instances);

    /**
     * 异步发送邮件
     *
     * @param to 收件人
     * @param subject 主题
     * @param content 内容（HTML格式）
     */
    void sendMailAsync(String to, String subject, String content);
}
