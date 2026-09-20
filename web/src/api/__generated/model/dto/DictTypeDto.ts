export type DictTypeDto = {
    /**
     * 默认抓取形状：全部标量属性
     */
    'DictTypeService/DEFAULT_FETCHER': {
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
         * 字典编码(如 sys_yes_no)，全局唯一(business_key 约束不含租户列)，
         * 前端 useDict 按该编码取字典
         */
        readonly type: string;
        /**
         * 字典名称
         */
        readonly name: string;
        /**
         * 描述
         */
        readonly description?: string | undefined;
        /**
         * 是否启用，禁用字典的条目不再下发(type 接口过滤)
         */
        readonly enabled: boolean;
    }
}
