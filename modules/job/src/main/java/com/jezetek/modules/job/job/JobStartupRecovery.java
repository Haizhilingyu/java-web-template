package com.jezetek.modules.job.job;

import com.jezetek.modules.job.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 应用就绪后按数据库状态重建全部"调度中"任务——
 * sys_job 是任务定义的唯一事实来源，内存注册表随重启重建
 */
@Component
public class JobStartupRecovery {

    private static final Logger log = LoggerFactory.getLogger(JobStartupRecovery.class);

    private final JobRepository jobRepository;

    private final JobScheduler jobScheduler;

    public JobStartupRecovery(JobRepository jobRepository, JobScheduler jobScheduler) {
        this.jobRepository = jobRepository;
        this.jobScheduler = jobScheduler;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recover() {
        var running = jobRepository.findByStatus(JobScheduler.STATUS_RUNNING);
        for (var job : running) {
            jobScheduler.register(job);
        }
        log.info("定时任务恢复完成，共 {} 个调度中任务", running.size());
    }
}
