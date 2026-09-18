package com.jezetek.core.runtime.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 安全配置项(application.yml 的 core.security 段)。
 *
 * <p>HMAC-SHA256 要求密钥至少 32 字节；默认值仅供演示，
 * 生产环境必须通过配置覆盖</p>
 */
@ConfigurationProperties(prefix = "core.security")
public record SecurityProperties(
        String secretKey,
        int expireHours
) {

    public SecurityProperties {
        if (secretKey == null || secretKey.isBlank()) {
            secretKey = "java-web-template-demo-secret-key-change-me-32B";
        }
        if (expireHours <= 0) {
            expireHours = 24;
        }
    }
}
