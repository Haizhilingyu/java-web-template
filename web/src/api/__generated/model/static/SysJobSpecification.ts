export interface SysJobSpecification {
    /**
     * 任务名，唯一
     */
    readonly keyword?: string | undefined;
    /**
     * 状态：0 正常(调度中) / 1 暂停
     */
    readonly status?: number | undefined;
}
