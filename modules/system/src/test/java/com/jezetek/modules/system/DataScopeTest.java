package com.jezetek.modules.system;

import com.jezetek.core.runtime.security.DataScope;
import com.jezetek.core.runtime.security.LoginUser;
import com.jezetek.modules.system.model.User;
import com.jezetek.modules.system.model.UserDraft;
import com.jezetek.modules.system.repository.UserRepository;
import com.jezetek.modules.system.service.DeptService;
import com.jezetek.modules.system.service.UserService;
import com.jezetek.modules.system.service.dto.UserInput;
import com.jezetek.modules.system.service.dto.UserSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据范围五档生效点断言(用户管理分页列表)。
 * 会话由直接构造 LoginUser 注入，范围在会话上而非角色表——
 * 角色表单→角色→用户链路由 RoleServiceTest/UserDetailsServiceImpl 侧覆盖
 */
@SpringBootTest
@Transactional
class DataScopeTest {

    @Autowired
    private UserService userService;

    @Autowired
    private DeptService deptService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void loginAsAdmin() {
        TestLogin.loginAs(TestLogin.ADMIN);
    }

    private LoginUser loginScoped(DataScope scope, long userId, String username) {
        LoginUser user = new LoginUser(
                userId, username, username,
                Set.of("USER"), Set.of(2L), Set.of("system:user:list"),
                scope
        );
        TestLogin.loginAs(user);
        return user;
    }

    private long createUserInDept(String username, long deptId) {
        UserInput input = new UserInput();
        input.setUsername(username);
        input.setPassword("123456");
        input.setEnabled(true);
        input.setDeptId(deptId);
        return userService.saveUser(input).id();
    }

    @Test
    void 全部不加过滤() {
        loginScoped(DataScope.ALL, 1L, "admin");

        Page<User> page = userService.findUsersBySuperQBE(0, 10, "username asc", new UserSpecification(), null);
        assertEquals(3, page.getTotalElements());
    }

    @Test
    void 自定义为精确勾选集合不含未勾选子孙() {
        // 在研发部(2)下再挂一个子部门+用户：勾选集合只有{2}时该用户不可见
        long childDeptId = deptService.saveDept(deptInput("研发一组", 2L)).id();
        createUserInDept("child_user", childDeptId);

        loginScoped(new DataScope(DataScope.Level.CUSTOM, Set.of(2L), 2L), 2L, "demo");

        Page<User> page = userService.findUsersBySuperQBE(0, 10, "username asc", new UserSpecification(), null);
        assertEquals(2, page.getTotalElements());
        // demo 与 frozen 在研发部，child_user 在未勾选的子部门，不可见
        assertTrue(page.getContent().stream().allMatch(u -> u.dept().id() == 2L));
    }

    @Test
    void 本部门仅精确一层() {
        loginScoped(new DataScope(DataScope.Level.DEPT, Set.of(), 2L), 2L, "demo");

        Page<User> page = userService.findUsersBySuperQBE(0, 10, "username asc", new UserSpecification(), null);
        assertEquals(2, page.getTotalElements());
    }

    @Test
    void 本部门及以下含全部子孙() {
        // 市场部(3) 下挂子部门+用户：本部门及以下(总公司 1) 必须能看到
        long childDeptId = deptService.saveDept(deptInput("市场一组", 3L)).id();
        createUserInDept("market_user", childDeptId);

        loginScoped(new DataScope(DataScope.Level.DEPT_AND_CHILD, Set.of(), 1L), 1L, "admin");

        // 总公司+研发部(demo/frozen)+市场部+市场一组(market_user) = 4
        Page<User> page = userService.findUsersBySuperQBE(0, 10, "username asc", new UserSpecification(), null);
        assertEquals(4, page.getTotalElements());
    }

    @Test
    void 仅本人只看到自己() {
        loginScoped(new DataScope(DataScope.Level.SELF, Set.of(), 2L), 2L, "demo");

        Page<User> page = userService.findUsersBySuperQBE(0, 10, "username asc", new UserSpecification(), null);
        assertEquals(1, page.getTotalElements());
        assertEquals(2L, page.getContent().get(0).id());
    }

    @Test
    void 多角色取范围最大() {
        DataScope merged = DataScope.broadest(List.of(
                new DataScope(DataScope.Level.SELF, Set.of(), 2L),
                DataScope.ALL
        ));
        assertEquals(DataScope.Level.ALL, merged.level());

        DataScope merged2 = DataScope.broadest(List.of(
                new DataScope(DataScope.Level.DEPT_AND_CHILD, Set.of(), 1L),
                new DataScope(DataScope.Level.CUSTOM, Set.of(3L), 1L)
        ));
        // 编码最小=范围最大：自定义(2) 宽于 本部门及以下(4)
        assertEquals(DataScope.Level.CUSTOM, merged2.level());
        assertEquals(Set.of(3L), merged2.deptIds());
    }

    @Test
    void 非生效点查询不受数据范围影响() {
        loginScoped(new DataScope(DataScope.Level.SELF, Set.of(), 2L), 2L, "demo");

        // username 查重等场景必须全量可见
        User admin = userRepository.findByUsername("admin", null).orElseThrow();
        assertEquals("admin", admin.username());
    }

    private com.jezetek.modules.system.service.dto.DeptInput deptInput(String name, Long parentId) {
        com.jezetek.modules.system.service.dto.DeptInput input =
                new com.jezetek.modules.system.service.dto.DeptInput();
        input.setName(name);
        input.setParentId(parentId);
        input.setSortOrder(9);
        input.setEnabled(true);
        return input;
    }
}
