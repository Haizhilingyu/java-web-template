/**
 * 菜单树。
 * 
 * <p>菜单是全局数据，不挂租户；树形结构参考 TreeNode：
 * name 与 parent 组成组合键，同一父节点下不允许重名</p>
 */
export interface MenuInput {
    /**
     * 代理主键，自增，无业务含义
     */
    readonly id?: number | undefined;
    /**
     * 菜单名，与 parent 组成唯一约束
     */
    readonly name: string;
    /**
     * 父菜单，根节点为 null，与 name 组成唯一约束。
     * 
     * <p>删除父菜单时子菜单与断开关联而非级联删除</p>
     */
    readonly parentId?: number | undefined;
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
}
