package com.jezetek.modules.job.job;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * job 模块装配：动态调度的线程池。
 * 应用就绪后的任务恢复见 {@link JobStartupRecovery}
 */
@Configuration
public class JobSchedulingConfig {

    @Bean
    public ThreadPoolTaskScheduler jobTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("job-scheduler-");
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }
}
