import type {Executor} from '../';
import type {OperLogDto} from '../model/dto/';
import type {Page} from '../model/static/';

export class OperLogService {
    
    constructor(private executor: Executor) {}
    
    /**
     * 清空全部操作日志
     */
    readonly clear: () => Promise<
        void
    > = async() => {
        let _uri = '/api/v1/operlog/clear';
        return (await this.executor({uri: _uri, method: 'DELETE'})) as Promise<void>;
    }
    
    readonly findOperLogsBySuperQBE: (options: OperLogServiceOptions['findOperLogsBySuperQBE']) => Promise<
        Page<OperLogDto['OperLogService/DEFAULT_FETCHER']>
    > = async(options) => {
        let _uri = '/api/v1/operlog/list/bySuperQBE';
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
        _value = options.sortCode;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'sortCode='
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
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<Page<OperLogDto['OperLogService/DEFAULT_FETCHER']>>;
    }
}

export type OperLogServiceOptions = {
    'findOperLogsBySuperQBE': {
        readonly pageIndex?: number | undefined, 
        readonly pageSize?: number | undefined, 
        readonly sortCode?: string | undefined, 
        readonly keyword?: string | undefined
    }, 
    'clear': {}
}
