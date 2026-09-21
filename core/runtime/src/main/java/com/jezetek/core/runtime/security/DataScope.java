package com.jezetek.core.runtime.security;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

/**
 * 数据范围(见 CONTEXT.md 术语表)：限定会话可见的数据行所属部门。
 *
 * <p>五档语义(编码即 sys_role.data_scope)：
 * 1 全部 / 2 自定义(精确等于勾选部门集合，不含未勾选子孙) /
 * 3 本部门 / 4 本部门及以下(含全部子孙，子孙扩展由生效点负责) / 5 仅本人。
 * 多角色取范围最大(编码最小)；超级管理员直通。
 *
 * <p>非全局过滤器：由生效点(service 层)显式调用{@link #current()}并按
 * level 组装条件，避免误伤 username 查重等必须全量可见的查询</p>
 */
public record DataScope(Level level, Set<Long> deptIds, @Nullable Long selfDeptId) {

    public enum Level {
        ALL(1),
        CUSTOM(2),
        DEPT(3),
        DEPT_AND_CHILD(4),
        SELF(5);

        private final int code;

        Level(int code) {
            this.code = code;
        }

        /** sys_role.data_scope 列的编码 */
        public int code() {
            return code;
        }

        public static Level ofCode(int code) {
            for (Level level : values()) {
                if (level.code == code) {
                    return level;
                }
            }
            return ALL;
        }
    }

    /** 超级管理员/默认：不加任何过滤 */
    public static final DataScope ALL = new DataScope(Level.ALL, Set.of(), null);

    /**
     * 多角色合并取范围最大(编码最小)，deptIds 取该角色的集合。
     * 空集合按 ALL 处理
     */
    public static DataScope broadest(Collection<DataScope> scopes) {
        DataScope hit = null;
        for (DataScope scope : scopes) {
            if (hit == null || scope.level().code() < hit.level().code()) {
                hit = scope;
            }
        }
        return hit != null ? hit : ALL;
    }

    /**
     * 当前会话的数据范围规则；未登录或超级管理员返回 null(不加过滤)。
     * DEPT_AND_CHILD 的子孙扩展需要部门树，由生效点自行完成
     */
    @Nullable
    public static DataScope current() {
        LoginUser user = SecurityUtils.currentLoginUser();
        if (user == null || user.isSuperAdmin()) {
            return null;
        }
        return user.dataScope();
    }
}
