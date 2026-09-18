export interface SysJobInput {
    /**
     * 代理主键，自增，无业务含义
     */
    readonly id?: number | undefined;
    /**
     * 任务名，唯一
     */
    readonly name: string;
    /**
     * cron 表达式(6 位，spring CronTrigger 语法)
     */
    readonly cron: string;
    /**
     * 处理器 bean 名，对应实现了 JobHandler 接口的 Spring Bean
     */
    readonly handler: string;
    /**
     * 传给处理器的参数
     */
    readonly param?: string | undefined;
    /**
     * 状态：0 正常(调度中) / 1 暂停
     */
    readonly status: number;
    /**
     * 备注
     */
    readonly memo?: string | undefined;
}
