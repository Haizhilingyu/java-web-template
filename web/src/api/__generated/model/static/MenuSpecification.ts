/**
 * 菜单树。
 * 
 * <p>菜单是全局数据，不挂租户；树形结构参考 TreeNode：
 * name 与 parent 组成组合键，同一父节点下不允许重名</p>
 */
export interface MenuSpecification {
    /**
     * 菜单名，与 parent 组成唯一约束
     */
    readonly keyword?: string | undefined;
    /**
     * 菜单名，与 parent 组成唯一约束
     */
    readonly parentName?: string | undefined;
}
