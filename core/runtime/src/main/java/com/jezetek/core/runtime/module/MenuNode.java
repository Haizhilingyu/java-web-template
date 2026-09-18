package com.jezetek.core.runtime.module;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 业务模块声明的菜单树节点，是 sys_menu 的代码化来源。
 *
 * <p>约定：
 * <ul>
 *   <li>DIR 的 path 为一级路由(如 {@code /system})，MENU 的 path 为完整路由
 *       (如 {@code /system/user})，前端路由名与相对路径由下发时推导</li>
 *   <li>按钮节点无需 path/component，仅 name/perms 参与权限</li>
 *   <li>roleCodes 声明默认授权哪些角色编码(如 USER)，超级管理角色永远直通无需声明；
 *       授权在启动同步时落库，之后仍可在角色管理界面调整</li>
 * </ul></p>
 */
public class MenuNode {

    private String name;

    @Nullable
    private String path;

    @Nullable
    private String component;

    @Nullable
    private String icon;

    private MenuType type = MenuType.MENU;

    @Nullable
    private String perms;

    private boolean visible = true;

    private int sortOrder;

    private final Set<String> roleCodes = new LinkedHashSet<>();

    private final List<MenuNode> children = new ArrayList<>();

    public static MenuNode of(String name, MenuType type) {
        MenuNode node = new MenuNode();
        node.name = name;
        node.type = type;
        return node;
    }

    public MenuNode path(@Nullable String path) {
        this.path = path;
        return this;
    }

    public MenuNode component(@Nullable String component) {
        this.component = component;
        return this;
    }

    public MenuNode icon(@Nullable String icon) {
        this.icon = icon;
        return this;
    }

    public MenuNode perms(@Nullable String perms) {
        this.perms = perms;
        return this;
    }

    public MenuNode visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public MenuNode sortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
        return this;
    }

    /** 声明默认授权的角色编码，可变参数 */
    public MenuNode roles(String... roleCodes) {
        this.roleCodes.addAll(List.of(roleCodes));
        return this;
    }

    public MenuNode children(MenuNode... children) {
        this.children.addAll(List.of(children));
        return this;
    }

    public String getName() {
        return name;
    }

    public @Nullable String getPath() {
        return path;
    }

    public @Nullable String getComponent() {
        return component;
    }

    public @Nullable String getIcon() {
        return icon;
    }

    public MenuType getType() {
        return type;
    }

    public @Nullable String getPerms() {
        return perms;
    }

    public boolean isVisible() {
        return visible;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public Set<String> getRoleCodes() {
        return roleCodes;
    }

    public List<MenuNode> getChildren() {
        return children;
    }
}
