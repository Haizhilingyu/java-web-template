package com.jezetek.modules.job;

import com.jezetek.modules.job.job.JobScheduler;
import com.jezetek.modules.job.model.SysJob;
import com.jezetek.modules.job.model.SysJobDraft;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JobSchedulerTest {

    @Autowired
    private JobScheduler jobScheduler;

    @Autowired
    private ThreadPoolTaskScheduler jobTaskScheduler;

    @Test
    void cron表达式校验() {
        assertDoesNotThrow(() -> JobScheduler.validateCron("0/10 * * * * ?"));
        assertThrows(RuntimeException.class, () -> JobScheduler.validateCron("not-a-cron"));
    }

    @Test
    void 注册与摘除调度计划() {
        SysJob job = job("cron-0/2-test", "0/2 * * * * ?");
        jobScheduler.register(job);
        assertTrue(jobScheduler.isScheduled(job.id()));

        jobScheduler.unregister(job.id());
        assertFalse(jobScheduler.isScheduled(job.id()));
    }

    @Test
    void 立即执行分发到指定处理器() throws Exception {
        SysJob job = job("run-once-test", "0 0 0 * * ?");
        // sampleLogJob 已注册为处理器，同步执行一次不应抛异常
        assertDoesNotThrow(() -> jobScheduler.runOnce(job));

        SysJob badHandler = SysJobDraft.$.produce(job, draft -> draft.setHandler("no_such_handler"));
        assertThrows(IllegalArgumentException.class, () -> jobScheduler.runOnce(badHandler));
    }

    /**
     * 构造完整加载的任务对象：调度器约定入参包含 id/cron/param/handler
     * (生产路径由 JobService 的 allScalarFields fetcher 保证)，
     * 缺属性会抛 UnloadedException
     */
    private static SysJob job(String name, String cron) {
        return SysJobDraft.$.produce(draft -> {
            draft.setId(101L);
            draft.setName(name);
            draft.setCron(cron);
            draft.setHandler("sampleLogJob");
            draft.setParam(null);
            draft.setStatus(JobScheduler.STATUS_PAUSED);
        });
    }
}
