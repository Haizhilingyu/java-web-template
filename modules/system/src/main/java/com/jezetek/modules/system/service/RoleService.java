package com.jezetek.modules.system.service;

import com.jezetek.core.runtime.log.Log;
import com.jezetek.modules.system.model.Fetchers;
import com.jezetek.modules.system.model.Role;
import com.jezetek.modules.system.model.RoleDraft;
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
            Integer dataScope = intField(input, "dataScope");
            draft.setDataScope(dataScope == null ? 1 : dataScope);
            if (isProvided(input, "menuIds")) {
                draft.setMenuIds(input.getMenuIds());
            }
            if (isProvided(input, "customDeptIds")) {
                draft.setCustomDeptIds(input.getCustomDeptIds());
            }
        });
        return roleRepository
                .saveCommand(entity)
                .execute(DEFAULT_FETCHER)
                .getModifiedEntity();
    }

    /**
     * 读取 Input 的标量字段原始值(绕开"未提交即抛异常"的生成 getter)
     */
    private static Integer intField(RoleInput input, String field) {
        try {
            java.lang.reflect.Field f = RoleInput.class.getDeclaredField(field);
            f.setAccessible(true);
            return (Integer) f.get(input);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("RoleInput 缺少字段: " + field, e);
        }
    }

    /**
     * 判断 dto 集合属性是否被客户端提交(字段是否被 setter 写过)
     */
    private static boolean isProvided(RoleInput input, String field) {
        try {
            java.lang.reflect.Field f = RoleInput.class.getDeclaredField(field);
            f.setAccessible(true);
            return f.get(input) != null;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("RoleInput 缺少字段: " + field, e);
        }
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
}
