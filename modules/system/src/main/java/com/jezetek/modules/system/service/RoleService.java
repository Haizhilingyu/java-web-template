package com.jezetek.modules.system.service;

import com.jezetek.core.runtime.log.Log;
import com.jezetek.modules.system.model.User;
import com.jezetek.modules.system.model.Fetchers;
import com.jezetek.modules.system.model.Role;
import com.jezetek.modules.system.model.RoleDraft;
import com.jezetek.modules.system.model.UserDraft;
import com.jezetek.modules.system.repository.RoleRepository;
import com.jezetek.modules.system.repository.UserRepository;
import com.jezetek.modules.system.service.dto.RoleInput;
import com.jezetek.modules.system.service.dto.RoleSpecification;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.spring.model.SortUtils;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * 参照 jimmer-sql 示例的做法：富客户端时代 Controller 层意义弱化，
 * 直接把 web 注解放在 service 上，避免模板代码过度分层
 */
@RestController
@RequestMapping("/api/v1/role")
@Transactional
public class RoleService implements Fetchers {

    private final RoleRepository roleRepository;

    private final UserRepository userRepository;

    public RoleService(RoleRepository roleRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @PreAuthorize("@perm.has('system:role:list')")
    @GetMapping("/list/bySuperQBE")
    public Page<@FetchBy("DEFAULT_FETCHER") Role> findRolesBySuperQBE(
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "5") int pageSize,
            @RequestParam(defaultValue = "code asc") String sortCode,
            RoleSpecification specification
    ) {
        return roleRepository.find(
                PageRequest.of(pageIndex, pageSize, SortUtils.toSort(sortCode)),
                specification,
                DEFAULT_FETCHER
        );
    }

    @PreAuthorize("@perm.has('system:role:list')")
    @GetMapping("/{id}")
    @Nullable
    public @FetchBy("DEFAULT_FETCHER") Role findRole(
            @PathVariable("id") long id
    ) {
        return roleRepository.findById(id, DEFAULT_FETCHER);
    }

    /**
     * 手工组装 draft(同 UserService.saveUser 的理由)：Input 的
     * saveCommand/toEntity 对未提交属性无条件写 null；集合 id 视图的
     * "是否提交"读字段判断(getter 懒初始化)。dataScope 未提交默认 1(全部)
     */
    @Log(module = "角色管理", action = "保存角色")
    @PreAuthorize("@perm.hasAny('system:role:add', 'system:role:edit')")
    @PutMapping
    public @FetchBy("DEFAULT_FETCHER") Role saveRole(
            @Valid @RequestBody RoleInput input
    ) {
        Role entity = RoleDraft.$.produce(draft -> {
            if (input.getId() != null) {
                draft.setId(input.getId());
            }
            draft.setCode(input.getCode());
            draft.setName(input.getName());
            if (input.getDescription() != null) {
                draft.setDescription(input.getDescription());
            }
            Integer dataScope = InputFields.intValue(RoleInput.class, input, "dataScope");
            draft.setDataScope(dataScope == null ? 1 : dataScope);
            if (InputFields.isProvided(RoleInput.class, input, "menuIds")) {
                draft.setMenuIds(input.getMenuIds());
            }
            if (InputFields.isProvided(RoleInput.class, input, "customDeptIds")) {
                draft.setCustomDeptIds(input.getCustomDeptIds());
            }
        });
        return roleRepository
                .saveCommand(entity)
                .execute(DEFAULT_FETCHER)
                .getModifiedEntity();
    }

