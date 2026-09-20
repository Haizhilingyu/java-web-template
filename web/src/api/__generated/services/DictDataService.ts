import type {Executor} from '../';
import type {DictDataDto} from '../model/dto/';
import type {DictDataInput, DictDataSpecification, Page} from '../model/static/';

export class DictDataService {
    
    constructor(private executor: Executor) {}
    
    readonly deleteDictData: (options: DictDataServiceOptions['deleteDictData']) => Promise<
        void
    > = async(options) => {
        let _uri = '/api/v1/dict/data/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'DELETE'})) as Promise<void>;
    }
    
    readonly findDictDataBySuperQBE: (options: DictDataServiceOptions['findDictDataBySuperQBE']) => Promise<
        Page<DictDataDto['DictDataService/DEFAULT_FETCHER']>
    > = async(options) => {
        let _uri = '/api/v1/dict/data/list/bySuperQBE';
        let _separator = _uri.indexOf('?') === -1 ? '?' : '&';
        let _value: any = undefined;
        _value = options.specification.keyword;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'keyword='
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
        _value = options.dictTypeId;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'dictTypeId='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<Page<DictDataDto['DictDataService/DEFAULT_FETCHER']>>;
    }
    
    /**
     * 按字典编码取启用条目：前端 useDict 的数据源。
     * 管理端各表单的选择器也要消费，任何登录用户可见(与菜单/部门树一致)；
     * 后端不加缓存——单机 H2 无收益，前端 useDict 已缓存
     */
    readonly findEnabledByType: (options: DictDataServiceOptions['findEnabledByType']) => Promise<
        ReadonlyArray<DictDataDto['DictDataService/ITEM_FETCHER']>
    > = async(options) => {
        let _uri = '/api/v1/dict/data/type/';
        _uri += encodeURIComponent(options.type);
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<ReadonlyArray<DictDataDto['DictDataService/ITEM_FETCHER']>>;
    }
    
    readonly saveDictData: (options: DictDataServiceOptions['saveDictData']) => Promise<
        DictDataDto['DictDataService/DEFAULT_FETCHER']
    > = async(options) => {
        let _uri = '/api/v1/dict/data';
        return (await this.executor({uri: _uri, method: 'PUT', body: options.body})) as Promise<DictDataDto['DictDataService/DEFAULT_FETCHER']>;
    }
}

export type DictDataServiceOptions = {
    'findDictDataBySuperQBE': {
        readonly pageIndex?: number | undefined, 
        readonly pageSize?: number | undefined, 
        readonly sortCode?: string | undefined, 
        readonly specification: DictDataSpecification, 
        readonly dictTypeId?: number | undefined
    }, 
    'findEnabledByType': {
        readonly type: string
    }, 
    'saveDictData': {
        readonly body: DictDataInput
    }, 
    'deleteDictData': {
        readonly id: number
    }
}
