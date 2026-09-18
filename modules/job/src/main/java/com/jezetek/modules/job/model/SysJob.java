package com.jezetek.modules.job.model;

import com.jezetek.core.model.common.BaseEntity;
import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

@Entity
@Table(name = "sys_job")
@KeyUniqueConstraint(
        // Only for mysql
        noMoreUniqueConstraints = true,
        // Only for postgres
        isNullNotDistinct = true
)
public interface SysJob extends BaseEntity {

    /**
     * 代理主键，自增，无业务含义
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    /**
     * 任务名，唯一
     */
    @Key
    String name();

    /**
     * cron 表达式(6 位，spring CronTrigger 语法)
     */
    String cron();

    /**
     * 处理器 bean 名，对应实现了 JobHandler 接口的 Spring Bean
     */
    String handler();

    /**
     * 传给处理器的参数
     */
    @Nullable
    String param();

    /**
     * 状态：0 正常(调度中) / 1 暂停
     */
    int status();

    /**
     * 备注
     */
    @Nullable
    String memo();
}
