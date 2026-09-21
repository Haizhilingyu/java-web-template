export interface RoleInput {
    /**
     * 代理主键，自增，无业务含义
     */
    readonly id?: number | undefined;
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
     * 数据范围：1 全部 / 2 自定义 / 3 本部门 / 4 本部门及以下 / 5 仅本人，默认 1。
     * 仅在显式生效点(如用户管理分页列表)起作用
     */
    readonly dataScope: number;
    /**
     * 当前角色可访问的所有菜单(含按钮)，多对多关联，
     * 接口/按钮权限取其中 perms 字段，路由取 M/C 类型节点
     */
    readonly menuIds: ReadonlyArray<number>;
    /**
     * 数据范围=自定义(2)时的可见部门集合，语义为精确等于勾选集合
     */
    readonly customDeptIds: ReadonlyArray<number>;
}
