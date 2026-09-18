package com.jezetek.modules.job.job;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 演示用处理器：打印一行日志，供界面"立即执行"验证调度链路
 */
@Component
public class SampleLogJobHandler implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(SampleLogJobHandler.class);

    @Override
    public String name() {
        return "sampleLogJob";
    }

    @Override
    public void execute(@Nullable String param) {
        log.info("[sampleLogJob] 定时任务执行成功 param={}, at={}", param, LocalDateTime.now());
    }
}
