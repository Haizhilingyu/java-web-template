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
import java.util.UUID;

/**
 * JWT 令牌 + 会话注册表(ADR-0001)：subject 存用户 id，jti 关联注册表条目。
 *
 * <p>签名校验通过后还要求 jti 在册——令牌"默认有效、可被撤销"，
 * 登出/强退/改密作废删除注册表条目即即时生效。
 * 每次请求按 subject 回库加载用户，禁用/改角色即时生效</p>
 */
@Component
public class TokenService {

    private final SecretKey key;

    private final Duration expire;

    private final SessionRegistry sessionRegistry;

    public TokenService(SecurityProperties properties, SessionRegistry sessionRegistry) {
        this.key = Keys.hmacShaKeyFor(properties.secretKey().getBytes(StandardCharsets.UTF_8));
        this.expire = Duration.ofHours(properties.expireHours());
        this.sessionRegistry = sessionRegistry;
    }

    /**
     * 签发令牌并登记会话(jti → 会话信息)
     */
    public String create(long userId, String username, String nickname, String ip) {
        String jti = UUID.randomUUID().toString();
        Date now = new Date();
        String token = Jwts.builder()
                .id(jti)
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expire.toMillis()))
                .signWith(key)
                .compact();
        sessionRegistry.register(jti, userId, username, nickname, ip);
        return token;
    }

    /**
     * @return 令牌有效时返回用户 id 与 jti；缺失/伪造/过期一律返回 null。
     *         注意：此处不校验 jti 是否在册，认证过滤器负责
     */
    @Nullable
    public TokenPayload parse(@Nullable String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new TokenPayload(Long.valueOf(claims.getSubject()), claims.getId());
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 解析结果：用户 id + jti(会话注册表的键)
     */
    public record TokenPayload(long userId, String jti) {
    }
}
