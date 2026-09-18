package com.jezetek.modules.job;

import com.jezetek.modules.job.job.JobScheduler;
import com.jezetek.modules.job.model.SysJob;
import com.jezetek.modules.job.service.JobService;
import com.jezetek.modules.job.service.dto.SysJobInput;
import com.jezetek.modules.job.service.dto.SysJobSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static com.jezetek.modules.job.job.JobScheduler.STATUS_PAUSED;
import static com.jezetek.modules.job.job.JobScheduler.STATUS_RUNNING;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JobServiceTest {

    @Autowired
    private JobService jobService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void login() {
        TestLogin.loginAs(TestLogin.ADMIN);
    }

    @Test
    void 分页查询返回种子任务() {
        Page<SysJob> page = jobService.findJobsBySuperQBE(0, 5, "id asc", new SysJobSpecification());

        assertEquals(1, page.getTotalElements());
        assertEquals("示例日志任务", page.getContent().get(0).name());
        assertEquals(STATUS_PAUSED, page.getContent().get(0).status());
    }

    @Test
    void 新增任务自动填充时间戳() {
        SysJobInput input = new SysJobInput();
        input.setName("测试任务");
        input.setCron("0/5 * * * * ?");
        input.setHandler("sampleLogJob");
        input.setParam("p1");
        input.setStatus(STATUS_PAUSED);
        input.setMemo("单元测试");

        SysJob saved = jobService.saveJob(input);

        assertTrue(saved.id() > 0);
        assertNotNull(saved.createdTime());
        assertEquals("测试任务", saved.name());
    }

    @Test
    void cron非法保存前即报错() {
        SysJobInput input = new SysJobInput();
        input.setName("坏任务");
        input.setCron("not-a-cron");
        input.setHandler("sampleLogJob");
        input.setStatus(STATUS_RUNNING);

        assertThrows(RuntimeException.class, () -> jobService.saveJob(input));
    }

    @Test
    void 启停切换状态() {
        SysJob saved = jobService.changeStatus(1L, STATUS_RUNNING);
        assertEquals(STATUS_RUNNING, saved.status());

        saved = jobService.changeStatus(1L, STATUS_PAUSED);
        assertEquals(STATUS_PAUSED, saved.status());

        assertThrows(IllegalArgumentException.class, () -> jobService.changeStatus(1L, 9));
    }

    @Test
    void 立即执行示例任务() {
        // 同步执行成功即通过(处理器只打印日志)，任务不存在时报错
        assertDoesNotThrow(() -> jobService.run(1L));
        assertThrows(RuntimeException.class, () -> jobService.run(999L));
    }

    @Test
    void 删除任务() {
        jobService.deleteJob(1L);
        assertNull(jobService.findJob(1L));
    }

    @Test
    void 未携带令牌访问任务接口返回401() throws Exception {
        mockMvc.perform(get("/api/v1/job/list/bySuperQBE"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 仅查询权限的用户删除任务被拒绝() {
        TestLogin.loginAs(TestLogin.VIEWER);
        assertThrows(Exception.class, () -> jobService.deleteJob(1L));
        TestLogin.clear();
    }
}
