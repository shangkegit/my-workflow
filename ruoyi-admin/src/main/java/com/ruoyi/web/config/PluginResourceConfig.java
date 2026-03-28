package com.ruoyi.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 插件静态资源映射配置
 */
@Configuration
public class PluginResourceConfig implements WebMvcConfigurer {

    @Value("${plugin.storage.path:./plugins}")
    private String pluginStoragePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射插件静态资源
        // 访问 /plugins/** 会映射到插件存储目录
        registry.addResourceHandler("/plugins/**")
                .addResourceLocations("file:" + pluginStoragePath + "/");
    }
}
