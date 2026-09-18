package com.jezetek.modules.system.repository;

import com.jezetek.modules.system.model.User;
import com.jezetek.modules.system.model.UserTable;
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
public class UserRepository extends AbstractJavaRepository<User, Long> {

    private static final UserTable table = UserTable.$;

    public UserRepository(JSqlClient sql) {
        super(sql);
    }

    /**
     * 超级 QBE：Specification 描述动态查询条件，Fetcher 描述动态抓取形状，
     * Pageable 描述分页排序，三者均由调用方按需组装
     */
    public Page<@NotNull User> find(
            Pageable pageable,
            Specification<User> specification,
            @Nullable Fetcher<User> fetcher
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
     * 登录场景：按用户名加载用户，可用 fetcher 一并抓取角色
     */
    public Optional<User> findByUsername(String username, @Nullable Fetcher<User> fetcher) {
        return sql
                .createQuery(table)
                .where(table.username().eq(username))
                .select(table.fetch(fetcher))
                .fetchOptional();
    }
}
