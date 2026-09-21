package com.jezetek.modules.system;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 工单02：密码连错锁定。以 properties 覆盖单独开上下文
 * (测试 yml 默认关闭，避免跨用例累积误锁)；上下文独享 Caffeine 计数
 */
@SpringBootTest(properties = "core.security.login-protection.enabled=true")
@AutoConfigureMockMvc
@Transactional
class LoginLockoutTest {

    @Autowired
    private MockMvc mockMvc;

    private void wrongLogin(String username) throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 连错5次后正确密码也拒绝并提示剩余锁定时间() throws Exception {
        for (int i = 0; i < 5; i++) {
            wrongLogin("admin");
        }
        // 第 6 次：即使密码正确也被拒，提示剩余分钟
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("密码连续错误过多，账号已锁定，请约10分钟后再试"));
    }

    @Test
    void 用户名不存在不计数_连错多次仍返回密码错误() throws Exception {
        for (int i = 0; i < 6; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"no_such_user\",\"password\":\"whatever\"}"))
                    .andExpect(status().isUnauthorized())
                    // 未被锁定改写文案：始终是凭据错误
                    .andExpect(jsonPath("$.message").value("用户名或密码错误"));
        }
    }

    @Test
    void 中途成功登录清零计数() throws Exception {
        // 4 次(未达阈值 5)→ 成功登录清零 → 再 4 次仍未锁定
        for (int i = 0; i < 4; i++) {
            wrongLogin("admin");
        }
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk());
        for (int i = 0; i < 4; i++) {
            wrongLogin("demo");
        }
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo\",\"password\":\"123456\"}"))
                .andExpect(status().isOk());
    }
}
