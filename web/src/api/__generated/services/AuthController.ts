import type {Executor} from '../';
import type {
    AuthModels_CaptchaResponse, 
    AuthModels_ChangePasswordRequest, 
    AuthModels_GetInfoResponse, 
    AuthModels_LoginRequest, 
    AuthModels_LoginResult, 
    AuthModels_NicknameRequest, 
    AuthModels_ProfileResponse, 
    AuthModels_RouteVO
} from '../model/static/';

/**
 * 登录/登出/用户信息/动态路由。除 login 外均要求携带有效 JWT
 * (由 core 的 SecurityConfig 对 /auth/** 强制)。
 * 登录失败由 {@link AuthExceptionHandler} 统一转 401 JSON。
 * 
 * <p>注意：方法不要声明 HttpServletRequest/Response 参数——
 * jimmer-apt 为 API 生成元数据时无法解析 servlet 类型(编译期 NPE)，
 * 一律经 {@link #currentRequest()} 获取</p>
 */
export class AuthController {
    
    constructor(private executor: Executor) {}
    
    /**
     * 读取当前用户头像(工单07)：authenticated 流式返回(JWT 在 header，
     * 前端 fetch blob → objectURL 展示)；无头像返回 404
     */
    readonly avatar: () => Promise<
        void
    > = async() => {
        let _uri = '/api/v1/auth/avatar';
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<void>;
    }
    
    /**
     * 登录验证码(工单01)：开关关只回 enabled=false，前端不渲染验证码框；
     * 开关开时返回 key 与 Base64 图。免登录访问(SecurityConfig permitAll)
     */
    readonly captcha: () => Promise<
        AuthModels_CaptchaResponse
    > = async() => {
        let _uri = '/api/v1/auth/captcha';
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<AuthModels_CaptchaResponse>;
    }
    
    /**
     * 修改昵称：每次请求回库加载用户，无需作废会话
     */
    readonly changeNickname: (options: AuthControllerOptions['changeNickname']) => Promise<
        void
    > = async(options) => {
        let _uri = '/api/v1/auth/nickname';
        return (await this.executor({uri: _uri, method: 'PUT', body: options.body})) as Promise<void>;
    }
    
    /**
     * 修改密码：校验旧密码后更新，成功即作废该用户全部会话(含当前)，
     * 所有端需重新登录(ADR-0001)；落库+作废走 PasswordManager 共用通道
     */
    readonly changePassword: (options: AuthControllerOptions['changePassword']) => Promise<
        void
    > = async(options) => {
        let _uri = '/api/v1/auth/password';
        return (await this.executor({uri: _uri, method: 'PUT', body: options.body})) as Promise<void>;
    }
    
    /**
     * 当前登录用户的基本信息 + 角色 + 权限标识集合(按钮级权限指令的数据源)
     */
    readonly getInfo: () => Promise<
        AuthModels_GetInfoResponse
    > = async() => {
        let _uri = '/api/v1/auth/getInfo';
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<AuthModels_GetInfoResponse>;
    }
    
    /**
     * 当前用户可见的前端动态路由
     */
    readonly getRouters: () => Promise<
        ReadonlyArray<AuthModels_RouteVO>
    > = async() => {
        let _uri = '/api/v1/auth/getRouters';
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<ReadonlyArray<AuthModels_RouteVO>>;
    }
    
    /**
     * 认证成功签发 JWT 并登记会话(ADR-0001)；登录日志记录成功/失败
     */
    readonly login: (options: AuthControllerOptions['login']) => Promise<
        AuthModels_LoginResult
    > = async(options) => {
        let _uri = '/api/v1/auth/login';
        return (await this.executor({uri: _uri, method: 'POST', body: options.body})) as Promise<AuthModels_LoginResult>;
    }
    
    /**
     * 撤销当前会话(注册表删除 jti)，令牌即时失效；
     * 同用户其他并存会话不受影响(ADR-0001)
     */
    readonly logout: () => Promise<
        void
    > = async() => {
        let _uri = '/api/v1/auth/logout';
        return (await this.executor({uri: _uri, method: 'POST'})) as Promise<void>;
    }
    
    /**
     * 个人中心资料：部门/角色/岗位名称
     */
    readonly profile: () => Promise<
        AuthModels_ProfileResponse
    > = async() => {
        let _uri = '/api/v1/auth/profile';
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<AuthModels_ProfileResponse>;
    }
    
    /**
     * 上传当前用户头像(工单07)：multipart ≤2MB，扩展名白名单 png/jpg/jpeg/gif。
     * 内联实现不抽通用文件服务；签名不带 multipart 类型(见类注释)
     */
    readonly uploadAvatar: () => Promise<
        void
    > = async() => {
        let _uri = '/api/v1/auth/avatar';
        return (await this.executor({uri: _uri, method: 'POST'})) as Promise<void>;
    }
}

export type AuthControllerOptions = {
    'captcha': {}, 
    'login': {
        readonly body: AuthModels_LoginRequest
    }, 
    'getInfo': {}, 
    'getRouters': {}, 
    'logout': {}, 
    'profile': {}, 
    'changeNickname': {
        readonly body: AuthModels_NicknameRequest
    }, 
    'changePassword': {
        readonly body: AuthModels_ChangePasswordRequest
    }, 
    'uploadAvatar': {}, 
    'avatar': {}
}
