package com.jezetek.modules.job.service;

import com.jezetek.modules.job.model.Fetchers;
import com.jezetek.modules.job.job.JobScheduler;
import com.jezetek.modules.job.model.SysJob;
import com.jezetek.modules.job.model.SysJobDraft;
import com.jezetek.modules.job.repository.JobRepository;
import com.jezetek.modules.job.service.dto.SysJobInput;
import com.jezetek.modules.job.service.dto.SysJobSpecification;
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
 * 定时任务管理接口。保存/启停后即时重排调度计划，
 * 立即执行为同步调用，便于前端提示执行结果
 */
@RestController
@RequestMapping("/api/v1/job")
@Transactional
public class JobService implements Fetchers {

    /**
     * 默认抓取形状：全部标量属性
     */
    private static final Fetcher<SysJob> DEFAULT_FETCHER =
            SYS_JOB_FETCHER.allScalarFields();

    private final JobRepository jobRepository;

    private final JobScheduler jobScheduler;

    public JobService(JobRepository jobRepository, JobScheduler jobScheduler) {
        this.jobRepository = jobRepository;
        this.jobScheduler = jobScheduler;
    }

    @PreAuthorize("@perm.has('job:list')")
    @GetMapping("/list/bySuperQBE")
    public Page<@NotNull @FetchBy("DEFAULT_FETCHER") SysJob> findJobsBySuperQBE(
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "5") int pageSize,
            @RequestParam(defaultValue = "id asc") String sortCode,
            SysJobSpecification specification
    ) {
        return jobRepository.find(
                PageRequest.of(pageIndex, pageSize, SortUtils.toSort(sortCode)),
                specification,
                DEFAULT_FETCHER
        );
    }

    @PreAuthorize("@perm.has('job:list')")
    @GetMapping("/{id}")
    @Nullable
    public @FetchBy("DEFAULT_FETCHER") SysJob findJob(@PathVariable("id") long id) {
        return jobRepository.findById(id, DEFAULT_FETCHER);
    }

    /**
     * 新增/编辑。调度中的任务 cron 被修改时即时重排；
     * cron 非法在保存前即报错
     */
    @PreAuthorize("@perm.hasAny('job:add', 'job:edit')")
    @PutMapping
    public @FetchBy("DEFAULT_FETCHER") SysJob saveJob(
            @Valid @RequestBody SysJobInput input
    ) {
        JobScheduler.validateCron(input.getCron());
        SysJob saved = jobRepository
                .saveCommand(input)
                .execute(DEFAULT_FETCHER)
                .getModifiedEntity();
        if (saved.status() == JobScheduler.STATUS_RUNNING) {
            jobScheduler.register(saved);
        } else {
            jobScheduler.unregister(saved.id());
        }
        return saved;
    }

    /** 启停切换：0 调度中 / 1 暂停 */
    @PreAuthorize("@perm.has('job:status')")
    @PutMapping("/{id}/status/{status}")
    public @Nullable @FetchBy("DEFAULT_FETCHER") SysJob changeStatus(
            @PathVariable("id") long id,
            @PathVariable("status") int status
    ) {
        if (status != JobScheduler.STATUS_RUNNING && status != JobScheduler.STATUS_PAUSED) {
            throw new IllegalArgumentException("非法状态: " + status);
        }
        SysJob current = jobRepository.findById(id, DEFAULT_FETCHER);
        if (current == null) {
            throw new IllegalArgumentException("任务不存在: " + id);
        }
        SysJob saved = jobRepository
                .saveCommand(SysJobDraft.$.produce(draft -> draft.setId(id).setStatus(status)))
                .execute(DEFAULT_FETCHER)
                .getModifiedEntity();
        if (status == JobScheduler.STATUS_RUNNING) {
            jobScheduler.register(saved);
        } else {
            jobScheduler.unregister(id);
        }
        return saved;
    }

    /** 立即执行一次(同步)，不受启停状态影响 */
    @PreAuthorize("@perm.has('job:run')")
    @PostMapping("/{id}/run")
    public void run(@PathVariable("id") long id) {
        SysJob job = jobRepository.findById(id, DEFAULT_FETCHER);
        if (job == null) {
            throw new IllegalArgumentException("任务不存在: " + id);
        }
        try {
            jobScheduler.runOnce(job);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("任务执行失败: " + e.getMessage(), e);
        }
    }

    @PreAuthorize("@perm.has('job:delete')")
    @DeleteMapping("/{id}")
    public void deleteJob(@PathVariable("id") long id) {
        jobScheduler.unregister(id);
        jobRepository.deleteById(id);
    }
}
