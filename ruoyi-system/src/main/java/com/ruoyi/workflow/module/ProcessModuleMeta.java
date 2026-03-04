package com.ruoyi.workflow.module;

import java.util.Date;
import java.util.Map;

/**
 * 流程模块元信息
 * 
 * @author OpenClaw Agent
 * @date 2026-03-03
 */
public class ProcessModuleMeta {

    /** 模块ID（唯一标识） */
    private String id;

    /** 模块名称 */
    private String name;

    /** 流程定义键（对应 BPMN 中的 process id） */
    private String processKey;

    /** 版本号 */
    private String version;

    /** 作者 */
    private String author;

    /** 描述 */
    private String description;

    /** 模块状态：active - 激活, suspended - 暂停, disabled - 禁用 */
    private String status;

    /** 部署时间 */
    private Date deployTime;

    /** 更新时间 */
    private Date updateTime;

    /** 部署来源：dev - 开发环境导入, manual - 手动上传 */
    private String deploySource;

    /** 扩展配置（JSON格式） */
    private String config;

    /** 任务处理器映射 */
    private Map<String, TaskHandlerMeta> taskHandlers;

    /**
     * 任务处理器元信息
     */
    public static class TaskHandlerMeta {
        private String taskKey;
        private String taskName;
        private String formKey;
        private String handlerClass;
        private String handlerScript;

        // Getters and Setters
        public String getTaskKey() { return taskKey; }
        public void setTaskKey(String taskKey) { this.taskKey = taskKey; }
        public String getTaskName() { return taskName; }
        public void setTaskName(String taskName) { this.taskName = taskName; }
        public String getFormKey() { return formKey; }
        public void setFormKey(String formKey) { this.formKey = formKey; }
        public String getHandlerClass() { return handlerClass; }
        public void setHandlerClass(String handlerClass) { this.handlerClass = handlerClass; }
        public String getHandlerScript() { return handlerScript; }
        public void setHandlerScript(String handlerScript) { this.handlerScript = handlerScript; }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProcessKey() { return processKey; }
    public void setProcessKey(String processKey) { this.processKey = processKey; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getDeployTime() { return deployTime; }
    public void setDeployTime(Date deployTime) { this.deployTime = deployTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
    public String getDeploySource() { return deploySource; }
    public void setDeploySource(String deploySource) { this.deploySource = deploySource; }
    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }
    public Map<String, TaskHandlerMeta> getTaskHandlers() { return taskHandlers; }
    public void setTaskHandlers(Map<String, TaskHandlerMeta> taskHandlers) { this.taskHandlers = taskHandlers; }
}
