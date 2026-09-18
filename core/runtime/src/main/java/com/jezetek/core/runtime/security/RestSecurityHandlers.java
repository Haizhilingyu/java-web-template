package com.jezetek.core.runtime.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 401/403 统一返回 JSON 而非重定向页面，前端执行器据此跳转登录或提示无权限
 */
public final class RestSecurityHandlers {

    private RestSecurityHandlers() {
    }

    public static AuthenticationEntryPoint entryPoint() {
        return (HttpServletRequest request, HttpServletResponse response, AuthenticationException e) ->
                write(response, 401, "未登录或登录已过期");
    }

    public static AccessDeniedHandler accessDeniedHandler() {
        return (HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) ->
                write(response, 403, "没有操作权限");
    }

    private static void write(HttpServletResponse response, int status, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"status\":" + status + ",\"message\":\"" + message + "\"}");
    }
}
