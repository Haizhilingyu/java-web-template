export type NoticeDto = {
    /**
     * 默认抓取形状：全部标量属性(含 content，管理端弹窗直接展示)
     */
    'NoticeService/DEFAULT_FETCHER': {
        /**
         * 代理主键，自增，无业务含义
         */
        readonly id: number;
        /**
         * The time when the object was created.
         * 
         * <p>In this example, this property is not
         * explicitly modified by business code,
         * but is automatically modified by {@code DraftInterceptor}</p>
         */
        readonly createdTime: string;
        /**
         * The time when the object was last modified
         * 
         * <p>In this example, this property is not
         * explicitly modified by business code,
         * but is automatically modified by {@code DraftInterceptor}</p>
         */
        readonly modifiedTime: string;
        /**
         * The tenant to which the current object belongs.
         * 
         * <p>For the database in this example,
         * there are two values: {@code a} and {code b}</p>.
         * 
         * <p>In this example, this property is not
         * explicitly modified by business code,
         * but is automatically modified by {@code DraftInterceptor}</p>
         */
        readonly tenant: string;
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
}
