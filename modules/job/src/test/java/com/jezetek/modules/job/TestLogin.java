package com.jezetek.modules.job;

import com.jezetek.core.runtime.security.LoginUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Set;

/**
 * 测试登录辅助(job 模块测试类路径上没有 system 模块，
 * 直接手工构造 LoginUser，不依赖真实的用户体系)
 */
public final class TestLogin {

    public static final LoginUser ADMIN = new LoginUser(
            1L, "admin", "Administrator",
            Set.of("ADMIN"), Set.of(1L), Set.of(LoginUser.ALL_PERMS)
    );

    /** 只有 job:list 权限的普通用户，用于验证按钮级权限 */
    public static final LoginUser VIEWER = new LoginUser(
            2L, "viewer", "访客",
            Set.of("USER"), Set.of(2L), Set.of("job:list")
    );

    private TestLogin() {
    }

    public static void loginAs(LoginUser user) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.authorities()));
    }

    public static void clear() {
        SecurityContextHolder.clearContext();
    }

    public static RequestPostProcessor processor(LoginUser user) {
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(user, null, user.authorities()));
    }

    public static RequestPostProcessor asAdmin() {
        return processor(ADMIN);
    }
}
