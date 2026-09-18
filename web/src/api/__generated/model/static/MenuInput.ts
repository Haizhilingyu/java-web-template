export interface MenuInput {
    /**
     * 代理主键，自增，无业务含义
     */
    readonly id?: number | undefined;
    /**
     * 菜单名，与 parent 组成组合键，同一父节点下不允许重名
     */
    readonly name: string;
    /**
     * 父菜单，根节点为 null，与 name 组成唯一约束。
     * 
     * <p>删除父菜单时子菜单与断开关联而非级联删除</p>
     */
    readonly parentId?: number | undefined;
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
}
