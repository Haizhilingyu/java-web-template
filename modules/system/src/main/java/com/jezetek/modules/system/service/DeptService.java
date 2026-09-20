package com.jezetek.modules.system.service;

import com.jezetek.core.runtime.BusinessException;
import com.jezetek.modules.system.model.Dept;
import com.jezetek.modules.system.model.Fetchers;
import com.jezetek.modules.system.repository.DeptRepository;
import com.jezetek.modules.system.repository.UserRepository;
import com.jezetek.modules.system.service.dto.DeptInput;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * 参照 jimmer-sql 示例的做法：富客户端时代 Controller 层意义弱化，
 * 直接把 web 注解放在 service 上，避免模板代码过度分层
 */
@RestController
@RequestMapping("/api/v1/dept")
@Transactional
public class DeptService implements Fetchers {

    private final DeptRepository deptRepository;

    private final UserRepository userRepository;

    public DeptService(DeptRepository deptRepository, UserRepository userRepository) {
        this.deptRepository = deptRepository;
        this.userRepository = userRepository;
    }

    /**
     * 部门树：只查根节点，子部门由递归 fetcher 抓取。
     * 用户表单的部门选择器等也要消费，任何登录用户可见(与菜单树一致)
     */
    @GetMapping("/list")
    public List<@FetchBy("TREE_FETCHER") Dept> findDepts() {
        return deptRepository.findRootDepts(TREE_FETCHER);
    }

    @PreAuthorize("@perm.has('system:dept:list')")
    @GetMapping("/{id}")
    @Nullable
    public @FetchBy("DEFAULT_FETCHER") Dept findDept(
            @PathVariable("id") long id
    ) {
        return deptRepository.findById(id, DEFAULT_FETCHER);
    }

    @PreAuthorize("@perm.hasAny('system:dept:add', 'system:dept:edit')")
    @PutMapping
    public @FetchBy("DEFAULT_FETCHER") Dept saveDept(
            @Valid @RequestBody DeptInput input
    ) {
        return deptRepository
                .saveCommand(input)
                .execute(DEFAULT_FETCHER)
                .getModifiedEntity();
    }

    /**
     * 生命周期约束：存在子部门或部门下有用户时禁止删除；
     * 禁用不追溯，已挂用户照常生效
     */
    @PreAuthorize("@perm.has('system:dept:delete')")
    @DeleteMapping("/{id}")
    public void deleteDept(@PathVariable("id") long id) {
        if (deptRepository.existsByParentId(id)) {
            throw new BusinessException("存在子部门，请先删除或调整子部门");
        }
        if (userRepository.existsByDeptId(id)) {
            throw new BusinessException("部门下存在用户，请先调整用户所属部门");
        }
        deptRepository.deleteById(id);
    }

    /**
     * 部门树抓取形状：全部标量属性 + 递归的子部门(子层重复同形状)
     */
    private static final Fetcher<Dept> TREE_FETCHER =
            DEPT_FETCHER
                    .allScalarFields()
                    .recursiveChildren();

    /**
     * 默认抓取形状：全部标量属性(不含子部门)
     */
    private static final Fetcher<Dept> DEFAULT_FETCHER =
            DEPT_FETCHER
                    .allScalarFields();
}
