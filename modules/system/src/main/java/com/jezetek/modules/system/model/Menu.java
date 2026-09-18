package com.jezetek.modules.system.model;

import org.babyfish.jimmer.sql.*;
import com.jezetek.core.model.common.BaseEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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
     * 菜单名，与 parent 组成组合键，同一父节点下不允许重名
     */
    @Key
    String name();

    /**
     * 菜单类型：M 目录 / C 菜单(页面) / F 按钮
     */
    String type();

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
     * 前端路由地址。目录为一级路由(如 /system)，菜单为完整路由(如 /system/user)，按钮为空
     */
    @Nullable
    String path();

    /**
     * 前端页面组件路径(如 /system/user/index)，
     * 与 pages 目录下的 .vue 文件对应；目录/按钮为空
     */
    @Nullable
    String component();

    /**
     * 权限标识(如 system:user:add)，按钮节点必填，
     * 菜单/目录节点可标接口级权限；登录后下发为 perms 集合
     */
    @Nullable
    String perms();

    /**
     * 菜单图标名，与前端图标库对应
     */
    @Nullable
    String icon();

    /**
     * 是否在导航中显示(隐藏路由仍可直接访问，用于详情页等)
     */
    boolean visible();

    /**
     * 同级排序号，越小越靠前
     */
    int sortOrder();

    /**
     * 来源模块编码，由模块菜单同步器写入；
     * 手工创建的菜单为 null
     */
    @Nullable
    String moduleCode();

    /**
     * 当前菜单的子菜单，按 sortOrder 升序，
     * 映射关系由 parent 定义
     */
    @OneToMany(mappedBy = "parent", orderedProps = @OrderedProp("sortOrder"))
    List<Menu> children();

    /**
     * 关联属性 parent 的 id 视图，路由组装时只需父 id 不触碰关联对象
     */
    @Nullable
    @IdView("parent")
    Long parentId();

    /**
     * 可以访问当前菜单的所有角色，多对多关联的反向端，
     * 映射关系由 Role.menus 定义
     */
    @ManyToMany(mappedBy = "menus")
    List<Role> roles();
}
