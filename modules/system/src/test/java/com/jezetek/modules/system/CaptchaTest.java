package com.jezetek.modules.system;

import com.jezetek.modules.system.model.Config;
import com.jezetek.modules.system.model.ConfigDraft;
import com.jezetek.modules.system.repository.ConfigRepository;
import com.jezetek.modules.system.security.CaptchaService;
import com.jezetek.modules.system.service.LogininforService;
import com.jezetek.modules.system.model.Logininfor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 工单01：登录验证码。开关存 sys_config(测试种子默认 false)，
 * 登录链路每次直读库，事务内改值即可验证"参数页改后即时生效"
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CaptchaTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private ConfigRepository configRepository;

    @Autowired
    private LogininforService logininforService;

    @BeforeEach
    void login() {
        TestLogin.loginAs(TestLogin.ADMIN);
    }

    /** 模拟参数页把 captchaEnabled 改为 true(事务回滚，不污染其他用例) */
    private void enableCaptcha() {
        Config config = configRepository.findByConfigKey(CaptchaService.CONFIG_KEY, null).orElseThrow();
        Config updated = ConfigDraft.$.produce(config, draft -> draft.setConfigValue("true"));
        configRepository.saveCommand(updated).execute();
    }

    private Page<Logininfor> allLogs() {
        TestLogin.loginAs(TestLogin.ADMIN);
        return logininforService.findLogininforsBySuperQBE(0, 100, "id desc", null);
    }

    @Test
    void 开关关闭时captcha接口只回enabled且登录无需验证码() throws Exception {
        mockMvc.perform(get("/api/v1/auth/captcha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        // 不带 captchaKey/captchaCode 的传统登录不受影响(curl 冒烟同款)
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void 开关开启后captcha接口返回图形且错码拒绝登录并记日志() throws Exception {
        enableCaptcha();

        mockMvc.perform(get("/api/v1/auth/captcha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        CaptchaService.Challenge challenge = captchaService.generate();

        // 错误验证码：拒绝登录(400)且记登录日志失败
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\","
                                + "\"captchaKey\":\"" + challenge.key() + "\",\"captchaCode\":\"wrong\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("验证码错误或已过期"));

        Page<Logininfor> logs = allLogs();
        assertTrue(logs.getContent().stream()
                .anyMatch(l -> "admin".equals(l.username()) && "登录失败：验证码错误".equals(l.message())),
                "错码应记登录日志失败");

        // 正确验证码登录成功(错码那把 key 已被消费，用新挑战)
        CaptchaService.Challenge valid = captchaService.generate();
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\","
                                + "\"captchaKey\":\"" + valid.key() + "\",\"captchaCode\":\"" + valid.code() + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void 验证码一次性同key重放失败() throws Exception {
        enableCaptcha();
        CaptchaService.Challenge challenge = captchaService.generate();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\","
                                + "\"captchaKey\":\"" + challenge.key() + "\",\"captchaCode\":\"" + challenge.code() + "\"}"))
                .andExpect(status().isOk());

        // 同 key 同 code 重放：已被消费，拒绝
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\","
                                + "\"captchaKey\":\"" + challenge.key() + "\",\"captchaCode\":\"" + challenge.code() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 开关开启时缺验证码字段直接拒绝() throws Exception {
        enableCaptcha();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isBadRequest());

        assertEquals(Boolean.TRUE, captchaService.enabled());
        assertFalse(captchaService.verify(null, null));
    }
}
