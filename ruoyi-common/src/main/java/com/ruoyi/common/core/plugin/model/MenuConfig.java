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

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Integer getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum) {
        this.orderNum = orderNum;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getMenuType() {
        return menuType;
    }

    public void setMenuType(String menuType) {
        this.menuType = menuType;
    }

    public String getVisible() {
        return visible;
    }

    public void setVisible(String visible) {
        this.visible = visible;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getPerms() {
        return perms;
    }

    public void setPerms(String perms) {
        this.perms = perms;
    }

    public List<MenuConfig> getChildren() {
        return children;
    }

    public void setChildren(List<MenuConfig> children) {
        this.children = children;
    }
}
