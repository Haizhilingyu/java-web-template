package com.jezetek.modules.system;

import com.jezetek.modules.system.model.User;
import com.jezetek.modules.system.service.UserService;
import com.jezetek.modules.system.service.dto.UserInput;
import com.jezetek.modules.system.service.dto.UserSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void login() {
        TestLogin.loginAs(TestLogin.ADMIN);
    }

    @Test
    void 分页查询返回全部用户及其角色() {
        Page<User> page = userService.findUsersBySuperQBE(0, 5, "username asc", new UserSpecification(), null);

        // 种子数据：admin / demo / frozen 共 3 个用户
        assertEquals(3, page.getTotalElements());
        assertEquals("admin", page.getContent().get(0).username());
        assertEquals("demo", page.getContent().get(1).username());
        // DEFAULT_FETCHER 关联抓取了角色
        assertEquals("ADMIN", page.getContent().get(0).roles().get(0).code());
    }

    @Test
    void 按关键字模糊查询() {
        UserSpecification spec = new UserSpecification();
        spec.setKeyword("demo");

        Page<User> page = userService.findUsersBySuperQBE(0, 5, "username asc", spec, null);

        assertEquals(1, page.getTotalElements());
        assertEquals("demo", page.getContent().get(0).username());
    }

    @Test
    void 按角色名过滤() {
        UserSpecification spec = new UserSpecification();
        spec.setRoleName("管理员");

        Page<User> page = userService.findUsersBySuperQBE(0, 5, "username asc", spec, null);

        assertEquals(1, page.getTotalElements());
        assertEquals("admin", page.getContent().get(0).username());
    }

    @Test
    void 按启用状态过滤() {
        UserSpecification spec = new UserSpecification();
        spec.setEnabled(false);

        // 种子数据里仅 frozen 被禁用
        Page<User> page = userService.findUsersBySuperQBE(0, 5, "username asc", spec, null);
        assertEquals(1, page.getTotalElements());
        assertEquals("frozen", page.getContent().get(0).username());
    }

    @Test
    void 按id与用户名查询() {
        assertEquals("admin", userService.findUser(1L).username());
        assertNull(userService.findUser(999L));
        assertEquals("demo", userService.findUserByUsername("demo").username());
        assertNull(userService.findUserByUsername("no_such_user"));
    }

    @Test
    void 新增用户自动填充时间戳密码加密并以默认租户隔离可见() throws Exception {
        UserInput input = new UserInput();
        input.setUsername("tester_x");
        input.setPassword("123456");
        input.setNickname("测试用户");
        input.setEnabled(true);
        input.setRoleIds(java.util.List.of(2L));

        User saved = userService.saveUser(input);

        assertTrue(saved.id() > 0);
        assertNotNull(saved.createdTime());
        assertNotNull(saved.modifiedTime());
        assertEquals("tester_x", saved.username());
        // 明文密码落库前已被 BCrypt 加密
        assertNotEquals("123456", saved.password());
        assertTrue(saved.password().startsWith("$2"));
        assertEquals(1, saved.roles().size());
        assertEquals("USER", saved.roles().get(0).code());
        // 租户由 TenantAwareDraftInterceptor 自动填充为 default:
        // 带默认租户头能查到，其他租户头查不到(DEFAULT_FETCHER 不含 tenant 字段，只能这样间接验证)
        mockMvc.perform(get("/api/v1/user/username/tester_x")
                        .with(TestLogin.asAdmin())
                        .header("tenant", "default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("tester_x"));
        mockMvc.perform(get("/api/v1/user/username/tester_x")
                        .with(TestLogin.asAdmin())
                        .header("tenant", "other"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void 更新用户信息且不提交密码时保持原值() {
        String oldPassword = userService.findUser(2L).password();

        UserInput input = new UserInput();
        input.setId(2L);
        input.setUsername("demo");
        input.setEnabled(true);
        input.setRoleIds(java.util.List.of(2L));
        input.setNickname("新昵称");

        User saved = userService.saveUser(input);

        assertEquals(2L, saved.id());
        assertEquals("新昵称", saved.nickname());
        assertEquals(oldPassword, saved.password());
    }

    @Test
    void 删除用户() {
        userService.deleteUser(1L);
        assertNull(userService.findUser(1L));
    }

    @Test
    void 用户名不符合校验规则返回400() throws Exception {
        // 空用户名违反 @NotBlank/@Size/@Pattern
        mockMvc.perform(put("/api/v1/user")
                        .with(TestLogin.asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"enabled\":true,\"roleIds\":[]}"))
                .andExpect(status().isBadRequest());

        // 含非法字符违反 @Pattern
        mockMvc.perform(put("/api/v1/user")
                        .with(TestLogin.asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bad name!\",\"enabled\":true,\"roleIds\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 按部门筛选含全部子孙部门() {
        // 种子：admin 挂总公司(1)，demo/frozen 挂研发部(1 的子部门)
        Page<User> all = userService.findUsersBySuperQBE(0, 5, "username asc", new UserSpecification(), 1L);
        assertEquals(3, all.getTotalElements());

        Page<User> dev = userService.findUsersBySuperQBE(0, 5, "username asc", new UserSpecification(), 2L);
        assertEquals(2, dev.getTotalElements());

        // 财务部(4)无人
        Page<User> empty = userService.findUsersBySuperQBE(0, 5, "username asc", new UserSpecification(), 4L);
        assertEquals(0, empty.getTotalElements());
    }

    @Test
    void 保存用户带部门与岗位() {
        UserInput input = new UserInput();
        input.setUsername("dept_post_user");
        input.setPassword("123456");
        input.setEnabled(true);
        input.setDeptId(2L);
        input.setPostIds(java.util.List.of(1L, 2L));
        input.setRoleIds(java.util.List.of(2L));

        User saved = userService.saveUser(input);

        assertEquals(2L, saved.dept().id());
        assertEquals(2, saved.posts().size());
    }

    @Test
    void 更新用户不提交部门岗位时不清空既有关联() {
        UserInput input = new UserInput();
        input.setUsername("dept_post_user");
        input.setPassword("123456");
        input.setEnabled(true);
        input.setDeptId(2L);
        input.setPostIds(java.util.List.of(1L, 2L));
        input.setRoleIds(java.util.List.of(2L));
        User saved = userService.saveUser(input);

        // 二次更新不携带 deptId/postIds
        UserInput update = new UserInput();
        update.setId(saved.id());
        update.setUsername("dept_post_user");
        update.setEnabled(true);
        update.setNickname("只改昵称");

        User updated = userService.saveUser(update);

        assertEquals("只改昵称", updated.nickname());
        assertEquals(2L, updated.dept().id());
        assertEquals(2, updated.posts().size());
    }

    @Test
    void 更新用户提交空岗位列表时显式清空关联() {
        UserInput input = new UserInput();
        input.setUsername("dept_post_user");
        input.setPassword("123456");
        input.setEnabled(true);
        input.setPostIds(java.util.List.of(1L, 2L));
        input.setRoleIds(java.util.List.of(2L));
        User saved = userService.saveUser(input);
        assertEquals(2, saved.posts().size());

        // 显式提交空列表 = 清空岗位(区别于未提交)
        UserInput update = new UserInput();
        update.setId(saved.id());
        update.setUsername("dept_post_user");
        update.setEnabled(true);
        update.setPostIds(java.util.List.of());

        User updated = userService.saveUser(update);

        assertEquals(0, updated.posts().size());
    }

    @Test
    void 无权限用户查询用户列表返回403() throws Exception {
        mockMvc.perform(get("/api/v1/user/list/bySuperQBE")
                        .with(TestLogin.processor(TestLogin.DEMO)))
                .andExpect(status().isForbidden());
    }
}
