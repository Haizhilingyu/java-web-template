package com.jezetek.core.runtime.filter;

import com.jezetek.core.model.common.TenantAwareProps;
import com.jezetek.core.runtime.TenantProvider;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.springframework.stereotype.Component;

/**
 * 全局租户过滤器：所有针对 TenantAware 实体(User/Role)的查询自动追加
 * {@code TENANT = 当前租户} 条件，业务查询无需关心租户隔离。
 *
 * <p>当前模板未启用 Jimmer 对象缓存；若启用，需要参考 jimmer-sql 示例的
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
