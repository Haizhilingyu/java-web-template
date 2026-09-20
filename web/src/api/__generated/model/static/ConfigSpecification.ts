export interface ConfigSpecification {
    /**
     * 参数键(如 site.title)，全局唯一(business_key 约束不含租户列)，
     * 按 GET /api/v1/config/configKey/{key} 消费
     */
    readonly keyword?: string | undefined;
}
