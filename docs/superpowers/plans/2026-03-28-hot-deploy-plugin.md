# 流程热部署插件系统实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现流程热部署插件系统，支持从开发环境导出流程包并一键导入生产环境，无需重启服务。

**Architecture:** 采用插件化架构，使用自定义 ClassLoader 隔离加载 Java 类，Spring Bean 动态注册，前端组件通过 UMD 格式动态加载。

**Tech Stack:** Spring Boot 2.5.15、Activiti 6.0、Vue 2.6.12、Element UI

**Spec:** [设计文档](../specs/2026-03-28-hot-deploy-plugin-design.md)

---

## Task 1: 插件核心接口与模型定义

**Files:**
- Create: `ruoyi-common/src/main/java/com/ruoyi/common/core/plugin/ProcessPlugin.java`
- Create: `ruoyi-common/src/main/java/com/ruoyi/common/core/plugin/model/PluginManifest.java`
- Create: `ruoyi-common/src/main/java/com/ruoyi/common/core/plugin/model/PluginInfo.java`
- Create: `ruoyi-common/src/main/java/com/ruoyi/common/core/plugin/model/MenuConfig.java`

- [ ] **Step 1: 创建 ProcessPlugin 接口**

```java
package com.ruoyi.common.core.plugin;

import java.util.List;
import java.util.Map;

/**
 * 流程插件接口 - 所有流程插件必须实现此接口
 */
public interface ProcessPlugin {

    /** 获取流程类型标识 */
    String getProcessType();

    /** 获取流程名称 */
    String getProcessName();

    /** 获取表单数据（根据 businessKey） */
    Object getFormData(String businessKey);

    /** 提交表单（发起流程） */
    String submitForm(Map<String, Object> formData, String username);

    /** 获取待办列表数据 */
    List<?> getTodoList(String username);

    /** 初始化插件（可选） */
    default void initialize() {}

    /** 销毁插件（可选） */
    default void destroy() {}
}
```

- [ ] **Step 2: 创建 PluginManifest 模型**

```java
package com.ruoyi.common.core.plugin.model;

import java.util.List;
import java.util.Map;

/**
 * 插件清单信息（对应 manifest.json）
 */
public class PluginManifest {
    private String id;
    private String name;
    private String version;
    private String processKey;
    private String author;
    private String description;
    private BackendConfig backend;
    private FrontendConfig frontend;
    private Map<String, List<String>> permissions;

    // 内部类
    public static class BackendConfig {
        private String pluginClass;
        // getters, setters
    }

    public static class FrontendConfig {
        private String component;
        // getters, setters
    }

    // getters, setters
}
```

- [ ] **Step 3: 创建 PluginInfo 模型**

```java
package com.ruoyi.common.core.plugin.model;

import java.util.Date;

/**
 * 已安装插件信息
 */
public class PluginInfo {
    private String pluginId;
    private String pluginName;
    private String processKey;
    private String version;
    private String status; // ENABLED, DISABLED
    private Date installTime;
    private String author;
    private String description;
    // getters, setters
}
```

- [ ] **Step 4: 创建 MenuConfig 模型**

```java
package com.ruoyi.common.core.plugin.model;

import java.util.List;

/**
 * 菜单配置（对应 menu.json）
 */
public class MenuConfig {
    private String menuName;
    private Long parentId;
    private Integer orderNum;
    private String path;
    private String component;
    private String menuType;
    private String visible;
    private String icon;
    private String perms;
    private List<MenuConfig> children;
    // getters, setters
}
```

- [ ] **Step 5: 编译验证**

Run: `mvn compile -pl ruoyi-common`
Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add ruoyi-common/src/main/java/com/ruoyi/common/core/plugin/
git commit -m "feat(plugin): 添加插件核心接口和模型定义"
```

---

## Task 2: 插件类加载器与 Bean 注册

**Files:**
- Create: `ruoyi-admin/src/main/java/com/ruoyi/web/plugin/PluginClassLoader.java`
- Create: `ruoyi-admin/src/main/java/com/ruoyi/web/plugin/PluginBeanRegistry.java`
- Create: `ruoyi-admin/src/main/java/com/ruoyi/web/plugin/PluginManager.java`

- [ ] **Step 1: 创建 PluginClassLoader**

```java
package com.ruoyi.web.plugin;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * 插件类加载器，实现类隔离
 */
public class PluginClassLoader extends URLClassLoader {
    private final String pluginId;

    // 系统包前缀，这些类委派给父加载器
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
        // 系统类委派给父加载器
        if (isSystemClass(name)) {
            return super.loadClass(name, resolve);
        }

