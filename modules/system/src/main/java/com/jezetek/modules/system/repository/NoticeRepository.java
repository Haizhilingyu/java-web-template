package com.jezetek.modules.system.repository;

import com.jezetek.core.runtime.repository.PageOrders;
import org.babyfish.jimmer.sql.ast.Expression;
import com.jezetek.modules.system.model.Notice;
import com.jezetek.modules.system.model.NoticeTable;
import org.babyfish.jimmer.Specification;
import org.babyfish.jimmer.spring.repo.support.AbstractJavaRepository;
import org.babyfish.jimmer.spring.repository.support.SpringPageFactory;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class NoticeRepository extends AbstractJavaRepository<Notice, Long> {

    private static final NoticeTable table = NoticeTable.$;

    public NoticeRepository(JSqlClient sql) {
        super(sql);
    }

    /**
     * 超级 QBE：Specification 描述动态查询条件，Fetcher 描述动态抓取形状，
     * Pageable 描述分页排序，三者均由调用方按需组装
     */
    public Page<@NotNull Notice> find(
            Pageable pageable,
            Specification<Notice> specification,
            @Nullable Fetcher<Notice> fetcher
    ) {
        return sql
                .createQuery(table)
                .where(specification)
                .orderBy(PageOrders.translate(pageable.getSort(), NoticeRepository::sortable))
                .select(table.fetch(fetcher))
                .fetchPage(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        SpringPageFactory.getInstance()
                );
    }

    /** sortCode 属性白名单：白名单外回退 id(见 PageOrders) */
    private static Expression<?> sortable(String property) {
        return switch (property) {
            case "id" -> table.id();
            case "noticeTitle" -> table.noticeTitle();
            case "noticeType" -> table.noticeType();
            case "enabled" -> table.enabled();
            case "createdTime" -> table.createdTime();
            case "modifiedTime" -> table.modifiedTime();
            default -> null;
        };
    }
}
