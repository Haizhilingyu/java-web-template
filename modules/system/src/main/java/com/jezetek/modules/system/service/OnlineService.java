package com.jezetek.modules.system.service;

import com.jezetek.core.runtime.log.Log;
import com.jezetek.core.runtime.security.SessionRegistry;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 在线用户：实时读取会话注册表(ADR-0001)，管理员可强退任一会话。
 * 会话生命周期与注册表一致，不做分页(单实例内存集合)
 */
@RestController
@RequestMapping("/api/v1/online")
@Transactional(readOnly = true)
public class OnlineService {

    /**
     * 在线用户视图：jti 强退时作为路径参数回传
     */
    public record OnlineUser(
            String jti,
            String username,
            String nickname,
            String ip,
            LocalDateTime loginTime
    ) {
    }

    private final SessionRegistry sessionRegistry;

    public OnlineService(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @PreAuthorize("@perm.has('system:online:list')")
    @GetMapping("/list")
    public List<OnlineUser> list() {
        return sessionRegistry.snapshot().entrySet().stream()
                .map(e -> new OnlineUser(
                        e.getKey(),
                        e.getValue().username(),
                        e.getValue().nickname(),
                        e.getValue().ip(),
                        e.getValue().loginTime()
                ))
                .sorted((a, b) -> b.loginTime().compareTo(a.loginTime()))
                .toList();
    }

    /**
     * 强退：删除注册表条目，目标令牌下一次请求即 401
     */
    @Log(module = "在线用户", action = "强退用户")
    @PreAuthorize("@perm.has('system:online:forceLogout')")
    @DeleteMapping("/{jti}")
    public void forceLogout(@PathVariable("jti") String jti) {
        sessionRegistry.remove(jti);
    }
}
