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
            @jakarta.validation.constraints.NotBlank(message = "密码不能为空") String password,
            // 验证码开关开启时必填(工单01)；关闭时前端不渲染也不提交
            @Nullable String captchaKey,
            @Nullable String captchaCode) {
    }

    /**
     * 登录验证码(工单01)：开关关只回 enabled=false；
     * 开关开时 key 用于提交，image 为 Base64 PNG 可直接进 img src
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CaptchaResponse(
            boolean enabled,
            @Nullable String key,
            @Nullable String image) {
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

    /** 个人中心资料：部门/角色/岗位只取名称 */
    public record ProfileResponse(
            long id,
            String username,
            @Nullable String nickname,
            @Nullable String deptName,
            List<String> roleNames,
            List<String> postNames
    ) {
    }

    public record NicknameRequest(
            @jakarta.validation.constraints.NotBlank(message = "昵称不能为空")
            @jakarta.validation.constraints.Size(max = 50, message = "昵称长度不能超过50")
            String nickname
    ) {
    }

    public record ChangePasswordRequest(
            @jakarta.validation.constraints.NotBlank(message = "旧密码不能为空")
            String oldPassword,
            @jakarta.validation.constraints.NotBlank(message = "新密码不能为空")
            @jakarta.validation.constraints.Size(min = 6, max = 100, message = "新密码长度必须在6~100之间")
            String newPassword
    ) {
    }
}
