package com.ruoyi.web.service;

/**
 * 流程通知服务接口
 * 
 * @author ruoyi
 */
public interface IProcessNotificationService
{
    /**
     * 通知流程挂起
     * 发送邮件通知给管理员和流程申请人
     * 
     * @param processInstanceId 流程实例ID
     */
    void notifyProcessSuspended(String processInstanceId);
}
