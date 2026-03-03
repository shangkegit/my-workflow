package com.ruoyi.workflow.module;

import java.util.Map;

/**
 * 流程模块接口
 * 所有动态部署的流程模块都需要实现此接口
 * 
 * @author OpenClaw Agent
 * @date 2026-03-03
 */
public interface ProcessModule {

    /**
     * 获取模块元信息
     */
    ProcessModuleMeta getMeta();

    /**
     * 初始化模块
     * 在模块加载时调用，用于初始化资源
     */
    void initialize() throws Exception;

    /**
     * 销毁模块
     * 在模块卸载时调用，用于释放资源
     */
    void destroy();

    /**
     * 处理任务
     * 
     * @param taskKey 任务定义键（如 "deptLeaderCheck"）
     * @param taskId Activiti 任务ID
     * @param businessKey 业务主键
     * @param variables 流程变量
     * @return 处理结果
     */
    Map<String, Object> handleTask(String taskKey, String taskId, String businessKey, Map<String, Object> variables);

    /**
     * 获取任务表单数据
     * 
     * @param taskKey 任务定义键
     * @param businessKey 业务主键
     * @return 表单数据
     */
    Map<String, Object> getFormData(String taskKey, String businessKey);

    /**
     * 启动流程实例前的回调
     * 
     * @param businessKey 业务主键
     * @param variables 流程变量
     */
    void beforeStartProcess(String businessKey, Map<String, Object> variables);

    /**
     * 流程结束后的回调
     * 
     * @param businessKey 业务主键
     * @param variables 流程变量
     */
    void afterProcessComplete(String businessKey, Map<String, Object> variables);
}
