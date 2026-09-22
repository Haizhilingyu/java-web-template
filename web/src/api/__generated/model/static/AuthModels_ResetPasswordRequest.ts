/**
 * 管理员重置密码请求(工单05)：无需旧密码，按 system:user:resetPwd 授权
 */
export interface AuthModels_ResetPasswordRequest {
    readonly newPassword: string;
}
