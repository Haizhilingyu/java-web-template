import type {Executor} from '../';
import type {
    AuthModels_GetInfoResponse, 
    AuthModels_LoginRequest, 
    AuthModels_LoginResult, 
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
}

export type AuthControllerOptions = {
    'login': {
        readonly body: AuthModels_LoginRequest
    }, 
    'getInfo': {}, 
    'getRouters': {}, 
    'logout': {}
}
