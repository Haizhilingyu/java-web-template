package com.jezetek.modules.system;

import com.jezetek.modules.system.service.NoticeService;
import com.jezetek.modules.system.service.UserService;
import com.jezetek.modules.system.service.dto.NoticeInput;
import com.jezetek.modules.system.service.dto.UserInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 工单09：首页轻量版。summary 计数与库中一致、公告启用过滤、
 * 无权限点要求(demo 可访问)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HomeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private NoticeService noticeService;

    @BeforeEach
    void login() {
        TestLogin.loginAs(TestLogin.ADMIN);
    }

    private long createNotice(String title, boolean enabled, String content) {
        NoticeInput input = new NoticeInput();
        input.setNoticeTitle(title);
        input.setNoticeType("1");
        input.setEnabled(enabled);
        input.setContent(content);
        return noticeService.saveNotice(input).id();
    }

    @Test
    void summary计数与库中一致() throws Exception {
        // 登录日志(今天登录成功 1 条)
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/home/summary").with(TestLogin.asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userCount").value(3))
                .andExpect(jsonPath("$.roleCount").value(2))
                .andExpect(jsonPath("$.todayLoginSuccess").value(1))
                .andExpect(jsonPath("$.operLogTotal").value(0));
    }

    @Test
    void 公告仅返回启用项且详情含内容() throws Exception {
        long enabledId = createNotice("启用公告A", true, "<p>正文<b>A</b></p><script>alert(1)</script>");
        createNotice("停用公告B", false, "<p>B</p>");
        long enabledId2 = createNotice("启用公告C", true, "<p>C</p>");

        // 列表：只有 2 条启用公告，新的在前
        mockMvc.perform(get("/api/v1/home/notices").with(TestLogin.asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].noticeTitle").value("启用公告C"))
                .andExpect(jsonPath("$[1].noticeTitle").value("启用公告A"));

        // 详情：启用可见(含 content)，停用 400
        mockMvc.perform(get("/api/v1/home/notices/" + enabledId).with(TestLogin.asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noticeTitle").value("启用公告A"))
                .andExpect(jsonPath("$.content").value("<p>正文<b>A</b></p><script>alert(1)</script>"));
        mockMvc.perform(get("/api/v1/home/notices/" + enabledId2).with(TestLogin.asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("<p>C</p>"));
        mockMvc.perform(get("/api/v1/home/notices/" + (enabledId + 1)).with(TestLogin.asAdmin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void demo无公告权限也可访问首页接口() throws Exception {
        // 种子公告不存在时列表为空但不报错
        mockMvc.perform(get("/api/v1/home/notices").with(TestLogin.processor(TestLogin.DEMO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/v1/home/summary").with(TestLogin.processor(TestLogin.DEMO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userCount").value(3));
    }
}
