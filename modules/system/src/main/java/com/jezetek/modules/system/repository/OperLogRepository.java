package com.jezetek.modules.system.repository;

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
import org.springframework.stereotype.Repository;

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
                .select(table.fetch(fetcher))
                .fetchPage(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        SpringPageFactory.getInstance()
                );
    }

    /**
     * 清空全部操作日志
     */
    public void clearAll() {
        sql
                .createDelete(table)
                .execute();
    }
}
