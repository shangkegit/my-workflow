package com.ruoyi.system.service;

import com.ruoyi.common.core.plugin.model.SysPlugin;

import java.util.List;

/**
 * 插件信息Service接口
 */
public interface ISysPluginService {

    /**
     * 查询插件
     */
    public SysPlugin selectPluginById(String pluginId);

    /**
     * 查询插件列表
     */
    public List<SysPlugin> selectPluginList(SysPlugin plugin);

    /**
     * 新增插件
     */
    public int insertPlugin(SysPlugin plugin);

    /**
     * 修改插件
     */
    public int updatePlugin(SysPlugin plugin);

    /**
     * 删除插件
     */
    public int deletePluginById(String pluginId);

    /**
     * 批量删除插件
     */
    public int deletePluginByIds(String[] pluginIds);

    /**
     * 检查插件ID是否唯一
     */
    public boolean checkPluginIdUnique(String pluginId);
}
