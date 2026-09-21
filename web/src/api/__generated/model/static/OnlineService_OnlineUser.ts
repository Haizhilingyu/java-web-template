/**
 * 在线用户视图：jti 强退时作为路径参数回传
 */
export interface OnlineService_OnlineUser {
    readonly jti: string;
    readonly username: string;
    readonly nickname: string;
    readonly ip: string;
    readonly loginTime: string;
}
