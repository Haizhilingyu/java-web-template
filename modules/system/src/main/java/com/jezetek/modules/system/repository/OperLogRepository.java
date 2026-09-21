package com.jezetek.modules.system.repository;

import com.jezetek.core.runtime.repository.PageOrders;
import org.babyfish.jimmer.sql.ast.Expression;
import com.jezetek.modules.system.model.OperLog;
import com.jezetek.modules.system.model.OperLogTable;
import org.babyfish.jimmer.spring.repo.support.AbstractJavaRepository;
import org.babyfish.jimmer.spring.repository.support.SpringPageFactory;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class OperLogRepository extends AbstractJavaRepository<OperLog, Long> {

    private static final OperLogTable table = OperLogTable.$;

    public OperLogRepository(JSqlClient sql) {
        super(sql);
    }

    /**
     * keyword 对模块/动作/操作人/URI 模糊过滤(null 或空串不过滤)
     */
    public Page<@NotNull OperLog> find(
            Pageable pageable,
            @Nullable String keyword,
            @Nullable Fetcher<OperLog> fetcher
    ) {
        return sql
                .createQuery(table)
                .where(keyword == null || keyword.isBlank() ? null : Predicate.or(
                        table.module().like(keyword),
                        table.action().like(keyword),
                        table.operator().like(keyword),
                        table.uri().like(keyword)))
                .orderBy(PageOrders.translate(pageable.getSort(), OperLogRepository::sortable))
                .select(table.fetch(fetcher))
                .fetchPage(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        SpringPageFactory.getInstance()
                );
    }

    /**
     * 全量查询不分页(工单04 导出)：keyword 过滤同 find，按 id 倒序(新在前)
     */
    public List<@NotNull OperLog> listAll(
            @Nullable String keyword,
            @Nullable Fetcher<OperLog> fetcher
    ) {
        return sql
                .createQuery(table)
                .where(keyword == null || keyword.isBlank() ? null : Predicate.or(
                        table.module().like(keyword),
                        table.action().like(keyword),
                        table.operator().like(keyword),
                        table.uri().like(keyword)))
                .orderBy(PageOrders.translate(Sort.by("id").descending(), OperLogRepository::sortable))
                .select(table.fetch(fetcher))
                .execute();
    }

    /**
     * 清空全部操作日志
     */
    public void clearAll() {
        sql
                .createDelete(table)
                .execute();
    }

    /** sortCode 属性白名单：白名单外回退 id(见 PageOrders) */
    private static Expression<?> sortable(String property) {
        return switch (property) {
            case "id" -> table.id();
            case "module" -> table.module();
            case "action" -> table.action();
            case "operator" -> table.operator();
            case "uri" -> table.uri();
            case "costMs" -> table.costMs();
            case "success" -> table.success();
            case "createdTime" -> table.createdTime();
            case "modifiedTime" -> table.modifiedTime();
            default -> null;
        };
    }
}
