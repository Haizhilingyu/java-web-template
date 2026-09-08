package com.jezetek.core.service;

import com.jezetek.core.model.User;
import com.jezetek.core.service.dto.UserInput;
import com.jezetek.core.service.dto.UserSpecification;
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

    @Test
    void 分页查询返回全部用户及其角色() {
        Page<User> page = userService.findUsersBySuperQBE(0, 5, "username asc", new UserSpecification());

        assertEquals(2, page.getTotalElements());
        assertEquals("admin", page.getContent().get(0).username());
        assertEquals("demo", page.getContent().get(1).username());
        // DEFAULT_FETCHER 关联抓取了角色
        assertEquals("ADMIN", page.getContent().get(0).roles().get(0).code());
    }

    @Test
    void 按关键字模糊查询() {
        UserSpecification spec = new UserSpecification();
        spec.setKeyword("demo");

        Page<User> page = userService.findUsersBySuperQBE(0, 5, "username asc", spec);

        assertEquals(1, page.getTotalElements());
        assertEquals("demo", page.getContent().get(0).username());
    }

    @Test
    void 按角色名过滤() {
        UserSpecification spec = new UserSpecification();
        spec.setRoleName("管理员");

        Page<User> page = userService.findUsersBySuperQBE(0, 5, "username asc", spec);

        assertEquals(1, page.getTotalElements());
        assertEquals("admin", page.getContent().get(0).username());
    }

    @Test
    void 按启用状态过滤() {
        UserSpecification spec = new UserSpecification();
        spec.setEnabled(false);

        assertEquals(0, userService.findUsersBySuperQBE(0, 5, "username asc", spec).getTotalElements());
    }

    @Test
    void 按id与用户名查询() {
        assertEquals("admin", userService.findUser(1L).username());
        assertNull(userService.findUser(999L));
        assertEquals("demo", userService.findUserByUsername("demo").username());
        assertNull(userService.findUserByUsername("no_such_user"));
    }

    @Test
    void 新增用户自动填充时间戳并以默认租户隔离可见() throws Exception {
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
        assertEquals(1, saved.roles().size());
        assertEquals("USER", saved.roles().get(0).code());
        // 租户由 TenantAwareDraftInterceptor 自动填充为 default:
        // 带默认租户头能查到，其他租户头查不到(DEFAULT_FETCHER 不含 tenant 字段，只能这样间接验证)
        mockMvc.perform(get("/user/username/tester_x").header("tenant", "default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("tester_x"));
        mockMvc.perform(get("/user/username/tester_x").header("tenant", "other"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void 更新用户信息() {
        UserInput input = new UserInput();
        input.setId(2L);
        input.setUsername("demo");
        input.setEnabled(true);
        input.setRoleIds(java.util.List.of(2L));
        input.setNickname("新昵称");

        User saved = userService.saveUser(input);

        assertEquals(2L, saved.id());
        assertEquals("新昵称", saved.nickname());
    }

    @Test
    void 删除用户() {
        userService.deleteUser(1L);
        assertNull(userService.findUser(1L));
    }

    @Test
    void 用户名不符合校验规则返回400() throws Exception {
        // 空用户名违反 @NotBlank/@Size/@Pattern
        mockMvc.perform(put("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"enabled\":true,\"roleIds\":[]}"))
                .andExpect(status().isBadRequest());

        // 含非法字符违反 @Pattern
        mockMvc.perform(put("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bad name!\",\"enabled\":true,\"roleIds\":[]}"))
                .andExpect(status().isBadRequest());
    }
}
