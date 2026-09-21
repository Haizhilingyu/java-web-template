package com.jezetek.modules.system.service;

import com.jezetek.modules.system.model.Fetchers;
import com.jezetek.modules.system.model.OperLog;
import com.jezetek.modules.system.repository.OperLogRepository;
import com.jezetek.core.runtime.log.Log;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.spring.model.SortUtils;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/*
 * 操作日志：记录由 OperLogAspect 异步写入，
 * 管理端提供分页查询与清空(与登录日志同款权限点 system:log:*)
 */
@RestController
@RequestMapping("/api/v1/operlog")
@Transactional
public class OperLogService implements Fetchers {

    private final OperLogRepository operLogRepository;

    public OperLogService(OperLogRepository operLogRepository) {
        this.operLogRepository = operLogRepository;
    }

    @PreAuthorize("@perm.has('system:log:list')")
    @GetMapping("/list/bySuperQBE")
    public Page<@NotNull @FetchBy("DEFAULT_FETCHER") OperLog> findOperLogsBySuperQBE(
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "id desc") String sortCode,
            @RequestParam(required = false) String keyword
    ) {
        return operLogRepository.find(
                PageRequest.of(pageIndex, pageSize, SortUtils.toSort(sortCode)),
                keyword == null || keyword.isBlank() ? null : "%" + keyword + "%",
                DEFAULT_FETCHER
        );
    }

    /**
     * 清空全部操作日志
     */
    @Log(module = "操作日志", action = "清空操作日志")
    @PreAuthorize("@perm.has('system:log:clear')")
    @DeleteMapping("/clear")
    public void clear() {
        operLogRepository.clearAll();
    }

    /**
     * 默认抓取形状：全部标量属性
     */
    private static final Fetcher<OperLog> DEFAULT_FETCHER =
            OPER_LOG_FETCHER
                    .allScalarFields();
}
