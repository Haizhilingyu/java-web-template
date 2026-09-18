package com.jezetek.core.runtime.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 从 Authorization: Bearer 头解析 JWT 并装配 Authentication。
 *
 * <p>无令牌/令牌非法/用户不存在时静默放行为匿名，
 * 由授权规则决定 401；令牌有效即认为已认证(无状态，不查会话)</p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenService tokenService;

    private final ObjectProvider<LoginUserLoader> loginUserLoader;

    public JwtAuthenticationFilter(TokenService tokenService, ObjectProvider<LoginUserLoader> loginUserLoader) {
        this.tokenService = tokenService;
        this.loginUserLoader = loginUserLoader;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            LoginUser loginUser = resolve(request);
            if (loginUser != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.authorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } finally {
            filterChain.doFilter(request, response);
        }
    }

    @Nullable
    private LoginUser resolve(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        Long userId = tokenService.parse(header.substring(BEARER_PREFIX.length()));
        if (userId == null) {
            return null;
        }
        LoginUserLoader loader = loginUserLoader.getIfAvailable();
        return loader == null ? null : loader.load(userId);
    }
}
