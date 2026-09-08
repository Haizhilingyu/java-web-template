export interface RoleInput {
    /**
     * 代理主键，自增，无业务含义
     */
    readonly id?: number | undefined;
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
    readonly menuIds: ReadonlyArray<number>;
}
