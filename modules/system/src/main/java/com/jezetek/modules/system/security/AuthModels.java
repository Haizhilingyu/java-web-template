package com.jezetek.modules.system.security;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * 认证接口的请求/响应契约(前端 web/src/api/auth.ts 依此对接)。
 * getRouters 直接返回前端 RouteItem 形状，前端拿到后仅做组件字符串→组件的转换
 */
public final class AuthModels {

    private AuthModels() {
    }

    public record LoginRequest(
            @jakarta.validation.constraints.NotBlank(message = "用户名不能为空") String username,
            @jakarta.validation.constraints.NotBlank(message = "密码不能为空") String password) {
    }

    /** 登录成功带 token，失败带 message(HTTP 401)，二者互斥 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record LoginResult(@Nullable String token, @Nullable String message) {
    }

    public record UserInfo(long id, String username, @Nullable String nickname) {
    }

    public record GetInfoResponse(UserInfo user, Set<String> roles, Set<String> perms) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RouteMeta(String title, @Nullable String icon, int orderNo, boolean hidden) {
    }

    /**
     * 与前端 tdesign-starter 的 RouteItem 对齐：
     * 目录 component 固定 "LAYOUT"，页面 component 为 pages 下的组件路径字符串
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RouteVO(
            String name,
            String path,
            @Nullable String component,
            @Nullable String redirect,
            RouteMeta meta,
            @Nullable List<RouteVO> children
    ) {
    }
}
