/**
 * 公告详情(含富文本 content)
 */
export interface HomeController_NoticeDetail {
    readonly id: number;
    readonly noticeTitle: string;
    readonly noticeType: string;
    readonly content?: string | undefined;
    readonly createdTime: string;
}
