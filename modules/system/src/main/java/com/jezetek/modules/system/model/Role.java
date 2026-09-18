package com.jezetek.modules.system.model;

import org.babyfish.jimmer.sql.*;
import com.jezetek.core.model.common.BaseEntity;
import com.jezetek.core.model.common.TenantAware;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Entity
// ROLE 在部分数据库中是保留字，表名加 sys_ 前缀规避
@Table(name = "sys_role")
@KeyUniqueConstraint(
        // Only for mysql
        noMoreUniqueConstraints = true,
        // Only for postgres
        isNullNotDistinct = true
)
public interface Role extends BaseEntity, TenantAware {

    /**
     * 代理主键，自增，无业务含义
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    /**
     * 角色编码(如 ADMIN)，租户内唯一。
     * 编码 ADMIN 为内置超级管理员：菜单与接口权限全部直通
     */
    @Key
    String code();

    /**
     * 角色显示名
     */
    String name();

    /**
     * 角色描述
     */
    @Nullable
    String description();

    /**
     * 拥有当前角色的所有用户，多对多关联的反向端，
     * 映射关系由 User.roles 定义
     */
    @ManyToMany(mappedBy = "roles")
    List<User> users();

    /**
     * 当前角色可访问的所有菜单(含按钮)，多对多关联，
     * 接口/按钮权限取其中 perms 字段，路由取 M/C 类型节点
     */
    @ManyToMany(orderedProps = {
            @OrderedProp("sortOrder")
    })
    @JoinTable(
            name = "sys_role_menu_mapping",
            joinColumnName = "ROLE_ID",
            inverseJoinColumnName = "MENU_ID"
    )
    List<Menu> menus();

    /**
     * 关联属性 menus 的 id 视图
     */
    @IdView("menus")
    List<Long> menuIds();
}
