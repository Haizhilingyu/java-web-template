export type UserDto = {
    /**
     * 默认抓取形状：User 全部标量属性(不含 tenant) + 角色的全部标量属性(不含 tenant)
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
         * 登录密码(加密后的密文)。
         * 
         * <p>可为空：预留给验证码/SSO 等免密登录的账号</p>
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
             * 角色编码(如 ADMIN)，租户内唯一
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
        }>;
    }
}
