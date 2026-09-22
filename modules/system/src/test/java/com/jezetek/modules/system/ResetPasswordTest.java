package com.jezetek.modules.system;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 工单05：管理员重置密码。重置后目标用户全部会话立即作废，
 * 新密码可登录、旧密码不可
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ResetPasswordTest {

    @Autowired
    private MockMvc mockMvc;

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

    @Test
    void 重置后旧会话作废且旧密码失效新密码可登录() throws Exception {
        // 目标用户 demo 先登录拿到会话
        String demoToken = login("demo", "123456");
        mockMvc.perform(get("/api/v1/auth/getInfo").header("Authorization", "Bearer " + demoToken))
                .andExpect(status().isOk());

        // 管理员重置 demo 密码
        mockMvc.perform(put("/api/v1/user/2/password").with(TestLogin.asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"new123456\"}"))
                .andExpect(status().isOk());

        // 旧会话立即 401
        mockMvc.perform(get("/api/v1/auth/getInfo").header("Authorization", "Bearer " + demoToken))
                .andExpect(status().isUnauthorized());

        // 旧密码 401、新密码可登录
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo\",\"password\":\"123456\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo\",\"password\":\"new123456\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void 无resetPwd权限的demo调用403() throws Exception {
        mockMvc.perform(put("/api/v1/user/1/password").with(TestLogin.processor(TestLogin.DEMO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"hack123456\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void 重置不存在的用户返回400() throws Exception {
        mockMvc.perform(put("/api/v1/user/999/password").with(TestLogin.asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"new123456\"}"))
                .andExpect(status().isBadRequest());
    }
}
