package com.jezetek.modules.system.menu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 启动时执行一次菜单同步。测试上下文通过
 * {@code core.module.menu-sync=false} 关闭(测试用自己的种子数据)
 */
@Component
@ConditionalOnProperty(prefix = "core.module", name = "menu-sync", havingValue = "true", matchIfMissing = true)
public class MenuSyncInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MenuSyncInitializer.class);

    private final MenuSyncService menuSyncService;

    public MenuSyncInitializer(MenuSyncService menuSyncService) {
        this.menuSyncService = menuSyncService;
    }

    @Override
    public void run(ApplicationArguments args) {
        menuSyncService.sync();
        log.info("模块菜单注册完毕，动态路由数据就绪");
    }
}
