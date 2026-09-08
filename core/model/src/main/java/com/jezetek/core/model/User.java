package com.jezetek.core.model;

import org.babyfish.jimmer.sql.*;
import com.jezetek.core.model.common.BaseEntity;
import com.jezetek.core.model.common.TenantAware;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Entity
// USER 是 SQL 保留字，表名加 sys_ 前规避
@Table(name = "sys_user")
@KeyUniqueConstraint(
        // Only for mysql
        noMoreUniqueConstraints = true,
        // Only for postgres
        isNullNotDistinct = true
)
public interface User extends BaseEntity, TenantAware {

    /**
     * 代理主键，自增，无业务含义
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    /**
     * 登录用户名，全局唯一
     */
    @Key
    String username();

    /**
     * 登录密码(加密后的密文)。
     *
     * <p>可为空：预留给验证码/SSO 等免密登录的账号</p>
     */
    @Nullable
    String password();

    /**
     * 显示昵称，缺省时可用用户名代替
     */
    @Nullable
    String nickname();

    /**
     * 是否启用，禁用账号不能登录
     */
    boolean enabled();

    /**
     * 当前用户拥有的所有角色，多对多关联
     */
    @ManyToMany(orderedProps = @OrderedProp("code"))
    @JoinTable(
            name = "sys_user_role_mapping",
            joinColumnName = "USER_ID",
            inverseJoinColumnName = "ROLE_ID"
    )
    List<Role> roles();

    /**
     * 关联属性 roles 的 id 视图，保存/加载时可以只处理 id 不触碰关联对象
     */
    @IdView("roles")
    List<Long> roleIds();
}
