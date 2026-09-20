export interface PostInput {
    /**
     * 代理主键，自增，无业务含义
     */
    readonly id?: number | undefined;
    /**
     * 岗位编码(如 CEO)，编码全局唯一(business_key 约束不含租户列)
     */
    readonly code: string;
    /**
     * 岗位名称
     */
    readonly name: string;
    /**
     * 排序号，越小越靠前
     */
    readonly sortOrder: number;
    /**
     * 是否启用，禁用岗位仅在用户表单等选择器中过滤，
     * 不追溯影响已担任该岗位的用户
     */
    readonly enabled: boolean;
}
