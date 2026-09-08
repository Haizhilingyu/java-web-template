export interface UserSpecification {
    /**
     * 登录用户名，全局唯一
     */
    readonly keyword?: string | undefined;
    /**
     * 是否启用，禁用账号不能登录
     */
    readonly enabled?: boolean | undefined;
    /**
     * 角色显示名
     */
    readonly roleName?: string | undefined;
}
