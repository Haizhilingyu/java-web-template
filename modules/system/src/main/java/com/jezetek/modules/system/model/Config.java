package com.jezetek.modules.system.model;

import org.babyfish.jimmer.sql.*;
import com.jezetek.core.model.common.BaseEntity;
import com.jezetek.core.model.common.TenantAware;
import org.jetbrains.annotations.Nullable;

@Entity
@Table(name = "sys_config")
@KeyUniqueConstraint(
        // Only for mysql
        noMoreUniqueConstraints = true,
        // Only for postgres
        isNullNotDistinct = true
)
public interface Config extends BaseEntity, TenantAware {

    /**
     * 代理主键，自增，无业务含义
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    /**
     * 参数键(如 site.title)，全局唯一(business_key 约束不含租户列)，
     * 按 GET /api/v1/config/configKey/{key} 消费
     */
    @Key
    String configKey();

    /**
     * 参数名称
     */
    String configName();

    /**
     * 参数值
     */
    @Nullable
    String configValue();

    /**
     * 备注
     */
    @Nullable
    String remark();
}
