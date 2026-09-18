package com.jezetek.core.runtime.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/**
 * 无状态 JWT 令牌：subject 存用户 id，HMAC-SHA256 签名。
 *
 * <p>不缓存会话——每次请求按 subject 回库加载用户，
 * 禁用/改角色即时生效；数据量小(H2 演示库)可接受</p>
 */
@Component
public class TokenService {

    private final SecretKey key;

    private final Duration expire;

    public TokenService(SecurityProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secretKey().getBytes(StandardCharsets.UTF_8));
        this.expire = Duration.ofHours(properties.expireHours());
    }

    public String create(long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expire.toMillis()))
                .signWith(key)
                .compact();
    }

    /**
     * @return 令牌有效时返回用户 id；缺失/伪造/过期一律返回 null
     */
    @Nullable
    public Long parse(@Nullable String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Long.valueOf(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
