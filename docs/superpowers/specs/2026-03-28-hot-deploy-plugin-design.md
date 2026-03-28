# 流程热部署插件系统设计文档

## 概述

为 my-workflow 项目实现流程热部署功能，支持在开发环境新建流程后，打包导出并一键导入生产环境，无需重启服务即可部署新流程。

## 需求总结

| 组件 | 方式 |
|------|------|
| 后端 Java 类 | 完整类动态加载 |
| 前端组件 | 预编译 JS 动态加载 |
| 数据库脚本 | 直接执行 SQL |
| 菜单/权限 | 生成新 ID 避免冲突 |
| 部署方式 | 运维人员手动导入 |

## 流程包结构

```
{processType}-plugin.zip
├── manifest.json                 # 必需：插件元信息
├── backend/
│   └── {processType}-plugin.jar  # 必需：编译后的 Java 插件
├── frontend/
│   └── {processType}Form.umd.js  # 必需：编译后的 Vue 组件
├── database/
│   ├── schema.sql                # 可选：建表脚本
│   └── data.sql                  # 可选：初始数据脚本
├── menu/
│   └── menu.json                 # 可选：菜单配置
└── bpmn/
    └── {processType}.bpmn20.xml  # 必需：BPMN 流程定义
```

### manifest.json 规范

```json
{
  "id": "expense-reimbursement",
  "name": "费用报销流程",
  "version": "1.0.0",
  "processKey": "expense_reimbursement",
  "author": "开发人员",
  "description": "员工费用报销审批流程",
  "backend": {
    "pluginClass": "com.ruoyi.plugin.expense.ExpensePluginImpl"
  },
  "frontend": {
    "component": "expenseForm"
  },
  "permissions": {
    "admin": ["expense:apply:list", "expense:apply:add"],
    "common": ["expense:apply:list"]
  }
}
```

### menu.json 规范

```json
[
  {
    "menuName": "费用报销",
    "parentId": 0,
    "orderNum": 1,
    "path": "expense",
    "component": "expense/index",
    "menuType": "M",
    "visible": "0",
    "icon": "money",
    "children": [
      {
        "menuName": "发起申请",
        "parentId": "expense",
        "orderNum": 1,
        "path": "apply",
        "component": "expense/apply",
        "menuType": "C",
        "perms": "expense:apply:list"
      }
    ]
  }
]
```

## 后端插件接口规范

### ProcessPlugin 接口

```java
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

    /** 初始化插件（可选，用于注册监听器等） */
    default void initialize() {}

    /** 销毁插件（可选，用于清理资源） */
    default void destroy() {}
}
```

### 插件类加载器

```java
public class PluginClassLoader extends URLClassLoader {
    private final String pluginId;

    public PluginClassLoader(String pluginId, URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.pluginId = pluginId;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) {
        // 核心类（java.*, javax.*, com.ruoyi.common.*）委派给父加载器
        if (isSystemClass(name)) {
            return super.loadClass(name, resolve);
        }
        // 插件自己的类优先自己加载
        // ...
    }
}
```

### Spring Bean 动态注册

```java
@Component
public class PluginBeanRegistry {

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    private final Map<String, ProcessPlugin> plugins = new ConcurrentHashMap<>();

    public void registerPlugin(String pluginId, ProcessPlugin plugin) {
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(plugin.getClass());
        beanDefinition.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON);

        ((DefaultListableBeanFactory) applicationContext.getBeanFactory())
            .registerBeanDefinition(pluginId, beanDefinition);

        plugins.put(pluginId, plugin);
        plugin.initialize();
    }

    public void unregisterPlugin(String pluginId) {
        ProcessPlugin plugin = plugins.get(pluginId);
        if (plugin != null) {
            plugin.destroy();
            plugins.remove(pluginId);
            ((DefaultListableBeanFactory) applicationContext.getBeanFactory())
                .removeBeanDefinition(pluginId);
        }
    }
}
```

## 前端组件动态加载

### 编译流程

```
开发环境                              生产环境
┌─────────────────┐                 ┌─────────────────┐
│ ExpenseForm.vue │ ──编译──►       │ ExpenseForm.js  │
└─────────────────┘                 └─────────────────┘
       │                                    │
       ▼                                    ▼
  导出流程包.zip                      动态加载到页面
  (含编译后的JS)
```

