/**
 * 个人中心资料：部门/角色/岗位只取名称
 */
export interface AuthModels_ProfileResponse {
    readonly id: number;
    readonly username: string;
    readonly nickname?: string | undefined;
    readonly deptName?: string | undefined;
    readonly roleNames: ReadonlyArray<string>;
    readonly postNames: ReadonlyArray<string>;
}
