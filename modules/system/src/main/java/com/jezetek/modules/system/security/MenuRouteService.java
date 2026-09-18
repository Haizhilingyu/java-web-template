package com.jezetek.modules.system.security;

import com.jezetek.core.runtime.security.LoginUser;
import com.jezetek.core.runtime.security.SecurityUtils;
import com.jezetek.modules.system.model.Fetchers;
import com.jezetek.modules.system.model.Menu;
import com.jezetek.modules.system.repository.MenuRepository;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 组装当前登录用户的前端动态路由(getRouters)。
 *
 * <p>超级管理员返回全部可见菜单；普通角色返回其角色绑定菜单的并集
 * (祖先目录由模块菜单同步器绑定时保证存在)。
 * 输出即前端 RouteItem 形状：目录→LAYOUT + 相对路径子路由，
 * 页面→pages 下的组件路径字符串</p>
 */
@Service
public class MenuRouteService implements Fetchers {

    /** 路由抓取形状：全部标量属性 + 父菜单 id(id 视图，用于树组装) */
    private static final Fetcher<Menu> ROUTE_FETCHER = MENU_FETCHER.allScalarFields().parentId();

    private final MenuRepository menuRepository;

    public MenuRouteService(MenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    public List<AuthModels.RouteVO> getRouters() {
        LoginUser user = SecurityUtils.currentLoginUser();
        List<Menu> menus = menuRepository.findRouteMenus(
                user.isSuperAdmin() ? null : user.roleIds(),
                ROUTE_FETCHER
        );
        return assemble(menus);
    }

    private List<AuthModels.RouteVO> assemble(List<Menu> menus) {
        Map<Long, List<Menu>> childrenByParent = menus.stream()
                .filter(menu -> menu.parentId() != null)
                .collect(Collectors.groupingBy(
                        Menu::parentId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<AuthModels.RouteVO> routes = new ArrayList<>();
        for (Menu menu : menus) {
            if (menu.parentId() == null) {
                routes.add(toRoute(menu, null, childrenByParent));
            }
        }
        return routes;
    }

    private AuthModels.RouteVO toRoute(Menu menu, @Nullable String parentPath, Map<Long, List<Menu>> childrenByParent) {
        List<Menu> children = childrenByParent.getOrDefault(menu.id(), List.of());
        AuthModels.RouteMeta meta = new AuthModels.RouteMeta(
                menu.name(),
                menu.icon(),
                menu.sortOrder(),
                !menu.visible()
        );

        if ("M".equals(menu.type())) {
            List<AuthModels.RouteVO> childRoutes = new ArrayList<>();
            String firstChildPath = null;
            for (Menu child : children) {
                if (firstChildPath == null && child.path() != null) {
                    firstChildPath = child.path();
                }
                childRoutes.add(toRoute(child, menu.path(), childrenByParent));
            }
            return new AuthModels.RouteVO(
                    routeName(menu),
                    menu.path(),
                    "LAYOUT",
                    firstChildPath,
                    meta,
                    childRoutes.isEmpty() ? null : childRoutes
            );
        }

        String component = menu.component() == null || menu.component().isBlank()
                ? menu.path() + "/index"
                : menu.component();
        return new AuthModels.RouteVO(
                routeName(menu),
                relativePath(menu.path(), parentPath),
                component,
                null,
                meta,
                null
        );
    }

    /** 目录的子路由用相对路径(去掉父目录前缀)，顶层保持绝对路径 */
    @Nullable
    private static String relativePath(@Nullable String path, @Nullable String parentPath) {
        if (path == null || parentPath == null) {
            return path;
        }
        String prefix = parentPath + "/";
        return path.startsWith(prefix) ? path.substring(prefix.length()) : path;
    }

    /** 路由名从路径推导，保证稳定唯一：/system/user → SystemUser */
    private static String routeName(Menu menu) {
        if (menu.path() == null || menu.path().isBlank()) {
            return "Menu" + menu.id();
        }
        StringBuilder name = new StringBuilder();
        for (String segment : menu.path().split("/")) {
            if (!segment.isBlank()) {
                name.append(Character.toUpperCase(segment.charAt(0)))
                        .append(segment, 1, segment.length());
            }
        }
        return name.isEmpty() ? "Menu" + menu.id() : name.toString();
    }
}
