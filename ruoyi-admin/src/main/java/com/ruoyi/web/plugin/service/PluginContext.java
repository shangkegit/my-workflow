package com.ruoyi.web.plugin.service;

import com.ruoyi.common.core.plugin.model.PluginManifest;
import com.ruoyi.common.core.plugin.model.MenuConfig;
import java.io.File;
import java.util.*;

public class PluginContext {
    private PluginManifest manifest;
    private String pluginId;
    private File pluginDir;
    private File jarFile;
    private File frontendFile;
    private File bpmnFile;
    private List<String> sqlScripts = new ArrayList<>();
    private List<MenuConfig> menus = new ArrayList<>();
    private Set<String> checkpoints = new LinkedHashSet<>();

    public void checkpoint(String step) {
        checkpoints.add(step);
    }

    public boolean hasCheckpoint(String step) {
        return checkpoints.contains(step);
    }

    // getters and setters
    public PluginManifest getManifest() { return manifest; }
    public void setManifest(PluginManifest manifest) { this.manifest = manifest; }
    public String getPluginId() { return pluginId; }
    public void setPluginId(String pluginId) { this.pluginId = pluginId; }
    public File getPluginDir() { return pluginDir; }
    public void setPluginDir(File pluginDir) { this.pluginDir = pluginDir; }
    public File getJarFile() { return jarFile; }
    public void setJarFile(File jarFile) { this.jarFile = jarFile; }
    public File getFrontendFile() { return frontendFile; }
    public void setFrontendFile(File frontendFile) { this.frontendFile = frontendFile; }
    public File getBpmnFile() { return bpmnFile; }
    public void setBpmnFile(File bpmnFile) { this.bpmnFile = bpmnFile; }
    public List<String> getSqlScripts() { return sqlScripts; }
    public void setSqlScripts(List<String> sqlScripts) { this.sqlScripts = sqlScripts; }
    public List<MenuConfig> getMenus() { return menus; }
    public void setMenus(List<MenuConfig> menus) { this.menus = menus; }
}
