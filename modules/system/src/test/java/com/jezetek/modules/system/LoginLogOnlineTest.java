package com.jezetek.modules.system;

import com.jezetek.core.runtime.security.TokenService;
import com.jezetek.modules.system.model.Logininfor;
import com.jezetek.modules.system.service.LogininforService;
import com.jezetek.modules.system.service.OnlineService;

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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoginLogOnlineTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private OnlineService onlineService;

    @Autowired
    private LogininforService logininforService;

    @BeforeEach
    void login() {
        TestLogin.loginAs(TestLogin.ADMIN);
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    private Page<Logininfor> allLogs() {
        // MockMvc 请求后测试框架会清空 SecurityContext，直连 service 前重登
        TestLogin.loginAs(TestLogin.ADMIN);
        return logininforService.findLogininforsBySuperQBE(0, 100, "id desc", null);
    }

    @Test
    void 登录成功失败登出均写登录日志() throws Exception {
        login("admin", "123456");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
        String token = login("admin", "123456");
        mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        Page<Logininfor> page = allLogs();
        String messages = page.getContent().stream()
                .map(l -> l.username() + ":" + l.message())
                .reduce("", (a, b) -> a + "|" + b);
        assertTrue(messages.contains("admin:登录成功"), messages);
        assertTrue(messages.contains("admin:登录失败：用户名或密码错误"), messages);
        assertTrue(messages.contains("admin:登出"), messages);
    }

    @Test
    void 在线用户实时反映注册表且强退立即生效() throws Exception {
        // MockMvc 请求后测试框架会清空 SecurityContext，直连 service 前重登
        TestLogin.loginAs(TestLogin.ADMIN);
        int before = onlineService.list().size();
        String token1 = login("admin", "123456");
        String token2 = login("admin", "123456");

        TestLogin.loginAs(TestLogin.ADMIN);
        assertEquals(before + 2, onlineService.list().size());

        // 强退 token1 会话：在线列表里必须能按 jti 找到它
        TokenService.TokenPayload payload = tokenService.parse(token1);
        TestLogin.loginAs(TestLogin.ADMIN);
        List<OnlineService.OnlineUser> online = onlineService.list();
        String mineJti = online.stream()
                .filter(u -> u.jti().equals(payload.jti()))
                .map(OnlineService.OnlineUser::jti)
                .findFirst()
                .orElseThrow();
        assertEquals(payload.jti(), mineJti);

        mockMvc.perform(delete("/api/v1/online/" + mineJti).header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk());

        // 被强退的 admin 立即 401，demo 不受影响
        mockMvc.perform(get("/api/v1/auth/getInfo").header("Authorization", "Bearer " + token1))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/auth/getInfo").header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk());
    }

    @Test
    void 清空登录日志需权限且无权限用户返回403() throws Exception {
        // 先产生日志：登录成功与失败各一条
        login("admin", "123456");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"bad\"}"))
                .andExpect(status().isUnauthorized());
        assertTrue(allLogs().getTotalElements() > 0);

        // demo(USER 角色，无 system:log:clear 权限) → 403
        String demoToken = login("demo", "123456");
        mockMvc.perform(delete("/api/v1/logininfor/clear").header("Authorization", "Bearer " + demoToken))
                .andExpect(status().isForbidden());

        // admin 清空 → 200 且日志为空
        String adminToken = login("admin", "123456");
        mockMvc.perform(delete("/api/v1/logininfor/clear").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        assertEquals(0, allLogs().getTotalElements());
    }
}
