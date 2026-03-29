package com.ruoyi.system.service.impl;

import com.ruoyi.common.core.plugin.model.SysPlugin;
import com.ruoyi.system.mapper.SysPluginMapper;
import com.ruoyi.system.service.ISysPluginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 插件信息Service业务层实现
 */
@Service
public class SysPluginServiceImpl implements ISysPluginService {

    @Autowired
    private SysPluginMapper pluginMapper;

    @Override
    public SysPlugin selectPluginById(String pluginId) {
        return pluginMapper.selectPluginById(pluginId);
    }

    @Override
    public List<SysPlugin> selectPluginList(SysPlugin plugin) {
        return pluginMapper.selectPluginList(plugin);
    }

    @Override
    public int insertPlugin(SysPlugin plugin) {
        return pluginMapper.insertPlugin(plugin);
    }

    @Override
    public int updatePlugin(SysPlugin plugin) {
        return pluginMapper.updatePlugin(plugin);
    }

    @Override
    public int deletePluginById(String pluginId) {
        return pluginMapper.deletePluginById(pluginId);
    }

    @Override
    public int deletePluginByIds(String[] pluginIds) {
        return pluginMapper.deletePluginByIds(pluginIds);
    }

    @Override
    public boolean checkPluginIdUnique(String pluginId) {
        return pluginMapper.checkPluginIdUnique(pluginId) == null;
    }
}
