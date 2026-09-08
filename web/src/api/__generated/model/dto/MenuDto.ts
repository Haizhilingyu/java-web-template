export type MenuDto = {
    /**
     * 默认抓取形状：全部标量属性(不含子菜单)
     */
    'MenuService/DEFAULT_FETCHER': {
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
    }, 
    /**
     * 菜单树抓取形状：全部标量属性 + 递归的子菜单(子层重复同形状)。
     * 注意 children(true) 只会抓子菜单 id，递归全形状必须用 recursiveChildren()
     */
    'MenuService/TREE_FETCHER': {
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
        /**
         * 当前菜单的子菜单，按 sortOrder 升序，
         * 映射关系由 parent 定义
         */
        readonly children?: ReadonlyArray<MenuDto['MenuService/TREE_FETCHER']> | undefined;
    }
}
