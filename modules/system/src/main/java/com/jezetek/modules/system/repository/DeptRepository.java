package com.jezetek.modules.system.repository;

import com.jezetek.modules.system.model.Dept;
import com.jezetek.modules.system.model.DeptTable;
import org.babyfish.jimmer.spring.repo.support.AbstractJavaRepository;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class DeptRepository extends AbstractJavaRepository<Dept, Long> {

    private static final DeptTable table = DeptTable.$;

    public DeptRepository(JSqlClient sql) {
        super(sql);
    }

    /**
     * 加载全部根部门，子部门用递归 fetcher 抓取，例如：
     * {@code DeptFetcher.$.allScalarFields().recursiveChildren()}
     */
    public List<Dept> findRootDepts(@Nullable Fetcher<Dept> fetcher) {
        return sql
                .createQuery(table)
                .where(table.parentId().isNull())
                .orderBy(table.sortOrder().asc(), table.id().asc())
                .select(table.fetch(fetcher))
                .execute();
    }

    /**
     * 是否存在子部门：删除部门前校验
     */
    public boolean existsByParentId(long parentId) {
        return !sql
                .createQuery(table)
                .where(table.parentId().eq(parentId))
                .select(table.id())
                .execute()
                .isEmpty();
    }

    /**
     * 指定部门及其全部子孙部门的 id：用户列表按部门筛选(含子孙)用。
     * 逐层下探，部门树深度有限，层数即查询次数
     */
    public List<Long> findSelfAndDescendantIds(long rootId) {
        List<Long> ids = new ArrayList<>();
        List<Long> frontier = List.of(rootId);
        while (!frontier.isEmpty()) {
            ids.addAll(frontier);
            frontier = sql
                    .createQuery(table)
                    .where(table.parentId().in(frontier))
                    .select(table.id())
                    .execute();
        }
        return ids;
    }
}
