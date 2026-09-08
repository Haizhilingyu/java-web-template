export interface UserInput {
    /**
     * 代理主键，自增，无业务含义
     */
    readonly id?: number | undefined;
    /**
     * 登录用户名，全局唯一
     */
    readonly username: string;
    /**
     * 登录密码(加密后的密文)。
     * 
     * <p>可为空：预留给验证码/SSO 等免密登录的账号</p>
     */
    readonly password?: string | undefined;
    /**
     * 显示昵称，缺省时可用用户名代替
     */
    readonly nickname?: string | undefined;
    /**
     * 是否启用，禁用账号不能登录
     */
    readonly enabled: boolean;
    /**
     * 当前用户拥有的所有角色，多对多关联
     */
    readonly roleIds: ReadonlyArray<number>;
}
