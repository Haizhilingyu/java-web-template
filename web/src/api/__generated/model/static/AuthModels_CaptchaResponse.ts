/**
 * 登录验证码(工单01)：开关关只回 enabled=false；
 * 开关开时 key 用于提交，image 为 Base64 PNG 可直接进 img src
 */
export interface AuthModels_CaptchaResponse {
    readonly enabled: boolean;
    readonly key?: string | undefined;
    readonly image?: string | undefined;
}
