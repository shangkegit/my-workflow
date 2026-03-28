package com.ruoyi.web.plugin;

import com.ruoyi.common.core.plugin.ProcessPlugin;
import com.ruoyi.common.core.plugin.model.PluginInfo;
import com.ruoyi.common.core.plugin.model.PluginManifest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class PluginManager {

    @Autowired
    private PluginBeanRegistry beanRegistry;

    private final Map<String, PluginClassLoader> classLoaders = new ConcurrentHashMap<>();
    private final Map<String, PluginInfo> pluginInfos = new ConcurrentHashMap<>();
    private final Map<String, PluginManifest> manifests = new ConcurrentHashMap<>();

    public void loadPlugin(File jarFile, PluginManifest manifest) throws Exception {
        String pluginId = manifest.getId();

        URL[] urls = { jarFile.toURI().toURL() };
        PluginClassLoader classLoader = new PluginClassLoader(
            pluginId, urls, getClass().getClassLoader()
        );
        classLoaders.put(pluginId, classLoader);

        String pluginClassName = manifest.getBackend().getPluginClass();
        Class<?> pluginClass = classLoader.loadClass(pluginClassName);
        ProcessPlugin plugin = (ProcessPlugin) pluginClass.getDeclaredConstructor().newInstance();

        beanRegistry.registerPlugin(pluginId, plugin);

        PluginInfo info = new PluginInfo();
        info.setPluginId(pluginId);
        info.setPluginName(manifest.getName());
        info.setProcessKey(manifest.getProcessKey());
        info.setVersion(manifest.getVersion());
        info.setStatus("ENABLED");
        info.setInstallTime(new java.util.Date());
        info.setAuthor(manifest.getAuthor());
        info.setDescription(manifest.getDescription());
        pluginInfos.put(pluginId, info);

        manifests.put(pluginId, manifest);
    }

    public void unloadPlugin(String pluginId) {
        beanRegistry.unregisterPlugin(pluginId);
        classLoaders.remove(pluginId);
        pluginInfos.remove(pluginId);
        manifests.remove(pluginId);
    }

    public PluginInfo getPluginInfo(String pluginId) {
        return pluginInfos.get(pluginId);
    }

    public List<PluginInfo> listAllPlugins() {
        return pluginInfos.values().stream().collect(Collectors.toList());
    }

    public PluginManifest getManifest(String pluginId) {
        return manifests.get(pluginId);
    }

    public ProcessPlugin getPlugin(String pluginId) {
        return beanRegistry.getPlugin(pluginId);
    }
}
