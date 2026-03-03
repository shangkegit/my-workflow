package com.ruoyi.workflow.module;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.HashMap;
import java.util.Map;

/**
 * 基于脚本的流程模块实现
 * 使用 Groovy 脚本处理任务逻辑
 * 
 * @author OpenClaw Agent
 * @date 2026-03-03
 */
public class ScriptProcessModule implements ProcessModule {

    private static final Logger log = LoggerFactory.getLogger(ScriptProcessModule.class);

    private final ProcessModuleMeta meta;
    private final Map<String, String> scripts;
    private final JSONObject fullConfig;
    private ScriptEngine groovyEngine;

    public ScriptProcessModule(ProcessModuleMeta meta, Map<String, String> scripts, JSONObject fullConfig) {
        this.meta = meta;
        this.scripts = scripts;
        this.fullConfig = fullConfig;
    }

    @Override
    public ProcessModuleMeta getMeta() {
        return meta;
    }

    @Override
    public void initialize() throws Exception {
        // 初始化 Groovy 脚本引擎
        ScriptEngineManager manager = new ScriptEngineManager();
        groovyEngine = manager.getEngineByName("groovy");
        
        if (groovyEngine == null) {
            log.warn("Groovy 引擎不可用，将使用简单的表达式解析");
        }
        
        log.info("模块初始化完成: {}", meta.getProcessKey());
    }

    @Override
    public void destroy() {
        groovyEngine = null;
        log.info("模块销毁完成: {}", meta.getProcessKey());
    }

    @Override
    public Map<String, Object> handleTask(String taskKey, String taskId, String businessKey, Map<String, Object> variables) {
        Map<String, Object> result = new HashMap<>();
        
        ProcessModuleMeta.TaskHandlerMeta handlerMeta = meta.getTaskHandlers().get(taskKey);
        if (handlerMeta == null) {
            log.warn("未找到任务处理器: {} -> {}", meta.getProcessKey(), taskKey);
            result.put("success", false);
            result.put("message", "未找到任务处理器");
            return result;
        }

        String handlerScript = handlerMeta.getHandlerScript();
        if (handlerScript == null || handlerScript.isEmpty()) {
            // 如果没有脚本，返回成功（仅记录）
            log.info("任务无处理脚本，跳过: {} -> {}", meta.getProcessKey(), taskKey);
            result.put("success", true);
            return result;
        }

        try {
            // 尝试执行 Groovy 脚本
            if (groovyEngine != null && handlerScript.contains("{")) {
                // 看起来是 Groovy 代码
                Map<String, Object> bindings = new HashMap<>();
                bindings.put("taskId", taskId);
                bindings.put("businessKey", businessKey);
                bindings.put("variables", variables);
                bindings.put("result", result);
                bindings.put("log", log);
                
                groovyEngine.getBindings(javax.script.ScriptContext.ENGINE_SCOPE).putAll(bindings);
                Object scriptResult = groovyEngine.eval(handlerScript);
                
                if (scriptResult instanceof Map) {
                    result.putAll((Map) scriptResult);
                }
                result.put("success", true);
            } else {
                // 简单的表达式或 JSON 配置
                log.info("执行简单处理器: {}", handlerScript);
                result.put("success", true);
                result.put("action", handlerScript);
            }
        } catch (Exception e) {
            log.error("任务处理失败: {} -> {}", meta.getProcessKey(), taskKey, e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> getFormData(String taskKey, String businessKey) {
        Map<String, Object> formData = new HashMap<>();
        
        // 从配置中获取表单配置
        JSONObject formsConfig = fullConfig.getJSONObject("forms");
        if (formsConfig != null) {
            JSONObject taskForm = formsConfig.getJSONObject(taskKey);
            if (taskForm != null) {
                formData.putAll(taskForm);
            }
        }
        
        return formData;
    }

    @Override
    public void beforeStartProcess(String businessKey, Map<String, Object> variables) {
        log.info("流程启动前回调: {} -> {}", meta.getProcessKey(), businessKey);
    }

    @Override
    public void afterProcessComplete(String businessKey, Map<String, Object> variables) {
        log.info("流程完成后回调: {} -> {}", meta.getProcessKey(), businessKey);
    }
}
