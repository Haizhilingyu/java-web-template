import type {Executor} from '../';
import type {NoticeDto} from '../model/dto/';
import type {NoticeInput, NoticeSpecification, Page} from '../model/static/';

export class NoticeService {
    
    constructor(private executor: Executor) {}
    
    readonly deleteNotice: (options: NoticeServiceOptions['deleteNotice']) => Promise<
        void
    > = async(options) => {
        let _uri = '/api/v1/notice/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'DELETE'})) as Promise<void>;
    }
    
    readonly findNoticesBySuperQBE: (options: NoticeServiceOptions['findNoticesBySuperQBE']) => Promise<
        Page<NoticeDto['NoticeService/DEFAULT_FETCHER']>
    > = async(options) => {
        let _uri = '/api/v1/notice/list/bySuperQBE';
        let _separator = _uri.indexOf('?') === -1 ? '?' : '&';
        let _value: any = undefined;
        _value = options.specification.keyword;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'keyword='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.specification.noticeType;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'noticeType='
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
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<Page<NoticeDto['NoticeService/DEFAULT_FETCHER']>>;
    }
    
    /**
     * 公告无自然业务键：NON_IDEMPOTENT_UPSERT 表示有 id 更新、无 id 插入
     */
    readonly saveNotice: (options: NoticeServiceOptions['saveNotice']) => Promise<
        NoticeDto['NoticeService/DEFAULT_FETCHER']
    > = async(options) => {
        let _uri = '/api/v1/notice';
        return (await this.executor({uri: _uri, method: 'PUT', body: options.body})) as Promise<NoticeDto['NoticeService/DEFAULT_FETCHER']>;
    }
}

export type NoticeServiceOptions = {
    'findNoticesBySuperQBE': {
        readonly pageIndex?: number | undefined, 
        readonly pageSize?: number | undefined, 
        readonly sortCode?: string | undefined, 
        readonly specification: NoticeSpecification
    }, 
    'saveNotice': {
        readonly body: NoticeInput
    }, 
    'deleteNotice': {
        readonly id: number
    }
}
