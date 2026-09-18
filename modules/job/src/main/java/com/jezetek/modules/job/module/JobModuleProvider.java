package com.jezetek.modules.job.module;

import com.jezetek.core.runtime.module.MenuNode;
import com.jezetek.core.runtime.module.MenuType;
import com.jezetek.core.runtime.module.ModuleProvider;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * job 模块自描述。
 *
 * <p>业务 REST 统一挂 {@code /api/v1/job}，与前端页面路由 /job/task 隔离，
 * 避免宽前缀匹配把 SPA 页面拦成 401 JSON(登录态刷新即白屏)；
 * 菜单默认不绑定 USER 角色(仅超管可见)，需要时在角色管理界面勾选</p>
 */
@Component
public class JobModuleProvider implements ModuleProvider {

    @Override
    public String code() {
        return "job";
    }

    @Override
    public String name() {
        return "定时任务";
    }

    @Override
    public int order() {
        return 10;
    }

    /** REST 统一挂在 /job/api 下，与前端页面路由 /job/task 分离(默认约定 /job/** 会误伤) */
    @Override
    public List<String> apiPrefixes() {
        return List.of("/api/v1/job/**");
    }

    @Override
    public List<MenuNode> menus() {
        return List.of(
                MenuNode.of("任务调度", MenuType.DIR)
                        .path("/job")
                        .icon("chart-bubble")
                        .sortOrder(2)
                        .children(
                                MenuNode.of("定时任务", MenuType.MENU)
                                        .path("/job/task")
                                        .component("/job/index")
                                        .icon("root-list")
                                        .sortOrder(1)
                                        .children(
                                                button("查询任务", "job:list", 1),
                                                button("新增任务", "job:add", 2),
                                                button("编辑任务", "job:edit", 3),
                                                button("删除任务", "job:delete", 4),
                                                button("启停任务", "job:status", 5),
                                                button("执行任务", "job:run", 6)
                                        )
                        )
        );
    }

    private static MenuNode button(String name, String perms, int sortOrder) {
        return MenuNode.of(name, MenuType.BUTTON).perms(perms).sortOrder(sortOrder);
    }
}
