package com.jezetek.core.model;

import org.babyfish.jimmer.sql.*;
import com.jezetek.core.model.common.BaseEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 菜单树。
 *
 * <p>菜单是全局数据，不挂租户；树形结构参考 TreeNode：
 * name 与 parent 组成组合键，同一父节点下不允许重名</p>
 */
@Entity
@Table(name = "sys_menu")
@KeyUniqueConstraint(
        // Only for mysql
        noMoreUniqueConstraints = true,
        // Only for postgres
        isNullNotDistinct = true
)
public interface Menu extends BaseEntity {

    /**
     * 代理主键，自增，无业务含义
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    /**
     * 菜单名，与 parent 组成唯一约束
     */
    @Key
    String name();

    /**
     * 父菜单，根节点为 null，与 name 组成唯一约束。
     *
     * <p>删除父菜单时子菜单与断开关联而非级联删除</p>
     */
    @Nullable
    @Key
    @ManyToOne
    @OnDissociate(DissociateAction.SET_NULL)
    Menu parent();

    /**
     * 前端路由地址，目录类菜单可为空
     */
    @Nullable
    String path();

    /**
     * 菜单图标名，与前端图标库对应
     */
    @Nullable
    String icon();

    /**
     * 同级排序号，越小越靠前
     */
    int sortOrder();

    /**
     * 当前菜单的子菜单，按 sortOrder 升序，
     * 映射关系由 parent 定义
     */
    @OneToMany(mappedBy = "parent", orderedProps = @OrderedProp("sortOrder"))
    List<Menu> children();

    /**
     * 可以访问当前菜单的所有角色，多对多关联的反向端，
     * 映射关系由 Role.menus 定义
     */
    @ManyToMany(mappedBy = "menus")
    List<Role> roles();
}
