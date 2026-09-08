import type {Executor} from '../';
import type {MenuDto} from '../model/dto/';
import type {MenuInput, MenuSpecification, Page} from '../model/static/';

export class MenuService {
    
    constructor(private executor: Executor) {}
    
    readonly deleteMenu: (options: MenuServiceOptions['deleteMenu']) => Promise<
        void
    > = async(options) => {
        let _uri = '/menu/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'DELETE'})) as Promise<void>;
    }
    
    readonly findMenu: (options: MenuServiceOptions['findMenu']) => Promise<
        MenuDto['MenuService/DEFAULT_FETCHER'] | undefined
    > = async(options) => {
        let _uri = '/menu/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<MenuDto['MenuService/DEFAULT_FETCHER'] | undefined>;
    }
    
    /**
     * 菜单树：只查根节点，子菜单由递归 fetcher 抓取
     */
    readonly findMenus: () => Promise<
        ReadonlyArray<MenuDto['MenuService/TREE_FETCHER']>
    > = async() => {
        let _uri = '/menu/list';
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<ReadonlyArray<MenuDto['MenuService/TREE_FETCHER']>>;
    }
    
    readonly findMenusBySuperQBE: (options: MenuServiceOptions['findMenusBySuperQBE']) => Promise<
        Page<MenuDto['MenuService/DEFAULT_FETCHER']>
    > = async(options) => {
        let _uri = '/menu/list/bySuperQBE';
        let _separator = _uri.indexOf('?') === -1 ? '?' : '&';
        let _value: any = undefined;
        _value = options.specification.keyword;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'keyword='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.specification.parentName;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'parentName='
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
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<Page<MenuDto['MenuService/DEFAULT_FETCHER']>>;
    }
    
    readonly saveMenu: (options: MenuServiceOptions['saveMenu']) => Promise<
        MenuDto['MenuService/DEFAULT_FETCHER']
    > = async(options) => {
        let _uri = '/menu';
        return (await this.executor({uri: _uri, method: 'PUT', body: options.body})) as Promise<MenuDto['MenuService/DEFAULT_FETCHER']>;
    }
}

export type MenuServiceOptions = {
    'findMenus': {}, 
    'findMenusBySuperQBE': {
        readonly pageIndex?: number | undefined, 
        readonly pageSize?: number | undefined, 
        readonly sortCode?: string | undefined, 
        readonly specification: MenuSpecification
    }, 
    'findMenu': {
        readonly id: number
    }, 
    'saveMenu': {
        readonly body: MenuInput
    }, 
    'deleteMenu': {
        readonly id: number
    }
}
