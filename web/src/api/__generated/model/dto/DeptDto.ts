export type DeptDto = {
    /**
     * 默认抓取形状：全部标量属性(不含子部门)
     */
    'DeptService/DEFAULT_FETCHER': {
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
         * 部门名，与 parent 组成组合键，同一父节点下不允许重名
         */
        readonly name: string;
        /**
         * 同级排序号，越小越靠前
         */
        readonly sortOrder: number;
        /**
         * 是否启用，禁用部门仅在用户表单等选择器中过滤，
         * 不追溯影响已挂在部门下的用户
         */
        readonly enabled: boolean;
        /**
         * 负责人，自由文本，不引用用户表
         */
        readonly leader?: string | undefined;
        /**
         * 联系电话
         */
        readonly phone?: string | undefined;
        /**
         * 邮箱
         */
        readonly email?: string | undefined;
    }, 
    /**
     * 部门树抓取形状：全部标量属性 + 递归的子部门(子层重复同形状)
     */
    'DeptService/TREE_FETCHER': {
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
         * 部门名，与 parent 组成组合键，同一父节点下不允许重名
         */
        readonly name: string;
        /**
         * 同级排序号，越小越靠前
         */
        readonly sortOrder: number;
        /**
         * 是否启用，禁用部门仅在用户表单等选择器中过滤，
         * 不追溯影响已挂在部门下的用户
         */
        readonly enabled: boolean;
        /**
         * 负责人，自由文本，不引用用户表
         */
        readonly leader?: string | undefined;
        /**
         * 联系电话
         */
        readonly phone?: string | undefined;
        /**
         * 邮箱
         */
        readonly email?: string | undefined;
        /**
         * 当前部门的子部门，按 sortOrder 升序，
         * 映射关系由 parent 定义
         */
        readonly children?: ReadonlyArray<DeptDto['DeptService/TREE_FETCHER']> | undefined;
    }
}
