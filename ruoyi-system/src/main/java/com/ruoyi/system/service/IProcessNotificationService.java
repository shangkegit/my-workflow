package com.ruoyi.system.service;

/**
 * 流程通知服务接口
 * 
 * @author ruoyi
 */
public interface IProcessNotificationService
{
    /**
     * 通知流程实例已被挂起
     * 
     * @param processInstanceId 流程实例ID
     */
    void notifyProcessSuspended(String processInstanceId);
}
