package com.ruoyi.web.controller.workflow;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.workflow.module.ProcessModule;
import com.ruoyi.workflow.module.ProcessModuleLoader;
import com.ruoyi.workflow.module.ProcessModuleMeta;
import com.ruoyi.workflow.module.ProcessModuleRegistry;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 流程模块管理控制器
 * 负责模块的导入、导出、启停等管理操作
 * 
 * @author OpenClaw Agent
 * @date 2026-03-03
 */
@Api(value = "流程模块管理接口")
@RestController
@RequestMapping("/workflow/module/manage")
public class ModuleManageController extends BaseController {

    @Autowired
    private ProcessModuleRegistry registry;

    @Autowired
    private ProcessModuleLoader loader;

    // 模块存储目录
    private static final String MODULE_DIR = "/data/workflow/modules";

    /**
     * 导入模块（JSON配置）
     */
    @ApiOperation("导入模块")
    @PreAuthorize("@ss.hasPermi('workflow:module:import')")
    @Log(title = "导入流程模块", businessType = BusinessType.INSERT)
    @PostMapping("/import")
    public AjaxResult importModule(@RequestBody String jsonConfig) {
        try {
            loader.loadAndRegister(jsonConfig);
            
            // 保存到文件
            JSONObject config = JSON.parseObject(jsonConfig);
            String processKey = config.getJSONObject("meta").getString("processKey");
            saveModuleFile(processKey, jsonConfig);
            
            return AjaxResult.success("模块导入成功");
        } catch (Exception e) {
            logger.error("模块导入失败", e);
            return AjaxResult.error("模块导入失败: " + e.getMessage());
        }
    }

    /**
     * 上传模块文件
     */
    @ApiOperation("上传模块文件")
    @PreAuthorize("@ss.hasPermi('workflow:module:import')")
    @Log(title = "上传流程模块", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public AjaxResult uploadModule(@RequestParam("file") MultipartFile file) {
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            loader.loadAndRegister(content);
            
            // 保存文件
            String filename = file.getOriginalFilename();
            Path path = Paths.get(MODULE_DIR, filename);
            Files.createDirectories(path.getParent());
            Files.write(path, file.getBytes());
            
            return AjaxResult.success("模块上传成功");
        } catch (Exception e) {
            logger.error("模块上传失败", e);
            return AjaxResult.error("模块上传失败: " + e.getMessage());
        }
    }

    /**
     * 导出模块
     */
    @ApiOperation("导出模块")
    @PreAuthorize("@ss.hasPermi('workflow:module:export')")
    @GetMapping("/export/{processKey}")
    public AjaxResult exportModule(@PathVariable String processKey) {
        try {
            String content = loadModuleFile(processKey);
            if (content == null) {
                return AjaxResult.error("模块文件不存在");
            }
            return AjaxResult.success("导出成功", content);
        } catch (Exception e) {
            logger.error("模块导出失败", e);
            return AjaxResult.error("模块导出失败: " + e.getMessage());
        }
    }

    /**
     * 暂停模块
     */
    @ApiOperation("暂停模块")
    @PreAuthorize("@ss.hasPermi('workflow:module:edit')")
    @Log(title = "暂停流程模块", businessType = BusinessType.UPDATE)
    @PostMapping("/suspend/{processKey}")
    public AjaxResult suspendModule(@PathVariable String processKey) {
        if (!registry.exists(processKey)) {
            return AjaxResult.error("模块不存在");
        }
        registry.suspend(processKey);
        return AjaxResult.success("模块已暂停");
    }

    /**
     * 激活模块
     */
    @ApiOperation("激活模块")
    @PreAuthorize("@ss.hasPermi('workflow:module:edit')")
    @Log(title = "激活流程模块", businessType = BusinessType.UPDATE)
    @PostMapping("/activate/{processKey}")
    public AjaxResult activateModule(@PathVariable String processKey) {
        if (!registry.exists(processKey)) {
            return AjaxResult.error("模块不存在");
        }
        registry.activate(processKey);
        return AjaxResult.success("模块已激活");
    }

    /**
     * 删除模块
     */
    @ApiOperation("删除模块")
    @PreAuthorize("@ss.hasPermi('workflow:module:remove')")
    @Log(title = "删除流程模块", businessType = BusinessType.DELETE)
    @PostMapping("/remove/{processKey}")
    public AjaxResult removeModule(@PathVariable String processKey) {
        registry.unregister(processKey);
        
        // 删除文件
        try {
            Path path = Paths.get(MODULE_DIR, processKey + ".json");
            Files.deleteIfExists(path);
        } catch (Exception e) {
            logger.warn("删除模块文件失败", e);
        }
        
        return AjaxResult.success("模块已删除");
    }

    /**
     * 获取模块列表
     */
    @ApiOperation("获取模块列表")
    @GetMapping("/list")
    public AjaxResult listModules() {
        List<Map<String, Object>> modules = registry.getAllMetas().stream()
                .map(meta -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", meta.getId());
                    m.put("name", meta.getName());
                    m.put("processKey", meta.getProcessKey());
                    m.put("version", meta.getVersion());
                    m.put("status", meta.getStatus());
                    m.put("description", meta.getDescription());
                    m.put("author", meta.getAuthor());
                    m.put("deployTime", meta.getDeployTime());
                    m.put("deploySource", meta.getDeploySource());
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
        ProcessModuleMeta meta = registry.getMeta(processKey);
        if (meta == null) {
            return AjaxResult.error("模块不存在");
        }
        return AjaxResult.success(meta);
    }

    /**
     * 重新加载模块
     */
    @ApiOperation("重新加载模块")
    @PreAuthorize("@ss.hasPermi('workflow:module:edit')")
    @Log(title = "重新加载流程模块", businessType = BusinessType.UPDATE)
    @PostMapping("/reload/{processKey}")
    public AjaxResult reloadModule(@PathVariable String processKey) {
        try {
            String content = loadModuleFile(processKey);
            if (content == null) {
                return AjaxResult.error("模块文件不存在");
            }
            loader.loadAndRegister(content);
            return AjaxResult.success("模块重新加载成功");
        } catch (Exception e) {
            logger.error("模块重新加载失败", e);
            return AjaxResult.error("模块重新加载失败: " + e.getMessage());
        }
    }

    /**
     * 保存模块到文件
     */
    private void saveModuleFile(String processKey, String content) throws Exception {
        Path dir = Paths.get(MODULE_DIR);
        Files.createDirectories(dir);
        Path file = dir.resolve(processKey + ".json");
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 从文件加载模块
     */
    private String loadModuleFile(String processKey) {
        try {
            Path file = Paths.get(MODULE_DIR, processKey + ".json");
            if (Files.exists(file)) {
                return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            logger.error("加载模块文件失败", e);
        }
        return null;
    }
}
