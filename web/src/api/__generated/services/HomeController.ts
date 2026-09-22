import type {Executor} from '../';
import type {HomeController_HomeSummary, HomeController_NoticeBrief, HomeController_NoticeDetail} from '../model/static/';

/**
 * 首页轻量版(工单09)：仅 authenticated、无权限点要求——
 * demo/USER 也能看到统计与启用公告(公告管理接口本身仍有权限点)。
 * apiPrefixes 已声明 /api/v1/home/**(SystemModuleProvider)
 */
export class HomeController {
    
    constructor(private executor: Executor) {}
    
    /**
     * 公告详情：启用才可见(含 content 富文本)，否则 400
     */
    readonly notice: (options: HomeControllerOptions['notice']) => Promise<
        HomeController_NoticeDetail
    > = async(options) => {
        let _uri = '/api/v1/home/notices/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<HomeController_NoticeDetail>;
    }
    
    /**
     * 公告列表卡：启用中的最新 5 条，标题级字段
     */
    readonly notices: () => Promise<
        ReadonlyArray<HomeController_NoticeBrief>
    > = async() => {
        let _uri = '/api/v1/home/notices';
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<ReadonlyArray<HomeController_NoticeBrief>>;
    }
    
    /**
     * 统计卡：用户数/角色数/今日登录成功次数/操作日志总数
     */
    readonly summary: () => Promise<
        HomeController_HomeSummary
    > = async() => {
        let _uri = '/api/v1/home/summary';
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<HomeController_HomeSummary>;
    }
}

export type HomeControllerOptions = {
    'summary': {}, 
    'notices': {}, 
    'notice': {
        readonly id: number
    }
}
