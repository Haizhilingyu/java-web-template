package com.jezetek.modules.system;

import com.jezetek.core.runtime.security.SessionRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 会话注册表(ADR-0001)：TTL 过期与撤销语义。
 * 不起 Spring 上下文，纯 Caffeine 行为验证
 */
class SessionRegistryTest {

    private SessionRegistry newRegistry() {
        return new SessionRegistry(Duration.ofMinutes(5));
    }

    @Test
    void 登记后在册且可撤销() {
        SessionRegistry registry = newRegistry();

        registry.register("jti-1", 1L, "admin", "Administrator", "127.0.0.1");

        assertTrue(registry.contains("jti-1"));
        assertFalse(registry.contains("jti-2"));
        assertFalse(registry.contains(null));

        registry.remove("jti-1");
        assertFalse(registry.contains("jti-1"));
    }

    @Test
    void 条目随TTL过期() throws InterruptedException {
        SessionRegistry registry = new SessionRegistry(Duration.ofMillis(80));

        registry.register("jti-ttl", 1L, "admin", "Administrator", "127.0.0.1");
        assertTrue(registry.contains("jti-ttl"));

        Thread.sleep(200);

        // TTL 到期后条目消失，与令牌过期同步(令牌过期时长 = 注册表 TTL)
        assertFalse(registry.contains("jti-ttl"));
    }

    @Test
    void 按用户作废全部会话() {
        SessionRegistry registry = newRegistry();
        registry.register("jti-a", 1L, "admin", "Administrator", "127.0.0.1");
        registry.register("jti-b", 1L, "admin", "Administrator", "192.168.0.2");
        registry.register("jti-c", 2L, "demo", "演示用户", "127.0.0.1");

        int removed = registry.removeByUser(1L);

        assertEquals(2, removed);
        assertFalse(registry.contains("jti-a"));
        assertFalse(registry.contains("jti-b"));
        // 其他用户的会话不受影响
        assertTrue(registry.contains("jti-c"));
        assertEquals(1, registry.all().size());
    }
}
