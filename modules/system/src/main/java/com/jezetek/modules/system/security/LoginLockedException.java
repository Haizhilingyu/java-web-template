package com.jezetek.modules.system.security;

import org.springframework.security.core.AuthenticationException;

/**
 * 密码连错触发的锁定拒绝(工单02)：携带剩余锁定分钟的提示文案，
 * 由 {@link AuthExceptionHandler} 转 401 + {message}
 */
public class LoginLockedException extends AuthenticationException {

    public LoginLockedException(String message) {
        super(message);
    }
}
