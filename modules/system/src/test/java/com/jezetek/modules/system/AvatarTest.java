package com.jezetek.modules.system;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 工单07：头像上传/读取。写读回环、超限拒绝、类型白名单、未登录 401
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AvatarTest {

    private static final byte[] PNG_BYTES = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 1, 2, 3, 4};

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void login() {
        TestLogin.loginAs(TestLogin.ADMIN);
    }

    private MockMultipartFile pngFile(String filename, byte[] bytes) {
        return new MockMultipartFile("file", filename, "image/png", bytes);
    }

    @Test
    void 上传后可读回且类型识别为png() throws Exception {
        mockMvc.perform(multipart("/api/v1/auth/avatar").file(pngFile("me.png", PNG_BYTES))
                        .with(TestLogin.asAdmin()))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/v1/auth/avatar").with(TestLogin.asAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andReturn();
        assertArrayEquals(PNG_BYTES, result.getResponse().getContentAsByteArray());
    }

    @Test
    void 重新登录后头像仍在且覆盖上传生效() throws Exception {
        mockMvc.perform(multipart("/api/v1/auth/avatar").file(pngFile("a.png", PNG_BYTES))
                        .with(TestLogin.asAdmin()))
                .andExpect(status().isOk());
        byte[] v2 = {'G', 'I', 'F', '8', '9', 'a', 9, 9};
        mockMvc.perform(multipart("/api/v1/auth/avatar")
                        .file(new MockMultipartFile("file", "b.gif", "image/gif", v2))
                        .with(TestLogin.asAdmin()))
                .andExpect(status().isOk());

        // 重新登录(新会话回库加载)
        MvcResult login = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String token = login.getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        mockMvc.perform(get("/api/v1/auth/avatar").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/gif"));
    }

    @Test
    void 超过2MB被拒绝() throws Exception {
        byte[] tooBig = new byte[2 * 1024 * 1024 + 1];
        mockMvc.perform(multipart("/api/v1/auth/avatar").file(pngFile("big.png", tooBig))
                        .with(TestLogin.asAdmin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 非图片扩展名被拒绝() throws Exception {
        MockMultipartFile evil = new MockMultipartFile("file", "shell.html", "text/html",
                "<html></html>".getBytes());
        mockMvc.perform(multipart("/api/v1/auth/avatar").file(evil).with(TestLogin.asAdmin()))
                .andExpect(status().isBadRequest());
        // 无头像：读取 404
        mockMvc.perform(get("/api/v1/auth/avatar").with(TestLogin.asAdmin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void 未登录访问头像接口401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/avatar"))
                .andExpect(status().isUnauthorized());
        assertNotNull(mockMvc);
    }
}
