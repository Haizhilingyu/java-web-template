package com.jezetek.modules.system;

import com.jezetek.core.runtime.security.LoginUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Set;

/**
 * 测试登录辅助：
 * <ul>
 *   <li>直接调用 service 方法前，用 {@link #loginAs} 造好 SecurityContext</li>
 *   <li>MockMvc 请求经真实安全过滤链，须用 {@link #asAdmin()} 之类的
 *       RequestPostProcessor 注入 Authentication</li>
 * </ul>
 */
public final class TestLogin {

    public static final LoginUser ADMIN = new LoginUser(
            1L, "admin", "Administrator",
            Set.of("ADMIN"), Set.of(1L), Set.of(LoginUser.ALL_PERMS),
            com.jezetek.core.runtime.security.DataScope.ALL
    );

    /** 与种子数据的 demo 用户一致：USER 角色，权限为空集合 */
    public static final LoginUser DEMO = new LoginUser(
            2L, "demo", "演示用户",
            Set.of("USER"), Set.of(2L), Set.of(),
            com.jezetek.core.runtime.security.DataScope.ALL
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
