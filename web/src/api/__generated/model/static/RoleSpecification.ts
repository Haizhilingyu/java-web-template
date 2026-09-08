export interface RoleSpecification {
    /**
     * 角色编码(如 ADMIN)，租户内唯一
     */
    readonly keyword?: string | undefined;
    /**
     * 菜单名，与 parent 组成唯一约束
     */
    readonly menuName?: string | undefined;
}
