package com.jezetek.modules.system.repository;

import com.jezetek.modules.system.model.Dept;
import com.jezetek.modules.system.model.DeptTable;
import org.babyfish.jimmer.spring.repo.support.AbstractJavaRepository;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Repository;

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
}
