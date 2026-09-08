package com.jezetek.core.runtime.interceptor;

import com.jezetek.core.model.common.TenantAware;
import com.jezetek.core.model.common.TenantAwareDraft;
import com.jezetek.core.model.common.TenantAwareProps;
import com.jezetek.core.runtime.TenantProvider;
import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.sql.DraftInterceptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 保存前自动填充 tenant：请求头有值用请求头，否则落到默认租户
 */
@Component
public class TenantAwareDraftInterceptor implements DraftInterceptor<TenantAware, TenantAwareDraft> {

    private final TenantProvider tenantProvider;

    private final String defaultTenant;

    public TenantAwareDraftInterceptor(
            TenantProvider tenantProvider,
            @Value("${core.default-tenant:default}") String defaultTenant
    ) {
        this.tenantProvider = tenantProvider;
        this.defaultTenant = defaultTenant;
    }

    @Override
    public void beforeSave(@NotNull TenantAwareDraft draft, @Nullable TenantAware original) {
        if (!ImmutableObjects.isLoaded(draft, TenantAwareProps.TENANT)) {
            String tenant = tenantProvider.get();
            if (tenant == null) {
                tenant = defaultTenant;
            }
            draft.setTenant(tenant);
        }
    }
}
