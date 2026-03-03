package com.ruoyi.workflow.module;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * 流程模块动态加载器
 * 从 JSON 配置和脚本文件加载流程模块
 * 
 * @author OpenClaw Agent
 * @date 2026-03-03
 */
@Component
public class ProcessModuleLoader {

    private static final Logger log = LoggerFactory.getLogger(ProcessModuleLoader.class);

    @Autowired
    private ProcessModuleRegistry registry;

    /**
     * 从 JSON 配置加载模块
     * 
     * @param jsonConfig JSON 配置字符串
     * @return 加载的模块
     */
    public ProcessModule loadFromJson(String jsonConfig) throws Exception {
        JSONObject config = JSON.parseObject(jsonConfig);
        
        // 解析元信息
        JSONObject metaJson = config.getJSONObject("meta");
        ProcessModuleMeta meta = new ProcessModuleMeta();
        meta.setId(metaJson.getString("id"));
        meta.setName(metaJson.getString("name"));
        meta.setProcessKey(metaJson.getString("processKey"));
        meta.setVersion(metaJson.getString("version"));
        meta.setAuthor(metaJson.getString("author"));
        meta.setDescription(metaJson.getString("description"));
        meta.setDeploySource("import");
        meta.setDeployTime(new java.util.Date());

        // 解析任务处理器
        JSONObject tasksJson = config.getJSONObject("tasks");
        Map<String, ProcessModuleMeta.TaskHandlerMeta> taskHandlers = new HashMap<>();
        
        if (tasksJson != null) {
            for (String taskKey : tasksJson.keySet()) {
                JSONObject taskJson = tasksJson.getJSONObject(taskKey);
                ProcessModuleMeta.TaskHandlerMeta handler = new ProcessModuleMeta.TaskHandlerMeta();
                handler.setTaskKey(taskKey);
                handler.setTaskName(taskJson.getString("name"));
                handler.setFormKey(taskJson.getString("formKey"));
                handler.setHandlerScript(taskJson.getString("handler"));
                taskHandlers.put(taskKey, handler);
            }
        }
        meta.setTaskHandlers(taskHandlers);

        // 解析脚本
        JSONObject scriptsJson = config.getJSONObject("scripts");
        Map<String, String> scripts = new HashMap<>();
        if (scriptsJson != null) {
            for (String scriptName : scriptsJson.keySet()) {
                scripts.put(scriptName, scriptsJson.getString(scriptName));
            }
        }

        // 创建模块实例
        ProcessModule module = new ScriptProcessModule(meta, scripts, config);
        
        return module;
    }

    /**
     * 从文件加载模块
     */
    public ProcessModule loadFromFile(File file) throws Exception {
        String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        return loadFromJson(content);
    }

    /**
     * 加载并注册模块
     */
    public void loadAndRegister(String jsonConfig) throws Exception {
        ProcessModule module = loadFromJson(jsonConfig);
        registry.register(module);
        log.info("模块加载并注册成功: {}", module.getMeta().getProcessKey());
    }

    /**
     * 加载并注册模块（从文件）
     */
    public void loadAndRegister(File file) throws Exception {
        ProcessModule module = loadFromFile(file);
        registry.register(module);
        log.info("模块从文件加载并注册成功: {}", module.getMeta().getProcessKey());
    }
}
