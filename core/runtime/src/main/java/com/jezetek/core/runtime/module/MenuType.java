package com.jezetek.core.runtime.module;

/**
 * 菜单类型，与前端路由约定对齐：
 * <ul>
 *   <li>{@link #DIR} 目录——渲染为布局容器(LAYOUT)，子菜单为其页面</li>
 *   <li>{@link #MENU} 菜单——一个页面路由，component 指向前端页面文件</li>
 *   <li>{@link #BUTTON} 按钮——不出现在路由中，仅 perms 参与按钮级权限</li>
 * </ul>
 */
public enum MenuType {

    DIR("M"),
    MENU("C"),
    BUTTON("F");

    private final String code;

    MenuType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static MenuType of(String code) {
        for (MenuType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知菜单类型: " + code);
    }
}
