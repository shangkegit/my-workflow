package com.ruoyi.web.plugin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.plugin.model.MenuConfig;
import com.ruoyi.common.core.plugin.model.PluginManifest;
import com.ruoyi.web.plugin.PluginManager;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Deployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;

@Service
public class PluginDeployService {

    private static final Logger log = LoggerFactory.getLogger(PluginDeployService.class);

    @Autowired
    private PluginStorageService storageService;
    @Autowired
    private PluginManager pluginManager;
    @Autowired
    private PluginDatabaseService databaseService;
    @Autowired
    private PluginMenuService menuService;
    @Autowired
    private RepositoryService repositoryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public PluginContext deploy(MultipartFile file) throws PluginDeployException {
        try {
            File tempFile = File.createTempFile("plugin-", ".zip");
            file.transferTo(tempFile);
            PluginContext context = deploy(tempFile);
            tempFile.delete();
            return context;
        } catch (Exception e) {
            throw new PluginDeployException("部署失败", e);
        }
    }

    public PluginContext deploy(File zipFile) throws PluginDeployException {
        PluginContext context = new PluginContext();

        try {
            // 步骤1：解压
            context = storageService.extractPlugin(zipFile);
            context.checkpoint("extracted");
            log.info("[{}] 步骤1: 解压完成", context.getPluginId());

            // 步骤2：解析 manifest
            File manifestFile = new File(context.getPluginDir(), "manifest.json");
            PluginManifest manifest = objectMapper.readValue(manifestFile, PluginManifest.class);
            context.setManifest(manifest);
            context.checkpoint("manifest_parsed");
            log.info("[{}] 步骤2: 解析 manifest 完成 - {}", context.getPluginId(), manifest.getName());

            validateContext(context);

            // 步骤3：执行 SQL
            if (!context.getSqlScripts().isEmpty()) {
                databaseService.executeSqlScripts(context.getSqlScripts(), context.getPluginId());
            }
            context.checkpoint("database_executed");
            log.info("[{}] 步骤3: 数据库脚本执行完成", context.getPluginId());

            // 步骤4：导入菜单
            File menuFile = new File(context.getPluginDir(), "menu/menu.json");
            if (menuFile.exists()) {
                List<MenuConfig> menus = Arrays.asList(objectMapper.readValue(menuFile, MenuConfig[].class));
                context.setMenus(menus);
                menuService.importMenus(menus, context.getPluginId());
            }
            context.checkpoint("menu_imported");
            log.info("[{}] 步骤4: 菜单导入完成", context.getPluginId());

            // 步骤5：加载 Java 插件
            if (context.getJarFile() != null) {
                pluginManager.loadPlugin(context.getJarFile(), manifest);
            }
            context.checkpoint("plugin_loaded");
            log.info("[{}] 步骤5: Java 插件加载完成", context.getPluginId());

            // 步骤6：部署 BPMN
            if (context.getBpmnFile() != null) {
                Deployment deployment = repositoryService.createDeployment()
                    .addInputStream(context.getBpmnFile().getName(), new FileInputStream(context.getBpmnFile()))
                    .name(manifest.getName())
                    .deploy();
                log.info("[{}] 步骤6: BPMN 部署完成 - {}", context.getPluginId(), deployment.getId());
            }
            context.checkpoint("bpmn_deployed");

            log.info("[{}] 插件部署成功!", context.getPluginId());
            return context;

        } catch (Exception e) {
            log.error("[{}] 部署失败，开始回滚", context.getPluginId(), e);
            rollback(context);
            throw new PluginDeployException(context.getPluginId(), "部署失败: " + e.getMessage(), e);
        }
    }

    private void validateContext(PluginContext context) throws PluginDeployException {
        if (context.getManifest() == null) {
            throw new PluginDeployException("缺少 manifest.json");
        }
        if (context.getBpmnFile() == null) {
            throw new PluginDeployException("缺少 BPMN 流程定义文件");
        }
    }

    private void rollback(PluginContext context) {
        String pluginId = context.getPluginId();

        if (context.hasCheckpoint("plugin_loaded")) {
            try { pluginManager.unloadPlugin(pluginId); } catch (Exception e) { }
        }
        if (context.hasCheckpoint("menu_imported")) {
            try { menuService.deleteMenus(pluginId); } catch (Exception e) { }
        }
        try { storageService.deletePluginDir(pluginId); } catch (Exception e) { }
    }

    public void undeploy(String pluginId) {
        PluginContext context = new PluginContext();
        context.setPluginId(pluginId);
        context.checkpoint("plugin_loaded");
        context.checkpoint("menu_imported");
        rollback(context);
    }
}
