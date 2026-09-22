import type {Executor} from '../';
import type {UserExcelController_ImportResult} from '../model/static/';

/**
 * 用户 Excel 导入导出(工单03)：普通 @RestController 端点。
 * multipart/流式下载与 jimmer 远程服务参数序列化不搭，照 AuthController 的写法——
 * 方法签名不出现 servlet/multipart 类型(规避 jimmer-apt 编译期 NPE)，
 * 一律经当前请求上下文获取。
 * 
 * <p>导出走与列表一致的 Specification + 部门树过滤；数据范围不在此生效
 * (导出权限点仅授予管理员，范围收紧时再接入)</p>
 */
export class UserExcelController {
    
    constructor(private executor: Executor) {}
    
    /**
     * 按当前查询条件全量导出(不分页)，表头中文
     */
    readonly export: (options: UserExcelControllerOptions['export']) => Promise<
        void
    > = async(options) => {
        let _uri = '/api/v1/user/export';
        let _separator = _uri.indexOf('?') === -1 ? '?' : '&';
        let _value: any = undefined;
        _value = options.keyword;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'keyword='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.enabled;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'enabled='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.roleName;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'roleName='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        _value = options.deptId;
        if (_value !== undefined && _value !== null) {
            _uri += _separator
            _uri += 'deptId='
            _uri += encodeURIComponent(_value);
            _separator = '&';
        }
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<void>;
    }
    
    /**
     * 导入模板下载：只有表头的空表
     */
    readonly importTemplate: () => Promise<
        void
    > = async() => {
        let _uri = '/api/v1/user/import-template';
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<void>;
    }
    
    /**
     * Excel 导入：成功行入库、失败行回显行号+原因(与若依行为一致)。
     * 行号 = Excel 数据行号(表头为第 1 行，首条数据为第 2 行)
     */
    readonly importUsers: () => Promise<
        UserExcelController_ImportResult
    > = async() => {
        let _uri = '/api/v1/user/import';
        return (await this.executor({uri: _uri, method: 'POST'})) as Promise<UserExcelController_ImportResult>;
    }
}

export type UserExcelControllerOptions = {
    'export': {
        readonly keyword?: string | undefined, 
        readonly enabled?: boolean | undefined, 
        readonly roleName?: string | undefined, 
        readonly deptId?: number | undefined
    }, 
    'importTemplate': {}, 
    'importUsers': {}
}
