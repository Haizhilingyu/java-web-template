package com.jezetek.modules.system.repository;

import com.jezetek.core.runtime.repository.PageOrders;
import com.jezetek.modules.system.model.User;
import com.jezetek.modules.system.model.UserTable;
import org.babyfish.jimmer.Specification;
import org.babyfish.jimmer.sql.ast.Expression;
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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository extends AbstractJavaRepository<User, Long> {

    private static final UserTable table = UserTable.$;

    public UserRepository(JSqlClient sql) {
        super(sql);
    }

    /**
     * 超级 QBE：Specification 描述动态查询条件，Fetcher 描述动态抓取形状，
     * Pageable 描述分页排序，三者均由调用方按需组装。
     *
     * @param treeDeptIds 部门树点选过滤(含子孙由调用方解析)；null 不过滤
     * @param scopedDeptIds 数据范围过滤(可见部门集合)；null 不过滤
     * @param selfUserId 仅本人过滤；null 不过滤
     */
    public Page<@NotNull User> find(
            Pageable pageable,
            Specification<User> specification,
            @Nullable Collection<Long> treeDeptIds,
            @Nullable Collection<Long> scopedDeptIds,
            @Nullable Long selfUserId,
            @Nullable Fetcher<User> fetcher
    ) {
        return sql
                .createQuery(table)
                .where(specification)
                .where(treeDeptIds == null ? null : table.deptId().in(treeDeptIds))
                .where(scopedDeptIds == null ? null : table.deptId().in(scopedDeptIds))
                .where(selfUserId == null ? null : table.id().eq(selfUserId))
                .orderBy(PageOrders.translate(pageable.getSort(), UserRepository::sortable))
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
            case "username" -> table.username();
            case "nickname" -> table.nickname();
            case "enabled" -> table.enabled();
            case "createdTime" -> table.createdTime();
            case "modifiedTime" -> table.modifiedTime();
            default -> null;
        };
    }

    /**
     * 全量查询不分页(工单03 导出)：同 find 的过滤形状，按用户名排序保证导出稳定
     */
    public List<User> listAll(
            Specification<User> specification,
            @Nullable Collection<Long> treeDeptIds,
            @Nullable Fetcher<User> fetcher
    ) {
        return sql
                .createQuery(table)
                .where(specification)
                .where(treeDeptIds == null ? null : table.deptId().in(treeDeptIds))
                .orderBy(PageOrders.translate(Sort.by("username"), UserRepository::sortable))
                .select(table.fetch(fetcher))
                .execute();
    }

    /**
     * 按角色 id 分页查已绑用户(工单06 分配用户)；keyword 对用户名/昵称模糊过滤
     */
    public Page<@NotNull User> findByRoleId(
            Pageable pageable,
            long roleId,
            @Nullable String keyword,
            @Nullable Fetcher<User> fetcher
    ) {
        return sql
                .createQuery(table)
                .where(table.roles(role -> role.id().eq(roleId)))
                .where(keyword == null || keyword.isBlank() ? null : Predicate.or(
                        table.username().like("%" + keyword + "%"),
                        table.nickname().like("%" + keyword + "%")))
                .orderBy(PageOrders.translate(pageable.getSort(), UserRepository::sortable))
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

    /**
     * 用户总数(首页统计，工单09)
     */
    public long countAll() {
        return sql
                .createQuery(table)
                .select(table.id().count())
                .execute()
                .stream()
                .findFirst()
                .orElse(0L);
    }

    /**
     * 是否存在挂在指定部门下的用户：删除部门前校验
     */
    public boolean existsByDeptId(long deptId) {
        return !sql
                .createQuery(table)
                .where(table.dept().id().eq(deptId))
                .select(table.id())
                .execute()
                .isEmpty();
    }

    /**
     * 是否有用户担任指定岗位：删除岗位前校验
     */
    public boolean existsByPostId(long postId) {
        return !sql
                .createQuery(table)
                .where(table.posts(post -> post.id().eq(postId)))
                .select(table.id())
                .execute()
                .isEmpty();
    }
}
