export type OperLogDto = {
    /**
     * 默认抓取形状：全部标量属性
     */
    'OperLogService/DEFAULT_FETCHER': {
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
         * 业务模块名，来自 @Log(module)
         */
        readonly module: string;
        /**
         * 动作名，来自 @Log(action)
         */
        readonly action: string;
        /**
         * 操作人账号，匿名操作为 anonymous
         */
        readonly operator: string;
        /**
         * 请求 URI
         */
        readonly uri?: string | undefined;
        /**
         * 入参 JSON(超长截断)
         */
        readonly params?: string | undefined;
        /**
         * 返回结果 JSON(超长截断)，失败时为空
         */
        readonly result?: string | undefined;
        /**
         * 异常信息(截断)，成功时为空
         */
        readonly errorMsg?: string | undefined;
        /**
         * 耗时(毫秒)
         */
        readonly costMs: number;
        /**
         * 是否成功
         */
        readonly success: boolean;
    }
}
