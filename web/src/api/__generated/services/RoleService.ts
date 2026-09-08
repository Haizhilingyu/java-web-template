import type {Executor} from '../';
import type {RoleDto} from '../model/dto/';
import type {Page, RoleInput, RoleSpecification} from '../model/static/';

export class RoleService {
    
    constructor(private executor: Executor) {}
    
    readonly deleteRole: (options: RoleServiceOptions['deleteRole']) => Promise<
        void
    > = async(options) => {
        let _uri = '/role/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'DELETE'})) as Promise<void>;
    }
    
    readonly findRole: (options: RoleServiceOptions['findRole']) => Promise<
        RoleDto['RoleService/DEFAULT_FETCHER'] | undefined
    > = async(options) => {
        let _uri = '/role/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<RoleDto['RoleService/DEFAULT_FETCHER'] | undefined>;
    }
    
    readonly findRolesBySuperQBE: (options: RoleServiceOptions['findRolesBySuperQBE']) => Promise<
        Page<RoleDto['RoleService/DEFAULT_FETCHER']>
    > = async(options) => {
        let _uri = '/role/list/bySuperQBE';
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
    
    readonly saveRole: (options: RoleServiceOptions['saveRole']) => Promise<
        RoleDto['RoleService/DEFAULT_FETCHER']
    > = async(options) => {
        let _uri = '/role';
        return (await this.executor({uri: _uri, method: 'PUT', body: options.body})) as Promise<RoleDto['RoleService/DEFAULT_FETCHER']>;
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
    'deleteRole': {
        readonly id: number
    }
}
