package com.ruoyi.common.core.plugin.model;

import com.ruoyi.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 插件信息表 sys_plugin
 */
public class SysPlugin extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 插件ID */
    private String pluginId;

    /** 插件名称 */
    private String pluginName;

    /** 流程标识 */
    private String processKey;

    /** 版本号 */
    private String version;

    /** 状态（0正常 1禁用） */
    private String status;

    /** 作者 */
    private String author;

    /** 描述 */
    private String description;

    /** manifest.json内容 */
    private String manifestJson;

    /** 安装时间 */
    private java.util.Date installTime;

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getManifestJson() {
        return manifestJson;
    }

    public void setManifestJson(String manifestJson) {
        this.manifestJson = manifestJson;
    }

    public java.util.Date getInstallTime() {
        return installTime;
    }

    public void setInstallTime(java.util.Date installTime) {
        this.installTime = installTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("pluginId", getPluginId())
            .append("pluginName", getPluginName())
            .append("processKey", getProcessKey())
            .append("version", getVersion())
            .append("status", getStatus())
            .append("author", getAuthor())
            .append("description", getDescription())
            .append("manifestJson", getManifestJson())
            .append("installTime", getInstallTime())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
