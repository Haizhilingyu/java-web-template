package com.jezetek.core.runtime.repository;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Spring Sort → jimmer orderBy 的显式翻译器。
 *
 * <p>jimmer 的 {@code fetchPage(pageNo, size, pageFactory)} 不读取
 * Spring Pageable 携带的 Sort，调用方传了排序 SQL 也无 ORDER BY，
 * 因此各 Repository 的分页 find 必须把 sortCode 显式翻译成 orderBy。
 * 翻译按属性名白名单进行(resolver 返回 null 即不在白名单)，
 * 全部属性非法时回退 id，且始终以 id 收尾保证分页窗口稳定</p>
 */
public final class PageOrders {

    private PageOrders() {
    }

    /**
     * @param resolver 属性名 → 表表达式；白名单外的属性返回 null(跳过)
     * @return 至少包含一条 id 排序，可直接交给 {@code orderBy(List)}
     */
    public static List<Order> translate(Sort sort, Function<String, Expression<?>> resolver) {
        List<Order> orders = new ArrayList<>();
        boolean idPresent = false;
        if (sort != null) {
            for (Sort.Order order : sort) {
                if ("id".equals(order.getProperty())) {
                    idPresent = true;
                }
                Expression<?> expression = resolver.apply(order.getProperty());
                if (expression != null) {
                    orders.add(order.isAscending() ? expression.asc() : expression.desc());
                }
            }
        }
        if (!idPresent) {
            orders.add(resolver.apply("id").asc());
        }
        return orders;
    }
}
