package com.jezetek.modules.system.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 登录保护配置(工单02)：密码连错锁定。
 *
 * <p>阈值/开关均为可注入配置，避免测试受种子数据影响：
 * 测试上下文默认关闭(见 system 测试 yml)，锁定用例以 properties
 * 覆盖单独开上下文验证</p>
 */
@ConfigurationProperties(prefix = "core.security.login-protection")
public class LoginProtectionProperties {

    /** 是否启用连错锁定 */
    private boolean enabled = true;

    /** 连续密码错误次数达到该值即锁定 */
    private int maxAttempts = 5;

    /** 锁定时长(分钟)，自最后一次失败起算 */
    private long lockMinutes = 10;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public long getLockMinutes() {
        return lockMinutes;
    }

    public void setLockMinutes(long lockMinutes) {
        this.lockMinutes = lockMinutes;
    }
}
