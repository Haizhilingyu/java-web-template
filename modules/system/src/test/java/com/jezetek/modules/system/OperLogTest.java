package com.jezetek.modules.system;

import com.jezetek.core.runtime.BusinessException;
import com.jezetek.modules.system.model.OperLog;
import com.jezetek.modules.system.service.DeptService;
import com.jezetek.modules.system.service.OperLogService;
import com.jezetek.modules.system.service.UserService;
import com.jezetek.modules.system.service.dto.UserInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 操作日志切面：测试 profile 的 operLogExecutor 是同步执行器，
 * 方法返回即已落库，断言确定
 */
@SpringBootTest
@Transactional
class OperLogTest {

    @Autowired
    private UserService userService;

    @Autowired
    private DeptService deptService;

    @Autowired
    private OperLogService operLogService;

    @BeforeEach
    void login() {
        TestLogin.loginAs(TestLogin.ADMIN);
    }

    private Page<OperLog> allLogs() {
        return operLogService.findOperLogsBySuperQBE(0, 50, "id desc", null);
    }

    @Test
    void 写操作落库且字段齐全() {
        UserInput input = new UserInput();
        input.setUsername("oper_log_user");
        input.setPassword("123456");
        input.setEnabled(true);
        userService.saveUser(input);

        Page<OperLog> page = allLogs();
        assertEquals(1, page.getTotalElements());

        OperLog log = page.getContent().get(0);
        assertEquals("用户管理", log.module());
        assertEquals("保存用户", log.action());
        assertEquals("admin", log.operator());
        // 入参 JSON 含业务数据
        assertTrue(log.params().contains("oper_log_user"), log.params());
        // 成功记录：结果非空、无异常、耗时非负
        assertTrue(log.success());
        assertNull(log.errorMsg());
        assertTrue(log.costMs() >= 0);
        assertNotNull(log.createdTime());
    }

    @Test
    void 抛异常的操作也落库并记录异常信息() {
        // 总公司(1) 存在子部门 → 删除被拒
        assertThrows(BusinessException.class, () -> deptService.deleteDept(1L));

        Page<OperLog> page = allLogs();
        assertEquals(1, page.getTotalElements());
        OperLog log = page.getContent().get(0);
        assertEquals("部门管理", log.module());
        assertEquals("删除部门", log.action());
        assertFalse(log.success());
        assertTrue(log.errorMsg().contains("子部门"), log.errorMsg());
        assertNull(log.result());
    }

    @Test
    void 按关键字过滤() {
        UserInput input = new UserInput();
        input.setUsername("oper_log_user");
        input.setPassword("123456");
        input.setEnabled(true);
        userService.saveUser(input);

        Page<OperLog> hit = operLogService.findOperLogsBySuperQBE(0, 10, "id desc", "用户管理");
        assertEquals(1, hit.getTotalElements());

        Page<OperLog> miss = operLogService.findOperLogsBySuperQBE(0, 10, "id desc", "角色管理");
        assertEquals(0, miss.getTotalElements());
    }

    @Test
    void 无权限清空被拒() {
        TestLogin.loginAs(TestLogin.DEMO);
        assertThrows(org.springframework.security.authorization.AuthorizationDeniedException.class,
                () -> operLogService.clear());
    }
}
