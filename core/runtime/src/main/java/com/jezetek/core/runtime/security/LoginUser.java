package com.jezetek.core.runtime.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 登录用户上下文对象，作为 Authentication 的 principal 存入 SecurityContext。
 *
 * <p>perms 为按钮/接口级权限标识(菜单上的 perms 字段)集合；
 * 超级管理员角色被赋值为 {"@code *:*:*"}，在 {@link PermissionChecker} 中直通</p>
 */
public record LoginUser(
        long id,
        String username,
        String nickname,
        Set<String> roles,
        Set<Long> roleIds,
        Set<String> perms
) {

    /** 权限标识是否直通(超级管理员) */
    public static final String ALL_PERMS = "*:*:*";

    public boolean isSuperAdmin() {
        return perms.contains(ALL_PERMS);
    }

    /**
     * 接口级权限用 hasAuthority(perm) 校验，
     * 角色用 hasRole(code) 校验，因此两类都放进 authorities
     */
    public List<GrantedAuthority> authorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String perm : perms) {
            authorities.add(new SimpleGrantedAuthority(perm));
        }
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return authorities;
    }
}
