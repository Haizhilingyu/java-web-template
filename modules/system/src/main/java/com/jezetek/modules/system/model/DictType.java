package com.jezetek.modules.system.model;

import org.babyfish.jimmer.sql.*;
import com.jezetek.core.model.common.BaseEntity;
import com.jezetek.core.model.common.TenantAware;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Entity
@Table(name = "sys_dict_type")
@KeyUniqueConstraint(
        // Only for mysql
        noMoreUniqueConstraints = true,
        // Only for postgres
        isNullNotDistinct = true
)
public interface DictType extends BaseEntity, TenantAware {

    /**
     * 代理主键，自增，无业务含义
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    /**
     * 字典编码(如 sys_yes_no)，全局唯一(business_key 约束不含租户列)，
     * 前端 useDict 按该编码取字典
     */
    @Key
    String type();

    /**
     * 字典名称
     */
    String name();

    /**
     * 描述
     */
    @Nullable
    String description();

    /**
     * 是否启用，禁用字典的条目不再下发(type 接口过滤)
     */
    boolean enabled();

    /**
     * 字典下的全部条目，映射关系由 dictType 定义
     */
    @OneToMany(mappedBy = "dictType", orderedProps = @OrderedProp("sortOrder"))
    List<DictData> data();
}
