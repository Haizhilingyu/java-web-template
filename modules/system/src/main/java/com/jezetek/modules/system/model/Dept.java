package com.jezetek.modules.system.model;

import org.babyfish.jimmer.sql.*;
import com.jezetek.core.model.common.BaseEntity;
import com.jezetek.core.model.common.TenantAware;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Entity
@Table(name = "sys_dept")
@KeyUniqueConstraint(
        // Only for mysql
        noMoreUniqueConstraints = true,
        // Only for postgres
        isNullNotDistinct = true
)
public interface Dept extends BaseEntity, TenantAware {

    /**
     * 代理主键，自增，无业务含义
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    /**
     * 部门名，与 parent 组成组合键，同一父节点下不允许重名
     */
    @Key
    String name();

    /**
     * 上级部门，根部门为 null，与 name 组成唯一约束。
     *
     * <p>删除受业务约束：存在子部门或部门下有用户时禁止删除，
     * 因此关联不会发生解除动作</p>
     */
    @Nullable
    @Key
    @ManyToOne
    Dept parent();

    /**
     * 同级排序号，越小越靠前
     */
    int sortOrder();

    /**
     * 是否启用，禁用部门仅在用户表单等选择器中过滤，
     * 不追溯影响已挂在部门下的用户
     */
    boolean enabled();

    /**
     * 负责人，自由文本，不引用用户表
     */
    @Nullable
    String leader();

    /**
     * 联系电话
     */
    @Nullable
    String phone();

    /**
     * 邮箱
     */
    @Nullable
    String email();

    /**
     * 当前部门的子部门，按 sortOrder 升序，
     * 映射关系由 parent 定义
     */
    @OneToMany(mappedBy = "parent", orderedProps = @OrderedProp("sortOrder"))
    List<Dept> children();

    /**
     * 关联属性 parent 的 id 视图，选择器组装时只需父 id 不触碰关联对象
     */
    @Nullable
    @IdView("parent")
    Long parentId();
}
