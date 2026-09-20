export interface DictDataInput {
    /**
     * 代理主键，自增，无业务含义
     */
    readonly id?: number | undefined;
    /**
     * 所属字典类型，与 value 组成组合键(同一字典内 value 唯一)。
     * 目标端有租户过滤器，jimmer 要求引用声明为 nullable，
     * 业务上的非空用 inputNotNull 表达
     */
    readonly dictTypeId?: number | undefined;
    /**
     * 显示标签(如"是")
     */
    readonly label: string;
    /**
     * 存储值(如"Y")，与 dictType 组成业务键，前端渲染与提交都用它。
     * 列名避开 H2 保留字 VALUE
     */
    readonly value: string;
    /**
     * 同字典内排序号，越小越靠前
     */
    readonly sortOrder: number;
    /**
     * 是否启用，禁用条目不再下发且不在选择器中出现
     */
    readonly enabled: boolean;
}
