import type { Schema } from '../schema.js';

/**
 * 生成 runtime 模块的基础设施(与手写版一致)：
 * TenantProvider / BaseEntityDraftInterceptor / TenantAwareDraftInterceptor / TenantFilter
 */
export function genRuntimeFiles(schema: Schema): Record<string, string> {
    const pkg = schema.project.javaPackage;
    const usesTenant = schema.entities.some(e => e.tenantAware);
    const files: Record<string, string> = {
        'TenantProvider.java': usesTenant ? `package ${pkg}.runtime;

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
` : '',
        'interceptor/BaseEntityDraftInterceptor.java': `package ${pkg}.runtime.interceptor;

import ${pkg}.model.common.BaseEntity;
import ${pkg}.model.common.BaseEntityDraft;
import ${pkg}.model.common.BaseEntityProps;
import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.sql.DraftInterceptor;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 保存前自动填充 createdTime/modifiedTime
 */
@Component
public class BaseEntityDraftInterceptor implements DraftInterceptor<BaseEntity, BaseEntityDraft> {

    @Override
    public void beforeSave(BaseEntityDraft draft, @Nullable BaseEntity original) {
        if (!ImmutableObjects.isLoaded(draft, BaseEntityProps.MODIFIED_TIME)) {
            draft.setModifiedTime(LocalDateTime.now());
        }
        // original == null 表示 INSERT
        if (original == null && !ImmutableObjects.isLoaded(draft, BaseEntityProps.CREATED_TIME)) {
            draft.setCreatedTime(LocalDateTime.now());
        }
    }
}
`,
        'interceptor/TenantAwareDraftInterceptor.java': usesTenant ? `package ${pkg}.runtime.interceptor;

import ${pkg}.model.common.TenantAware;
import ${pkg}.model.common.TenantAwareDraft;
import ${pkg}.model.common.TenantAwareProps;
import ${pkg}.runtime.TenantProvider;
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
            @Value("\${${schema.project.defaultTenantProperty}:default}") String defaultTenant
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
` : '',
        'filter/TenantFilter.java': usesTenant ? `package ${pkg}.runtime.filter;

import ${pkg}.model.common.TenantAwareProps;
import ${pkg}.runtime.TenantProvider;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.springframework.stereotype.Component;

/**
 * 全局租户过滤器：所有针对 TenantAware 实体的查询自动追加
 * {@code TENANT = 当前租户} 条件，业务查询无需关心租户隔离。
 *
 * <p>当前未启用 Jimmer 对象缓存；若启用，需要参考 jimmer-sql 示例的
 * TenantFilterForCacheMode 用缓存参数构造过滤条件</p>
 */
@Component
public class TenantFilter implements Filter<TenantAwareProps> {

    protected final TenantProvider tenantProvider;

    public TenantFilter(TenantProvider tenantProvider) {
        this.tenantProvider = tenantProvider;
    }

    @Override
    public void filter(FilterArgs<TenantAwareProps> args) {
        String tenant = tenantProvider.get();
        if (tenant != null) {
            args.where(args.getTable().tenant().eq(tenant));
        }
    }
}
` : '',
    };
    // 去掉未启用租户时的空文件
    return Object.fromEntries(Object.entries(files).filter(([, v]) => v !== ''));
}