        // 检查是否已加载
        Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass != null) {
            return loadedClass;
        }

        // 插件自己的类优先自己加载
        try {
            Class<?> clazz = findClass(name);
            if (resolve) {
                resolveClass(clazz);
            }
            return clazz;
        } catch (ClassNotFoundException e) {
            // 找不到再委派给父加载器
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
```

- [ ] **Step 2: 创建 PluginBeanRegistry**

```java
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

/**
 * 插件 Bean 注册中心
 */
@Component
public class PluginBeanRegistry {

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    private final Map<String, ProcessPlugin> plugins = new ConcurrentHashMap<>();

    /**
     * 注册插件到 Spring 容器
     */
    public void registerPlugin(String pluginId, ProcessPlugin plugin) {
        // 调用初始化
        plugin.initialize();

        // 缓存插件实例
        plugins.put(pluginId, plugin);

        // 注册为 Spring Bean（可选，用于 @Autowired 注入）
        DefaultListableBeanFactory beanFactory =
            (DefaultListableBeanFactory) applicationContext.getBeanFactory();

        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(plugin.getClass());
        beanDefinition.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON);

        beanFactory.registerBeanDefinition("plugin_" + pluginId, beanDefinition);
    }

    /**
     * 卸载插件
     */
    public void unregisterPlugin(String pluginId) {
        ProcessPlugin plugin = plugins.get(pluginId);
        if (plugin != null) {
            plugin.destroy();
            plugins.remove(pluginId);

            // 移除 Bean 定义
            DefaultListableBeanFactory beanFactory =
                (DefaultListableBeanFactory) applicationContext.getBeanFactory();
            if (beanFactory.containsBeanDefinition("plugin_" + pluginId)) {
                beanFactory.removeBeanDefinition("plugin_" + pluginId);
            }
        }
    }

    /**
     * 获取插件
     */
    public ProcessPlugin getPlugin(String pluginId) {
        return plugins.get(pluginId);
    }

    /**
     * 获取所有插件
     */
    public Collection<ProcessPlugin> getAllPlugins() {
        return plugins.values();
    }

    /**
     * 检查插件是否存在
     */
    public boolean hasPlugin(String pluginId) {
        return plugins.containsKey(pluginId);
    }
}
```

- [ ] **Step 3: 创建 PluginManager**

```java
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

/**
 * 插件管理器
 */
@Service
public class PluginManager {

    @Autowired
    private PluginBeanRegistry beanRegistry;

    // ClassLoader 缓存
    private final Map<String, PluginClassLoader> classLoaders = new ConcurrentHashMap<>();

    // 插件信息缓存
    private final Map<String, PluginInfo> pluginInfos = new ConcurrentHashMap<>();

    // 插件清单缓存
    private final Map<String, PluginManifest> manifests = new ConcurrentHashMap<>();

    /**
     * 加载插件 JAR
     */
    public void loadPlugin(File jarFile, PluginManifest manifest) throws Exception {
        String pluginId = manifest.getId();

        // 创建隔离的 ClassLoader
        URL[] urls = { jarFile.toURI().toURL() };
        PluginClassLoader classLoader = new PluginClassLoader(
            pluginId, urls, getClass().getClassLoader()
        );
        classLoaders.put(pluginId, classLoader);

        // 加载插件实现类
        String pluginClassName = manifest.getBackend().getPluginClass();
        Class<?> pluginClass = classLoader.loadClass(pluginClassName);
        ProcessPlugin plugin = (ProcessPlugin) pluginClass.getDeclaredConstructor().newInstance();

        // 注册到 Spring
        beanRegistry.registerPlugin(pluginId, plugin);

        // 缓存信息
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

    /**
     * 卸载插件
     */
    public void unloadPlugin(String pluginId) {
        beanRegistry.unregisterPlugin(pluginId);
        classLoaders.remove(pluginId);
        pluginInfos.remove(pluginId);
        manifests.remove(pluginId);
    }

    /**
     * 获取插件信息
     */
    public PluginInfo getPluginInfo(String pluginId) {
        return pluginInfos.get(pluginId);
    }

    /**
     * 获取所有插件信息
     */
    public List<PluginInfo> listAllPlugins() {
        return pluginInfos.values().stream().collect(Collectors.toList());
    }

    /**
     * 获取插件清单
     */
    public PluginManifest getManifest(String pluginId) {
        return manifests.get(pluginId);
    }

    /**
     * 获取插件实例
     */
    public ProcessPlugin getPlugin(String pluginId) {
        return beanRegistry.getPlugin(pluginId);
    }
}
```

- [ ] **Step 4: 编译验证**

Run: `mvn compile -pl ruoyi-admin -am`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add ruoyi-admin/src/main/java/com/ruoyi/web/plugin/
git commit -m "feat(plugin): 添加插件类加载器和 Bean 注册机制"
```

---

## Task 3: 数据库脚本执行服务

**Files:**
- Create: `ruoyi-admin/src/main/java/com/ruoyi/web/plugin/service/PluginDatabaseService.java`

- [ ] **Step 1: 创建 PluginDatabaseService**

```java
package com.ruoyi.web.plugin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * 插件数据库脚本执行服务
 */
@Service
public class PluginDatabaseService {

    private static final Logger log = LoggerFactory.getLogger(PluginDatabaseService.class);

    @Autowired
    private DataSource dataSource;

    /**
     * 执行插件数据库脚本
     */
    @Transactional
    public void executeSqlScripts(List<String> sqlScripts, String pluginId) {
        try (Connection conn = dataSource.getConnection()) {
            for (String sql : sqlScripts) {
                executeSqlFile(conn, sql, pluginId);
            }
            log.info("插件 [{}] 数据库脚本执行成功", pluginId);
        } catch (SQLException e) {
            throw new RuntimeException("执行插件 SQL 失败: " + pluginId, e);
        }
    }

    private void executeSqlFile(Connection conn, String sqlContent, String pluginId) throws SQLException {
        // 按分号分割执行多条 SQL
        String[] statements = sqlContent.split(";");

        try (Statement stmt = conn.createStatement()) {
            for (String sql : statements) {
                String trimmed = sql.trim();
                // 跳过空语句和注释
                if (trimmed.isEmpty() || trimmed.startsWith("--") || trimmed.startsWith("/*")) {
                    continue;
                }
                try {
                    stmt.execute(trimmed);
                    log.debug("执行 SQL: {}", trimmed.substring(0, Math.min(50, trimmed.length())));
                } catch (SQLException e) {
                    log.error("SQL 执行失败: {}", trimmed);
                    throw e;
                }
            }
        }
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl ruoyi-admin -am`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add ruoyi-admin/src/main/java/com/ruoyi/web/plugin/service/PluginDatabaseService.java
git commit -m "feat(plugin): 添加数据库脚本执行服务"
```

---

## Task 4: 菜单导入服务

**Files:**
- Create: `ruoyi-admin/src/main/java/com/ruoyi/web/plugin/service/PluginMenuService.java`
- Modify: `ruoyi-system/src/main/java/com/ruoyi/system/mapper/SysMenuMapper.java`
- Modify: `ruoyi-system/src/main/resources/mapper/system/SysMenuMapper.xml`

- [ ] **Step 1: 修改 SysMenuMapper 添加插件相关方法**

在 `SysMenuMapper.java` 中添加：

```java
/**
 * 根据插件ID查询菜单
 */
public List<SysMenu> selectMenuByPluginId(String pluginId);

/**
 * 根据插件ID删除菜单
 */
public int deleteMenuByPluginId(String pluginId);
```

- [ ] **Step 2: 修改 SysMenuMapper.xml 添加 SQL**

在 `SysMenuMapper.xml` 中添加：

```xml
<select id="selectMenuByPluginId" parameterType="String" resultMap="SysMenuResult">
    select menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
    from sys_menu where plugin_id = #{pluginId}
</select>

<delete id="deleteMenuByPluginId" parameterType="String">
    delete from sys_menu where plugin_id = #{pluginId}
</delete>
```

- [ ] **Step 3: 创建 PluginMenuService**

```java
package com.ruoyi.web.plugin.service;

import com.ruoyi.common.core.plugin.model.MenuConfig;
import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.system.mapper.SysMenuMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 插件菜单导入服务
 */
@Service
public class PluginMenuService {

