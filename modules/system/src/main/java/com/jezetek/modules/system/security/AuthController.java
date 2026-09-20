package com.jezetek.modules.system.security;

import com.jezetek.core.runtime.security.LoginUser;
import com.jezetek.core.runtime.security.SecurityUtils;
import com.jezetek.core.runtime.security.SessionRegistry;
import com.jezetek.core.runtime.security.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 登录/登出/用户信息/动态路由。除 login 外均要求携带有效 JWT
 * (由 core 的 SecurityConfig 对 /auth/** 强制)。
 * 登录失败由 {@link AuthExceptionHandler} 统一转 401 JSON
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;

    private final TokenService tokenService;

    private final SessionRegistry sessionRegistry;

    private final MenuRouteService menuRouteService;

    public AuthController(
            AuthenticationManager authenticationManager,
            TokenService tokenService,
            SessionRegistry sessionRegistry,
            MenuRouteService menuRouteService
    ) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.sessionRegistry = sessionRegistry;
        this.menuRouteService = menuRouteService;
    }

    /** 认证成功签发 JWT 并登记会话(ADR-0001) */
    @PostMapping("/login")
    public AuthModels.LoginResult login(
            @Valid @RequestBody AuthModels.LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        LoginUserDetails details = (LoginUserDetails) authentication.getPrincipal();
        LoginUser loginUser = details.loginUser();
        String token = tokenService.create(
                details.userId(),
                loginUser.username(),
                loginUser.nickname(),
                clientIp(httpRequest)
        );
        return new AuthModels.LoginResult(token, null);
    }

    /** 当前登录用户的基本信息 + 角色 + 权限标识集合(按钮级权限指令的数据源) */
    @GetMapping("/getInfo")
    public AuthModels.GetInfoResponse getInfo() {
        LoginUser user = requireLoginUser();
        return new AuthModels.GetInfoResponse(
                new AuthModels.UserInfo(user.id(), user.username(), user.nickname()),
                user.roles(),
                user.perms()
        );
    }

    /** 当前用户可见的前端动态路由 */
    @GetMapping("/getRouters")
    public List<AuthModels.RouteVO> getRouters() {
        requireLoginUser();
        return menuRouteService.getRouters();
    }

    /**
     * 撤销当前会话(注册表删除 jti)，令牌即时失效；
     * 同用户其他并存会话不受影响(ADR-0001)
     */
    @PostMapping("/logout")
    public void logout(HttpServletRequest request) {
        String token = bearerToken(request);
        TokenService.TokenPayload payload = tokenService.parse(token);
        if (payload != null) {
            sessionRegistry.remove(payload.jti());
        }
    }

    private static String bearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring("Bearer ".length());
        }
        return null;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // 多级代理取首个(客户端真实 IP)
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        return request.getRemoteAddr();
    }

    private static LoginUser requireLoginUser() {
        LoginUser user = SecurityUtils.currentLoginUser();
        if (user == null) {
            // 理论上被 SecurityConfig 拦截不会到这里，防御性兜底
            throw new IllegalStateException("未登录");
        }
        return user;
    }
}
