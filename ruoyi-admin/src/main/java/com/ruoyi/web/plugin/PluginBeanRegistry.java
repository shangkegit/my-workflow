package com.ruoyi.web.plugin;

import com.ruoyi.common.core.plugin.ProcessPlugin;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PluginBeanRegistry {

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    private final Map<String, ProcessPlugin> plugins = new ConcurrentHashMap<>();

    public void registerPlugin(String pluginId, ProcessPlugin plugin) {
        plugin.initialize();
        plugins.put(pluginId, plugin);

        DefaultListableBeanFactory beanFactory =
            (DefaultListableBeanFactory) applicationContext.getBeanFactory();

        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(plugin.getClass());
        beanDefinition.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON);

        beanFactory.registerBeanDefinition("plugin_" + pluginId, beanDefinition);
    }

    public void unregisterPlugin(String pluginId) {
        ProcessPlugin plugin = plugins.get(pluginId);
        if (plugin != null) {
            plugin.destroy();
            plugins.remove(pluginId);

            DefaultListableBeanFactory beanFactory =
                (DefaultListableBeanFactory) applicationContext.getBeanFactory();
            if (beanFactory.containsBeanDefinition("plugin_" + pluginId)) {
                beanFactory.removeBeanDefinition("plugin_" + pluginId);
            }
        }
    }

    public ProcessPlugin getPlugin(String pluginId) {
        return plugins.get(pluginId);
    }

    public Collection<ProcessPlugin> getAllPlugins() {
        return plugins.values();
    }

    public boolean hasPlugin(String pluginId) {
        return plugins.containsKey(pluginId);
    }
}
