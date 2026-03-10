package com.ruoyi.system.service;

/**
 * 邮件服务接口
 *
 * @author ruoyi
 */
public interface ISysEmailService
{
    /**
     * 发送流程挂起通知
     *
     * @param processName 流程名称
     * @param applicantId 申请人ID
     */
    void sendSuspendNotification(String processName, Long applicantId);
}
