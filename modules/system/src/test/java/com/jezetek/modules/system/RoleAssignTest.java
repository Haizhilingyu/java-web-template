package com.jezetek.modules.system;

import com.jezetek.modules.system.model.User;
import com.jezetek.modules.system.repository.UserRepository;
import com.jezetek.modules.system.service.RoleService;
import com.jezetek.modules.system.service.UserService;
import com.jezetek.modules.system.service.dto.UserInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 工单06：角色批量授权/取消授权。角色变更对 getRouters/权限点即时生效
 * (认证过滤器每次请求回库加载，无缓存)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RoleAssignTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void login() {
        TestLogin.loginAs(TestLogin.ADMIN);
    }

    /** 造一个无角色用户，返回 (用户名, id) */
    private String createRolelessUser() {
        UserInput input = new UserInput();
        input.setUsername("role_assign_user");
        input.setPassword("123456");
        input.setEnabled(true);
        userService.saveUser(input);
        User user = userRepository.findByUsername("role_assign_user", null).orElseThrow();
        return user.username();
    }

    private long rolelessUserId() {
        return userRepository.findByUsername("role_assign_user", null).orElseThrow().id();
    }

    private String loginToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    @Test
    void 批量授权后出现在已绑列表且getRouters即时生效() throws Exception {
        String username = createRolelessUser();
        long userId = rolelessUserId();

        // 无角色：路由为空
        String tokenBefore = loginToken(username, "123456");
        mockMvc.perform(get("/api/v1/auth/getRouters").header("Authorization", "Bearer " + tokenBefore))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // 批量授权 USER(id=2)
        mockMvc.perform(post("/api/v1/role/2/users").with(TestLogin.asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userIds\":[" + userId + "]}"))
                .andExpect(status().isOk());

        // 已绑列表可见
        Page<com.jezetek.modules.system.model.User> boundPage = mockMvcLoginPage();
        assertTrue(boundPage.getContent().stream().anyMatch(u -> username.equals(u.username())),
                "授权后应出现在已绑列表");

        // 即时生效：重新登录后 getRouters 出现系统管理目录
        String tokenAfter = loginToken(username, "123456");
        mockMvc.perform(get("/api/v1/auth/getRouters").header("Authorization", "Bearer " + tokenAfter))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].path").value("/system"));
    }

    private Page<com.jezetek.modules.system.model.User> mockMvcLoginPage() {
        // 已绑列表经 service 直查(MockMvc 后 SecurityContext 被清空，直连前重登)
        TestLogin.loginAs(TestLogin.ADMIN);
        return roleService.findRoleUsers(2L, 0, 20, null);
    }

    @Test
    void 批量取消授权后角色权限即时失效() throws Exception {
        String username = createRolelessUser();
        long userId = rolelessUserId();
        mockMvc.perform(post("/api/v1/role/2/users").with(TestLogin.asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userIds\":[" + userId + "]}"))
                .andExpect(status().isOk());
        String tokenBound = loginToken(username, "123456");
        mockMvc.perform(get("/api/v1/auth/getRouters").header("Authorization", "Bearer " + tokenBound))
                .andExpect(jsonPath("$[0].path").value("/system"));

        // 批量取消授权
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/role/2/users").with(TestLogin.asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userIds\":[" + userId + "]}"))
                .andExpect(status().isOk());

        // 已绑列表不再包含
        Page<com.jezetek.modules.system.model.User> boundPage = mockMvcLoginPage();
        assertTrue(boundPage.getContent().stream().noneMatch(u -> username.equals(u.username())),
                "取消后应从已绑列表消失");

        // 即时失效：重新登录后路由为空
        String tokenUnbound = loginToken(username, "123456");
        mockMvc.perform(get("/api/v1/auth/getRouters").header("Authorization", "Bearer " + tokenUnbound))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void 无roleEdit权限的demo调用403() throws Exception {
        mockMvc.perform(post("/api/v1/role/2/users").with(TestLogin.processor(TestLogin.DEMO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userIds\":[2]}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/role/2/users").with(TestLogin.processor(TestLogin.DEMO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userIds\":[2]}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/role/2/users").with(TestLogin.processor(TestLogin.DEMO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void 授权重复请求幂等且空列表400() throws Exception {
        String username = createRolelessUser();
        long userId = rolelessUserId();
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/role/2/users").with(TestLogin.asAdmin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userIds\":[" + userId + "," + userId + "]}"))
                    .andExpect(status().isOk());
        }
        // 双写+重复请求后仍只绑定一次：已绑列表恰好 1 条该用户
        Page<User> bound = mockMvcLoginPage();
        assertEquals(1, bound.getContent().stream().filter(u -> username.equals(u.username())).count());

        // 空列表 → 400
        mockMvc.perform(post("/api/v1/role/2/users").with(TestLogin.asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userIds\":[]}"))
                .andExpect(status().isBadRequest());
    }
}
