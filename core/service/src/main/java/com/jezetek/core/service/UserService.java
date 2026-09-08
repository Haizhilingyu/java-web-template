package com.jezetek.core.service;

import com.jezetek.core.model.Fetchers;
import com.jezetek.core.model.User;
import com.jezetek.core.repository.UserRepository;
import com.jezetek.core.service.dto.UserInput;
import com.jezetek.core.service.dto.UserSpecification;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.spring.model.SortUtils;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;
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
@RequestMapping("/user")
@Transactional
public class UserService implements Fetchers {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

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

    @GetMapping("/{id}")
    @Nullable
    public @FetchBy("DEFAULT_FETCHER") User findUser(
            @PathVariable("id") long id
    ) {
        return userRepository.findById(id, DEFAULT_FETCHER);
    }

    @GetMapping("/username/{username}")
    @Nullable
    public @FetchBy("DEFAULT_FETCHER") User findUserByUsername(
            @PathVariable("username") String username
    ) {
        return userRepository
                .findByUsername(username, DEFAULT_FETCHER)
                .orElse(null);
    }

    @PutMapping
    public @FetchBy("DEFAULT_FETCHER") User saveUser(
            @Valid @RequestBody UserInput input
    ) {
        return userRepository
                .saveCommand(input)
                .execute(DEFAULT_FETCHER)
                .getModifiedEntity();
    }

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
