package com.jezetek.modules.system.service;

import com.jezetek.core.runtime.log.Log;
import com.jezetek.core.runtime.security.DataScope;
import com.jezetek.core.runtime.security.SecurityUtils;
import com.jezetek.modules.system.model.Fetchers;
import com.jezetek.modules.system.model.User;
import com.jezetek.modules.system.model.UserDraft;
import com.jezetek.modules.system.repository.DeptRepository;
import com.jezetek.modules.system.repository.UserRepository;
import com.jezetek.modules.system.service.dto.UserInput;
import com.jezetek.modules.system.service.dto.UserSpecification;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.spring.model.SortUtils;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

/*
 * 参照 jimmer-sql 示例的做法：富客户端时代 Controller 层意义弱化，
 * 直接把 web 注解放在 service 上，避免模板代码过度分层。
 * 接口权限统一用 @PreAuthorize("@perm.has('模块:实体:动作')")，
 * ADMIN 角色(*:*:*)在 PermissionChecker 中直通
 */
@RestController
@RequestMapping("/api/v1/user")
@Transactional
public class UserService implements Fetchers {

    private final UserRepository userRepository;

    private final DeptRepository deptRepository;

    private final PasswordEncoder passwordEncoder;

    private final com.jezetek.modules.system.security.PasswordManager passwordManager;

    public UserService(
            UserRepository userRepository,
            DeptRepository deptRepository,
            PasswordEncoder passwordEncoder,
            com.jezetek.modules.system.security.PasswordManager passwordManager
    ) {
        this.userRepository = userRepository;
        this.deptRepository = deptRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordManager = passwordManager;
    }

