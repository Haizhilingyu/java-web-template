import type {Executor} from '../';

/**
 * 操作日志/登录日志导出(工单04)：复用工单03 的 FastExcel 依赖与
 * 流式下载写法(签名不出现 servlet 类型，见 UserExcelController 类注释)。
 * 权限沿用日志族约定：system:log:export
 */
export class LogExcelController {
    
    constructor(private executor: Executor) {}
    
    readonly exportLogininfors: (options: LogExcelControllerOptions['exportLogininfors']) => Promise<
        void
    > = async(options) => {
        let _uri = '/api/v1/logininfor/export';
        let _separator = _uri.indexOf('?') === -1 ? '?' : '&';
        let _value: any = undefined;
        _value = options.keyword;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'keyword='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<void>;
    }
    
    readonly exportOperLogs: (options: LogExcelControllerOptions['exportOperLogs']) => Promise<
        void
    > = async(options) => {
        let _uri = '/api/v1/operlog/export';
        let _separator = _uri.indexOf('?') === -1 ? '?' : '&';
        let _value: any = undefined;
        _value = options.keyword;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'keyword='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<void>;
    }
}

export type LogExcelControllerOptions = {
    'exportOperLogs': {
        readonly keyword?: string | undefined
    }, 
    'exportLogininfors': {
        readonly keyword?: string | undefined
    }
}
