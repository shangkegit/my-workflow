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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;

@Api(value = "插件管理", tags = "插件管理")
@RestController
@RequestMapping("/plugin")
public class PluginController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(PluginController.class);

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

    @ApiOperation("更新插件状态")
    @Log(title = "插件管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{pluginId}/status")
    @PreAuthorize("@ss.hasPermi('plugin:edit')")
    public AjaxResult updateStatus(@PathVariable String pluginId, @RequestParam String status) {
        try {
            if ("0".equals(status)) {
                pluginManager.enablePlugin(pluginId);
                return AjaxResult.success("启用成功");
            } else if ("1".equals(status)) {
                pluginManager.disablePlugin(pluginId);
                return AjaxResult.success("禁用成功");
            } else {
                return AjaxResult.error("无效的状态值");
            }
        } catch (Exception e) {
            return AjaxResult.error("操作失败: " + e.getMessage());
        }
    }

    @ApiOperation("导出插件")
    @Log(title = "插件管理", businessType = BusinessType.EXPORT)
    @GetMapping("/export/{processType}")
    @PreAuthorize("@ss.hasPermi('plugin:export')")
    public void exportPlugin(@PathVariable String processType, HttpServletResponse response) {
        try {
            File zipFile = pluginManager.exportPlugin(processType, System.getProperty("java.io.tmpdir"));

            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;filename=" +
                URLEncoder.encode(zipFile.getName(), "UTF-8"));

            try (FileInputStream fis = new FileInputStream(zipFile);
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }

            // 删除临时文件
            zipFile.delete();
        } catch (Exception e) {
            log.error("导出插件失败", e);
        }
    }
}
