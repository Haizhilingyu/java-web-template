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
         * 当前角色可访问的所有菜单(含按钮)，多对多关联，
         * 接口/按钮权限取其中 perms 字段，路由取 M/C 类型节点
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
             * 菜单名，与 parent 组成组合键，同一父节点下不允许重名
             */
            readonly name: string;
            /**
             * 菜单类型：M 目录 / C 菜单(页面) / F 按钮
             */
            readonly type: string;
            /**
             * 前端路由地址。目录为一级路由(如 /system)，菜单为完整路由(如 /system/user)，按钮为空
             */
            readonly path?: string | undefined;
            /**
             * 前端页面组件路径(如 /system/user/index)，
             * 与 pages 目录下的 .vue 文件对应；目录/按钮为空
             */
            readonly component?: string | undefined;
            /**
             * 权限标识(如 system:user:add)，按钮节点必填，
             * 菜单/目录节点可标接口级权限；登录后下发为 perms 集合
             */
            readonly perms?: string | undefined;
            /**
             * 菜单图标名，与前端图标库对应
             */
            readonly icon?: string | undefined;
            /**
             * 是否在导航中显示(隐藏路由仍可直接访问，用于详情页等)
             */
            readonly visible: boolean;
            /**
             * 同级排序号，越小越靠前
             */
            readonly sortOrder: number;
            /**
             * 来源模块编码，由模块菜单同步器写入；
             * 手工创建的菜单为 null
             */
            readonly moduleCode?: string | undefined;
        }>;
    }
}
