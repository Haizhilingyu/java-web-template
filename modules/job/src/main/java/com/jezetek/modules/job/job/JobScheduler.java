package com.jezetek.modules.job.job;

import com.jezetek.modules.job.model.SysJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 基于 Spring TaskScheduler 的轻量动态调度引擎(对比若依的 Quartz：
 * 无额外依赖，重启后由数据库状态恢复，能力上够覆盖增删改/启停/立即执行)。
 *
 * <p>内存注册表只保存 ScheduledFuture，任务定义以 sys_job 为准；
 * 应用启动后由 JobInitializer 重建全部 status=0 的任务</p>
 */
@Component
public class JobScheduler {

    private static final Logger log = LoggerFactory.getLogger(JobScheduler.class);

    /** 任务状态：调度中 */
    public static final int STATUS_RUNNING = 0;

    /** 任务状态：暂停 */
    public static final int STATUS_PAUSED = 1;

    private final ThreadPoolTaskScheduler taskScheduler;

    private final Map<String, JobHandler> handlers;

    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public JobScheduler(
            ThreadPoolTaskScheduler taskScheduler,
            List<JobHandler> jobHandlers
    ) {
        this.taskScheduler = taskScheduler;
        this.handlers = jobHandlers.stream()
                .collect(Collectors.toUnmodifiableMap(JobHandler::name, Function.identity()));
    }

    /** 注册/重排(幂等)：先摘除旧计划再按当前 cron 挂新计划 */
    public void register(SysJob job) {
        unregister(job.id());
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> executeQuietly(job),
                new CronTrigger(job.cron())
        );
        scheduledTasks.put(job.id(), future);
        log.info("任务[{}:{}]已注册调度 cron={}", job.id(), job.name(), job.cron());
    }

    public void unregister(long jobId) {
        ScheduledFuture<?> future = scheduledTasks.remove(jobId);
        if (future != null) {
            future.cancel(false);
            log.info("任务[{}]已摘除调度", jobId);
        }
    }

    public boolean isScheduled(long jobId) {
        return scheduledTasks.containsKey(jobId);
    }

    /** 立即同步执行一次(不走调度计划)，供界面"立即执行"与测试调用 */
    public void runOnce(SysJob job) throws Exception {
        JobHandler handler = resolveHandler(job);
        handler.execute(job.param());
    }

    /** 校验 cron 表达式合法性(保存任务前调用) */
    public static void validateCron(String cron) {
        new CronTrigger(cron);
    }

    private void executeQuietly(SysJob job) {
        try {
            resolveHandler(job).execute(job.param());
        } catch (Exception e) {
            log.error("任务[{}:{}]执行失败", job.id(), job.name(), e);
        }
    }

    private JobHandler resolveHandler(SysJob job) {
        JobHandler handler = handlers.get(job.handler());
        if (handler == null) {
            throw new IllegalArgumentException("找不到任务处理器: " + job.handler()
                    + "，可用处理器: " + handlers.keySet());
        }
        return handler;
    }
}
