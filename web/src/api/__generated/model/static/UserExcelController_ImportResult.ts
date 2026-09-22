import type {UserExcelController_RowFailure} from './';

/**
 * 导入结果：total=文件数据行数；failures 回显行号+原因
 */
export interface UserExcelController_ImportResult {
    readonly total: number;
    readonly successCount: number;
    readonly failures: ReadonlyArray<UserExcelController_RowFailure>;
}
