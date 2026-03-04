package com.ruoyi.web.controller.workflow;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.workflow.module.ProcessModule;
import com.ruoyi.workflow.module.ProcessModuleMeta;
import com.ruoyi.workflow.module.ProcessModuleRegistry;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.activiti.engine.FormService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.TaskService;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统一流程模块任务控制器
 * 所有动态模块的任务都通过此控制器处理
 * 
 * @author OpenClaw Agent
 * @date 2026-03-03
 */
@Api(value = "流程模块任务接口")
@RestController
@RequestMapping("/workflow/module")
public class UnifiedModuleController extends BaseController {

    @Autowired
    private ProcessModuleRegistry moduleRegistry;

    @Autowired
    private TaskService taskService;

    @Autowired
    private FormService formService;
    
    @Autowired
    private RepositoryService repositoryService;
    
    /**
     * 从任务获取流程定义Key
     */
    private String getProcessDefinitionKey(Task task) {
        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
            .processDefinitionId(task.getProcessDefinitionId())
            .singleResult();
        return pd != null ? pd.getKey() : null;
    }

    /**
     * 获取所有已注册的模块列表
     */
    @ApiOperation("获取所有模块列表")
    @GetMapping("/list")
    public AjaxResult listModules() {
        List<Map<String, Object>> modules = moduleRegistry.getAllMetas().stream()
                .map(meta -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", meta.getId());
                    m.put("name", meta.getName());
                    m.put("processKey", meta.getProcessKey());
                    m.put("version", meta.getVersion());
                    m.put("status", meta.getStatus());
                    m.put("description", meta.getDescription());
                    m.put("deployTime", meta.getDeployTime());
                    return m;
                })
                .collect(Collectors.toList());
        return AjaxResult.success(modules);
    }

    /**
     * 获取模块详情
     */
    @ApiOperation("获取模块详情")
    @GetMapping("/detail/{processKey}")
    public AjaxResult getModuleDetail(@PathVariable String processKey) {
        ProcessModuleMeta meta = moduleRegistry.getMeta(processKey);
        if (meta == null) {
            return AjaxResult.error("模块不存在");
        }
        return AjaxResult.success(meta);
    }

    /**
     * 获取任务的表单数据
     * 
     * @param taskId Activiti 任务ID
     */
    @ApiOperation("获取任务表单数据")
    @GetMapping("/task/form/{taskId}")
    public AjaxResult getTaskForm(@PathVariable String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return AjaxResult.error("任务不存在");
        }

        String processDefinitionKey = getProcessDefinitionKey(task);
        String taskDefinitionKey = task.getTaskDefinitionKey();
        String businessKey = getBusinessKey(task.getProcessInstanceId());

        // 查找对应的模块
        ProcessModule module = moduleRegistry.getModule(processDefinitionKey);
        if (module == null) {
            // 如果没有注册模块，返回默认表单信息
            Map<String, Object> defaultForm = new HashMap<>();
            defaultForm.put("taskId", taskId);
            defaultForm.put("taskName", task.getName());
            String formKey = formService.getTaskFormData(taskId).getFormKey();
            defaultForm.put("formKey", formKey);
            return AjaxResult.success(defaultForm);
        }

        // 调用模块获取表单数据
        Map<String, Object> formData = module.getFormData(taskDefinitionKey, businessKey);
        formData.put("taskId", taskId);
        formData.put("taskName", task.getName());
        formData.put("processKey", processDefinitionKey);

        return AjaxResult.success(formData);
    }

    /**
     * 处理任务（统一入口）
     * 
     * @param taskId 任务ID
     * @param variables 流程变量
     */
    @ApiOperation("处理任务")
    @Log(title = "处理流程模块任务", businessType = BusinessType.UPDATE)
    @PostMapping("/task/complete/{taskId}")
    public AjaxResult completeTask(@PathVariable String taskId, @RequestBody(required = false) Map<String, Object> variables) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return AjaxResult.error("任务不存在");
        }

        String processDefinitionKey = getProcessDefinitionKey(task);
        String taskDefinitionKey = task.getTaskDefinitionKey();
        String businessKey = getBusinessKey(task.getProcessInstanceId());

        // 查找对应的模块
        ProcessModule module = moduleRegistry.getModule(processDefinitionKey);
        
        if (module != null && moduleRegistry.isActive(processDefinitionKey)) {
            // 调用模块处理任务
            Map<String, Object> result = module.handleTask(taskDefinitionKey, taskId, businessKey, variables);
            
            if (result.containsKey("success") && Boolean.FALSE.equals(result.get("success"))) {
                return AjaxResult.error(result.get("message").toString());
            }
        }

        // 完成任务
        String username = getUsername();
        taskService.setAssignee(taskId, username);
        
        if (variables != null && variables.containsKey("comment")) {
            taskService.addComment(taskId, task.getProcessInstanceId(), (String) variables.remove("comment"));
        }
        
        taskService.complete(taskId, variables);

        return AjaxResult.success("任务处理成功");
    }

    /**
     * 获取业务主键
     */
    private String getBusinessKey(String processInstanceId) {
        // 这里需要根据实际情况获取 businessKey
        // 可以通过 RuntimeService 或 HistoryService 获取
        return processInstanceId; // 临时返回流程实例ID
    }
}
