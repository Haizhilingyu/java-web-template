package com.jezetek.core.runtime.security;

import org.springframework.stereotype.Component;

/**
 * 方法级权限校验的 SpEL 入口，用法：
 * {@code @PreAuthorize("@perm.has('system:user:list')")}
 *
 * <p>支持超级管理员直通({@code *:*:*})，因此 ADMIN 角色无需绑定任何按钮菜单</p>
 */
@Component("perm")
public class PermissionChecker {

    public boolean has(String perm) {
        LoginUser loginUser = SecurityUtils.currentLoginUser();
        if (loginUser == null) {
            return false;
        }
        return loginUser.isSuperAdmin() || loginUser.perms().contains(perm);
    }

    public boolean hasAny(String... perms) {
        LoginUser loginUser = SecurityUtils.currentLoginUser();
        if (loginUser == null) {
            return false;
        }
        if (loginUser.isSuperAdmin()) {
            return true;
        }
        for (String perm : perms) {
            if (loginUser.perms().contains(perm)) {
                return true;
            }
        }
        return false;
    }
}
