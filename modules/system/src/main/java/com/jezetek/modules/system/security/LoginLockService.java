package com.jezetek.modules.system.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 密码连错锁定(工单02)：进程内 Caffeine 计数(key=username)。
 *
 * <p>语义：只有"用户存在但密码错误"才计数(由调用方判定)；计数条目
 * expireAfterWrite=lockMinutes，每次失败重写条目即滑动窗口，达到阈值后
 * 不再重写，锁定自最后一次失败起算，到期条目过期自动解锁。
 * 成功登录由调用方 {@link #reset} 清零</p>
 *
 * <p>内存实现仅单实例有效，与会话注册表(ADR-0001)同一取舍</p>
 */
@Component
public class LoginLockService {

    private final LoginProtectionProperties properties;

    private final Cache<String, AttemptState> attempts;

    /** 阈值与锁定时长来自可注入配置(多构造器需显式指定装配入口) */
    @Autowired
    public LoginLockService(LoginProtectionProperties properties) {
        this(properties, Duration.ofMinutes(properties.getLockMinutes()));
    }

    /** 供测试以短 TTL 验证到期解锁 */
    public LoginLockService(LoginProtectionProperties properties, Duration ttl) {
        this.properties = properties;
        this.attempts = Caffeine.newBuilder().expireAfterWrite(ttl).build();
    }

    /**
     * 记一次密码错误；达到阈值时该次即锁定起点
     */
    public void recordFailure(String username) {
        if (!properties.isEnabled()) {
            return;
        }
        attempts.asMap().compute(username, (key, state) -> {
            // 已锁定：保持原条目，避免重写延长 TTL(锁定期内调用方应先拒绝)
            if (state != null && state.lockedAt() != null) {
                return state;
            }
            int count = state == null ? 1 : state.count() + 1;
            if (count < properties.getMaxAttempts()) {
                return new AttemptState(count, null);
            }
            // 达到阈值：记锁定起点；条目 TTL 到期即解锁
            return new AttemptState(count, LocalDateTime.now());
        });
    }

    /**
     * 是否锁定期内
     */
    public boolean isLocked(String username) {
        return remainingMinutes(username).isPresent();
    }

    /**
     * 剩余锁定分钟数(向上取整，至少 1)；未锁定返回空
     */
    public Optional<Long> remainingMinutes(String username) {
        AttemptState state = attempts.getIfPresent(username);
        if (state == null || state.lockedAt() == null) {
            return Optional.empty();
        }
        long elapsed = Duration.between(state.lockedAt(), LocalDateTime.now()).toMinutes();
        long remaining = properties.getLockMinutes() - elapsed;
        return remaining > 0 ? Optional.of(remaining) : Optional.empty();
    }

    /**
     * 成功登录清零
     */
    public void reset(String username) {
        attempts.invalidate(username);
    }

    /** 清空全部计数(测试隔离用) */
    public void clear() {
        attempts.invalidateAll();
    }

    /**
     * 计数状态：lockedAt 非空表示已达到阈值(锁定起点)
     */
    private record AttemptState(int count, LocalDateTime lockedAt) {
    }
}
