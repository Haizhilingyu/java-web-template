package com.jezetek.modules.system;

import cn.idev.excel.FastExcel;
import com.jezetek.modules.system.excel.UserExportRow;
import com.jezetek.modules.system.excel.UserImportRow;
import com.jezetek.modules.system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 工单03：用户 Excel 导入导出。导出回读断言行数/筛选一致，
 * 导入覆盖成功行入库 + 失败行回显行号与原因
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserExcelTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void login() {
        TestLogin.loginAs(TestLogin.ADMIN);
    }

    private List<UserExportRow> readExportRows(byte[] bytes) {
        List<UserExportRow> rows = new ArrayList<>();
        FastExcel.read(new ByteArrayInputStream(bytes), UserExportRow.class,
                new cn.idev.excel.event.AnalysisEventListener<UserExportRow>() {
                    @Override
                    public void invoke(UserExportRow row, cn.idev.excel.context.AnalysisContext context) {
                        rows.add(row);
                    }

                    @Override
                    public void doAfterAllAnalysed(cn.idev.excel.context.AnalysisContext context) {
                        // 收集模式
                    }
                }).sheet().doRead();
        return rows;
    }

    @Test
    void 按筛选条件全量导出且表头中文() throws Exception {
        // 全量：种子 3 用户
        byte[] all = mockMvc.perform(get("/api/v1/user/export").with(TestLogin.asAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        List<UserExportRow> rows = readExportRows(all);
        assertEquals(3, rows.size());
        assertEquals("admin", rows.get(0).getUsername());
        assertEquals("总公司", rows.get(0).getDept());
        assertEquals("启用", rows.get(0).getStatus());
        assertTrue(rows.get(0).getRoles().contains("管理员"));

        // 筛选 status=禁用：仅 frozen
        byte[] disabled = mockMvc.perform(get("/api/v1/user/export").with(TestLogin.asAdmin())
                        .param("enabled", "false"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        List<UserExportRow> disabledRows = readExportRows(disabled);
        assertEquals(1, disabledRows.size());
        assertEquals("frozen", disabledRows.get(0).getUsername());
    }

    @Test
    void 导入模板下载非空() throws Exception {
        byte[] bytes = mockMvc.perform(get("/api/v1/user/import-template").with(TestLogin.asAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertTrue(bytes.length > 1000, "xlsx 模板应非空");
    }

    private byte[] buildImportFile(UserImportRow... rows) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        FastExcel.write(out, UserImportRow.class).inMemory(true).sheet("用户数据").doWrite(List.of(rows));
        return out.toByteArray();
    }

    @Test
    void 导入合法行入库_重复与非法行回显行号原因() throws Exception {
        UserImportRow ok = new UserImportRow();
        ok.setUsername("import_ok");
        ok.setNickname("导入成功");
        ok.setPassword("123456");
        ok.setDeptId(2L);
        ok.setStatus("启用");

        UserImportRow duplicate = new UserImportRow();
        duplicate.setUsername("admin");
        duplicate.setStatus("启用");

        UserImportRow invalid = new UserImportRow();
        invalid.setUsername("x");
        invalid.setStatus("未知状态");

        MockMultipartFile file = new MockMultipartFile(
                "file", "users.xlsx", MediaType.APPLICATION_OCTET_STREAM_VALUE,
                buildImportFile(ok, duplicate, invalid));

        mockMvc.perform(multipart("/api/v1/user/import").file(file).with(TestLogin.asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.successCount").value(1))
                .andExpect(jsonPath("$.failures.length()").value(2))
                .andExpect(jsonPath("$.failures[0].rowNum").value(3))
                .andExpect(jsonPath("$.failures[0].reason").value("用户名已存在"))
                .andExpect(jsonPath("$.failures[1].rowNum").value(4))
                .andExpect(jsonPath("$.failures[1].reason").value("用户名须为3~50位字母、数字或下划线"));

        // 成功行真实入库，密码已加密，部门已挂载
        com.jezetek.modules.system.model.User saved =
                userRepository.findByUsername("import_ok", null).orElseThrow();
        assertEquals("导入成功", saved.nickname());
        assertTrue(saved.password().startsWith("$2"));
        assertEquals(2L, saved.deptId());
    }

    @Test
    void 无导出导入权限的demo调用403() throws Exception {
        mockMvc.perform(get("/api/v1/user/export").with(TestLogin.processor(TestLogin.DEMO)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/user/import-template").with(TestLogin.processor(TestLogin.DEMO)))
                .andExpect(status().isForbidden());
        mockMvc.perform(multipart("/api/v1/user/import").with(TestLogin.processor(TestLogin.DEMO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void 导入缺文件返回400() throws Exception {
        mockMvc.perform(multipart("/api/v1/user/import").with(TestLogin.asAdmin()))
                .andExpect(status().isBadRequest());
        assertNotNull(userRepository);
    }
}
