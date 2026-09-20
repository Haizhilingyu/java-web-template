import type {Executor} from '../';
import type {ConfigDto} from '../model/dto/';
import type {ConfigInput, ConfigSpecification, Page} from '../model/static/';

export class ConfigService {
    
    constructor(private executor: Executor) {}
    
    readonly deleteConfig: (options: ConfigServiceOptions['deleteConfig']) => Promise<
        void
    > = async(options) => {
        let _uri = '/api/v1/config/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'DELETE'})) as Promise<void>;
    }
    
    /**
     * 按参数键取配置：模板内置消费点尚未出现(决策：不种无人读取的键)，
     * 登录即可访问，供将来前端/业务读取
     */
    readonly findConfigByKey: (options: ConfigServiceOptions['findConfigByKey']) => Promise<
        ConfigDto['ConfigService/DEFAULT_FETCHER'] | undefined
    > = async(options) => {
        let _uri = '/api/v1/config/configKey/';
        _uri += encodeURIComponent(options.key);
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<ConfigDto['ConfigService/DEFAULT_FETCHER'] | undefined>;
    }
    
    readonly findConfigsBySuperQBE: (options: ConfigServiceOptions['findConfigsBySuperQBE']) => Promise<
        Page<ConfigDto['ConfigService/DEFAULT_FETCHER']>
    > = async(options) => {
        let _uri = '/api/v1/config/list/bySuperQBE';
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
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<Page<ConfigDto['ConfigService/DEFAULT_FETCHER']>>;
    }
    
    readonly saveConfig: (options: ConfigServiceOptions['saveConfig']) => Promise<
        ConfigDto['ConfigService/DEFAULT_FETCHER']
    > = async(options) => {
        let _uri = '/api/v1/config';
        return (await this.executor({uri: _uri, method: 'PUT', body: options.body})) as Promise<ConfigDto['ConfigService/DEFAULT_FETCHER']>;
    }
}

export type ConfigServiceOptions = {
    'findConfigsBySuperQBE': {
        readonly pageIndex?: number | undefined, 
        readonly pageSize?: number | undefined, 
        readonly sortCode?: string | undefined, 
        readonly specification: ConfigSpecification
    }, 
    'findConfigByKey': {
        readonly key: string
    }, 
    'saveConfig': {
        readonly body: ConfigInput
    }, 
    'deleteConfig': {
        readonly id: number
    }
}
