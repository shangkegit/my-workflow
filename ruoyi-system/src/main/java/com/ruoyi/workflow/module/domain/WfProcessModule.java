package com.ruoyi.workflow.module.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 流程模块 wf_process_module
 * 
 * @author OpenClaw Agent
 * @date 2026-03-03
 */
public class WfProcessModule extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 模块ID（唯一标识） */
    private String moduleId;

    /** 模块名称 */
    private String moduleName;

    /** 流程定义键 */
    private String processKey;

    /** 版本号 */
    private String version;

    /** 作者 */
    private String author;

    /** 描述 */
    private String description;

    /** 状态 */
    private String status;

    /** 部署来源 */
    private String deploySource;

    /** 模块配置JSON */
    private String configJson;

    /** BPMN流程定义XML */
    private String bpmnXml;

    /** 部署时间 */
    private java.util.Date deployTime;

    /** 部署人 */
    private String deployBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getModuleId() {
        return moduleId;
    }

    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getProcessKey() {
        return processKey;
    }

    public void setProcessKey(String processKey) {
        this.processKey = processKey;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDeploySource() {
        return deploySource;
    }

    public void setDeploySource(String deploySource) {
        this.deploySource = deploySource;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public String getBpmnXml() {
        return bpmnXml;
    }

    public void setBpmnXml(String bpmnXml) {
        this.bpmnXml = bpmnXml;
    }

    public java.util.Date getDeployTime() {
        return deployTime;
    }

    public void setDeployTime(java.util.Date deployTime) {
        this.deployTime = deployTime;
    }

    public String getDeployBy() {
        return deployBy;
    }

    public void setDeployBy(String deployBy) {
        this.deployBy = deployBy;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("moduleId", getModuleId())
                .append("moduleName", getModuleName())
                .append("processKey", getProcessKey())
                .append("version", getVersion())
                .append("status", getStatus())
                .append("deployTime", getDeployTime())
                .toString();
    }
}
