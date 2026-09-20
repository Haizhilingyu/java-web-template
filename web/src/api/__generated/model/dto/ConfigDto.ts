export type ConfigDto = {
    /**
     * 默认抓取形状：全部标量属性
     */
    'ConfigService/DEFAULT_FETCHER': {
        /**
         * 代理主键，自增，无业务含义
         */
        readonly id: number;
        /**
         * The time when the object was created.
         * 
         * <p>In this example, this property is not
         * explicitly modified by business code,
         * but is automatically modified by {@code DraftInterceptor}</p>
         */
        readonly createdTime: string;
        /**
         * The time when the object was last modified
         * 
         * <p>In this example, this property is not
         * explicitly modified by business code,
         * but is automatically modified by {@code DraftInterceptor}</p>
         */
        readonly modifiedTime: string;
        /**
         * The tenant to which the current object belongs.
         * 
         * <p>For the database in this example,
         * there are two values: {@code a} and {code b}</p>.
         * 
         * <p>In this example, this property is not
         * explicitly modified by business code,
         * but is automatically modified by {@code DraftInterceptor}</p>
         */
        readonly tenant: string;
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
}
