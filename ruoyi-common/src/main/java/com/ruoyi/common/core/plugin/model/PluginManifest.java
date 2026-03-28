package com.ruoyi.common.core.plugin.model;

import java.util.List;
import java.util.Map;

/**
 * 插件清单信息（对应 manifest.json）
 */
public class PluginManifest {
    private String id;
    private String name;
    private String version;
    private String processKey;
    private String author;
    private String description;
    private BackendConfig backend;
    private FrontendConfig frontend;
    private Map<String, List<String>> permissions;

    public static class BackendConfig {
        private String pluginClass;

        public String getPluginClass() {
            return pluginClass;
        }

        public void setPluginClass(String pluginClass) {
            this.pluginClass = pluginClass;
        }
    }

    public static class FrontendConfig {
        private String component;

        public String getComponent() {
            return component;
        }

        public void setComponent(String component) {
            this.component = component;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getProcessKey() {
        return processKey;
    }

    public void setProcessKey(String processKey) {
        this.processKey = processKey;
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

    public BackendConfig getBackend() {
        return backend;
    }

    public void setBackend(BackendConfig backend) {
        this.backend = backend;
    }

    public FrontendConfig getFrontend() {
        return frontend;
    }

    public void setFrontend(FrontendConfig frontend) {
        this.frontend = frontend;
    }

    public Map<String, List<String>> getPermissions() {
        return permissions;
    }

    public void setPermissions(Map<String, List<String>> permissions) {
        this.permissions = permissions;
    }
}
