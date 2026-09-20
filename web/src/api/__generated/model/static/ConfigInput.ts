export interface ConfigInput {
    /**
     * 代理主键，自增，无业务含义
     */
    readonly id?: number | undefined;
    /**
     * 参数键(如 site.title)，全局唯一(business_key 约束不含租户列)，
     * 按 GET /api/v1/config/configKey/{key} 消费
     */
    readonly configKey: string;
    /**
     * 参数名称
     */
    readonly configName: string;
    /**
     * 参数值
     */
    readonly configValue?: string | undefined;
    /**
     * 备注
     */
    readonly remark?: string | undefined;
}
