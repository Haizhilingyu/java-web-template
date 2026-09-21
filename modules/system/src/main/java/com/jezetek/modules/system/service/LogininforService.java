package com.jezetek.modules.system.service;

import com.jezetek.modules.system.model.Fetchers;
import com.jezetek.modules.system.model.Logininfor;
import com.jezetek.modules.system.repository.LogininforRepository;
import com.jezetek.core.runtime.log.Log;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.spring.model.SortUtils;
import org.jetbrains.annotations.NotNull;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/*
 * 登录日志(最小复现：GET + FetchBy)
 */
@RestController
@RequestMapping("/api/v1/logininfor")
@Transactional
public class LogininforService implements Fetchers {

    private final LogininforRepository logininforRepository;

    public LogininforService(LogininforRepository logininforRepository) {
        this.logininforRepository = logininforRepository;
    }

    private static final Fetcher<Logininfor> DEFAULT_FETCHER =
            LOGININFOR_FETCHER
                    .allScalarFields();

    @PreAuthorize("@perm.has('system:log:list')")
    @GetMapping("/list/bySuperQBE")
    public Page<@NotNull @FetchBy("DEFAULT_FETCHER") Logininfor> findLogininforsBySuperQBE(
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "id desc") String sortCode,
            @RequestParam(required = false) String keyword
    ) {
        return logininforRepository.find(
                PageRequest.of(pageIndex, pageSize, SortUtils.toSort(sortCode)),
                keyword == null || keyword.isBlank() ? null : "%" + keyword + "%",
                DEFAULT_FETCHER
        );
    }

    @Log(module = "登录日志", action = "清空登录日志")
    @PreAuthorize("@perm.has('system:log:clear')")
    @DeleteMapping("/clear")
    public void clear() {
        logininforRepository.clearAll();
    }
}
