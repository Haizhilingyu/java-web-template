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
 * 登录失败由 {@link AuthExceptionHandler} 统一转 401 JSON
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
     * 认证成功签发 JWT
     */
    readonly login: (options: AuthControllerOptions['login']) => Promise<
        AuthModels_LoginResult
    > = async(options) => {
        let _uri = '/api/v1/auth/login';
        return (await this.executor({uri: _uri, method: 'POST', body: options.body})) as Promise<AuthModels_LoginResult>;
    }
    
    /**
     * 无状态令牌没有服务端会话可销毁，
     * 前端删除本地 token 即完成登出；此处留作令牌黑名单等扩展点
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
