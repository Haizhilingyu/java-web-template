export interface RoleSpecification {
    /**
     * 角色编码(如 ADMIN)，租户内唯一。
     * 编码 ADMIN 为内置超级管理员：菜单与接口权限全部直通
     */
    readonly keyword?: string | undefined;
    /**
     * 菜单名，与 parent 组成组合键，同一父节点下不允许重名
     */
    readonly menuName?: string | undefined;
}
