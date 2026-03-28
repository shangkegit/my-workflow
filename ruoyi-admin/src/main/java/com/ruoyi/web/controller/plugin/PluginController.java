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

import java.util.List;

@Api(value = "插件管理", tags = "插件管理")
@RestController
@RequestMapping("/plugin")
public class PluginController extends BaseController {

    @Autowired
    private PluginDeployService deployService;

    @Autowired
    private PluginManager pluginManager;

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

    @ApiOperation("获取插件列表")
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('plugin:list')")
    public TableDataInfo listPlugins() {
        List<PluginInfo> list = pluginManager.listAllPlugins();
        return getDataTable(list);
    }

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
