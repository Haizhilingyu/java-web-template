package com.jezetek.modules.system;

import com.jezetek.modules.system.security.LoginLockService;
import com.jezetek.modules.system.security.LoginProtectionProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 工单02：锁定服务的纯单元测试——短 TTL 验证到期自动解锁
 * (MockMvc 端到端行为见 LoginLockoutTest)
 */
class LoginLockServiceTest {

    private LoginLockService newService(long lockMinutes) {
        LoginProtectionProperties properties = new LoginProtectionProperties();
        properties.setMaxAttempts(5);
        properties.setLockMinutes(lockMinutes);
        return new LoginLockService(properties, Duration.ofMillis(lockMinutes * 100));
    }

    @Test
    void 达阈值锁定且剩余分钟向上取整() {
        LoginLockService service = newService(10);
        for (int i = 0; i < 4; i++) {
            service.recordFailure("u");
            assertFalse(service.isLocked("u"), "第" + (i + 1) + "次不应锁定");
        }
        service.recordFailure("u");
        assertTrue(service.isLocked("u"));
        assertEquals(10L, service.remainingMinutes("u").orElseThrow());
    }

    @Test
    void 到期自动解锁() throws InterruptedException {
        LoginLockService service = newService(2); // TTL = 200ms
        for (int i = 0; i < 5; i++) {
            service.recordFailure("u");
        }
        assertTrue(service.isLocked("u"));
        Thread.sleep(300);
        assertFalse(service.isLocked("u"), "条目过期应自动解锁");
    }

    @Test
    void 成功登录清零() {
        LoginLockService service = newService(10);
        for (int i = 0; i < 4; i++) {
            service.recordFailure("u");
        }
        service.reset("u");
        for (int i = 0; i < 4; i++) {
            service.recordFailure("u");
        }
        assertFalse(service.isLocked("u"));
    }

    @Test
    void 锁定期内重复失败不延长锁定() throws InterruptedException {
        LoginLockService service = newService(2); // TTL = 200ms
        for (int i = 0; i < 5; i++) {
            service.recordFailure("u");
        }
        assertTrue(service.isLocked("u"));
        service.recordFailure("u"); // 锁定期内的失败不重写条目
        Thread.sleep(300);
        assertFalse(service.isLocked("u"), "锁定时长不应被后续失败延长");
    }
}
