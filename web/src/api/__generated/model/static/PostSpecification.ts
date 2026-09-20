export interface PostSpecification {
    /**
     * 岗位编码(如 CEO)，编码全局唯一(business_key 约束不含租户列)
     */
    readonly keyword?: string | undefined;
}
