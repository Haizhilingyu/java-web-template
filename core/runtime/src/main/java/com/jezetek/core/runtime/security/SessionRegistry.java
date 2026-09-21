package com.jezetek.core.runtime.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 会话注册表(ADR-0001)：jti → 会话信息，令牌"默认有效、可被撤销"。
 *
 * <p>登录签发时登记，TTL = 令牌过期时长(条目与令牌同寿)；
 * 认证过滤器要求 jti 必须在册，登出/强退/改密作废删除对应条目即时生效。
 * 内存实现仅单实例部署有效，服务重启全部会话失效(ADR-0001 接受)</p>
 */
@Component
public class SessionRegistry {

    private final Cache<String, SessionInfo> cache;

    @Autowired
    public SessionRegistry(SecurityProperties properties) {
        this(Duration.ofHours(properties.expireHours()));
    }

    /** 供测试以短 TTL 验证过期行为 */
    public SessionRegistry(Duration ttl) {
        this.cache = Caffeine.newBuilder().expireAfterWrite(ttl).build();
    }

    public void register(String jti, long userId, String username, String nickname, String ip) {
        cache.put(jti, new SessionInfo(userId, username, nickname, LocalDateTime.now(), ip));
    }

    /** jti 是否在册(缺失/伪造/过期令牌到达这里前已被 parse 拒绝) */
    public boolean contains(String jti) {
        return jti != null && cache.getIfPresent(jti) != null;
    }

    public void remove(String jti) {
        if (jti != null) {
            cache.invalidate(jti);
        }
    }

    /** 作废某用户的全部会话(改密等场景)，返回删除条数 */
    public int removeByUser(long userId) {
        Map<String, SessionInfo> all = cache.asMap();
        Collection<String> hit = all.entrySet().stream()
                .filter(e -> e.getValue().userId() == userId)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        hit.forEach(cache::invalidate);
        return hit.size();
    }

    /** 全部在册会话的不可变快照(jti → 会话信息)，供在线用户列表回传 jti */
    public Map<String, SessionInfo> snapshot() {
        return Map.copyOf(cache.asMap());
    }
}
