package com.jezetek.modules.system.service;

import com.jezetek.core.runtime.log.Log;
import com.jezetek.modules.system.model.Fetchers;
import com.jezetek.modules.system.model.Notice;
import com.jezetek.modules.system.repository.NoticeRepository;
import com.jezetek.modules.system.service.dto.NoticeInput;
import com.jezetek.modules.system.service.dto.NoticeSpecification;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.spring.model.SortUtils;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/*
 * 参照 jimmer-sql 示例的做法：富客户端时代 Controller 层意义弱化，
 * 直接把 web 注解放在 service 上，避免模板代码过度分层。
 * 公告类型消费字典 sys_notice_type(通知/公告)，前端用 useDict 渲染
 */
@RestController
@RequestMapping("/api/v1/notice")
@Transactional
public class NoticeService implements Fetchers {

    private final NoticeRepository noticeRepository;

    public NoticeService(NoticeRepository noticeRepository) {
        this.noticeRepository = noticeRepository;
    }

    @PreAuthorize("@perm.has('system:notice:list')")
    @GetMapping("/list/bySuperQBE")
    public Page<@NotNull @FetchBy("DEFAULT_FETCHER") Notice> findNoticesBySuperQBE(
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "5") int pageSize,
            @RequestParam(defaultValue = "id asc") String sortCode,
            NoticeSpecification specification
    ) {
        return noticeRepository.find(
                PageRequest.of(pageIndex, pageSize, SortUtils.toSort(sortCode)),
                specification,
                DEFAULT_FETCHER
        );
    }

    /**
     * 公告无自然业务键：NON_IDEMPOTENT_UPSERT 表示有 id 更新、无 id 插入
     */
    @Log(module = "公告管理", action = "保存公告")
    @PreAuthorize("@perm.hasAny('system:notice:add', 'system:notice:edit')")
    @PutMapping
    public @FetchBy("DEFAULT_FETCHER") Notice saveNotice(
            @Valid @RequestBody NoticeInput input
    ) {
        return noticeRepository
                .saveCommand(input)
                .setMode(org.babyfish.jimmer.sql.ast.mutation.SaveMode.NON_IDEMPOTENT_UPSERT)
                .execute(DEFAULT_FETCHER)
                .getModifiedEntity();
    }

    @Log(module = "公告管理", action = "删除公告")
    @PreAuthorize("@perm.has('system:notice:delete')")
    @DeleteMapping("/{id}")
    public void deleteNotice(@PathVariable("id") long id) {
        noticeRepository.deleteById(id);
    }

    /**
     * 默认抓取形状：全部标量属性(含 content，管理端弹窗直接展示)
     */
    private static final Fetcher<Notice> DEFAULT_FETCHER =
            NOTICE_FETCHER
                    .allScalarFields();
}
