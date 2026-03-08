package com.ruoyi.system.service;

/**
 * 邮件服务接口
 *
 * @author ruoyi
 */
public interface IEmailService
{
    /**
     * 发送流程挂起通知邮件
     *
     * @param processInstanceId 流程实例ID
     * @param processName 流程名称
     * @param operator 操作人
     * @param reason 挂起原因
     */
    void sendSuspendNotification(String processInstanceId, String processName,
                                  String operator, String reason);
}
