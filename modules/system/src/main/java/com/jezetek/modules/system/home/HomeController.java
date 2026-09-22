package com.jezetek.modules.system.home;

import com.jezetek.core.runtime.BusinessException;
import com.jezetek.modules.system.model.Notice;
import com.jezetek.modules.system.repository.LogininforRepository;
import com.jezetek.modules.system.repository.NoticeRepository;
import com.jezetek.modules.system.repository.OperLogRepository;
import com.jezetek.modules.system.repository.RoleRepository;
import com.jezetek.modules.system.repository.UserRepository;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 首页轻量版(工单09)：仅 authenticated、无权限点要求——
 * demo/USER 也能看到统计与启用公告(公告管理接口本身仍有权限点)。
 * apiPrefixes 已声明 /api/v1/home/**(SystemModuleProvider)
 */
@RestController
@RequestMapping("/api/v1/home")
@Transactional
public class HomeController {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final LogininforRepository logininforRepository;

    private final OperLogRepository operLogRepository;

    private final NoticeRepository noticeRepository;

    public HomeController(
            UserRepository userRepository,
            RoleRepository roleRepository,
            LogininforRepository logininforRepository,
            OperLogRepository operLogRepository,
            NoticeRepository noticeRepository
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.logininforRepository = logininforRepository;
        this.operLogRepository = operLogRepository;
        this.noticeRepository = noticeRepository;
    }

    /** 统计卡：用户数/角色数/今日登录成功次数/操作日志总数 */
    @GetMapping("/summary")
    public HomeSummary summary() {
        return new HomeSummary(
                userRepository.countAll(),
                roleRepository.countAll(),
                logininforRepository.countTodaySuccess(),
                operLogRepository.countAll()
        );
    }

    /** 公告列表卡：启用中的最新 5 条，标题级字段 */
    @GetMapping("/notices")
    public List<NoticeBrief> notices() {
        return noticeRepository.findEnabledTop5(NOTICE_BRIEF_FETCHER).stream()
                .map(notice -> new NoticeBrief(
                        notice.id(),
                        notice.noticeTitle(),
                        notice.noticeType(),
                        notice.createdTime()))
                .toList();
    }

    /** 公告详情：启用才可见(含 content 富文本)，否则 400 */
    @GetMapping("/notices/{id}")
    public NoticeDetail notice(@PathVariable("id") long id) {
        Notice notice = noticeRepository.findEnabledById(id, NOTICE_DETAIL_FETCHER);
        if (notice == null) {
            throw new BusinessException("公告不存在或未启用");
        }
        return new NoticeDetail(
                notice.id(),
                notice.noticeTitle(),
                notice.noticeType(),
                notice.content(),
                notice.createdTime()
        );
    }

    private static final Fetcher<Notice> NOTICE_BRIEF_FETCHER =
            com.jezetek.modules.system.model.Fetchers.NOTICE_FETCHER
                    .noticeTitle()
                    .noticeType()
                    .createdTime();

    private static final Fetcher<Notice> NOTICE_DETAIL_FETCHER =
            com.jezetek.modules.system.model.Fetchers.NOTICE_FETCHER
                    .noticeTitle()
                    .noticeType()
                    .content()
                    .createdTime();

    /** 首页统计 */
    public record HomeSummary(
            long userCount,
            long roleCount,
            long todayLoginSuccess,
            long operLogTotal
    ) {
    }

    /** 公告标题级字段 */
    public record NoticeBrief(long id, String noticeTitle, String noticeType, java.time.LocalDateTime createdTime) {
    }

    /** 公告详情(含富文本 content) */
    public record NoticeDetail(
            long id,
            String noticeTitle,
            String noticeType,
            @org.jetbrains.annotations.Nullable String content,
            java.time.LocalDateTime createdTime
    ) {
    }
}
