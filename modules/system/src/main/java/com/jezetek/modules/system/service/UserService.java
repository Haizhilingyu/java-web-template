package com.jezetek.modules.system.service;

import com.jezetek.modules.system.model.Fetchers;
import com.jezetek.modules.system.model.User;
import com.jezetek.modules.system.model.UserDraft;
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

    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PreAuthorize("@perm.has('system:user:list')")
    @GetMapping("/list/bySuperQBE")
    public Page<@FetchBy("DEFAULT_FETCHER") User> findUsersBySuperQBE(
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "5") int pageSize,
            // sortCode 支持隐式关联排序，如 `roles.code asc`
            @RequestParam(defaultValue = "username asc") String sortCode,
            UserSpecification specification
    ) {
        return userRepository.find(
                PageRequest.of(pageIndex, pageSize, SortUtils.toSort(sortCode)),
                specification,
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
     * 会把库里原值覆盖为 NULL，因此这里手工组装 draft 控制属性的加载态
     */
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
            if (input.getRoleIds() != null) {
                draft.setRoleIds(input.getRoleIds());
            }
        });
        return userRepository
                .saveCommand(entity)
                .execute(DEFAULT_FETCHER)
                .getModifiedEntity();
    }

    @PreAuthorize("@perm.has('system:user:delete')")
    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable("id") long id) {
        userRepository.deleteById(id);
    }

    /**
     * 默认抓取形状：User 全部标量属性(不含 tenant) + 角色的全部标量属性(不含 tenant)
     */
    private static final Fetcher<User> DEFAULT_FETCHER =
            USER_FETCHER
                    .allScalarFields()
                    .tenant(false)
                    .roles(
                            ROLE_FETCHER
                                    .allScalarFields()
                                    .tenant(false)
                    );
}
