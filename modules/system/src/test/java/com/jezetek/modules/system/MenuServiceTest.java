package com.jezetek.modules.system;

import com.jezetek.modules.system.model.Menu;
import com.jezetek.modules.system.service.MenuService;
import com.jezetek.modules.system.service.dto.MenuInput;
import com.jezetek.modules.system.service.dto.MenuSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MenuServiceTest {

    @Autowired
    private MenuService menuService;

    @BeforeEach
    void login() {
        TestLogin.loginAs(TestLogin.ADMIN);
    }

    @Test
    void 菜单树包含递归子菜单() {
        java.util.List<Menu> roots = menuService.findMenus();

        assertEquals(1, roots.size());
        Menu root = roots.getFirst();
        assertEquals("系统管理", root.name());
        assertEquals("M", root.type());
        // 递归抓取：子菜单全字段加载并按 sortOrder 升序
        assertEquals(3, root.children().size());
        assertEquals("用户管理", root.children().get(0).name());
        assertEquals("C", root.children().get(0).type());
        assertEquals("/user/index", root.children().get(0).path());
        assertEquals(1, root.children().get(0).sortOrder());
        // 叶子节点的 children 为空集合而非 null
        assertTrue(root.children().get(0).children().isEmpty());
    }

    @Test
    void 按关键字模糊查询() {
        MenuSpecification spec = new MenuSpecification();
        spec.setKeyword("角色");

        Page<Menu> page = menuService.findMenusBySuperQBE(0, 5, "sortOrder asc", spec);

        assertEquals(1, page.getTotalElements());
        assertEquals("角色管理", page.getContent().get(0).name());
    }

    @Test
    void 按id查询() {
        assertEquals("用户管理", menuService.findMenu(2L).name());
        assertNull(menuService.findMenu(999L));
    }

    @Test
    void 新增子菜单挂到指定父节点() {
        MenuInput input = new MenuInput();
        input.setName("测试菜单");
        input.setParentId(1L);
        input.setType("C");
        input.setPath("/test/index");
        input.setVisible(true);
        input.setSortOrder(9);

        Menu saved = menuService.saveMenu(input);

        assertTrue(saved.id() > 0);
        assertNotNull(saved.createdTime());
        // 树中根节点的子菜单从 3 变 4
        assertEquals(4, menuService.findMenus().get(0).children().size());
    }

    @Test
    void 更新菜单路由() {
        MenuInput input = new MenuInput();
        input.setId(2L);
        input.setName("用户管理");
        input.setParentId(1L);
        input.setType("C");
        input.setVisible(true);
        input.setSortOrder(1);
        input.setPath("/user/new-path");

        Menu saved = menuService.saveMenu(input);

        assertEquals(2L, saved.id());
        assertEquals("/user/new-path", saved.path());
    }

    @Test
    void 删除菜单() {
        menuService.deleteMenu(2L);
        assertNull(menuService.findMenu(2L));
    }
}
