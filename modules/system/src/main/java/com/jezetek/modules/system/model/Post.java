package com.jezetek.modules.system.model;

import org.babyfish.jimmer.sql.*;
import com.jezetek.core.model.common.BaseEntity;
import com.jezetek.core.model.common.TenantAware;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Entity
@Table(name = "sys_post")
@KeyUniqueConstraint(
        // Only for mysql
        noMoreUniqueConstraints = true,
        // Only for postgres
        isNullNotDistinct = true
)
public interface Post extends BaseEntity, TenantAware {

    /**
     * 代理主键，自增，无业务含义
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    /**
     * 岗位编码(如 CEO)，编码全局唯一(business_key 约束不含租户列)
     */
    @Key
    String code();

    /**
     * 岗位名称
     */
    String name();

    /**
     * 排序号，越小越靠前
     */
    int sortOrder();

    /**
     * 是否启用，禁用岗位仅在用户表单等选择器中过滤，
     * 不追溯影响已担任该岗位的用户
     */
    boolean enabled();

    /**
     * 担任当前岗位的所有用户，多对多关联的反向端，
     * 映射关系由 User.posts 定义
     */
    @ManyToMany(mappedBy = "posts")
    List<User> users();
}
