import type {Executor} from '../';
import type {PostDto} from '../model/dto/';
import type {Page, PostInput, PostSpecification} from '../model/static/';

export class PostService {
    
    constructor(private executor: Executor) {}
    
    /**
     * 生命周期约束：岗位被用户绑定时禁止删除；
     * 禁用不追溯，已担任该岗位的用户照常生效
     */
    readonly deletePost: (options: PostServiceOptions['deletePost']) => Promise<
        void
    > = async(options) => {
        let _uri = '/api/v1/post/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'DELETE'})) as Promise<void>;
    }
    
    readonly findPost: (options: PostServiceOptions['findPost']) => Promise<
        PostDto['PostService/DEFAULT_FETCHER'] | undefined
    > = async(options) => {
        let _uri = '/api/v1/post/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<PostDto['PostService/DEFAULT_FETCHER'] | undefined>;
    }
    
    readonly findPostsBySuperQBE: (options: PostServiceOptions['findPostsBySuperQBE']) => Promise<
        Page<PostDto['PostService/DEFAULT_FETCHER']>
    > = async(options) => {
        let _uri = '/api/v1/post/list/bySuperQBE';
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
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<Page<PostDto['PostService/DEFAULT_FETCHER']>>;
    }
    
    readonly savePost: (options: PostServiceOptions['savePost']) => Promise<
        PostDto['PostService/DEFAULT_FETCHER']
    > = async(options) => {
        let _uri = '/api/v1/post';
        return (await this.executor({uri: _uri, method: 'PUT', body: options.body})) as Promise<PostDto['PostService/DEFAULT_FETCHER']>;
    }
}

export type PostServiceOptions = {
    'findPostsBySuperQBE': {
        readonly pageIndex?: number | undefined, 
        readonly pageSize?: number | undefined, 
        readonly sortCode?: string | undefined, 
        readonly specification: PostSpecification
    }, 
    'findPost': {
        readonly id: number
    }, 
    'savePost': {
        readonly body: PostInput
    }, 
    'deletePost': {
        readonly id: number
    }
}
