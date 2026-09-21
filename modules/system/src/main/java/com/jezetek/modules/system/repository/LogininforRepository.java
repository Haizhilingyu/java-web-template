package com.jezetek.modules.system.repository;

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
import org.springframework.stereotype.Repository;

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
                .select(table.fetch(fetcher))
                .fetchPage(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        SpringPageFactory.getInstance()
                );
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
}
