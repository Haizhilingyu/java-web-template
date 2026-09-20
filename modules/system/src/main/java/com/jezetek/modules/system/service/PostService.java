package com.jezetek.modules.system.service;

import com.jezetek.core.runtime.BusinessException;
import com.jezetek.modules.system.model.Fetchers;
import com.jezetek.modules.system.model.Post;
import com.jezetek.modules.system.repository.PostRepository;
import com.jezetek.modules.system.repository.UserRepository;
import com.jezetek.modules.system.service.dto.PostInput;
import com.jezetek.modules.system.service.dto.PostSpecification;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.spring.model.SortUtils;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.NotNull;
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
@RequestMapping("/api/v1/post")
@Transactional
public class PostService implements Fetchers {

    private final PostRepository postRepository;

    private final UserRepository userRepository;

    public PostService(PostRepository postRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @PreAuthorize("@perm.has('system:post:list')")
    @GetMapping("/list/bySuperQBE")
    public Page<@NotNull @FetchBy("DEFAULT_FETCHER") Post> findPostsBySuperQBE(
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "5") int pageSize,
            @RequestParam(defaultValue = "sortOrder asc") String sortCode,
            PostSpecification specification
    ) {
        return postRepository.find(
                PageRequest.of(pageIndex, pageSize, SortUtils.toSort(sortCode)),
                specification,
                DEFAULT_FETCHER
        );
    }

    @PreAuthorize("@perm.has('system:post:list')")
    @GetMapping("/{id}")
    @Nullable
    public @FetchBy("DEFAULT_FETCHER") Post findPost(
            @PathVariable("id") long id
    ) {
        return postRepository.findById(id, DEFAULT_FETCHER);
    }

    @PreAuthorize("@perm.hasAny('system:post:add', 'system:post:edit')")
    @PutMapping
    public @FetchBy("DEFAULT_FETCHER") Post savePost(
            @Valid @RequestBody PostInput input
    ) {
        return postRepository
                .saveCommand(input)
                .execute(DEFAULT_FETCHER)
                .getModifiedEntity();
    }

    /**
     * 生命周期约束：岗位被用户绑定时禁止删除；
     * 禁用不追溯，已担任该岗位的用户照常生效
     */
    @PreAuthorize("@perm.has('system:post:delete')")
    @DeleteMapping("/{id}")
    public void deletePost(@PathVariable("id") long id) {
        if (userRepository.existsByPostId(id)) {
            throw new BusinessException("岗位已被用户绑定，请先调整用户岗位");
        }
        postRepository.deleteById(id);
    }

    /**
     * 默认抓取形状：全部标量属性
     */
    private static final Fetcher<Post> DEFAULT_FETCHER =
            POST_FETCHER
                    .allScalarFields();
}
