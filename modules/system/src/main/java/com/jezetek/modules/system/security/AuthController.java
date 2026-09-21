package com.jezetek.modules.system.security;

import com.jezetek.core.runtime.security.LoginUser;
import com.jezetek.core.runtime.security.SecurityUtils;
import com.jezetek.core.runtime.security.SessionRegistry;
import com.jezetek.core.runtime.security.TokenService;
import com.jezetek.modules.system.service.LoginLogWriter;
import com.jezetek.modules.system.service.LogininforService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * 登录/登出/用户信息/动态路由。除 login 外均要求携带有效 JWT
 * (由 core 的 SecurityConfig 对 /auth/** 强制)。
 * 登录失败由 {@link AuthExceptionHandler} 统一转 401 JSON。
 *
 * <p>注意：方法不要声明 HttpServletRequest/Response 参数——
 * jimmer-apt 为 API 生成元数据时无法解析 servlet 类型(编译期 NPE)，
 * 一律经 {@link #currentRequest()} 获取</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;

    private final TokenService tokenService;

    private final SessionRegistry sessionRegistry;

    private final LoginLogWriter loginLogWriter;

    private final MenuRouteService menuRouteService;

    public AuthController(
            AuthenticationManager authenticationManager,
            TokenService tokenService,
            SessionRegistry sessionRegistry,
            LoginLogWriter loginLogWriter,
            MenuRouteService menuRouteService
    ) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.sessionRegistry = sessionRegistry;
        this.loginLogWriter = loginLogWriter;
        this.menuRouteService = menuRouteService;
    }

    /** 认证成功签发 JWT 并登记会话(ADR-0001)；登录日志记录成功/失败 */
    @PostMapping("/login")
    public AuthModels.LoginResult login(@Valid @RequestBody AuthModels.LoginRequest request) {
        String ip = clientIp();
        org.springframework.security.core.Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (DisabledException e) {
            loginLogWriter.append(request.username(), ip, "登录失败：账号已禁用");
            throw e;
        } catch (org.springframework.security.core.AuthenticationException e) {
            loginLogWriter.append(request.username(), ip, "登录失败：用户名或密码错误");
            throw e;
        }
        LoginUserDetails details = (LoginUserDetails) authentication.getPrincipal();
        LoginUser loginUser = details.loginUser();
        String token = tokenService.create(
                details.userId(),
                loginUser.username(),
                loginUser.nickname(),
                ip
        );
        loginLogWriter.append(loginUser.username(), ip, "登录成功");
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
    public void logout() {
        HttpServletRequest request = currentRequest();
        String header = request.getHeader("Authorization");
        String token = header != null && header.startsWith("Bearer ")
                ? header.substring("Bearer ".length())
                : null;
        TokenService.TokenPayload payload = tokenService.parse(token);
        if (payload != null) {
            LoginUser user = SecurityUtils.currentLoginUser();
            loginLogWriter.append(
                    user != null ? user.username() : "unknown",
                    clientIp(),
                    "登出"
            );
            sessionRegistry.remove(payload.jti());
        }
    }

    private static HttpServletRequest currentRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }

    private static String clientIp() {
        String forwarded = currentRequest().getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // 多级代理取首个(客户端真实 IP)
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        return currentRequest().getRemoteAddr();
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
