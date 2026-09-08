export type RoleDto = {
    /**
     * 默认抓取形状：Role 全部标量属性(不含 tenant) + 菜单的全部标量属性
     */
    'RoleService/DEFAULT_FETCHER': {
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
        /**
         * 当前角色可访问的所有菜单，多对多关联
         */
        readonly menus: ReadonlyArray<{
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
             * 菜单名，与 parent 组成唯一约束
             */
            readonly name: string;
            /**
             * 前端路由地址，目录类菜单可为空
             */
            readonly path?: string | undefined;
            /**
             * 菜单图标名，与前端图标库对应
             */
            readonly icon?: string | undefined;
            /**
             * 同级排序号，越小越靠前
             */
            readonly sortOrder: number;
        }>;
    }
}
