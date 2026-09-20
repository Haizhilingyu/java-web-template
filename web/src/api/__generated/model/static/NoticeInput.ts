export interface NoticeInput {
    /**
     * 代理主键，自增，无业务含义
     */
    readonly id?: number | undefined;
    /**
     * 公告标题
     */
    readonly noticeTitle: string;
    /**
     * 公告类型，字典 sys_notice_type 的存储值(1 通知 / 2 公告)
     */
    readonly noticeType: string;
    /**
     * 公告内容，纯文本(textarea)，不引富文本
     */
    readonly content?: string | undefined;
    /**
     * 状态：启用才对外可见；管理端一律可见
     */
    readonly enabled: boolean;
}