    private static final Logger log = LoggerFactory.getLogger(PluginMenuService.class);

    @Autowired
    private SysMenuMapper menuMapper;

    /**
     * 导入菜单配置，自动生成新 ID
     */
    public void importMenus(List<MenuConfig> menus, String pluginId) {
        // ID 映射表：原始ID/标识 -> 新生成的ID
        Map<String, Long> idMapping = new HashMap<>();

        // 递归导入菜单
        importMenuRecursive(menus, 0L, pluginId, idMapping);

        log.info("插件 [{}] 导入菜单 {} 个", pluginId, idMapping.size());
    }

    private void importMenuRecursive(List<MenuConfig> menus, Long parentId, String pluginId, Map<String, Long> idMapping) {
        if (menus == null) return;

        int orderNum = 1;
        for (MenuConfig config : menus) {
            Long newId = generateId();
            idMapping.put(config.getPath(), newId); // 使用 path 作为原始标识

            SysMenu menu = new SysMenu();
            menu.setMenuId(newId);
            menu.setMenuName(config.getMenuName());
            menu.setParentId(parentId);
            menu.setOrderNum(config.getOrderNum() != null ? config.getOrderNum() : orderNum++);
            menu.setPath(config.getPath());
            menu.setComponent(config.getComponent());
            menu.setMenuType(config.getMenuType() != null ? config.getMenuType() : "C");
            menu.setVisible(config.getVisible() != null ? config.getVisible() : "0");
            menu.setIcon(config.getIcon());
            menu.setPerms(config.getPerms());
            menu.setStatus("0");
            menu.setCreateBy("plugin");
            menu.setRemark("plugin:" + pluginId);

            menuMapper.insertMenu(menu);

            // 递归处理子菜单
            if (config.getChildren() != null && !config.getChildren().isEmpty()) {
                importMenuRecursive(config.getChildren(), newId, pluginId, idMapping);
            }
        }
    }

    /**
     * 删除插件菜单
     */
    public void deleteMenus(String pluginId) {
        menuMapper.deleteMenuByPluginId(pluginId);
        log.info("插件 [{}] 菜单已删除", pluginId);
    }

