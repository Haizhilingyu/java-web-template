package com.jezetek.core.runtime.module;

import java.util.List;

/**
 * 业务模块自描述 SPI——模块化机制的核心契约。
 *
 * <p>每个业务模块提供一个实现并注册为 Spring Bean，主应用通过
 * Maven 依赖决定引入哪些模块，被引入模块的 Bean 会被组件扫描发现：
 * <ul>
 *   <li>启动时菜单同步器收集所有 ModuleProvider，把 {@link #menus()}
 *       幂等 upsert 进 sys_menu 并按 roleCodes 绑定角色——引入即出菜单，
 *       移除依赖即整体消失</li>
 *   <li>{@link #apiPrefixes()} 声明模块 REST 接口的认证范围(默认 {@code /api/v1/{code}/**})</li>
 * </ul></p>
 *
 * <p>新模块开发清单见 docs/module-dev-guide.md</p>
 */
public interface ModuleProvider {

    /** 模块唯一编码，小写，如 system/job；同时是 sys_menu.module_code 的来源 */
    String code();

    /** 模块显示名 */
    String name();

    /** 排序，越小越靠前(影响菜单同步与聚合顺序) */
    default int order() {
        return 0;
    }

    /** 模块的 REST 接口前缀，命中即要求登录；默认按编码约定 /{code}/** */
    default List<String> apiPrefixes() {
        return List.of("/api/v1/" + code() + "/**");
    }

    /**
     * 模块菜单树(可为空列表——纯后端模块没有界面)。
     * 每次启动都会以本声明为准覆盖 sys_menu 对应行，勿在此返回动态数据
     */
    default List<MenuNode> menus() {
        return List.of();
    }
}
