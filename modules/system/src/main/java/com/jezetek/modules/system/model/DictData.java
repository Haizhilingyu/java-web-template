package com.jezetek.modules.system.model;

import org.babyfish.jimmer.sql.*;
import com.jezetek.core.model.common.BaseEntity;
import com.jezetek.core.model.common.TenantAware;
import org.jetbrains.annotations.Nullable;

@Entity
@Table(name = "sys_dict_data")
@KeyUniqueConstraint(
        // Only for mysql
        noMoreUniqueConstraints = true,
        // Only for postgres
        isNullNotDistinct = true
)
public interface DictData extends BaseEntity, TenantAware {

    /**
     * 代理主键，自增，无业务含义
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    /**
     * 所属字典类型，与 value 组成组合键(同一字典内 value 唯一)。
     * 目标端有租户过滤器，jimmer 要求引用声明为 nullable，
     * 业务上的非空用 inputNotNull 表达
     */
    @Nullable
    @Key
    @ManyToOne(inputNotNull = true)
    DictType dictType();

    /**
     * 显示标签(如"是")
     */
    String label();

    /**
     * 存储值(如"Y")，与 dictType 组成业务键，前端渲染与提交都用它。
     * 列名避开 H2 保留字 VALUE
     */
    @Key
    @Column(name = "data_value")
    String value();

    /**
     * 同字典内排序号，越小越靠前
     */
    int sortOrder();

    /**
     * 是否启用，禁用条目不再下发且不在选择器中出现
     */
    boolean enabled();

    /**
     * 关联属性 dictType 的 id 视图
     */
    @Nullable
    @IdView("dictType")
    Long dictTypeId();
}
