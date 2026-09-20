export type DictDataDto = {
    /**
     * 默认抓取形状：全部标量属性(不含字典类型)
     */
    'DictDataService/DEFAULT_FETCHER': {
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
         * 显示标签(如"是")
         */
        readonly label: string;
        /**
         * 存储值(如"Y")，与 dictType 组成业务键，前端渲染与提交都用它。
         * 列名避开 H2 保留字 VALUE
         */
        readonly value: string;
        /**
         * 同字典内排序号，越小越靠前
         */
        readonly sortOrder: number;
        /**
         * 是否启用，禁用条目不再下发且不在选择器中出现
         */
        readonly enabled: boolean;
    }, 
    /**
     * 下发条目的抓取形状：全部标量属性
     */
    'DictDataService/ITEM_FETCHER': {
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
         * 显示标签(如"是")
         */
        readonly label: string;
        /**
         * 存储值(如"Y")，与 dictType 组成业务键，前端渲染与提交都用它。
         * 列名避开 H2 保留字 VALUE
         */
        readonly value: string;
        /**
         * 同字典内排序号，越小越靠前
         */
        readonly sortOrder: number;
        /**
         * 是否启用，禁用条目不再下发且不在选择器中出现
         */
        readonly enabled: boolean;
    }
}
