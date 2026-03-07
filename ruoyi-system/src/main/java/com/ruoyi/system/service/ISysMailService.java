package com.ruoyi.system.service;

/**
 * 邮件服务接口
 * 
 * @author ruoyi
 */
public interface ISysMailService
{
    /**
     * 发送简单邮件
     * 
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    public void sendSimpleMail(String to, String subject, String content);

    /**
     * 发送流程停用通知邮件给管理员
     * 
     * @param processName 流程名称
     * @param processKey 流程Key
     * @param operator 操作人
     */
    public void sendProcessSuspendNotification(String processName, String processKey, String operator);
}
