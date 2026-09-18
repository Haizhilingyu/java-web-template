import type {Executor} from '../';
import type {SysJobDto} from '../model/dto/';
import type {Page, SysJobInput, SysJobSpecification} from '../model/static/';

export class JobService {
    
    constructor(private executor: Executor) {}
    
    /**
     * 启停切换：0 调度中 / 1 暂停
     */
    readonly changeStatus: (options: JobServiceOptions['changeStatus']) => Promise<
        SysJobDto['JobService/DEFAULT_FETCHER'] | undefined
    > = async(options) => {
        let _uri = '/api/v1/job/';
        _uri += encodeURIComponent(options.id);
        _uri += '/status/';
        _uri += encodeURIComponent(options.status);
        return (await this.executor({uri: _uri, method: 'PUT'})) as Promise<SysJobDto['JobService/DEFAULT_FETCHER'] | undefined>;
    }
    
    readonly deleteJob: (options: JobServiceOptions['deleteJob']) => Promise<
        void
    > = async(options) => {
        let _uri = '/api/v1/job/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'DELETE'})) as Promise<void>;
    }
    
    readonly findJob: (options: JobServiceOptions['findJob']) => Promise<
        SysJobDto['JobService/DEFAULT_FETCHER'] | undefined
    > = async(options) => {
        let _uri = '/api/v1/job/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<SysJobDto['JobService/DEFAULT_FETCHER'] | undefined>;
    }
    
    readonly findJobsBySuperQBE: (options: JobServiceOptions['findJobsBySuperQBE']) => Promise<
        Page<SysJobDto['JobService/DEFAULT_FETCHER']>
    > = async(options) => {
        let _uri = '/api/v1/job/list/bySuperQBE';
        let _separator = _uri.indexOf('?') === -1 ? '?' : '&';
        let _value: any = undefined;
        _value = options.specification.keyword;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'keyword='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.specification.status;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'status='
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
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<Page<SysJobDto['JobService/DEFAULT_FETCHER']>>;
    }
    
    /**
     * 立即执行一次(同步)，不受启停状态影响
     */
    readonly run: (options: JobServiceOptions['run']) => Promise<
        void
    > = async(options) => {
        let _uri = '/api/v1/job/';
        _uri += encodeURIComponent(options.id);
        _uri += '/run';
        return (await this.executor({uri: _uri, method: 'POST'})) as Promise<void>;
    }
    
    /**
     * 新增/编辑。调度中的任务 cron 被修改时即时重排；
     * cron 非法在保存前即报错
     */
    readonly saveJob: (options: JobServiceOptions['saveJob']) => Promise<
        SysJobDto['JobService/DEFAULT_FETCHER']
    > = async(options) => {
        let _uri = '/api/v1/job';
        return (await this.executor({uri: _uri, method: 'PUT', body: options.body})) as Promise<SysJobDto['JobService/DEFAULT_FETCHER']>;
    }
}

export type JobServiceOptions = {
    'findJobsBySuperQBE': {
        readonly pageIndex?: number | undefined, 
        readonly pageSize?: number | undefined, 
        readonly sortCode?: string | undefined, 
        readonly specification: SysJobSpecification
    }, 
    'findJob': {
        readonly id: number
    }, 
    'saveJob': {
        readonly body: SysJobInput
    }, 
    'changeStatus': {
        readonly id: number, 
        readonly status: number
    }, 
    'run': {
        readonly id: number
    }, 
    'deleteJob': {
        readonly id: number
    }
}
