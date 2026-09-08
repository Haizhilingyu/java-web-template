package com.jezetek.core.runtime;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 当前租户提供者：从请求头 tenant 中取值。
 *
 * <p>非 Web 环境返回 null，由 TenantAwareDraftInterceptor 落到默认租户</p>
 */
@Component
public class TenantProvider {

    @Nullable
    public String get() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            String tenant = ((ServletRequestAttributes) requestAttributes).getRequest().getHeader("tenant");
            return tenant == null || tenant.isEmpty() ? null : tenant;
        }
        return null;
    }
}
