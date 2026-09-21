package com.jezetek.modules.system.log;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 操作日志异步落库执行器。
 *
 * <p>默认单线程后台执行；test profile 用同步执行器(Runnable::run)，
 * 保证 OperLogTest 断言时日志已落库</p>
 */
@Configuration
public class OperLogConfig {

    @Bean(name = "operLogExecutor")
    @Profile("!test")
    public Executor asyncOperLogExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "oper-log");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Bean(name = "operLogExecutor")
    @Profile("test")
    public Executor syncOperLogExecutor() {
        return Runnable::run;
    }
}
