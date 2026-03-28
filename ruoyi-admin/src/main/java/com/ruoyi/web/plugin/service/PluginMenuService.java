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

@Service
public class PluginMenuService {

    private static final Logger log = LoggerFactory.getLogger(PluginMenuService.class);

    @Autowired
    private SysMenuMapper menuMapper;

    public void importMenus(List<MenuConfig> menus, String pluginId) {
        Map<String, Long> idMapping = new HashMap<>();
        importMenuRecursive(menus, 0L, pluginId, idMapping);
        log.info("插件 [{}] 导入菜单 {} 个", pluginId, idMapping.size());
    }

    private void importMenuRecursive(List<MenuConfig> menus, Long parentId, String pluginId, Map<String, Long> idMapping) {
        if (menus == null) return;

        int orderNum = 1;
        for (MenuConfig config : menus) {
            Long newId = generateId();
            idMapping.put(config.getPath(), newId);

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
            menu.setPluginId(pluginId);

            menuMapper.insertMenu(menu);

            if (config.getChildren() != null && !config.getChildren().isEmpty()) {
                importMenuRecursive(config.getChildren(), newId, pluginId, idMapping);
            }
        }
    }

    public void deleteMenus(String pluginId) {
        menuMapper.deleteMenuByPluginId(pluginId);
        log.info("插件 [{}] 菜单已删除", pluginId);
    }

    private Long generateId() {
        return System.currentTimeMillis() * 1000 + (long)(Math.random() * 1000);
    }
}
