package com.jezetek.modules.system.repository;

import com.jezetek.core.runtime.repository.PageOrders;
import org.babyfish.jimmer.sql.ast.Expression;
import com.jezetek.modules.system.model.DictData;
import com.jezetek.modules.system.model.DictDataFetcher;
import com.jezetek.modules.system.model.DictDataTable;
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
public class DictDataRepository extends AbstractJavaRepository<DictData, Long> {

    private static final DictDataTable table = DictDataTable.$;

    public DictDataRepository(JSqlClient sql) {
        super(sql);
    }

    /**
     * 超级 QBE：Specification 描述动态查询条件，Fetcher 描述动态抓取形状，
     * Pageable 描述分页排序，三者均由调用方按需组装。
     *
     * @param dictTypeId 字典类型 id 过滤；null 表示不过滤
     */
    public Page<@NotNull DictData> find(
            Pageable pageable,
            Specification<DictData> specification,
            @Nullable Long dictTypeId,
            @Nullable Fetcher<DictData> fetcher
    ) {
        return sql
                .createQuery(table)
                .where(specification)
                .where(dictTypeId == null ? null : table.dictTypeId().eq(dictTypeId))
                .orderBy(PageOrders.translate(pageable.getSort(), DictDataRepository::sortable))
                .select(table.fetch(fetcher))
                .fetchPage(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        SpringPageFactory.getInstance()
                );
    }

    /**
     * 按字典编码取全部启用条目，按 sortOrder 升序：
     * 前端 useDict 的数据源，按类型编码消费
     */
    public List<DictData> findEnabledByType(String type) {
        return sql
                .createQuery(table)
                .where(table.dictType().type().eq(type))
                .where(table.enabled().eq(true))
                .where(table.dictType().enabled().eq(true))
                .orderBy(table.sortOrder().asc(), table.id().asc())
                .select(table.fetch(DictDataFetcher.$.allScalarFields()))
                .execute();
    }

    /**
     * 指定类型下是否存在条目：删除字典类型前校验
     */
    public boolean existsByDictTypeId(long dictTypeId) {
        return !sql
                .createQuery(table)
                .where(table.dictTypeId().eq(dictTypeId))
                .select(table.id())
                .execute()
                .isEmpty();
    }

    /** sortCode 属性白名单：白名单外回退 id(见 PageOrders) */
    private static Expression<?> sortable(String property) {
        return switch (property) {
            case "id" -> table.id();
            case "label" -> table.label();
            case "value" -> table.value();
            case "sortOrder" -> table.sortOrder();
            case "enabled" -> table.enabled();
            case "createdTime" -> table.createdTime();
            case "modifiedTime" -> table.modifiedTime();
            default -> null;
        };
    }
}