    /**
     * 生成菜单 ID（使用时间戳 + 随机数）
     */
    private Long generateId() {
        return System.currentTimeMillis() * 1000 + (long)(Math.random() * 1000);
    }
}
```

- [ ] **Step 4: 修改数据库表结构**

需要在 `sys_menu` 表添加 `plugin_id` 字段：

```sql
ALTER TABLE sys_menu ADD COLUMN plugin_id VARCHAR(64) DEFAULT NULL COMMENT '插件ID';
```

- [ ] **Step 5: 修改 SysMenu 实体类**

在 `SysMenu.java` 中添加：

```java
/** 插件ID */
private String pluginId;

public String getPluginId() {
    return pluginId;
}

public void setPluginId(String pluginId) {
    this.pluginId = pluginId;
}
```

- [ ] **Step 6: 编译验证**

Run: `mvn compile -pl ruoyi-admin -am`
Expected: BUILD SUCCESS

- [ ] **Step 7: 提交**

```bash
git add ruoyi-admin/src/main/java/com/ruoyi/web/plugin/service/PluginMenuService.java
git add ruoyi-system/src/main/java/com/ruoyi/system/mapper/SysMenuMapper.java
git add ruoyi-system/src/main/resources/mapper/system/SysMenuMapper.xml
git add ruoyi-common/src/main/java/com/ruoyi/common/core/domain/entity/SysMenu.java
git commit -m "feat(plugin): 添加菜单导入服务"
```

---

## Task 5: 插件部署服务（核心）

**Files:**
- Create: `ruoyi-admin/src/main/java/com/ruoyi/web/plugin/service/PluginDeployService.java`
- Create: `ruoyi-admin/src/main/java/com/ruoyi/web/plugin/service/PluginDeployException.java`
- Create: `ruoyi-admin/src/main/java/com/ruoyi/web/plugin/service/PluginContext.java`
- Create: `ruoyi-admin/src/main/java/com/ruoyi/web/plugin/service/PluginStorageService.java`

- [ ] **Step 1: 创建 PluginDeployException**

```java
package com.ruoyi.web.plugin.service;

/**
 * 插件部署异常
 */
public class PluginDeployException extends Exception {
    private String step;

    public PluginDeployException(String message) {
        super(message);
    }

    public PluginDeployException(String message, Throwable cause) {
        super(message, cause);
    }

    public PluginDeployException(String step, String message, Throwable cause) {
        super(message, cause);
        this.step = step;
    }

    public String getStep() {
        return step;
    }
}
```

- [ ] **Step 2: 创建 PluginContext**

```java
package com.ruoyi.web.plugin.service;

import com.ruoyi.common.core.plugin.model.PluginManifest;
import com.ruoyi.common.core.plugin.model.MenuConfig;

import java.io.File;
import java.util.*;

/**
 * 插件部署上下文，用于跟踪部署进度和回滚
 */
public class PluginContext {
    private PluginManifest manifest;
    private String pluginId;
    private File pluginDir;
    private File jarFile;
    private File frontendFile;
    private File bpmnFile;
    private List<String> sqlScripts = new ArrayList<>();
    private List<MenuConfig> menus = new ArrayList<>();

    // 已完成的检查点
    private Set<String> checkpoints = new LinkedHashSet<>();

    public void checkpoint(String step) {
        checkpoints.add(step);
    }

    public boolean hasCheckpoint(String step) {
        return checkpoints.contains(step);
    }

    // getters, setters
}
```

- [ ] **Step 3: 创建 PluginStorageService**

```java
package com.ruoyi.web.plugin.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 插件存储服务
 */
@Service
public class PluginStorageService {

    @Value("${plugin.storage.path:./plugins}")
    private String storagePath;

