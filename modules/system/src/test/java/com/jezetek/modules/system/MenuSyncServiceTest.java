package com.jezetek.modules.system;

import com.jezetek.modules.system.menu.MenuSyncService;
import com.jezetek.modules.system.model.Menu;
import com.jezetek.modules.system.model.Role;
import com.jezetek.modules.system.repository.MenuRepository;
import com.jezetek.modules.system.repository.RoleRepository;
import com.jezetek.modules.system.service.dto.MenuSpecification;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MenuSyncServiceTest {

    @Autowired
    private MenuSyncService menuSyncService;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void 同步幂等且写入模块来源与角色绑定() {
        menuSyncService.sync();

        List<Menu> roots = menuRepository.findRootMenus(TREE_FETCHER);
        assertEquals(2, roots.size());
        Menu root = roots.getFirst();
        assertEquals("系统管理", root.name());
        assertEquals("M", root.type());
        assertEquals("system", root.moduleCode());
        assertEquals(8, root.children().size());

        Menu userMenu = root.children().getFirst();
        assertEquals("/system/user", userMenu.path());
        assertEquals("/system/user/index", userMenu.component());
        assertEquals(6, userMenu.children().size());
        assertEquals("F", userMenu.children().getFirst().type());
        assertEquals("system:user:list", userMenu.children().getFirst().perms());

        // 声明共 2 目录 + 11 页面 + 40 按钮 = 53 个节点(工单03 用户导入导出各 +1)
        long total = count();
        assertEquals(53, total);

        // 再次同步不产生重复数据
        menuSyncService.sync();
        assertEquals(total, count());
        assertEquals(2, menuRepository.findRootMenus(TREE_FETCHER).size());

        // USER 角色按声明获得：目录 + 用户管理 + 查询按钮
        Role userRole = roleRepository.findByCode("USER", ROLE_FETCHER).orElseThrow();
        List<String> boundNames = userRole.menus().stream().map(Menu::name).toList();
        assertTrue(boundNames.contains("系统管理"));
        assertTrue(boundNames.contains("用户管理"));
        assertTrue(boundNames.contains("查询用户"));
        assertFalse(boundNames.contains("删除用户"));
    }

    private long count() {
        Page<Menu> page = menuRepository.find(PageRequest.of(0, 1), new MenuSpecification(), null);
        return page.getTotalElements();
    }

    private static final org.babyfish.jimmer.sql.fetcher.Fetcher<Menu> TREE_FETCHER =
            com.jezetek.modules.system.model.Fetchers.MENU_FETCHER.allScalarFields().recursiveChildren();

    private static final org.babyfish.jimmer.sql.fetcher.Fetcher<Role> ROLE_FETCHER =
            com.jezetek.modules.system.model.Fetchers.ROLE_FETCHER.allScalarFields().tenant(false).menus(
                    com.jezetek.modules.system.model.Fetchers.MENU_FETCHER.allScalarFields()
            );
}
