import type {Executor} from '../';
import type {DictTypeDto} from '../model/dto/';
import type {DictTypeInput, DictTypeSpecification, Page} from '../model/static/';

export class DictTypeService {
    
    constructor(private executor: Executor) {}
    
    /**
     * 生命周期约束：字典下存在条目时禁止删除，防止误删整套字典
     */
    readonly deleteDictType: (options: DictTypeServiceOptions['deleteDictType']) => Promise<
        void
    > = async(options) => {
        let _uri = '/api/v1/dict/type/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'DELETE'})) as Promise<void>;
    }
    
    readonly findDictTypesBySuperQBE: (options: DictTypeServiceOptions['findDictTypesBySuperQBE']) => Promise<
        Page<DictTypeDto['DictTypeService/DEFAULT_FETCHER']>
    > = async(options) => {
        let _uri = '/api/v1/dict/type/list/bySuperQBE';
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
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<Page<DictTypeDto['DictTypeService/DEFAULT_FETCHER']>>;
    }
    
    readonly saveDictType: (options: DictTypeServiceOptions['saveDictType']) => Promise<
        DictTypeDto['DictTypeService/DEFAULT_FETCHER']
    > = async(options) => {
        let _uri = '/api/v1/dict/type';
        return (await this.executor({uri: _uri, method: 'PUT', body: options.body})) as Promise<DictTypeDto['DictTypeService/DEFAULT_FETCHER']>;
    }
}

export type DictTypeServiceOptions = {
    'findDictTypesBySuperQBE': {
        readonly pageIndex?: number | undefined, 
        readonly pageSize?: number | undefined, 
        readonly sortCode?: string | undefined, 
        readonly specification: DictTypeSpecification
    }, 
    'deleteDictType': {
        readonly id: number
    }, 
    'saveDictType': {
        readonly body: DictTypeInput
    }
}
