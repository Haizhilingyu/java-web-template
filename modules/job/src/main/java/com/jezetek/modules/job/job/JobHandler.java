package com.jezetek.modules.job.job;

import org.jetbrains.annotations.Nullable;

/**
 * 定时任务处理器 SPI：业务方实现本接口并注册为 Spring Bean，
 * {@code name()} 即任务数据(sys_job.handler)引用的处理器名。
 *
 * <p>执行线程是调度线程池，没有登录上下文，
 * 处理器内部不要依赖 SecurityUtils 等请求态设施</p>
 */
public interface JobHandler {

    /** 处理器唯一名，任务数据通过它引用处理器 */
    String name();

    /**
     * @param param 任务上配置的参数，可为空
     */
    void execute(@Nullable String param) throws Exception;
}
