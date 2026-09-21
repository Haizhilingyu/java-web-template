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
        return List.of(
                "/api/v1/user/**",
                "/api/v1/role/**",
                "/api/v1/menu/**",
                "/api/v1/dept/**",
                "/api/v1/post/**",
                "/api/v1/dict/**",
                "/api/v1/config/**",
                "/api/v1/notice/**",
                "/api/v1/logininfor/**",
                "/api/v1/online/**"
        );
    }

    @Override
    public List<MenuNode> menus() {
        return List.of(
                MenuNode.of("系统管理", MenuType.DIR)                        .path("/system")
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
                                        ),
                                MenuNode.of("部门管理", MenuType.MENU)
                                        .path("/system/dept")
                                        .component("/system/dept/index")
                                        .icon("city")
                                        .sortOrder(4)
                                        .children(
                                                button("查询部门", "system:dept:list", 1),
                                                button("新增部门", "system:dept:add", 2),
                                                button("编辑部门", "system:dept:edit", 3),
                                                button("删除部门", "system:dept:delete", 4)
                                        ),
                                MenuNode.of("岗位管理", MenuType.MENU)
                                        .path("/system/post")
                                        .component("/system/post/index")
                                        .icon("assignment")
                                        .sortOrder(5)
                                        .children(
                                                button("查询岗位", "system:post:list", 1),
                                                button("新增岗位", "system:post:add", 2),
                                                button("编辑岗位", "system:post:edit", 3),
                                                button("删除岗位", "system:post:delete", 4)
                                        ),
                                MenuNode.of("字典管理", MenuType.MENU)
                                        .path("/system/dict")
                                        .component("/system/dict/index")
                                        .icon("book")
                                        .sortOrder(6)
                                        .children(
                                                button("查询字典", "system:dict:list", 1),
                                                button("新增字典", "system:dict:add", 2),
                                                button("编辑字典", "system:dict:edit", 3),
                                                button("删除字典", "system:dict:delete", 4)
                                        ),
                                MenuNode.of("参数配置", MenuType.MENU)
                                        .path("/system/config")
                                        .component("/system/config/index")
                                        .icon("setting")
                                        .sortOrder(7)
                                        .children(
                                                button("查询参数", "system:config:list", 1),
                                                button("新增参数", "system:config:add", 2),
                                                button("编辑参数", "system:config:edit", 3),
                                                button("删除参数", "system:config:delete", 4)
                                        ),
                                MenuNode.of("公告管理", MenuType.MENU)
                                        .path("/system/notice")
                                        .component("/system/notice/index")
                                        .icon("sound")
                                        .sortOrder(8)
                                        .children(
                                                button("查询公告", "system:notice:list", 1),
                                                button("新增公告", "system:notice:add", 2),
                                                button("编辑公告", "system:notice:edit", 3),
                                                button("删除公告", "system:notice:delete", 4)
                                        )
                        ),
                // 系统监控目录：登录日志/在线用户由本工单创建，操作日志(工单07)落地时在同一目录追加同名节点
                MenuNode.of("系统监控", MenuType.DIR)
                        .path("/monitor")
                        .icon("chart")
                        .sortOrder(3)
                        .children(
                                MenuNode.of("登录日志", MenuType.MENU)
                                        .path("/monitor/logininfor")
                                        .component("/monitor/logininfor/index")
                                        .icon("time")
                                        .sortOrder(1)
                                        .children(
                                                button("查询登录日志", "system:log:list", 1),
                                                button("清空登录日志", "system:log:clear", 2)
                                        ),
                                MenuNode.of("在线用户", MenuType.MENU)
                                        .path("/monitor/online")
                                        .component("/monitor/online/index")
                                        .icon("internet")
                                        .sortOrder(2)
                                        .children(
                                                button("查询在线用户", "system:online:list", 1),
                                                button("强退用户", "system:online:forceLogout", 2)
                                        )
                        )
        );
    }
    private static MenuNode button(String name, String perms, int sortOrder) {
        return MenuNode.of(name, MenuType.BUTTON).perms(perms).sortOrder(sortOrder);
    }
}
