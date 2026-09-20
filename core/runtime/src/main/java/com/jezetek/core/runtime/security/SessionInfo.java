package com.jezetek.core.runtime.security;

import java.time.LocalDateTime;

/**
 * 一条在线会话：登录时登记进 {@link SessionRegistry}，
 * 供登出撤销、在线用户列表(工单06)与改密作废(工单09)消费
 */
public record SessionInfo(
        long userId,
        String username,
        String nickname,
        LocalDateTime loginTime,
        String ip
) {
}