    @PreAuthorize("@perm.has('system:user:list')")
    @GetMapping("/list/bySuperQBE")
    public Page<@FetchBy("DEFAULT_FETCHER") User> findUsersBySuperQBE(
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "5") int pageSize,
            // sortCode 支持隐式关联排序，如 `roles.code asc`
            @RequestParam(defaultValue = "username asc") String sortCode,
            UserSpecification specification,
            // 部门树点选筛选：按该部门及其全部子孙过滤，缺省不过滤
            @RequestParam(required = false) Long deptId
    ) {
        Collection<Long> treeDeptIds = deptId == null ? null : deptRepository.findSelfAndDescendantIds(deptId);
        // 数据范围生效点(本批仅此一处，显式调用而非全局过滤器，避免误伤
        // username 查重等必须全量可见的查询)：
        // 超管/全部返回 null；CUSTOM=精确勾选集合；DEPT=仅本部门；
        // DEPT_AND_CHILD=本部门+全部子孙；SELF=仅本人(user id 条件)
        DataScope scope = DataScope.current();
        if (scope == null) {
            return userRepository.find(
                    PageRequest.of(pageIndex, pageSize, SortUtils.toSort(sortCode)),
                    specification,
                    treeDeptIds,
                    null,
                    null,
                    DEFAULT_FETCHER
            );
        }
        Set<Long> scopedDeptIds = switch (scope.level()) {
            case ALL -> null;
            case CUSTOM -> scope.deptIds();
            case DEPT -> scope.selfDeptId() == null ? Set.<Long>of() : Set.of(scope.selfDeptId());
            case DEPT_AND_CHILD -> scope.selfDeptId() == null
                    ? Set.<Long>of()
                    : new HashSet<>(deptRepository.findSelfAndDescendantIds(scope.selfDeptId()));
            case SELF -> null;
        };
        Long selfUserId = scope.level() == DataScope.Level.SELF
                ? SecurityUtils.currentLoginUser().id()
                : null;
        return userRepository.find(
                PageRequest.of(pageIndex, pageSize, SortUtils.toSort(sortCode)),
                specification,
                treeDeptIds,
                scopedDeptIds,
                selfUserId,
                DEFAULT_FETCHER
        );
    }

    @PreAuthorize("@perm.has('system:user:list')")
    @GetMapping("/{id}")
    @Nullable
    public @FetchBy("DEFAULT_FETCHER") User findUser(
            @PathVariable("id") long id
    ) {
        return userRepository.findById(id, DEFAULT_FETCHER);
    }

    @PreAuthorize("@perm.has('system:user:list')")
    @GetMapping("/username/{username}")
    @Nullable
    public @FetchBy("DEFAULT_FETCHER") User findUserByUsername(
            @PathVariable("username") String username
    ) {
        return userRepository
                .findByUsername(username, DEFAULT_FETCHER)
                .orElse(null);
    }

    /**
     * password 属性未提交(更新场景)时保持原值；
     * 提交了明文则落库前 BCrypt 加密。
     * 不能用 input.toEntity()：dto 生成的映射对未提交属性无条件 set null，
     * 会把库里原值覆盖为 NULL，因此这里手工组装 draft 控制属性的加载态。
     *
     * <p>集合 id 视图(postIds/roleIds)的"是否提交"必须读字段而非 getter：
     * 生成的集合 getter 懒初始化空列表(永远 != null)，未提交也会被当成
     * "提交了空列表"而清空关联；提交了空列表则显式清空</p>
     */
    @Log(module = "用户管理", action = "保存用户")
    @PreAuthorize("@perm.hasAny('system:user:add', 'system:user:edit')")
    @PutMapping
    public @FetchBy("DEFAULT_FETCHER") User saveUser(
            @Valid @RequestBody UserInput input
    ) {
        User entity = UserDraft.$.produce(draft -> {
            if (input.getId() != null) {
                draft.setId(input.getId());
            }
            draft.setUsername(input.getUsername());
            if (input.getPassword() != null && !input.getPassword().isBlank()) {
                draft.setPassword(passwordEncoder.encode(input.getPassword()));
            }
            if (input.getNickname() != null) {
                draft.setNickname(input.getNickname());
            }
            draft.setEnabled(input.isEnabled());
            if (input.getDeptId() != null) {
                draft.setDeptId(input.getDeptId());
            }
            if (InputFields.isProvided(UserInput.class, input, "postIds")) {
                draft.setPostIds(input.getPostIds());
            }
            if (InputFields.isProvided(UserInput.class, input, "roleIds")) {
                draft.setRoleIds(input.getRoleIds());
            }
        });
        return userRepository
                .saveCommand(entity)
                .execute(DEFAULT_FETCHER)
                .getModifiedEntity();
    }

    @Log(module = "用户管理", action = "删除用户")
    @PreAuthorize("@perm.has('system:user:delete')")
    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable("id") long id) {
        userRepository.deleteById(id);
    }

    /**
     * 管理员重置密码(工单05)：无需旧密码，按 system:user:resetPwd 授权；
     * 落库+作废目标用户全部会话走 PasswordManager 共用通道(与改密同语义)
     */
    @Log(module = "用户管理", action = "重置密码")
    @PreAuthorize("@perm.has('system:user:resetPwd')")
    @PutMapping("/{id}/password")
    public void resetPassword(
            @PathVariable("id") long id,
            @Valid @RequestBody com.jezetek.modules.system.security.AuthModels.ResetPasswordRequest request
    ) {
        User user = userRepository.findById(id, USER_FETCHER.username());
        if (user == null) {
            throw new com.jezetek.core.runtime.BusinessException("用户不存在");
        }
        passwordManager.updatePasswordAndRevokeSessions(id, passwordEncoder.encode(request.newPassword()));
    }

    /**
     * 默认抓取形状：User 全部标量属性(不含 tenant)
     * + 角色/部门/岗位的全部标量属性(不含 tenant)
     */
    private static final Fetcher<User> DEFAULT_FETCHER =
            USER_FETCHER
                    .allScalarFields()
                    .avatar(false)
                    .tenant(false)
                    .roles(
                            ROLE_FETCHER
                                    .allScalarFields()
                                    .tenant(false)
                    )
                    .dept(
                            DEPT_FETCHER
                                    .allScalarFields()
                                    .tenant(false)
                    )
                    .posts(
                            POST_FETCHER
                                    .allScalarFields()
                                    .tenant(false)
                    );
}
