package com.jezetek.modules.system.repository;

import com.jezetek.core.runtime.repository.PageOrders;
import org.babyfish.jimmer.sql.ast.Expression;
import com.jezetek.modules.system.model.Logininfor;
import com.jezetek.modules.system.model.LogininforTable;
import org.babyfish.jimmer.Specification;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.spring.repo.support.AbstractJavaRepository;
import org.babyfish.jimmer.spring.repository.support.SpringPageFactory;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LogininforRepository extends AbstractJavaRepository<Logininfor, Long> {

    private static final LogininforTable table = LogininforTable.$;

    public LogininforRepository(JSqlClient sql) {
        super(sql);
    }

    /**
     * 超级 QBE：keyword 对账号/IP/消息模糊过滤(null 或空串不过滤)
     */
    public Page<@NotNull Logininfor> find(
            Pageable pageable,
            @Nullable String keyword,
            @Nullable Fetcher<Logininfor> fetcher
    ) {
        return sql
                .createQuery(table)
                .where(keyword == null ? null : Predicate.or(
                        table.username().like(keyword),
                        table.ip().like(keyword),
                        table.message().like(keyword)))
                .orderBy(PageOrders.translate(pageable.getSort(), LogininforRepository::sortable))
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
    public List<@NotNull Logininfor> listAll(
            @Nullable String keyword,
            @Nullable Fetcher<Logininfor> fetcher
    ) {
        return sql
                .createQuery(table)
                .where(keyword == null || keyword.isBlank() ? null : Predicate.or(
                        table.username().like(keyword),
                        table.ip().like(keyword),
                        table.message().like(keyword)))
                .orderBy(PageOrders.translate(Sort.by("id").descending(), LogininforRepository::sortable))
                .select(table.fetch(fetcher))
                .execute();
    }


    /**
     * 今日登录成功次数(首页统计，工单09)：message=登录成功 且 createdTime 在今天零点后
     */
    public long countTodaySuccess() {
        return sql
                .createQuery(table)
                .where(table.message().eq("登录成功"))
                .where(table.createdTime().ge(java.time.LocalDate.now().atStartOfDay()))
                .select(table.id().count())
                .execute()
                .stream()
                .findFirst()
                .orElse(0L);
    }

    /**
     * 清空全部登录日志
     *
     * @return 删除条数
     */
    public int clearAll() {
        return sql
                .createDelete(table)
                .execute();
    }

    /** sortCode 属性白名单：白名单外回退 id(见 PageOrders) */
    private static Expression<?> sortable(String property) {
        return switch (property) {
            case "id" -> table.id();
            case "username" -> table.username();
            case "ip" -> table.ip();
            case "message" -> table.message();
            case "createdTime" -> table.createdTime();
            case "modifiedTime" -> table.modifiedTime();
            default -> null;
        };
    }
}
