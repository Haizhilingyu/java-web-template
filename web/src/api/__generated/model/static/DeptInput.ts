export interface DeptInput {
    /**
     * 代理主键，自增，无业务含义
     */
    readonly id?: number | undefined;
    /**
     * 部门名，与 parent 组成组合键，同一父节点下不允许重名
     */
    readonly name: string;
    /**
     * 上级部门，根部门为 null，与 name 组成唯一约束。
     * 
     * <p>删除受业务约束：存在子部门或部门下有用户时禁止删除，
     * 因此关联不会发生解除动作</p>
     */
    readonly parentId?: number | undefined;
    /**
     * 同级排序号，越小越靠前
     */
    readonly sortOrder: number;
    /**
     * 是否启用，禁用部门仅在用户表单等选择器中过滤，
     * 不追溯影响已挂在部门下的用户
     */
    readonly enabled: boolean;
    /**
     * 负责人，自由文本，不引用用户表
     */
    readonly leader?: string | undefined;
    /**
     * 联系电话
     */
    readonly phone?: string | undefined;
    /**
     * 邮箱
     */
    readonly email?: string | undefined;
}
