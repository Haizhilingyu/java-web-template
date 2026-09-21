package com.jezetek.modules.system.model;

import org.babyfish.jimmer.sql.*;
import com.jezetek.core.model.common.BaseEntity;
import com.jezetek.core.model.common.TenantAware;
import org.jetbrains.annotations.Nullable;

@Entity
@Table(name = "sys_logininfor")
public interface Logininfor extends BaseEntity, TenantAware {

    /**
     * 代理主键，自增，无业务含义
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    /**
     * 登录账号(尝试登录的账号，无论成败)
     */
    String username();

    /**
     * 客户端 IP(X-Forwarded-For 首值或 remoteAddr)
     */
    String ip();

    /**
     * 结果消息(登录成功/登录失败：xxx/登出)
     */
    @Nullable
    String message();
}
