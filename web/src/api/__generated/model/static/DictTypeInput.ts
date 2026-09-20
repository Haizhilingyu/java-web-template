export interface DictTypeInput {
    /**
     * 代理主键，自增，无业务含义
     */
    readonly id?: number | undefined;
    /**
     * 字典编码(如 sys_yes_no)，全局唯一(business_key 约束不含租户列)，
     * 前端 useDict 按该编码取字典
     */
    readonly type: string;
    /**
     * 字典名称
     */
    readonly name: string;
    /**
     * 描述
     */
    readonly description?: string | undefined;
    /**
     * 是否启用，禁用字典的条目不再下发(type 接口过滤)
     */
    readonly enabled: boolean;
}
