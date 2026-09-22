/**
 * 首页统计
 */
export interface HomeController_HomeSummary {
    readonly userCount: number;
    readonly roleCount: number;
    readonly todayLoginSuccess: number;
    readonly operLogTotal: number;
}