    /**
     * 分配用户(工单06)：已绑用户的分页列表，keyword 对用户名/昵称模糊过滤。
     * 权限沿用 system:role:edit
     */
    @PreAuthorize("@perm.has('system:role:edit')")
    @GetMapping("/{id}/users")
    public Page<@FetchBy("ROLE_USER_FETCHER") User> findRoleUsers(
            @PathVariable("id") long id,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "5") int pageSize,
            @RequestParam(required = false) String keyword
    ) {
        return userRepository.findByRoleId(
                PageRequest.of(pageIndex, pageSize, SortUtils.toSort("username asc")),
                id,
                keyword,
                ROLE_USER_FETCHER
        );
    }

    /**
     * 批量授权：把 userIds 加入该角色(已绑定的跳过，幂等)。
     * 手工组 draft 读改 roleIds——Input.toEntity 会把未提交集合写空
     */
    @Log(module = "角色管理", action = "分配用户")
    @PreAuthorize("@perm.has('system:role:edit')")
    @PostMapping("/{id}/users")
    public void assignUsers(
            @PathVariable("id") long id,
            @Valid @RequestBody RoleUserIdsRequest request
    ) {
        requireRoleExists(id);
        for (long userId : distinct(request.userIds())) {
            User user = userRepository.findById(
                    userId, USER_FETCHER.username().roleIds());
            if (user == null) {
                throw new com.jezetek.core.runtime.BusinessException("用户不存在: " + userId);
            }
            if (user.roleIds().contains(id)) {
                continue;
            }
            java.util.List<Long> merged = new java.util.ArrayList<>(user.roleIds());
            merged.add(id);
            User entity = UserDraft.$.produce(
                    user, draft -> draft.setRoleIds(merged));
            userRepository.saveCommand(entity)
                    .setMode(org.babyfish.jimmer.sql.ast.mutation.SaveMode.NON_IDEMPOTENT_UPSERT)
                    .execute();
        }
    }

    /**
     * 批量取消授权：把 userIds 移出该角色(未绑定的跳过，幂等)
     */
    @Log(module = "角色管理", action = "取消分配用户")
    @PreAuthorize("@perm.has('system:role:edit')")
    @org.springframework.web.bind.annotation.DeleteMapping("/{id}/users")
    public void unassignUsers(
            @PathVariable("id") long id,
            @Valid @RequestBody RoleUserIdsRequest request
    ) {
        requireRoleExists(id);
        for (long userId : distinct(request.userIds())) {
            User user = userRepository.findById(
                    userId, USER_FETCHER.username().roleIds());
            if (user == null || !user.roleIds().contains(id)) {
                continue;
            }
            java.util.List<Long> remaining = user.roleIds().stream()
                    .filter(roleId -> roleId != id)
                    .toList();
            User entity = UserDraft.$.produce(
                    user, draft -> draft.setRoleIds(remaining));
            userRepository.saveCommand(entity)
                    .setMode(org.babyfish.jimmer.sql.ast.mutation.SaveMode.NON_IDEMPOTENT_UPSERT)
                    .execute();
        }
    }

    private void requireRoleExists(long id) {
        if (!roleRepository.existsById(id)) {
            throw new com.jezetek.core.runtime.BusinessException("角色不存在: " + id);
        }
    }

    private static List<Long> distinct(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new com.jezetek.core.runtime.BusinessException("请选择用户");
        }
        return userIds.stream().distinct().toList();
    }

    @Log(module = "角色管理", action = "删除角色")
    @PreAuthorize("@perm.has('system:role:delete')")
    @DeleteMapping("/{id}")
    public void deleteRole(@PathVariable("id") long id) {
        roleRepository.deleteById(id);
    }

    /**
     * 默认抓取形状：Role 全部标量属性(不含 tenant) + 菜单的全部标量属性
     * + 自定义数据范围部门(仅 id)
     */
    private static final Fetcher<Role> DEFAULT_FETCHER =
            ROLE_FETCHER
                    .allScalarFields()
                    .tenant(false)
                    .menus(
                            MENU_FETCHER
                                    .allScalarFields()
                    )
                    .customDepts(DEPT_FETCHER);

    /**
     * 分配用户列表的用户抓取形状(工单06)：用户/部门/岗位标量，不抓角色
     */
    private static final Fetcher<User> ROLE_USER_FETCHER =
            USER_FETCHER
                    .username()
                    .nickname()
                    .enabled()
                    .createdTime()
                    .dept(DEPT_FETCHER.name())
                    .posts(POST_FETCHER.name());
}
