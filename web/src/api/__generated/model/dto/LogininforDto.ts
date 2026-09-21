export type LogininforDto = {
    'LogininforService/DEFAULT_FETCHER': {
        /**
         * 代理主键，自增，无业务含义
         */
        readonly id: number;
        /**
         * The time when the object was created.
         * 
         * <p>In this example, this property is not
         * explicitly modified by business code,
         * but is automatically modified by {@code DraftInterceptor}</p>
         */
        readonly createdTime: string;
        /**
         * The time when the object was last modified
         * 
         * <p>In this example, this property is not
         * explicitly modified by business code,
         * but is automatically modified by {@code DraftInterceptor}</p>
         */
        readonly modifiedTime: string;
        /**
         * The tenant to which the current object belongs.
         * 
         * <p>For the database in this example,
         * there are two values: {@code a} and {code b}</p>.
         * 
         * <p>In this example, this property is not
         * explicitly modified by business code,
         * but is automatically modified by {@code DraftInterceptor}</p>
         */
        readonly tenant: string;
        /**
         * 登录账号(尝试登录的账号，无论成败)
         */
        readonly username: string;
        /**
         * 客户端 IP(X-Forwarded-For 首值或 remoteAddr)
         */
        readonly ip: string;
        /**
         * 结果消息(登录成功/登录失败：xxx/登出)
         */
        readonly message?: string | undefined;
    }
}
