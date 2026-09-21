package com.jezetek.modules.job.repository;

import com.jezetek.core.runtime.repository.PageOrders;
import org.babyfish.jimmer.sql.ast.Expression;
import com.jezetek.modules.job.model.SysJob;
import com.jezetek.modules.job.model.SysJobTable;
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

import java.util.List;

@Repository
public class JobRepository extends AbstractJavaRepository<SysJob, Long> {

    private static final SysJobTable table = SysJobTable.$;

    public JobRepository(JSqlClient sql) {
        super(sql);
    }

    /**
     * 超级 QBE：Specification 描述动态查询条件，Fetcher 描述动态抓取形状，
     * Pageable 描述分页排序，三者均由调用方按需组装
     */
    public Page<@NotNull SysJob> find(
            Pageable pageable,
            Specification<SysJob> specification,
            @Nullable Fetcher<SysJob> fetcher
    ) {
        return sql
                .createQuery(table)
                .where(specification)
                .orderBy(PageOrders.translate(pageable.getSort(), JobRepository::sortable))
                .select(table.fetch(fetcher))
                .fetchPage(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        SpringPageFactory.getInstance()
                );
    }

    /**
     * 启动时加载全部待调度任务
     */
    public List<SysJob> findByStatus(int status) {
        return sql
                .createQuery(table)
                .where(table.status().eq(status))
                .select(table)
                .execute();
    }

    /** sortCode 属性白名单：白名单外回退 id(见 PageOrders) */
    private static Expression<?> sortable(String property) {
        return switch (property) {
            case "id" -> table.id();
            case "name" -> table.name();
            case "cron" -> table.cron();
            case "handler" -> table.handler();
            case "param" -> table.param();
            case "status" -> table.status();
            case "memo" -> table.memo();
            case "createdTime" -> table.createdTime();
            case "modifiedTime" -> table.modifiedTime();
            default -> null;
        };
    }
}
