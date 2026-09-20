package com.jezetek.modules.system;

import com.jezetek.modules.system.menu.MenuSyncService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MenuSyncService menuSyncService;

    @Test
    void 登录成功签发令牌并可访问getInfo() throws Exception {
        String token = login("admin", "123456");

        mockMvc.perform(get("/api/v1/auth/getInfo").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("admin"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.perms[0]").value("*:*:*"));
    }

    @Test
    void 登录后可拉取按角色过滤的动态路由() throws Exception {
        // 测试上下文关闭了启动同步，这里显式执行一次(system 模块声明 1 目录 + 5 页面 + 20 按钮)
        menuSyncService.sync();

        String token = login("admin", "123456");

        MvcResult result = mockMvc.perform(get("/api/v1/auth/getRouters").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].path").value("/system"))
                .andExpect(jsonPath("$[0].component").value("LAYOUT"))
                .andExpect(jsonPath("$[0].children.length()").value(5))
                .andExpect(jsonPath("$[0].children[0].component").value("/system/user/index"))
                .andExpect(jsonPath("$[0].children[0].meta.title").value("用户管理"))
                .andReturn();
        // 按钮不应出现在路由里
        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains("system:user:add"));
    }

    @Test
    void 密码错误返回401与提示() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void 禁用账号登录返回401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"frozen\",\"password\":\"123456\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("账号已禁用"));
    }

    @Test
    void 未携带令牌访问受保护接口返回401() throws Exception {
        mockMvc.perform(get("/api/v1/user/list/bySuperQBE"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 无权限用户访问受限接口返回403() throws Exception {
        // demo 只有 USER 角色，测试种子未给它任何按钮权限
        String token = login("demo", "123456");

        mockMvc.perform(get("/api/v1/user/list/bySuperQBE").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("token").asString();
    }
}
