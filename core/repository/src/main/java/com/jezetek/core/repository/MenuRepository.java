package com.jezetek.core.repository;

import com.jezetek.core.model.Menu;
import com.jezetek.core.model.MenuTable;
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

import java.util.Collection;
import java.util.List;

@Repository
public class MenuRepository extends AbstractJavaRepository<Menu, Long> {

    private static final MenuTable table = MenuTable.$;

    public MenuRepository(JSqlClient sql) {
        super(sql);
    }

    /**
     * 超级 QBE：Specification 描述动态查询条件，Fetcher 描述动态抓取形状，
     * Pageable 描述分页排序，三者均由调用方按需组装
     */
    public Page<@NotNull Menu> find(
            Pageable pageable,
            Specification<Menu> specification,
            @Nullable Fetcher<Menu> fetcher
    ) {
        return sql
                .createQuery(table)
                .where(specification)
                .select(table.fetch(fetcher))
                .fetchPage(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        SpringPageFactory.getInstance()
                );
    }

    /**
     * 加载全部根菜单，子菜单用递归 fetcher 抓取，例如：
     * {@code MenuFetcher.$.allScalarFields().recursiveChildren()}
     */
    public List<Menu> findRootMenus(@Nullable Fetcher<Menu> fetcher) {
        return sql
                .createQuery(table)
                .where(table.parentId().isNull())
                .orderBy(table.sortOrder().asc(), table.id().asc())
                .select(table.fetch(fetcher))
                .execute();
    }

    /**
     * 加载指定角色集合可访问的菜单(exists 子查询，不会产生重复行)，
     * 用于组装当前登录用户可见的菜单树
     */
    public List<Menu> findByRoleIds(Collection<Long> roleIds, @Nullable Fetcher<Menu> fetcher) {
        return sql
                .createQuery(table)
                .where(table.roles(role -> role.id().in(roleIds)))
                .orderBy(table.sortOrder().asc(), table.id().asc())
                .select(table.fetch(fetcher))
                .execute();
    }
}
