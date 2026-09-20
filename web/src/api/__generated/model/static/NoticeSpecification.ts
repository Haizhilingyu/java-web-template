export interface NoticeSpecification {
    /**
     * 公告标题
     */
    readonly keyword?: string | undefined;
    /**
     * 公告类型，字典 sys_notice_type 的存储值(1 通知 / 2 公告)
     */
    readonly noticeType?: string | undefined;
}
