import type {Executor} from '../';
import type {OnlineService_OnlineUser} from '../model/static/';

/**
 * 在线用户：实时读取会话注册表(ADR-0001)，管理员可强退任一会话。
 * 会话生命周期与注册表一致，不做分页(单实例内存集合)
 */
export class OnlineService {
    
    constructor(private executor: Executor) {}
    
    /**
     * 强退：删除注册表条目，目标令牌下一次请求即 401
     */
    readonly forceLogout: (options: OnlineServiceOptions['forceLogout']) => Promise<
        void
    > = async(options) => {
        let _uri = '/api/v1/online/';
        _uri += encodeURIComponent(options.jti);
        return (await this.executor({uri: _uri, method: 'DELETE'})) as Promise<void>;
    }
    
    readonly list: () => Promise<
        ReadonlyArray<OnlineService_OnlineUser>
    > = async() => {
        let _uri = '/api/v1/online/list';
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<ReadonlyArray<OnlineService_OnlineUser>>;
    }
}

export type OnlineServiceOptions = {
    'list': {}, 
    'forceLogout': {
        readonly jti: string
    }
}
