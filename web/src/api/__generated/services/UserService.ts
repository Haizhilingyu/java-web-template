import type {Executor} from '../';
import type {UserDto} from '../model/dto/';
import type {Page, UserInput, UserSpecification} from '../model/static/';

export class UserService {
    
    constructor(private executor: Executor) {}
    
    readonly deleteUser: (options: UserServiceOptions['deleteUser']) => Promise<
        void
    > = async(options) => {
        let _uri = '/api/v1/user/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'DELETE'})) as Promise<void>;
    }
    
    readonly findUser: (options: UserServiceOptions['findUser']) => Promise<
        UserDto['UserService/DEFAULT_FETCHER'] | undefined
    > = async(options) => {
        let _uri = '/api/v1/user/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<UserDto['UserService/DEFAULT_FETCHER'] | undefined>;
    }
    
    readonly findUserByUsername: (options: UserServiceOptions['findUserByUsername']) => Promise<
        UserDto['UserService/DEFAULT_FETCHER'] | undefined
    > = async(options) => {
        let _uri = '/api/v1/user/username/';
        _uri += encodeURIComponent(options.username);
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<UserDto['UserService/DEFAULT_FETCHER'] | undefined>;
    }
    
    readonly findUsersBySuperQBE: (options: UserServiceOptions['findUsersBySuperQBE']) => Promise<
        Page<UserDto['UserService/DEFAULT_FETCHER']>
    > = async(options) => {
        let _uri = '/api/v1/user/list/bySuperQBE';
        let _separator = _uri.indexOf('?') === -1 ? '?' : '&';
        let _value: any = undefined;
        _value = options.specification.keyword;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'keyword='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.specification.enabled;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'enabled='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.specification.roleName;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'roleName='
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
        _value = options.deptId;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'deptId='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<Page<UserDto['UserService/DEFAULT_FETCHER']>>;
    }
    
    /**
     * password 属性未提交(更新场景)时保持原值；
     * 提交了明文则落库前 BCrypt 加密。
     * 不能用 input.toEntity()：dto 生成的映射对未提交属性无条件 set null，
     * 会把库里原值覆盖为 NULL，因此这里手工组装 draft 控制属性的加载态。
     * 
     * <p>集合 id 视图(postIds/roleIds)的"是否提交"必须读字段而非 getter：
     * 生成的集合 getter 懒初始化空列表(永远 != null)，未提交也会被当成
     * "提交了空列表"而清空关联；提交了空列表则显式清空</p>
     */
    readonly saveUser: (options: UserServiceOptions['saveUser']) => Promise<
        UserDto['UserService/DEFAULT_FETCHER']
    > = async(options) => {
        let _uri = '/api/v1/user';
        return (await this.executor({uri: _uri, method: 'PUT', body: options.body})) as Promise<UserDto['UserService/DEFAULT_FETCHER']>;
    }
}

export type UserServiceOptions = {
    'findUsersBySuperQBE': {
        readonly pageIndex?: number | undefined, 
        readonly pageSize?: number | undefined, 
        readonly sortCode?: string | undefined, 
        readonly specification: UserSpecification, 
        readonly deptId?: number | undefined
    }, 
    'findUser': {
        readonly id: number
    }, 
    'findUserByUsername': {
        readonly username: string
    }, 
    'saveUser': {
        readonly body: UserInput
    }, 
    'deleteUser': {
        readonly id: number
    }
}
