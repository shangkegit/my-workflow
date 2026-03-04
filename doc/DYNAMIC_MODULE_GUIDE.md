# 动态流程模块系统

## 📖 概述

本系统实现了流程模块的动态部署功能，允许在不重启后端的情况下：
- 导入新的流程模块
- 暂停/激活已部署的模块
- 导出模块配置用于环境迁移

## 🏗️ 架构

```
┌─────────────────────────────────────────────────┐
│          ModuleManageController                  │
│  (导入/导出/暂停/激活)                           │
└───────────────────┬─────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────┐
│          ProcessModuleLoader                     │
│  (从JSON配置加载模块)                            │
└───────────────────┬─────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────┐
│          ProcessModuleRegistry                   │
│  (模块注册表 - 管理所有已部署模块)               │
└───────────────────┬─────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────┐
│          UnifiedModuleController                 │
│  (统一任务处理入口)                              │
└─────────────────────────────────────────────────┘
```

## 📋 API 接口

### 1. 模块管理

#### 获取模块列表
```
GET /workflow/module/manage/list
```

#### 导入模块
```
POST /workflow/module/manage/import
Content-Type: application/json

{
  "meta": { ... },
  "tasks": { ... },
  "scripts": { ... }
}
```

#### 上传模块文件
```
POST /workflow/module/manage/upload
Content-Type: multipart/form-data
file: module.json
```

#### 导出模块
```
GET /workflow/module/manage/export/{processKey}
```

#### 暂停模块
```
POST /workflow/module/manage/suspend/{processKey}
```

#### 激活模块
```
POST /workflow/module/manage/activate/{processKey}
```

#### 删除模块
```
POST /workflow/module/manage/remove/{processKey}
```

### 2. 任务处理

#### 获取任务表单
```
GET /workflow/module/task/form/{taskId}
```

#### 完成任务
```
POST /workflow/module/task/complete/{taskId}
Content-Type: application/json

{
  "approved": true,
  "comment": "审批通过"
}
```

## 📝 模块配置格式

### 基本结构
```json
{
  "meta": {
    "id": "模块唯一ID",
    "name": "模块名称",
    "processKey": "流程定义键（对应BPMN的process id）",
    "version": "版本号",
    "author": "作者",
    "description": "描述"
  },
  "tasks": {
    "taskKey": {
      "name": "任务名称",
      "formKey": "表单键",
      "handler": "脚本路径"
    }
  },
  "scripts": {
    "scriptName.groovy": "// Groovy脚本代码"
  },
  "forms": {
    "taskKey": {
      "fields": [...]
    }
  }
}
```

### 任务处理器脚本
```groovy
// 可用变量：
// - taskId: 任务ID
// - businessKey: 业务主键
// - variables: 流程变量
// - result: 返回结果 Map
// - log: 日志对象

log.info('处理任务: {}', taskId);

if (variables.get('approved') == true) {
    result.put('success', true);
    result.put('message', '审批通过');
} else {
    result.put('success', false);
    result.put('message', '审批驳回');
}
```

## 🚀 使用流程

### 1. 开发环境

1. 创建模块配置 JSON 文件
2. 编写任务处理脚本
3. 设计 BPMN 流程图
4. 本地测试验证

### 2. 部署到生产

```bash
# 方式1：通过 API 导入
curl -X POST http://localhost:8080/workflow/module/manage/import \
  -H "Content-Type: application/json" \
  -d @module.json

# 方式2：上传文件
curl -X POST http://localhost:8080/workflow/module/manage/upload \
  -F "file=@module.json"
```

### 3. 管理模块

```bash
# 查看已部署模块
curl http://localhost:8080/workflow/module/manage/list

# 暂停模块
curl -X POST http://localhost:8080/workflow/module/manage/suspend/expense

# 激活模块
curl -X POST http://localhost:8080/workflow/module/manage/activate/expense

# 导出模块
curl http://localhost:8080/workflow/module/manage/export/expense > expense-backup.json
```

## 📂 目录结构

```
/data/workflow/modules/
├── leave.json           # 请假流程模块
├── expense.json         # 费用报销模块
├── purchase.json        # 采购流程模块
└── ...
```

## 🔒 权限配置

需要在菜单管理中添加以下权限：

- `workflow:module:list` - 查看模块列表
- `workflow:module:import` - 导入模块
- `workflow:module:export` - 导出模块
- `workflow:module:edit` - 编辑模块（暂停/激活）
- `workflow:module:remove` - 删除模块

## 📌 注意事项

1. **模块ID唯一性**：每个模块的 `processKey` 必须唯一
2. **脚本安全**：Groovy 脚本执行需要注意安全性，建议限制权限
3. **版本管理**：更新模块时建议先导出旧版本备份
4. **BPMN 同步**：模块配置中的 `processKey` 需要与 BPMN 流程定义一致

## 🔧 扩展开发

### 自定义模块实现

```java
public class MyProcessModule implements ProcessModule {
    @Override
    public ProcessModuleMeta getMeta() {
        // 返回模块元信息
    }

    @Override
    public Map<String, Object> handleTask(String taskKey, String taskId, 
                                          String businessKey, Map<String, Object> variables) {
        // 自定义任务处理逻辑
    }
    
    // ... 其他方法
}
```

### 注册自定义模块

```java
@Autowired
private ProcessModuleRegistry registry;

public void registerMyModule() {
    MyProcessModule module = new MyProcessModule();
    registry.register(module);
}
```

## 📚 示例

参考 `doc/example-expense-module.json` 查看完整的模块配置示例。