### 插件组件加载器

```javascript
const loadedPlugins = new Map()

export async function loadPluginComponent(processType, componentUrl) {
  if (loadedPlugins.has(processType)) {
    return loadedPlugins.get(processType)
  }

  return new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = componentUrl
    script.onload = () => {
      const component = window.__PLUGINS__?.[processType]
      if (component) {
        loadedPlugins.set(processType, component)
        resolve(component)
      } else {
        reject(new Error(`插件组件未正确注册: ${processType}`))
      }
    }
    script.onerror = () => reject(new Error(`加载插件失败: ${componentUrl}`))
    document.head.appendChild(script)
  })
}
```

### 插件组件规范

```javascript
;(function() {
  const ExpenseForm = {
    name: 'ExpenseForm',
    props: ['step', 'taskId', 'businessKey', 'formInfo'],
    template: `<div>...</div>`,
    methods: {
      // 遵循 formMixin 约定
    }
  }

  window.__PLUGINS__ = window.__PLUGINS__ || {}
  window.__PLUGINS__['expense'] = ExpenseForm
})()
```

### 注册中心改造

```javascript
import { loadPluginComponent } from '@/utils/plugin-loader'

const formRegistry = {
  leaveapply: () => import('../forms/leaveApplyForm.vue'),
  meeting: () => import('../forms/meetingForm.vue'),
  purchase: () => import('../forms/purchaseForm.vue')
}

export async function registerPlugin(processType, componentUrl) {
  const component = await loadPluginComponent(processType, componentUrl)
  formRegistry[processType] = () => Promise.resolve(component)
}
```

## 数据库与菜单处理

### 数据库脚本执行

```java
@Service
public class PluginDatabaseService {

    @Autowired
    private DataSource dataSource;

    @Transactional
    public void executeSqlScripts(List<String> sqlScripts, String pluginId) {
        try (Connection conn = dataSource.getConnection()) {
            for (String sql : sqlScripts) {
                for (String statement : sql.split(";")) {
                    String trimmed = statement.trim();
                    if (!trimmed.isEmpty()) {
                        conn.createStatement().execute(trimmed);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("执行插件 SQL 失败: " + pluginId, e);
        }
    }
}
```

### 菜单导入服务

```java
@Service
public class PluginMenuService {

    @Autowired
    private SysMenuMapper menuMapper;

    public void importMenus(List<MenuConfig> menus, String pluginId) {
        Map<Long, Long> idMapping = new HashMap<>();

        for (MenuConfig menu : menus) {
            Long newId = generateId();
            Long newParentId = idMapping.getOrDefault(menu.getParentId(), menu.getParentId());

            SysMenu sysMenu = new SysMenu();
            sysMenu.setMenuId(newId);
            sysMenu.setMenuName(menu.getMenuName());
            sysMenu.setParentId(newParentId);
            sysMenu.setPath(menu.getPath());
            sysMenu.setComponent(menu.getComponent());
            sysMenu.setPerms(menu.getPerms());
            sysMenu.setPluginId(pluginId);

            menuMapper.insertMenu(sysMenu);
            idMapping.put(menu.getMenuId(), newId);
        }
    }

    public void deleteByPluginId(String pluginId) {
        menuMapper.deleteByPluginId(pluginId);
    }
}
```

## API 接口设计

| 接口 | 方法 | 说明 |
|------|------|------|
| `/plugin/export/{processType}` | GET | 导出流程包 |
| `/plugin/import` | POST | 导入流程包（multipart/form-data） |
| `/plugin/list` | GET | 获取已安装插件列表 |
| `/plugin/{pluginId}` | GET | 获取插件详情 |
| `/plugin/{pluginId}` | DELETE | 卸载插件 |
| `/plugin/{pluginId}/status` | PUT | 启用/禁用插件 |

### 核心接口实现

