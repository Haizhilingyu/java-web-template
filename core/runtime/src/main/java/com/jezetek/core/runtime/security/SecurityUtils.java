package com.jezetek.core.runtime.security;

import org.jetbrains.annotations.Nullable;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * 读取当前登录上下文的静态入口。
 *
 * <p>未认证(定时任务线程/匿名请求)返回 empty，
 * 业务代码不要假设一定有登录态</p>
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<LoginUser> getLoginUser() {
        return Optional.ofNullable(currentLoginUser());
    }

    @Nullable
    public static LoginUser currentLoginUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }
}
