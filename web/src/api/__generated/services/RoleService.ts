import type {Executor} from '../';
import type {RoleDto, UserDto} from '../model/dto/';
import type {
    Page, 
    RoleInput, 
    RoleSpecification, 
    RoleUserIdsRequest
} from '../model/static/';

export class RoleService {
    
    constructor(private executor: Executor) {}
    
    /**
     * 批量授权：把 userIds 加入该角色(已绑定的跳过，幂等)。
     * 手工组 draft 读改 roleIds——Input.toEntity 会把未提交集合写空
     */
    readonly assignUsers: (options: RoleServiceOptions['assignUsers']) => Promise<
        void
    > = async(options) => {
        let _uri = '/api/v1/role/';
        _uri += encodeURIComponent(options.id);
        _uri += '/users';
        return (await this.executor({uri: _uri, method: 'POST', body: options.body})) as Promise<void>;
    }
    
    readonly deleteRole: (options: RoleServiceOptions['deleteRole']) => Promise<
        void
    > = async(options) => {
        let _uri = '/api/v1/role/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'DELETE'})) as Promise<void>;
    }
    
    readonly findRole: (options: RoleServiceOptions['findRole']) => Promise<
        RoleDto['RoleService/DEFAULT_FETCHER'] | undefined
    > = async(options) => {
        let _uri = '/api/v1/role/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<RoleDto['RoleService/DEFAULT_FETCHER'] | undefined>;
    }
    
    /**
     * 分配用户(工单06)：已绑用户的分页列表，keyword 对用户名/昵称模糊过滤。
     * 权限沿用 system:role:edit
     */
    readonly findRoleUsers: (options: RoleServiceOptions['findRoleUsers']) => Promise<
        Page<UserDto['RoleService/ROLE_USER_FETCHER']>
    > = async(options) => {
        let _uri = '/api/v1/role/';
        _uri += encodeURIComponent(options.id);
        _uri += '/users';
        let _separator = _uri.indexOf('?') === -1 ? '?' : '&';
        let _value: any = undefined;
        _value = options.pageIndex;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'pageIndex='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.pageSize;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'pageSize='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.keyword;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'keyword='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<Page<UserDto['RoleService/ROLE_USER_FETCHER']>>;
    }
    
    readonly findRolesBySuperQBE: (options: RoleServiceOptions['findRolesBySuperQBE']) => Promise<
        Page<RoleDto['RoleService/DEFAULT_FETCHER']>
    > = async(options) => {
        let _uri = '/api/v1/role/list/bySuperQBE';
        let _separator = _uri.indexOf('?') === -1 ? '?' : '&';
        let _value: any = undefined;
        _value = options.specification.keyword;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'keyword='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.specification.menuName;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'menuName='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.pageIndex;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'pageIndex='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.pageSize;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'pageSize='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.sortCode;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'sortCode='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<Page<RoleDto['RoleService/DEFAULT_FETCHER']>>;
    }
    
    /**
     * 手工组装 draft(同 UserService.saveUser 的理由)：Input 的
     * saveCommand/toEntity 对未提交属性无条件写 null；集合 id 视图的
     * "是否提交"读字段判断(getter 懒初始化)。dataScope 未提交默认 1(全部)
     */
    readonly saveRole: (options: RoleServiceOptions['saveRole']) => Promise<
        RoleDto['RoleService/DEFAULT_FETCHER']
    > = async(options) => {
        let _uri = '/api/v1/role';
        return (await this.executor({uri: _uri, method: 'PUT', body: options.body})) as Promise<RoleDto['RoleService/DEFAULT_FETCHER']>;
    }
    
    /**
     * 批量取消授权：把 userIds 移出该角色(未绑定的跳过，幂等)
     */
    readonly unassignUsers: (options: RoleServiceOptions['unassignUsers']) => Promise<
        void
    > = async(options) => {
        let _uri = '/api/v1/role/';
        _uri += encodeURIComponent(options.id);
        _uri += '/users';
        return (await this.executor({uri: _uri, method: 'DELETE', body: options.body})) as Promise<void>;
    }
}

export type RoleServiceOptions = {
    'findRolesBySuperQBE': {
        readonly pageIndex?: number | undefined, 
        readonly pageSize?: number | undefined, 
        readonly sortCode?: string | undefined, 
        readonly specification: RoleSpecification
    }, 
    'findRole': {
        readonly id: number
    }, 
    'saveRole': {
        readonly body: RoleInput
    }, 
    'findRoleUsers': {
        readonly id: number, 
        readonly pageIndex?: number | undefined, 
        readonly pageSize?: number | undefined, 
        readonly keyword?: string | undefined
    }, 
    'assignUsers': {
        readonly id: number, 
        readonly body: RoleUserIdsRequest
    }, 
    'unassignUsers': {
        readonly id: number, 
        readonly body: RoleUserIdsRequest
    }, 
    'deleteRole': {
        readonly id: number
    }
}