```java
@RestController
@RequestMapping("/plugin")
public class PluginController {

    @Autowired
    private PluginManager pluginManager;

    @PostMapping("/import")
    @PreAuthorize("@ss.hasPermi('plugin:import')")
    public AjaxResult importPlugin(@RequestParam("file") MultipartFile file) {
        try {
            PluginInfo info = pluginManager.deploy(file);
            return AjaxResult.success("导入成功", info);
        } catch (PluginDeployException e) {
            return AjaxResult.error("导入失败: " + e.getMessage());
        }
    }

    @GetMapping("/export/{processType}")
    @PreAuthorize("@ss.hasPermi('plugin:export')")
    public void exportPlugin(@PathVariable String processType, HttpServletResponse response) {
        File zipFile = pluginManager.export(processType);
        // 下载文件...
    }

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('plugin:list')")
    public TableDataInfo listPlugins() {
        return getDataTable(pluginManager.listAll());
    }

    @DeleteMapping("/{pluginId}")
    @PreAuthorize("@ss.hasPermi('plugin:delete')")
    public AjaxResult uninstallPlugin(@PathVariable String pluginId) {
        pluginManager.undeploy(pluginId);
        return AjaxResult.success();
    }
}
```

## 前端管理界面

### 文件结构

```
views/plugin/
├── index.vue              # 插件列表页面
├── importDialog.vue       # 导入对话框
├── exportDialog.vue       # 导出对话框
├── detailDialog.vue       # 插件详情
└── api/
    └── plugin.js          # API 接口
```

### 插件列表页面

- 展示已安装插件列表（名称、流程标识、版本、状态、安装时间）
- 操作按钮：详情、启用/禁用、卸载
- 顶部按钮：导入、导出

### 导出对话框（开发环境）

- 流程类型选择下拉框
- 包含内容复选框（后端、前端、数据库、菜单、BPMN）
- 版本号输入框

### 导入对话框（生产环境）

- 文件上传区域（支持 .zip 拖拽上传）
- 导入选项（覆盖已存在流程、导入后立即启用）

## 错误处理与回滚

### 错误类型处理

| 错误类型 | 处理方式 |
|---------|---------|
| manifest 格式错误 | 导入前校验，返回具体错误信息 |
| JAR 加载失败 | 记录日志，回滚已执行的操作 |
| SQL 执行失败 | 事务回滚，返回 SQL 错误位置 |
| 菜单 ID 冲突 | 自动生成新 ID，记录映射关系 |
| BPMN 部署失败 | 回滚插件加载，返回验证错误 |
| 组件 JS 加载失败 | 前端提示，允许重试 |

### 部署回滚机制

```java
@Service
public class PluginDeployService {

    public void deploy(File zipFile) throws PluginDeployException {
        PluginContext context = new PluginContext();

        try {
            // 步骤1：解析 manifest
            context.setManifest(parseManifest(zipFile));
            checkpoint("manifest_parsed", context);

            // 步骤2：执行 SQL
            executeDatabase(context);
            checkpoint("database_executed", context);

            // 步骤3：导入菜单
            importMenus(context);
            checkpoint("menu_imported", context);

            // 步骤4：加载 Java 插件
            loadPlugin(context);
            checkpoint("plugin_loaded", context);

            // 步骤5：部署 BPMN
            deployBpmn(context);
            checkpoint("bpmn_deployed", context);

            // 步骤6：存储前端 JS
            storeFrontend(context);
            checkpoint("frontend_stored", context);

        } catch (Exception e) {
            rollback(context);
            throw new PluginDeployException(e.getMessage(), e);
        }
    }

    private void rollback(PluginContext context) {
        // 按逆序回滚
        if (context.hasCheckpoint("frontend_stored")) {
            deleteFrontend(context);
        }
        if (context.hasCheckpoint("bpmn_deployed")) {
            undeployBpmn(context);
        }
        if (context.hasCheckpoint("plugin_loaded")) {
            unloadPlugin(context);
        }
        if (context.hasCheckpoint("menu_imported")) {
            deleteMenus(context);
        }
        if (context.hasCheckpoint("database_executed")) {
            rollbackDatabase(context);
        }
    }
}
```

## 后续扩展

- 插件版本升级（增量更新）
- 插件依赖管理
- 插件市场/仓库
- CI/CD 集成支持
