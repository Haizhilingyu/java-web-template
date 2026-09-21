package com.jezetek.modules.system;

import cn.idev.excel.FastExcel;
import com.jezetek.modules.system.excel.LogininforExportRow;
import com.jezetek.modules.system.excel.OperLogExportRow;
import com.jezetek.modules.system.service.UserService;
import com.jezetek.modules.system.service.dto.UserInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 工单04：操作日志/登录日志导出。用真实业务写操作与登录尝试
 * 造日志数据，回读 Excel 断言行数与内容；demo 无导出权限点 403
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LogExportTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @BeforeEach
    void login() {
        TestLogin.loginAs(TestLogin.ADMIN);
    }

    @Test
    void 操作日志导出行数与筛选一致() throws Exception {
        // 触发两条操作日志(@Log 切面同步落库)
        UserInput input = new UserInput();
        input.setUsername("log_export_user");
        input.setPassword("123456");
        input.setEnabled(true);
        userService.saveUser(input);

        byte[] all = mockMvc.perform(get("/api/v1/operlog/export").with(TestLogin.asAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        List<OperLogExportRow> rows = readOperRows(all);
        assertEquals(1, rows.size());
        assertEquals("用户管理", rows.get(0).getModule());
        assertEquals("保存用户", rows.get(0).getAction());
        assertEquals("admin", rows.get(0).getOperator());
        assertEquals("成功", rows.get(0).getStatus());

        // keyword 过滤：无匹配关键字导出为空
        byte[] none = mockMvc.perform(get("/api/v1/operlog/export").with(TestLogin.asAdmin())
                        .param("keyword", "不存在的关键字"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertEquals(0, readOperRows(none).size());
    }

    @Test
    void 登录日志导出行数与内容一致() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk());

        byte[] all = mockMvc.perform(get("/api/v1/logininfor/export").with(TestLogin.asAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        List<LogininforExportRow> rows = readLogininforRows(all);
        assertEquals(1, rows.size());
        assertEquals("admin", rows.get(0).getUsername());
        assertEquals("登录成功", rows.get(0).getMessage());
        assertTrue(rows.get(0).getIp() != null && !rows.get(0).getIp().isBlank());
    }

    @Test
    void demo无导出权限调用403() throws Exception {
        mockMvc.perform(get("/api/v1/operlog/export").with(TestLogin.processor(TestLogin.DEMO)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/logininfor/export").with(TestLogin.processor(TestLogin.DEMO)))
                .andExpect(status().isForbidden());
    }

    private List<OperLogExportRow> readOperRows(byte[] bytes) {
        List<OperLogExportRow> rows = new ArrayList<>();
        FastExcel.read(new ByteArrayInputStream(bytes), OperLogExportRow.class,
                new cn.idev.excel.event.AnalysisEventListener<OperLogExportRow>() {
                    @Override
                    public void invoke(OperLogExportRow row, cn.idev.excel.context.AnalysisContext context) {
                        rows.add(row);
                    }

                    @Override
                    public void doAfterAllAnalysed(cn.idev.excel.context.AnalysisContext context) {
                        // 收集模式
                    }
                }).sheet().doRead();
        return rows;
    }

    private List<LogininforExportRow> readLogininforRows(byte[] bytes) {
        List<LogininforExportRow> rows = new ArrayList<>();
        FastExcel.read(new ByteArrayInputStream(bytes), LogininforExportRow.class,
                new cn.idev.excel.event.AnalysisEventListener<LogininforExportRow>() {
                    @Override
                    public void invoke(LogininforExportRow row, cn.idev.excel.context.AnalysisContext context) {
                        rows.add(row);
                    }

                    @Override
                    public void doAfterAllAnalysed(cn.idev.excel.context.AnalysisContext context) {
                        // 收集模式
                    }
                }).sheet().doRead();
        return rows;
    }
}
