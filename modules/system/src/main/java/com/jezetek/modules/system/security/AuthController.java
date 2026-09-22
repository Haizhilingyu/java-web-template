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
import java.util.Set;

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

    private final LoginLockService loginLockService;

    private final PasswordManager passwordManager;

    public AuthController(
            AuthenticationManager authenticationManager,
            TokenService tokenService,
            SessionRegistry sessionRegistry,
            LoginLogWriter loginLogWriter,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            MenuRouteService menuRouteService,
            CaptchaService captchaService,
            LoginLockService loginLockService,
            PasswordManager passwordManager
    ) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.sessionRegistry = sessionRegistry;
        this.loginLogWriter = loginLogWriter;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.menuRouteService = menuRouteService;
        this.captchaService = captchaService;
        this.loginLockService = loginLockService;
        this.passwordManager = passwordManager;
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
        // 验证码开关开启时先验码(一次性)，错码记登录日志并拒绝(工单01)。
        // 验证码失败不计入密码错误计数(工单02)
        if (captchaService.enabled() && !captchaService.verify(request.captchaKey(), request.captchaCode())) {
            loginLogWriter.append(request.username(), ip, "登录失败：验证码错误");
            throw new BusinessException("验证码错误或已过期");
        }
        // 锁定期内直接拒绝，不再尝试认证(工单02)
        Long remainingMinutes = loginLockService.remainingMinutes(request.username()).orElse(null);
        if (remainingMinutes != null) {
            loginLogWriter.append(request.username(), ip,
                    "登录失败：账号已锁定，剩余约" + remainingMinutes + "分钟");
            throw new LoginLockedException("密码连续错误过多，账号已锁定，请约" + remainingMinutes + "分钟后再试");
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
            // 只有"用户存在但密码错误"才计入连错(工单02)；用户名不存在不计
            boolean userExists = userRepository
                    .findByUsername(request.username(), USER_FETCHER.username())
                    .isPresent();
            if (userExists) {
                loginLockService.recordFailure(request.username());
            }
            loginLogWriter.append(request.username(), ip, "登录失败：用户名或密码错误");
            throw e;
        }
        loginLockService.reset(request.username());
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
     * 所有端需重新登录(ADR-0001)；落库+作废走 PasswordManager 共用通道
     */
    @PutMapping("/password")
    public void changePassword(@Valid @RequestBody AuthModels.ChangePasswordRequest request) {
        LoginUser current = requireLoginUser();
        User user = userRepository.findById(current.id(), USER_FETCHER.password());
        if (user == null || user.password() == null
                || !passwordEncoder.matches(request.oldPassword(), user.password())) {
            throw new BusinessException("旧密码错误");
        }
        passwordManager.updatePasswordAndRevokeSessions(
                current.id(), passwordEncoder.encode(request.newPassword()));
    }

    /**
     * 上传当前用户头像(工单07)：multipart ≤2MB，扩展名白名单 png/jpg/jpeg/gif。
     * 内联实现不抽通用文件服务；签名不带 multipart 类型(见类注释)
     */
    @org.springframework.web.bind.annotation.PostMapping("/avatar")
    public void uploadAvatar() throws java.io.IOException, jakarta.servlet.ServletException {
        LoginUser current = requireLoginUser();
        org.springframework.web.multipart.MultipartFile file = currentMultipartFile();
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择头像图片");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new BusinessException("头像大小不能超过2MB");
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = filename.lastIndexOf('.');
        String ext = dot < 0 ? "" : filename.substring(dot + 1).toLowerCase();
        if (!Set.of("png", "jpg", "jpeg", "gif").contains(ext)) {
            throw new BusinessException("仅支持 png/jpg/jpeg/gif 图片");
        }
        byte[] bytes = file.getBytes();
        User entity = UserDraft.$.produce(draft -> {
            draft.setId(current.id());
            draft.setAvatar(bytes);
        });
        userRepository.saveCommand(entity)
                .setMode(SaveMode.NON_IDEMPOTENT_UPSERT)
                .execute();
    }

    /**
     * 读取当前用户头像(工单07)：authenticated 流式返回(JWT 在 header，
     * 前端 fetch blob → objectURL 展示)；无头像返回 404
     */
    @org.springframework.web.bind.annotation.GetMapping("/avatar")
    public void avatar() throws java.io.IOException {
        LoginUser current = requireLoginUser();
        User user = userRepository.findById(current.id(), USER_FETCHER.avatar());
        jakarta.servlet.http.HttpServletResponse response = ((ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes()).getResponse();
        if (response == null) {
            throw new IllegalStateException("无当前响应上下文");
        }
        if (user == null || user.avatar() == null) {
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        byte[] bytes = user.avatar();
        response.setContentType(detectImageContentType(bytes));
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
    }

    /** 魔数识别图片类型，兜底 octet-stream */
    private static String detectImageContentType(byte[] bytes) {
        if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P') {
            return "image/png";
        }
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            return "image/jpeg";
        }
        if (bytes.length >= 6 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
            return "image/gif";
        }
        return "application/octet-stream";
    }

    /**
     * 从当前请求取 multipart 文件：签名不出现 multipart 类型(见类注释)。
     * Spring 包装(MockMvc)优先，生产回退 Servlet Part(见 PartMultipartFile)
     */
    private static org.springframework.web.multipart.MultipartFile currentMultipartFile()
            throws java.io.IOException, jakarta.servlet.ServletException {
        return PartMultipartFile.fromRequest(currentRequest(), "file");
    }

    private static final int MAX_AVATAR_BYTES = 2 * 1024 * 1024;

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
