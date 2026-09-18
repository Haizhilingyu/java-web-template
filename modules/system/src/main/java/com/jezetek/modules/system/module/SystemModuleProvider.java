package com.jezetek.modules.system.module;

import com.jezetek.core.runtime.module.MenuNode;
import com.jezetek.core.runtime.module.MenuType;
import com.jezetek.core.runtime.module.ModuleProvider;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 系统管理模块自描述：菜单树声明 + 受保护接口前缀。
 *
 * <p>权限标识约定 {@code system:实体:动作}；USER 角色只授予
 * 系统管理目录 + 用户管理页面 + 查询按钮(演示按钮级权限)，
 * ADMIN 超管在安全层直通无需声明</p>
 */
@Component
public class SystemModuleProvider implements ModuleProvider {

    @Override
    public String code() {
        return "system";
    }

    @Override
    public String name() {
        return "系统管理";
    }

    @Override
    public List<String> apiPrefixes() {
        return List.of("/api/v1/user/**", "/api/v1/role/**", "/api/v1/menu/**");
    }

    @Override
    public List<MenuNode> menus() {
        return List.of(
                MenuNode.of("系统管理", MenuType.DIR)
                        .path("/system")
                        .icon("setting")
                        .sortOrder(1)
                        .roles("USER")
                        .children(
                                MenuNode.of("用户管理", MenuType.MENU)
                                        .path("/system/user")
                                        .component("/system/user/index")
                                        .icon("user")
                                        .sortOrder(1)
                                        .roles("USER")
                                        .children(
                                                button("查询用户", "system:user:list", 1).roles("USER"),
                                                button("新增用户", "system:user:add", 2),
                                                button("编辑用户", "system:user:edit", 3),
                                                button("删除用户", "system:user:delete", 4)
                                        ),
                                MenuNode.of("角色管理", MenuType.MENU)
                                        .path("/system/role")
                                        .component("/system/role/index")
                                        .icon("usergroup")
                                        .sortOrder(2)
                                        .children(
                                                button("查询角色", "system:role:list", 1),
                                                button("新增角色", "system:role:add", 2),
                                                button("编辑角色", "system:role:edit", 3),
                                                button("删除角色", "system:role:delete", 4)
                                        ),
                                MenuNode.of("菜单管理", MenuType.MENU)
                                        .path("/system/menu")
                                        .component("/system/menu/index")
                                        .icon("menu")
                                        .sortOrder(3)
                                        .children(
                                                button("查询菜单", "system:menu:list", 1),
                                                button("新增菜单", "system:menu:add", 2),
                                                button("编辑菜单", "system:menu:edit", 3),
                                                button("删除菜单", "system:menu:delete", 4)
                                        )
                        )
        );
    }

    private static MenuNode button(String name, String perms, int sortOrder) {
        return MenuNode.of(name, MenuType.BUTTON).perms(perms).sortOrder(sortOrder);
    }
}
