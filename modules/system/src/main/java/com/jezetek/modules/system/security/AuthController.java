package com.jezetek.modules.system.security;

import com.jezetek.core.runtime.security.LoginUser;
import com.jezetek.core.runtime.security.SecurityUtils;
import com.jezetek.core.runtime.security.TokenService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
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

    private final MenuRouteService menuRouteService;

    public AuthController(
            AuthenticationManager authenticationManager,
            TokenService tokenService,
            MenuRouteService menuRouteService
    ) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.menuRouteService = menuRouteService;
    }

    /** 认证成功签发 JWT */
    @PostMapping("/login")
    public AuthModels.LoginResult login(@Valid @RequestBody AuthModels.LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        UserDetails details = (UserDetails) authentication.getPrincipal();
        long userId = ((LoginUserDetails) details).userId();
        return new AuthModels.LoginResult(tokenService.create(userId), null);
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
     * 无状态令牌没有服务端会话可销毁，
     * 前端删除本地 token 即完成登出；此处留作令牌黑名单等扩展点
     */
    @PostMapping("/logout")
    public void logout() {
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
