package com.jezetek.core.runtime.security;

import com.jezetek.core.runtime.module.ModuleProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全规则(契约在 core，全部业务模块共享)：
 *
 * <ul>
 *   <li>无状态 JWT：关闭 csrf、不建会话</li>
 *   <li>API 前缀需要认证——core 固定 {@code /api/v1/auth/**}(登录除外)，
 *       各业务模块由 {@link ModuleProvider#apiPrefixes()} 声明(默认 {@code /{code}/**})，
 *       新模块忘记声明不会静默裸奔，而是默认放行给 SPA/静态资源规则，
 *       因此模块开发清单要求必须声明</li>
 *   <li>其余请求(页面路由/静态资源/文档)放行：内嵌 SPA 的页面路由
 *       无法区分导航与接口，认证靠接口层，页面由前端守卫负责</li>
 *   <li>方法级权限用 {@code @PreAuthorize("@perm.has('模块:实体:动作')")}</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            List<ModuleProvider> moduleProviders
    ) throws Exception {
        List<String> protectedPatterns = new ArrayList<>();
        protectedPatterns.add("/api/v1/auth/**");
        for (ModuleProvider provider : moduleProviders) {
            protectedPatterns.addAll(provider.apiPrefixes());
        }

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers(protectedPatterns.toArray(String[]::new)).authenticated()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().permitAll())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(RestSecurityHandlers.entryPoint())
                        .accessDeniedHandler(RestSecurityHandlers.accessDeniedHandler()))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
