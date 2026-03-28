package com.ruoyi.web.plugin;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * 插件类加载器，实现类隔离
 */
public class PluginClassLoader extends URLClassLoader {
    private final String pluginId;

    private static final String[] SYSTEM_PACKAGES = {
        "java.", "javax.", "sun.", "com.sun.",
        "com.ruoyi.common.", "com.ruoyi.system.", "com.ruoyi.framework."
    };

    public PluginClassLoader(String pluginId, URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.pluginId = pluginId;
    }

    public String getPluginId() {
        return pluginId;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (isSystemClass(name)) {
            return super.loadClass(name, resolve);
        }

        Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass != null) {
            return loadedClass;
        }

        try {
            Class<?> clazz = findClass(name);
            if (resolve) {
                resolveClass(clazz);
            }
            return clazz;
        } catch (ClassNotFoundException e) {
            return super.loadClass(name, resolve);
        }
    }

    private boolean isSystemClass(String name) {
        for (String pkg : SYSTEM_PACKAGES) {
            if (name.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }
}
