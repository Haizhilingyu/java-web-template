export type UserDto = {
    /**
     * 默认抓取形状：User 全部标量属性(不含 tenant)
     * + 角色/部门/岗位的全部标量属性(不含 tenant)
     */
    'UserService/DEFAULT_FETCHER': {
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
         * 登录用户名，全局唯一
         */
        readonly username: string;
        /**
         * BCrypt 密文。
         * 
         * <p>可为空：预留给验证码/SSO 等免密登录的账号；
         * 更新用户时该属性缺省表示不修改密码</p>
         */
        readonly password?: string | undefined;
        /**
         * 显示昵称，缺省时可用用户名代替
         */
        readonly nickname?: string | undefined;
        /**
         * 是否启用，禁用账号不能登录
         */
        readonly enabled: boolean;
        /**
         * 当前用户拥有的所有角色，多对多关联
         */
        readonly roles: ReadonlyArray<{
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
             * 角色编码(如 ADMIN)，租户内唯一。
             * 编码 ADMIN 为内置超级管理员：菜单与接口权限全部直通
             */
            readonly code: string;
            /**
             * 角色显示名
             */
            readonly name: string;
            /**
             * 角色描述
             */
            readonly description?: string | undefined;
            /**
             * 数据范围：1 全部 / 2 自定义 / 3 本部门 / 4 本部门及以下 / 5 仅本人，默认 1。
             * 仅在显式生效点(如用户管理分页列表)起作用
             */
            readonly dataScope: number;
        }>;
        /**
         * 所属部门，可空(未分配部门的用户)
         */
        readonly dept?: {
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
        } | undefined;
        /**
         * 担任的岗位，多对多关联
         */
        readonly posts: ReadonlyArray<{
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
             * 岗位编码(如 CEO)，编码全局唯一(business_key 约束不含租户列)
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
        }>;
    }
}