    /**
     * 解压插件包
     */
    public PluginContext extractPlugin(File zipFile) throws IOException {
        PluginContext context = new PluginContext();

        String fileName = zipFile.getName().replace(".zip", "");
        Path pluginDir = Paths.get(storagePath, fileName);
        Files.createDirectories(pluginDir);
        context.setPluginDir(pluginDir.toFile());
        context.setPluginId(fileName.replace("-plugin", ""));

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path filePath = pluginDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    Files.createDirectories(filePath.getParent());
                    Files.copy(zis, filePath);

                    // 识别文件类型
                    String name = entry.getName();
                    if (name.equals("manifest.json")) {
                        // manifest 稍后解析
                    } else if (name.startsWith("backend/") && name.endsWith(".jar")) {
                        context.setJarFile(filePath.toFile());
                    } else if (name.startsWith("frontend/") && name.endsWith(".js")) {
                        context.setFrontendFile(filePath.toFile());
                    } else if (name.startsWith("bpmn/") && name.endsWith(".xml")) {
                        context.setBpmnFile(filePath.toFile());
                    } else if (name.startsWith("database/") && name.endsWith(".sql")) {
                        context.getSqlScripts().add(new String(Files.readAllBytes(filePath)));
                    }
                }
            }
        }

        return context;
    }

    /**
     * 删除插件目录
     */
    public void deletePluginDir(String pluginId) throws IOException {
        Path pluginDir = Paths.get(storagePath, pluginId + "-plugin");
        if (Files.exists(pluginDir)) {
            Files.walk(pluginDir)
                .sorted((a, b) -> -a.compareTo(b))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        // ignore
                    }
                });
        }
    }

    /**
     * 获取插件前端文件 URL
     */
    public String getFrontendUrl(String pluginId) {
        return "/plugins/" + pluginId + "-plugin/frontend/" + pluginId + "Form.umd.js";
    }
}
```

- [ ] **Step 4: 创建 PluginDeployService**

```java
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
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * 插件部署服务（核心）
 */
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

    /**
     * 部署插件
     */
    public PluginContext deploy(MultipartFile file) throws PluginDeployException {
        try {
            // 保存上传文件
            File tempFile = File.createTempFile("plugin-", ".zip");
            file.transferTo(tempFile);

            PluginContext context = deploy(tempFile);
            tempFile.delete();
            return context;
        } catch (Exception e) {
            throw new PluginDeployException("部署失败", e);
        }
    }

    /**
     * 部署插件
     */
    public PluginContext deploy(File zipFile) throws PluginDeployException {
        PluginContext context = new PluginContext();

        try {
            // 步骤1：解压插件包
            context = storageService.extractPlugin(zipFile);
            context.checkpoint("extracted");
            log.info("[{}] 步骤1: 解压完成", context.getPluginId());

            // 步骤2：解析 manifest
            File manifestFile = new File(context.getPluginDir(), "manifest.json");
            PluginManifest manifest = objectMapper.readValue(manifestFile, PluginManifest.class);
            context.setManifest(manifest);
            context.checkpoint("manifest_parsed");
            log.info("[{}] 步骤2: 解析 manifest 完成 - {}", context.getPluginId(), manifest.getName());

            // 验证必需文件
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
                    .addInputStream(context.getBpmnFile().getName(),
                        org.activiti.engine.repository.InputStreamProvider
                            .of(context.getBpmnFile()))
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
        if (context.getJarFile() == null) {
            log.warn("[{}] 缺少后端 JAR 文件", context.getPluginId());
        }
        if (context.getFrontendFile() == null) {
            log.warn("[{}] 缺少前端 JS 文件", context.getPluginId());
        }
        if (context.getBpmnFile() == null) {
            throw new PluginDeployException("缺少 BPMN 流程定义文件");
        }
    }

    /**
     * 回滚部署
     */
    private void rollback(PluginContext context) {
        String pluginId = context.getPluginId();

        // 按逆序回滚
        if (context.hasCheckpoint("bpmn_deployed")) {
            // Activiti 部署通常不回滚，记录日志
            log.warn("[{}] BPMN 已部署，需手动处理", pluginId);
        }

        if (context.hasCheckpoint("plugin_loaded")) {
            try {
                pluginManager.unloadPlugin(pluginId);
                log.info("[{}] 已卸载 Java 插件", pluginId);
            } catch (Exception e) {
                log.error("[{}] 卸载插件失败", pluginId, e);
            }
        }

        if (context.hasCheckpoint("menu_imported")) {
            try {
                menuService.deleteMenus(pluginId);
                log.info("[{}] 已删除菜单", pluginId);
            } catch (Exception e) {
                log.error("[{}] 删除菜单失败", pluginId, e);
            }
        }

        if (context.hasCheckpoint("database_executed")) {
            // SQL 回滚需要提供 rollback.sql，这里暂不实现
            log.warn("[{}] 数据库脚本需手动回滚", pluginId);
        }

        // 删除插件目录
        try {
            storageService.deletePluginDir(pluginId);
        } catch (Exception e) {
            log.error("[{}] 删除插件目录失败", pluginId, e);
        }
    }

    /**
     * 卸载插件
     */
    public void undeploy(String pluginId) {
        PluginContext context = new PluginContext();
        context.setPluginId(pluginId);
        context.checkpoint("plugin_loaded");
        context.checkpoint("menu_imported");
        rollback(context);
    }
}
```

- [ ] **Step 5: 编译验证**

Run: `mvn compile -pl ruoyi-admin -am`
Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add ruoyi-admin/src/main/java/com/ruoyi/web/plugin/service/
git commit -m "feat(plugin): 添加插件部署服务（核心）"
```

---

## Task 6: 插件控制器

**Files:**
- Create: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/plugin/PluginController.java`

- [ ] **Step 1: 创建 PluginController**

```java
package com.ruoyi.web.controller.plugin;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.core.plugin.model.PluginInfo;
import com.ruoyi.web.plugin.PluginManager;
import com.ruoyi.web.plugin.service.PluginDeployService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 插件管理控制器
 */
@Api(value = "插件管理", tags = "插件管理")
@RestController
@RequestMapping("/plugin")
public class PluginController extends BaseController {

    @Autowired
    private PluginDeployService deployService;

    @Autowired
    private PluginManager pluginManager;

    /**
     * 导入插件
     */
    @ApiOperation("导入插件")
    @Log(title = "插件管理", businessType = BusinessType.IMPORT)
    @PostMapping("/import")
    @PreAuthorize("@ss.hasPermi('plugin:import')")
    public AjaxResult importPlugin(@RequestParam("file") MultipartFile file) {
        try {
            deployService.deploy(file);
            return AjaxResult.success("导入成功");
        } catch (Exception e) {
            return AjaxResult.error("导入失败: " + e.getMessage());
        }
    }

    /**
     * 获取已安装插件列表
     */
    @ApiOperation("获取插件列表")
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('plugin:list')")
    public TableDataInfo listPlugins() {
        List<PluginInfo> list = pluginManager.listAllPlugins();
        return getDataTable(list);
    }

