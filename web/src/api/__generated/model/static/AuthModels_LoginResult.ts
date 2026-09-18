/**
 * 登录成功带 token，失败带 message(HTTP 401)，二者互斥
 */
export interface AuthModels_LoginResult {
    readonly token?: string | undefined;
    readonly message?: string | undefined;
}
