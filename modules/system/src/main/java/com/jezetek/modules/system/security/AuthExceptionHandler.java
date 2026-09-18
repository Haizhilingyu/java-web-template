package com.jezetek.modules.system.security;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 登录失败统一转 401 + {message}。
 * 前端 auth.ts 据状态码与 message 提示，不跳转
 */
@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<AuthModels.LoginResult> badCredentials(AuthenticationException e) {
        return build("用户名或密码错误");
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<AuthModels.LoginResult> disabled(AuthenticationException e) {
        return build("账号已禁用");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<AuthModels.LoginResult> authentication(AuthenticationException e) {
        return build("登录失败: " + e.getMessage());
    }

    private static ResponseEntity<AuthModels.LoginResult> build(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new AuthModels.LoginResult(null, message));
    }
}
