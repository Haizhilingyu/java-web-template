package com.jezetek.modules.system.model;

import org.babyfish.jimmer.sql.*;
import com.jezetek.core.model.common.BaseEntity;
import com.jezetek.core.model.common.TenantAware;
import org.jetbrains.annotations.Nullable;

@Entity
@Table(name = "sys_oper_log")
public interface OperLog extends BaseEntity, TenantAware {

    /**
     * 代理主键，自增，无业务含义
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    /**
     * 业务模块名，来自 @Log(module)
     */
    String module();

    /**
     * 动作名，来自 @Log(action)
     */
    String action();

    /**
     * 操作人账号，匿名操作为 anonymous
     */
    String operator();

    /**
     * 请求 URI
     */
    @Nullable
    String uri();

    /**
     * 入参 JSON(超长截断)
     */
    @Nullable
    String params();

    /**
     * 返回结果 JSON(超长截断)，失败时为空
     */
    @Nullable
    String result();

    /**
     * 异常信息(截断)，成功时为空
     */
    @Nullable
    String errorMsg();

    /**
     * 耗时(毫秒)
     */
    long costMs();

    /**
     * 是否成功
     */
    boolean success();
}
