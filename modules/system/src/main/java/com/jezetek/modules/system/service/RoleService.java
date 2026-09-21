package com.jezetek.modules.system.service;

import com.jezetek.core.runtime.log.Log;
import com.jezetek.modules.system.model.Fetchers;
import com.jezetek.modules.system.model.Role;
import com.jezetek.modules.system.repository.RoleRepository;
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

/*
 * 参照 jimmer-sql 示例的做法：富客户端时代 Controller 层意义弱化，
 * 直接把 web 注解放在 service 上，避免模板代码过度分层
 */
@RestController
@RequestMapping("/api/v1/role")
@Transactional
public class RoleService implements Fetchers {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
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

    @Log(module = "角色管理", action = "保存角色")
    @PreAuthorize("@perm.hasAny('system:role:add', 'system:role:edit')")
    @PutMapping
    public @FetchBy("DEFAULT_FETCHER") Role saveRole(
            @Valid @RequestBody RoleInput input
    ) {
        return roleRepository
                .saveCommand(input)
                .execute(DEFAULT_FETCHER)
                .getModifiedEntity();
    }

    @Log(module = "角色管理", action = "删除角色")
    @PreAuthorize("@perm.has('system:role:delete')")
    @DeleteMapping("/{id}")
    public void deleteRole(@PathVariable("id") long id) {
        roleRepository.deleteById(id);
    }

    /**
     * 默认抓取形状：Role 全部标量属性(不含 tenant) + 菜单的全部标量属性
     */
    private static final Fetcher<Role> DEFAULT_FETCHER =
            ROLE_FETCHER
                    .allScalarFields()
                    .tenant(false)
                    .menus(
                            MENU_FETCHER
                                    .allScalarFields()
                    );
}
