package com.jezetek.modules.system.repository;

import com.jezetek.modules.system.model.Role;
import com.jezetek.modules.system.model.RoleTable;
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

import java.util.Optional;

@Repository
public class RoleRepository extends AbstractJavaRepository<Role, Long> {

    private static final RoleTable table = RoleTable.$;

    public RoleRepository(JSqlClient sql) {
        super(sql);
    }

    /**
     * 超级 QBE：Specification 描述动态查询条件，Fetcher 描述动态抓取形状，
     * Pageable 描述分页排序，三者均由调用方按需组装
     */
    public Page<@NotNull Role> find(
            Pageable pageable,
            Specification<Role> specification,
            @Nullable Fetcher<Role> fetcher
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
     * 按角色编码加载角色，可用 fetcher 一并抓取菜单
     */
    public Optional<Role> findByCode(String code, @Nullable Fetcher<Role> fetcher) {
        return sql
                .createQuery(table)
                .where(table.code().eq(code))
                .select(table.fetch(fetcher))
                .fetchOptional();
    }
}
