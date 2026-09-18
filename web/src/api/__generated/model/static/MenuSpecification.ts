export interface MenuSpecification {
    /**
     * 菜单名，与 parent 组成组合键，同一父节点下不允许重名
     */
    readonly keyword?: string | undefined;
    /**
     * 菜单名，与 parent 组成组合键，同一父节点下不允许重名
     */
    readonly parentName?: string | undefined;
}
