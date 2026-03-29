package com.ruoyi.web.plugin;

import com.ruoyi.common.core.plugin.ProcessPlugin;
import com.ruoyi.common.core.plugin.model.PluginInfo;
import com.ruoyi.common.core.plugin.model.PluginManifest;
import com.ruoyi.common.core.plugin.model.SysPlugin;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysPluginService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class PluginManager {

    private static final Logger log = LoggerFactory.getLogger(PluginManager.class);

    @Autowired
    private PluginBeanRegistry beanRegistry;

    @Autowired
    private ISysPluginService pluginService;

    private final Map<String, PluginClassLoader> classLoaders = new ConcurrentHashMap<>();
    private final Map<String, PluginInfo> pluginInfos = new ConcurrentHashMap<>();
    private final Map<String, PluginManifest> manifests = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 应用启动后恢复已安装的插件
     */
    @PostConstruct
    public void init() {
                restorePlugins();
    }

    /**
     * 从数据库恢复已安装的插件
     */
    private void restorePlugins() {
                try {
                        List<SysPlugin> dbPlugins = pluginService.selectPluginList(new SysPlugin());
                        for (SysPlugin dbPlugin : dbPlugins) {
                                try {
                                        // 恢复插件到内存
                                        PluginInfo info = new PluginInfo();
                                        info.setPluginId(dbPlugin.getPluginId());
                                        info.setPluginName(dbPlugin.getPluginName());
                                        info.setProcessKey(dbPlugin.getProcessKey());
                                        info.setVersion(dbPlugin.getVersion());
                                        info.setStatus(dbPlugin.getStatus());
                                        info.setInstallTime(dbPlugin.getInstallTime());
                                        info.setAuthor(dbPlugin.getAuthor());
                                        info.setDescription(dbPlugin.getDescription());
                                        pluginInfos.put(dbPlugin.getPluginId(), info);

                                        // 恢复manifest
                                        if (StringUtils.isNotEmpty(dbPlugin.getManifestJson())) {
                                                PluginManifest manifest = objectMapper.readValue(
                                                        dbPlugin.getManifestJson(), PluginManifest.class);
                                                manifests.put(dbPlugin.getPluginId(), manifest);
                                        }

                                        log.info("恢复插件: {}", dbPlugin.getPluginId());
                                } catch (Exception e) {
                                        log.error("恢复插件失败: {}", dbPlugin.getPluginId(), e);
                                }
                        }
                        log.info("共恢复 {} 个插件", dbPlugins.size());
                } catch (Exception e) {
                        log.error("恢复插件失败", e);
                }
    }

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

    /**
     * 启用插件
     */
    public void enablePlugin(String pluginId) {
        PluginInfo info = pluginInfos.get(pluginId);
        if (info != null) {
            info.setStatus("0"); // 0=正常/启用
            // 更新数据库
            SysPlugin dbPlugin = new SysPlugin();
            dbPlugin.setPluginId(pluginId);
            dbPlugin.setStatus("0");
            pluginService.updatePlugin(dbPlugin);
            log.info("插件 [{}] 已启用", pluginId);
        }
    }

    /**
     * 禁用插件
     */
    public void disablePlugin(String pluginId) {
        PluginInfo info = pluginInfos.get(pluginId);
        if (info != null) {
            info.setStatus("1"); // 1=禁用
            // 更新数据库
            SysPlugin dbPlugin = new SysPlugin();
            dbPlugin.setPluginId(pluginId);
            dbPlugin.setStatus("1");
            pluginService.updatePlugin(dbPlugin);
            log.info("插件 [{}] 已禁用", pluginId);
        }
    }

    /**
     * 检查插件是否启用
     */
    public boolean isPluginEnabled(String pluginId) {
        PluginInfo info = pluginInfos.get(pluginId);
        return info != null && "0".equals(info.getStatus());
    }

    /**
     * 导出插件包
     * @param processType 流程类型
     * @param outputDir 输出目录
     * @return 导出的zip文件
     */
    public File exportPlugin(String processType, String outputDir) throws Exception {
        // 查找对应的插件
        PluginInfo targetPlugin = null;
        for (PluginInfo info : pluginInfos.values()) {
            if (info.getProcessKey().equals(processType)) {
                targetPlugin = info;
                break;
            }
        }

        if (targetPlugin == null) {
            throw new RuntimeException("未找到流程类型为 " + processType + " 的插件");
        }

        String pluginId = targetPlugin.getPluginId();
        PluginManifest manifest = manifests.get(pluginId);
        if (manifest == null) {
            throw new RuntimeException("插件清单不存在: " + pluginId);
        }

        // 创建临时目录用于打包
        File tempDir = new File(outputDir, pluginId + "-plugin-temp");
        if (tempDir.exists()) {
            deleteDirectory(tempDir);
        }
        tempDir.mkdirs();

        try {
            // 1. 复制 manifest.json
            File manifestFile = new File(tempDir, "manifest.json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(manifestFile, manifest);

            // 2. 复制后端 JAR
            File pluginDir = new File(System.getProperty("user.dir") + "/plugins/" + pluginId + "-plugin");
            if (pluginDir.exists()) {
                File backendDir = new File(tempDir, "backend");
                backendDir.mkdirs();
                File[] jars = new File(pluginDir, "backend").listFiles((d, name) -> name.endsWith(".jar"));
                if (jars != null && jars.length > 0) {
                    Files.copy(jars[0].toPath(), new File(backendDir, jars[0].getName()).toPath());
                }

                // 3. 复制前端 JS
                File frontendDir = new File(tempDir, "frontend");
                frontendDir.mkdirs();
                File[] jsFiles = new File(pluginDir, "frontend").listFiles((d, name) -> name.endsWith(".js"));
                if (jsFiles != null && jsFiles.length > 0) {
                    Files.copy(jsFiles[0].toPath(), new File(frontendDir, jsFiles[0].getName()).toPath());
                }

                // 4. 复制 BPMN
                File bpmnDir = new File(tempDir, "bpmn");
                bpmnDir.mkdirs();
                File[] bpmnFiles = new File(pluginDir, "bpmn").listFiles((d, name) -> name.endsWith(".xml"));
                if (bpmnFiles != null && bpmnFiles.length > 0) {
                    Files.copy(bpmnFiles[0].toPath(), new File(bpmnDir, bpmnFiles[0].getName()).toPath());
                }
            }

            // 5. 打包为 zip
            File zipFile = new File(outputDir, pluginId + "-plugin.zip");
            zipDirectory(tempDir, zipFile);

            // 清理临时目录
            deleteDirectory(tempDir);

            log.info("插件 [{}] 导出成功: {}", pluginId, zipFile.getAbsolutePath());
            return zipFile;

        } catch (Exception e) {
            deleteDirectory(tempDir);
            throw e;
        }
    }

    private void zipDirectory(File dir, File zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            Path dirPath = dir.toPath();
            Files.walk(dirPath)
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    ZipEntry zipEntry = new ZipEntry(dirPath.relativize(path).toString().replace("\\", "/"));
                    try {
                        zos.putNextEntry(zipEntry);
                        Files.copy(path, zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        log.error("打包文件失败: {}", path, e);
                    }
                });
        }
    }

    private void deleteDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            dir.delete();
        }
    }
}
