package com.jezetek.modules.system.security;

import com.jezetek.core.runtime.security.LoginUser;
import com.jezetek.core.runtime.security.SecurityUtils;
import com.jezetek.core.runtime.security.SessionRegistry;
import com.jezetek.core.runtime.security.TokenService;
import com.jezetek.core.runtime.BusinessException;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import com.jezetek.modules.system.model.Fetchers;
import com.jezetek.modules.system.model.User;
import com.jezetek.modules.system.model.UserDraft;
import com.jezetek.modules.system.repository.UserRepository;
import com.jezetek.modules.system.service.LoginLogWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
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
public class AuthController implements Fetchers {

    private final AuthenticationManager authenticationManager;

    private final TokenService tokenService;

    private final SessionRegistry sessionRegistry;

    private final LoginLogWriter loginLogWriter;

    private final MenuRouteService menuRouteService;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final CaptchaService captchaService;

    public AuthController(
            AuthenticationManager authenticationManager,
            TokenService tokenService,
            SessionRegistry sessionRegistry,
            LoginLogWriter loginLogWriter,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            MenuRouteService menuRouteService,
            CaptchaService captchaService
    ) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.sessionRegistry = sessionRegistry;
        this.loginLogWriter = loginLogWriter;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.menuRouteService = menuRouteService;
        this.captchaService = captchaService;
    }

    /**
     * 登录验证码(工单01)：开关关只回 enabled=false，前端不渲染验证码框；
     * 开关开时返回 key 与 Base64 图。免登录访问(SecurityConfig permitAll)
     */
    @GetMapping("/captcha")
    public AuthModels.CaptchaResponse captcha() {
        if (!captchaService.enabled()) {
            return new AuthModels.CaptchaResponse(false, null, null);
        }
        CaptchaService.Challenge challenge = captchaService.generate();
        return new AuthModels.CaptchaResponse(true, challenge.key(), challenge.image());
    }

    /** 认证成功签发 JWT 并登记会话(ADR-0001)；登录日志记录成功/失败 */
    @PostMapping("/login")
    public AuthModels.LoginResult login(@Valid @RequestBody AuthModels.LoginRequest request) {
        String ip = clientIp();
        // 验证码开关开启时先验码(一次性)，错码记登录日志并拒绝(工单01)
        if (captchaService.enabled() && !captchaService.verify(request.captchaKey(), request.captchaCode())) {
            loginLogWriter.append(request.username(), ip, "登录失败：验证码错误");
            throw new BusinessException("验证码错误或已过期");
        }
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

    /**
     * 个人中心资料：部门/角色/岗位名称
     */
    @GetMapping("/profile")
    public AuthModels.ProfileResponse profile() {
        LoginUser current = requireLoginUser();
        User user = userRepository.findById(
                current.id(),
                USER_FETCHER
                        .username()
                        .nickname()
                        .dept(DEPT_FETCHER.name())
                        .roles(ROLE_FETCHER.name())
                        .posts(POST_FETCHER.name())
        );
        if (user == null) {
            throw new IllegalStateException("用户不存在");
        }
        return new AuthModels.ProfileResponse(
                user.id(),
                user.username(),
                user.nickname(),
                user.dept() != null ? user.dept().name() : null,
                user.roles().stream().map(com.jezetek.modules.system.model.Role::name).toList(),
                user.posts().stream().map(com.jezetek.modules.system.model.Post::name).toList()
        );
    }

    /**
     * 修改昵称：每次请求回库加载用户，无需作废会话
     */
    @PutMapping("/nickname")
    public void changeNickname(@Valid @RequestBody AuthModels.NicknameRequest request) {
        LoginUser current = requireLoginUser();
        User entity = UserDraft.$.produce(draft -> {
            draft.setId(current.id());
            draft.setNickname(request.nickname());
        });
        userRepository.saveCommand(entity).setMode(SaveMode.NON_IDEMPOTENT_UPSERT).execute();
    }

    /**
     * 修改密码：校验旧密码后更新，成功即作废该用户全部会话(含当前)，
     * 所有端需重新登录(ADR-0001)
     */
    @PutMapping("/password")
    public void changePassword(@Valid @RequestBody AuthModels.ChangePasswordRequest request) {
        LoginUser current = requireLoginUser();
        User user = userRepository.findById(current.id(), USER_FETCHER.password());
        if (user == null || user.password() == null
                || !passwordEncoder.matches(request.oldPassword(), user.password())) {
            throw new BusinessException("旧密码错误");
        }
        User entity = UserDraft.$.produce(draft -> {
            draft.setId(current.id());
            draft.setPassword(passwordEncoder.encode(request.newPassword()));
        });
        userRepository.saveCommand(entity).setMode(SaveMode.NON_IDEMPOTENT_UPSERT).execute();
        sessionRegistry.removeByUser(current.id());
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
