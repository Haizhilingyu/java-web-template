export interface AuthModels_LoginRequest {
    readonly username: string;
    readonly password: string;
    readonly captchaKey?: string | undefined;
    readonly captchaCode?: string | undefined;
}
