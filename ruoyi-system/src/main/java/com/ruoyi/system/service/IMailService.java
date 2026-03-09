package com.ruoyi.system.service;

/**
 * 邮件服务接口
 * 
 * @author ruoyi
 */
public interface IMailService
{
    /**
     * 发送流程挂起通知邮件
     * 
     * @param processInstanceId 流程实例ID
     */
    void sendProcessSuspendNotice(String processInstanceId);
}