    /**
     * 获取插件详情
     */
    @ApiOperation("获取插件详情")
    @GetMapping("/{pluginId}")
    @PreAuthorize("@ss.hasPermi('plugin:query')")
    public AjaxResult getPlugin(@PathVariable String pluginId) {
        PluginInfo info = pluginManager.getPluginInfo(pluginId);
        if (info == null) {
            return AjaxResult.error("插件不存在");
        }
        return AjaxResult.success(info);
    }

    /**
     * 卸载插件
     */
    @ApiOperation("卸载插件")
    @Log(title = "插件管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{pluginId}")
    @PreAuthorize("@ss.hasPermi('plugin:remove')")
    public AjaxResult uninstallPlugin(@PathVariable String pluginId) {
        try {
            deployService.undeploy(pluginId);
            return AjaxResult.success("卸载成功");
        } catch (Exception e) {
            return AjaxResult.error("卸载失败: " + e.getMessage());
        }
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl ruoyi-admin -am`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add ruoyi-admin/src/main/java/com/ruoyi/web/controller/plugin/PluginController.java
git commit -m "feat(plugin): 添加插件管理控制器"
```

---

## Task 7: 前端插件加载器

**Files:**
- Create: `ruoyi-ui/src/utils/plugin-loader.js`
- Modify: `ruoyi-ui/src/views/todo/components/registry/index.js`

- [ ] **Step 1: 创建 plugin-loader.js**

```javascript
/**
 * 插件组件动态加载器
 */

// 已加载的插件组件缓存
const loadedPlugins = new Map()

// 加载状态追踪
const loadingPlugins = new Map()

/**
 * 动态加载插件前端组件
 * @param {string} processType - 流程类型
 * @param {string} componentUrl - 组件 JS 文件 URL
 * @returns {Promise<Object>} Vue 组件对象
 */
export async function loadPluginComponent(processType, componentUrl) {
  // 检查缓存
  if (loadedPlugins.has(processType)) {
    return loadedPlugins.get(processType)
  }

  // 检查是否正在加载
  if (loadingPlugins.has(processType)) {
    return loadingPlugins.get(processType)
  }

  // 创建加载 Promise
  const loadPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = componentUrl
    script.async = true

    script.onload = () => {
      // 约定：插件组件挂载到 window.__PLUGINS__[processType]
      const component = window.__PLUGINS__?.[processType]
      if (component) {
        loadedPlugins.set(processType, component)
        loadingPlugins.delete(processType)
        resolve(component)
      } else {
        loadingPlugins.delete(processType)
        reject(new Error(`插件组件未正确注册: ${processType}`))
      }
    }

    script.onerror = () => {
      loadingPlugins.delete(processType)
      reject(new Error(`加载插件失败: ${componentUrl}`))
    }

    document.head.appendChild(script)
  })

  loadingPlugins.set(processType, loadPromise)
  return loadPromise
}

/**
 * 检查插件是否已加载
 * @param {string} processType
 * @returns {boolean}
 */
export function isPluginLoaded(processType) {
  return loadedPlugins.has(processType)
}

/**
 * 清除插件缓存
 * @param {string} processType
 */
export function clearPluginCache(processType) {
  loadedPlugins.delete(processType)
  // 清除全局注册
  if (window.__PLUGINS__) {
    delete window.__PLUGINS__[processType]
  }
}
```

- [ ] **Step 2: 修改注册中心**

修改 `ruoyi-ui/src/views/todo/components/registry/index.js`：

```javascript
/**
 * 表单组件注册中心
 * 新增流程只需在此注册组件，无需修改 processTask.vue
 */

import { loadPluginComponent, isPluginLoaded } from '@/utils/plugin-loader'

// 内置组件注册表
const formRegistry = {
  leaveapply: () => import('../forms/leaveApplyForm.vue'),
  meeting: () => import('../forms/meetingForm.vue'),
  purchase: () => import('../forms/purchaseForm.vue')
}

// 插件组件 URL 映射
const pluginUrls = new Map()

/**
 * 获取表单组件加载器
 * @param {string} taskType - 流程类型
 * @returns {Function|null} 组件加载函数
 */
export function getFormComponent(taskType) {
  // 优先检查内置组件
  const loader = formRegistry[taskType]
  if (loader) {
    return loader
  }

  // 检查是否有插件组件
  const pluginUrl = pluginUrls.get(taskType)
  if (pluginUrl) {
    return () => loadPluginComponent(taskType, pluginUrl)
  }

  console.warn(`未注册的流程类型: ${taskType}`)
  return null
}

/**
 * 注册新的表单组件
 * @param {string} taskType - 流程类型
 * @param {Function} componentLoader - 组件加载函数
 */
export function registerForm(taskType, componentLoader) {
  formRegistry[taskType] = componentLoader
}

/**
 * 注册插件组件 URL
 * @param {string} taskType - 流程类型
 * @param {string} componentUrl - 组件 JS 文件 URL
 */
export function registerPluginUrl(taskType, componentUrl) {
  pluginUrls.set(taskType, componentUrl)
}

/**
 * 检查流程类型是否已注册
 * @param {string} taskType
 * @returns {boolean}
 */
export function hasForm(taskType) {
  return !!formRegistry[taskType] || pluginUrls.has(taskType)
}

/**
 * 获取所有已注册的流程类型
 * @returns {string[]}
 */
export function getRegisteredTypes() {
  return [
    ...Object.keys(formRegistry),
    ...pluginUrls.keys()
  ]
}
```

- [ ] **Step 3: 提交**

```bash
git add ruoyi-ui/src/utils/plugin-loader.js
git add ruoyi-ui/src/views/todo/components/registry/index.js
git commit -m "feat(plugin): 添加前端插件组件动态加载器"
```

---

## Task 8: 前端插件管理页面

**Files:**
- Create: `ruoyi-ui/src/views/plugin/index.vue`
- Create: `ruoyi-ui/src/views/plugin/importDialog.vue`
- Create: `ruoyi-ui/src/views/plugin/detailDialog.vue`
- Create: `ruoyi-ui/src/views/plugin/api/plugin.js`

- [ ] **Step 1: 创建 API 文件**

`ruoyi-ui/src/views/plugin/api/plugin.js`:

```javascript
import request from '@/utils/request'

// 获取插件列表
export function listPlugins() {
  return request({
    url: '/plugin/list',
    method: 'get'
  })
}

// 获取插件详情
export function getPlugin(pluginId) {
  return request({
    url: '/plugin/' + pluginId,
    method: 'get'
  })
}

// 导入插件
export function importPlugin(data) {
  return request({
    url: '/plugin/import',
    method: 'post',
    headers: { 'Content-Type': 'multipart/form-data' },
    data: data
  })
}

// 卸载插件
export function uninstallPlugin(pluginId) {
  return request({
    url: '/plugin/' + pluginId,
    method: 'delete'
  })
}
```

- [ ] **Step 2: 创建插件列表页面**

`ruoyi-ui/src/views/plugin/index.vue`:

```vue
<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          icon="el-icon-upload2"
          size="mini"
          @click="handleImport"
          v-hasPermi="['plugin:import']"
        >导入</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="pluginList">
      <el-table-column label="插件名称" prop="pluginName" />
      <el-table-column label="流程标识" prop="processKey" />
      <el-table-column label="版本" prop="version" width="80" />
      <el-table-column label="状态" prop="status" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === 'ENABLED' ? 'success' : 'info'">
            {{ scope.row.status === 'ENABLED' ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="安装时间" prop="installTime" width="160" />
      <el-table-column label="作者" prop="author" width="100" />
      <el-table-column label="操作" width="150">
        <template slot-scope="scope">
          <el-button size="mini" type="text" @click="handleDetail(scope.row)">详情</el-button>
          <el-button size="mini" type="text" @click="handleUninstall(scope.row)"
                     v-hasPermi="['plugin:remove']">卸载</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 导入对话框 -->
    <import-dialog ref="importDialog" @success="getList" />

    <!-- 详情对话框 -->
    <detail-dialog ref="detailDialog" />
  </div>
</template>

<script>
import { listPlugins, uninstallPlugin } from './api/plugin'
import ImportDialog from './importDialog'
import DetailDialog from './detailDialog'

export default {
  name: 'Plugin',
  components: { ImportDialog, DetailDialog },
  data() {
    return {
      loading: false,
      pluginList: []
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listPlugins().then(res => {
        this.pluginList = res.rows || res.data || []
        this.loading = false
      })
    },
    handleImport() {
      this.$refs.importDialog.show()
    },
    handleDetail(row) {
      this.$refs.detailDialog.show(row)
    },
    handleUninstall(row) {
      this.$confirm('确认卸载插件 [' + row.pluginName + ']?', '警告', {
        type: 'warning'
      }).then(() => {
        uninstallPlugin(row.pluginId).then(() => {
          this.$message.success('卸载成功')
          this.getList()
        })
      })
    }
  }
}
</script>
```

- [ ] **Step 3: 创建导入对话框**

`ruoyi-ui/src/views/plugin/importDialog.vue`:

```vue
<template>
  <el-dialog title="导入插件" :visible.sync="visible" width="500px" append-to-body>
    <el-upload
      ref="upload"
      :limit="1"
      accept=".zip"
      :auto-upload="false"
      :on-change="handleFileChange"
      :file-list="fileList"
      drag
    >
      <i class="el-icon-upload"></i>
      <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
      <div class="el-upload__tip" slot="tip">支持 .zip 格式的流程包</div>
    </el-upload>

    <div slot="footer">
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="uploading" @click="handleImport">导入</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { importPlugin } from './api/plugin'

export default {
  name: 'ImportDialog',
  data() {
    return {
      visible: false,
      uploading: false,
      fileList: [],
      file: null
    }
  },
  methods: {
    show() {
      this.visible = true
      this.fileList = []
      this.file = null
    },
    handleFileChange(file, fileList) {
      this.file = file.raw
    },
    handleImport() {
      if (!this.file) {
        this.$message.warning('请选择文件')
        return
      }

      this.uploading = true
      const formData = new FormData()
      formData.append('file', this.file)

      importPlugin(formData).then(res => {
        this.uploading = false
        if (res.code === 200) {
          this.$message.success('导入成功')
          this.visible = false
          this.$emit('success')
        } else {
          this.$message.error(res.msg || '导入失败')
        }
      }).catch(() => {
        this.uploading = false
        this.$message.error('导入失败')
      })
    }
  }
}
</script>
```

- [ ] **Step 4: 创建详情对话框**

`ruoyi-ui/src/views/plugin/detailDialog.vue`:

```vue
<template>
  <el-dialog title="插件详情" :visible.sync="visible" width="500px" append-to-body>
    <el-descriptions :column="1" border>
      <el-descriptions-item label="插件ID">{{ plugin.pluginId }}</el-descriptions-item>
      <el-descriptions-item label="插件名称">{{ plugin.pluginName }}</el-descriptions-item>
      <el-descriptions-item label="流程标识">{{ plugin.processKey }}</el-descriptions-item>
      <el-descriptions-item label="版本">{{ plugin.version }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="plugin.status === 'ENABLED' ? 'success' : 'info'">
          {{ plugin.status === 'ENABLED' ? '启用' : '禁用' }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="安装时间">{{ plugin.installTime }}</el-descriptions-item>
      <el-descriptions-item label="作者">{{ plugin.author }}</el-descriptions-item>
      <el-descriptions-item label="描述">{{ plugin.description }}</el-descriptions-item>
    </el-descriptions>

    <div slot="footer">
      <el-button @click="visible = false">关闭</el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'DetailDialog',
  data() {
    return {
      visible: false,
      plugin: {}
    }
  },
  methods: {
    show(plugin) {
      this.plugin = plugin
      this.visible = true
    }
  }
}
</script>
```

- [ ] **Step 5: 提交**

```bash
git add ruoyi-ui/src/views/plugin/
git commit -m "feat(plugin): 添加前端插件管理页面"
```

---

## Task 9: 配置与菜单

**Files:**
- Modify: `ruoyi-admin/src/main/resources/application.yml`
- Create: 数据库菜单插入脚本

- [ ] **Step 1: 添加配置**

在 `application.yml` 添加：

```yaml
# 插件配置
plugin:
  storage:
    path: ./plugins
```

- [ ] **Step 2: 添加菜单数据**

执行 SQL 添加插件管理菜单：

```sql
-- 插件管理菜单
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2000, '插件管理', 1, 10, 'plugin', 'plugin/index', 'C', '0', '0', 'plugin:list', 'tool', 'admin', NOW(), '插件管理菜单');

-- 插件管理按钮权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('插件导入', 2000, 1, '', '', 'F', '0', '0', 'plugin:import', '#', 'admin', NOW());

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('插件删除', 2000, 2, '', '', 'F', '0', '0', 'plugin:remove', '#', 'admin', NOW());
```

- [ ] **Step 3: 提交**

```bash
git add ruoyi-admin/src/main/resources/application.yml
git commit -m "feat(plugin): 添加插件配置"
```

---

## Task 10: 静态资源映射

**Files:**
- Create: `ruoyi-admin/src/main/java/com/ruoyi/web/config/PluginResourceConfig.java`

- [ ] **Step 1: 创建静态资源映射配置**

```java
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
        registry.addResourceHandler("/plugins/**")
                .addResourceLocations("file:" + pluginStoragePath + "/");
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl ruoyi-admin -am`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add ruoyi-admin/src/main/java/com/ruoyi/web/config/PluginResourceConfig.java
git commit -m "feat(plugin): 添加插件静态资源映射"
```

---

## Task 11: 整体测试与集成

- [ ] **Step 1: 完整编译**

Run: `mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 2: 前端编译**

Run: `cd ruoyi-ui && npm run build:prod`
Expected: BUILD SUCCESS

- [ ] **Step 3: 创建示例插件包**

手动创建一个测试插件包验证流程：
1. 创建目录结构
2. 编写 manifest.json
3. 编写简单的 Java 插件实现 ProcessPlugin
4. 编译打包 JAR
5. 编写前端组件并编译为 UMD
6. 编写 BPMN 文件
7. 打包为 zip

- [ ] **Step 4: 测试导入功能**

1. 启动应用
2. 登录系统
3. 访问插件管理页面
4. 上传测试插件包
5. 验证菜单是否添加
6. 验证 BPMN 是否部署
7. 验证前端组件是否可加载

- [ ] **Step 5: 最终提交**

```bash
git add .
git commit -m "feat(plugin): 完成流程热部署插件系统"
```

---

## 自检清单

| Spec 章节 | 对应 Task |
|-----------|----------|
| 流程包结构 | Task 1, 5 |
| ProcessPlugin 接口 | Task 1 |
| 插件类加载器 | Task 2 |
| Spring Bean 注册 | Task 2 |
| 前端组件加载 | Task 7 |
| 数据库脚本执行 | Task 3 |
| 菜单导入 | Task 4 |
| API 接口 | Task 6 |
| 前端管理界面 | Task 8 |
| 错误处理与回滚 | Task 5 |

---

**计划完成，保存至 `docs/superpowers/plans/2026-03-28-hot-deploy-plugin.md`**
