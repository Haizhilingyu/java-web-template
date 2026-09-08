package com.jezetek.core.service;

import com.jezetek.core.model.Role;
import com.jezetek.core.service.dto.RoleInput;
import com.jezetek.core.service.dto.RoleSpecification;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RoleServiceTest {

    @Autowired
    private RoleService roleService;

    @Test
    void 分页查询返回全部角色() {
        Page<Role> page = roleService.findRolesBySuperQBE(0, 5, "code asc", new RoleSpecification());

        assertEquals(2, page.getTotalElements());
        assertEquals("ADMIN", page.getContent().get(0).code());
        assertEquals("USER", page.getContent().get(1).code());
    }

    @Test
    void 按关键字模糊查询() {
        RoleSpecification spec = new RoleSpecification();
        spec.setKeyword("adm");

        Page<Role> page = roleService.findRolesBySuperQBE(0, 5, "code asc", spec);

        assertEquals(1, page.getTotalElements());
        assertEquals("ADMIN", page.getContent().get(0).code());
    }

    @Test
    void 按菜单名过滤() {
        RoleSpecification spec = new RoleSpecification();
        spec.setMenuName("用户管理");

        Page<Role> page = roleService.findRolesBySuperQBE(0, 5, "code asc", spec);

        // ADMIN(4个菜单)与 USER(2个菜单) 都包含"用户管理"
        assertEquals(2, page.getTotalElements());
    }

    @Test
    void 按id查询关联抓取菜单() {
        Role role = roleService.findRole(1L);

        assertEquals("ADMIN", role.code());
        // DEFAULT_FETCHER 关联抓取了菜单，ADMIN 拥有全部 4 个
        assertEquals(4, role.menus().size());
        // 按 sortOrder 升序
        assertEquals("系统管理", role.menus().get(0).name());
    }

    @Test
    void 新增角色并建立菜单关联() {
        RoleInput input = new RoleInput();
        input.setCode("TEST");
        input.setName("测试角色");
        input.setDescription("单元测试创建");
        input.setMenuIds(java.util.List.of(1L, 2L));

        Role saved = roleService.saveRole(input);

        assertTrue(saved.id() > 0);
        assertEquals("TEST", saved.code());
        assertNotNull(saved.createdTime());
        assertEquals(2, saved.menus().size());
        // tenant 由拦截器自动填充，但 DEFAULT_FETCHER 不含该字段(UserServiceTest 已间接验证租户落库)
    }

    @Test
    void 更新角色描述() {
        RoleInput input = new RoleInput();
        input.setId(2L);
        input.setCode("USER");
        input.setName("普通用户");
        input.setDescription("新描述");

        Role saved = roleService.saveRole(input);

        assertEquals(2L, saved.id());
        assertEquals("新描述", saved.description());
    }

    @Test
    void 删除角色() {
        roleService.deleteRole(2L);
        assertNull(roleService.findRole(2L));
    }
}
