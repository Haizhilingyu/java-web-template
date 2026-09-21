package com.jezetek.modules.system;

import com.jezetek.core.runtime.BusinessException;
import com.jezetek.modules.system.model.User;
import com.jezetek.modules.system.repository.UserRepository;
import com.jezetek.modules.system.security.AuthModels;
import com.jezetek.modules.system.security.AuthController;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 个人中心：资料/改昵称/改密(作废全部会话，ADR-0001)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProfileTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthController authController;

    @Autowired
    private UserRepository userRepository;

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    @BeforeEach
    void loginAsAdmin() {
        TestLogin.loginAs(TestLogin.ADMIN);
    }

    @Test
    void 资料含部门角色岗位() {
        // 种子 demo 挂研发部/普通用户角色
        TestLogin.loginAs(TestLogin.DEMO);
        AuthModels.ProfileResponse profile = authController.profile();

        assertEquals("demo", profile.username());
        assertEquals("研发部", profile.deptName());
        assertTrue(profile.roleNames().contains("普通用户"));
    }

    @Test
    void 旧密码错误被拒且不改密() throws Exception {
        String oldHash = userRepository.findById(1L, com.jezetek.modules.system.model.Fetchers.USER_FETCHER.password()).password();

        mockMvc.perform(put("/api/v1/auth/password")
                        .header("Authorization", "Bearer " + login("admin", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"wrong\",\"newPassword\":\"654321\"}"))
                .andExpect(status().isBadRequest());

        assertEquals(oldHash, userRepository.findById(1L, com.jezetek.modules.system.model.Fetchers.USER_FETCHER.password()).password());
    }

    @Test
    void 改密成功后全部会话作废且新密码可登录() throws Exception {
        String token1 = login("admin", "123456");
        String token2 = login("admin", "123456");

        mockMvc.perform(put("/api/v1/auth/password")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"123456\",\"newPassword\":\"654321\"}"))
                .andExpect(status().isOk());

        // 两个会话(含发起改密的当前会话)全部 401
        mockMvc.perform(get("/api/v1/auth/getInfo").header("Authorization", "Bearer " + token1))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/auth/getInfo").header("Authorization", "Bearer " + token2))
                .andExpect(status().isUnauthorized());

        // 旧密码不能再登录，新密码可以
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"654321\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void 改昵称后资料更新() throws Exception {
        String token = login("demo", "123456");

        mockMvc.perform(put("/api/v1/auth/nickname")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"演示改名\"}"))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/v1/auth/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(result.getResponse().getContentAsString().contains("演示改名"));
    }
}
