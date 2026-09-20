export type PostDto = {
    /**
     * 默认抓取形状：全部标量属性
     */
    'PostService/DEFAULT_FETCHER': {
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
         * 岗位编码(如 CEO)，租户内唯一
         */
        readonly code: string;
        /**
         * 岗位名称
         */
        readonly name: string;
        /**
         * 排序号，越小越靠前
         */
        readonly sortOrder: number;
        /**
         * 是否启用，禁用岗位仅在用户表单等选择器中过滤，
         * 不追溯影响已担任该岗位的用户
         */
        readonly enabled: boolean;
    }
}
