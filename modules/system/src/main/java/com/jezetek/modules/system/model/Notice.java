package com.jezetek.modules.system.model;

import org.babyfish.jimmer.sql.*;
import com.jezetek.core.model.common.BaseEntity;
import com.jezetek.core.model.common.TenantAware;
import org.jetbrains.annotations.Nullable;

@Entity
@Table(name = "sys_notice")
public interface Notice extends BaseEntity, TenantAware {

    /**
     * 代理主键，自增，无业务含义
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    /**
     * 公告标题
     */
    String noticeTitle();

    /**
     * 公告类型，字典 sys_notice_type 的存储值(1 通知 / 2 公告)
     */
    String noticeType();

    /**
     * 公告内容，纯文本(textarea)，不引富文本
     */
    @Nullable
    String content();

    /**
     * 状态：启用才对外可见；管理端一律可见
     */
    boolean enabled();
}
