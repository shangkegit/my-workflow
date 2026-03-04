package com.ruoyi.workflow.module;

import com.ruoyi.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流程模块注册表
 * 管理所有已部署的流程模块
 * 
 * @author OpenClaw Agent
 * @date 2026-03-03
 */
@Component
public class ProcessModuleRegistry {

    private static final Logger log = LoggerFactory.getLogger(ProcessModuleRegistry.class);

    /**
     * 模块存储：processKey -> ProcessModule
     */
    private final Map<String, ProcessModule> moduleMap = new ConcurrentHashMap<>();

    /**
     * 元数据存储：processKey -> ProcessModuleMeta
     */
    private final Map<String, ProcessModuleMeta> metaMap = new ConcurrentHashMap<>();

    /**
     * 注册模块
     */
    public void register(ProcessModule module) {
        ProcessModuleMeta meta = module.getMeta();
        if (meta == null || StringUtils.isEmpty(meta.getProcessKey())) {
            throw new IllegalArgumentException("模块元信息或 processKey 不能为空");
        }

        String processKey = meta.getProcessKey();
        
        // 如果已存在同名模块，先销毁旧模块
        if (moduleMap.containsKey(processKey)) {
            try {
                ProcessModule oldModule = moduleMap.get(processKey);
                oldModule.destroy();
                log.info("已销毁旧模块: {}", processKey);
            } catch (Exception e) {
                log.warn("销毁旧模块失败: {}", processKey, e);
            }
        }

        // 初始化新模块
        try {
            module.initialize();
        } catch (Exception e) {
            throw new RuntimeException("模块初始化失败: " + processKey, e);
        }

        // 注册
        moduleMap.put(processKey, module);
        metaMap.put(processKey, meta);
        meta.setStatus("active");
        meta.setUpdateTime(new Date());

        log.info("模块注册成功: {} - {}", processKey, meta.getName());
    }

    /**
     * 注销模块
     */
    public void unregister(String processKey) {
        ProcessModule module = moduleMap.remove(processKey);
        metaMap.remove(processKey);

        if (module != null) {
            try {
                module.destroy();
                log.info("模块已注销: {}", processKey);
            } catch (Exception e) {
                log.warn("销毁模块失败: {}", processKey, e);
            }
        }
    }

    /**
     * 获取模块
     */
    public ProcessModule getModule(String processKey) {
        return moduleMap.get(processKey);
    }

    /**
     * 获取模块元信息
     */
    public ProcessModuleMeta getMeta(String processKey) {
        return metaMap.get(processKey);
    }

    /**
     * 获取所有已注册的模块
     */
    public List<ProcessModule> getAllModules() {
        return new ArrayList<>(moduleMap.values());
    }

    /**
     * 获取所有模块元信息
     */
    public List<ProcessModuleMeta> getAllMetas() {
        return new ArrayList<>(metaMap.values());
    }

    /**
     * 检查模块是否存在
     */
    public boolean exists(String processKey) {
        return moduleMap.containsKey(processKey);
    }

    /**
     * 暂停模块
     */
    public void suspend(String processKey) {
        ProcessModuleMeta meta = metaMap.get(processKey);
        if (meta != null) {
            meta.setStatus("suspended");
            meta.setUpdateTime(new Date());
            log.info("模块已暂停: {}", processKey);
        }
    }

    /**
     * 激活模块
     */
    public void activate(String processKey) {
        ProcessModuleMeta meta = metaMap.get(processKey);
        if (meta != null) {
            meta.setStatus("active");
            meta.setUpdateTime(new Date());
            log.info("模块已激活: {}", processKey);
        }
    }

    /**
     * 检查模块是否激活
     */
    public boolean isActive(String processKey) {
        ProcessModuleMeta meta = metaMap.get(processKey);
        return meta != null && "active".equals(meta.getStatus());
    }

    /**
     * 获取模块数量
     */
    public int size() {
        return moduleMap.size();
    }
}
