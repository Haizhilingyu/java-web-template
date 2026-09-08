package com.jezetek.core.runtime.interceptor;

import com.jezetek.core.model.common.BaseEntity;
import com.jezetek.core.model.common.BaseEntityDraft;
import com.jezetek.core.model.common.BaseEntityProps;
import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.sql.DraftInterceptor;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 保存前自动填充 createdTime/modifiedTime，
 * 业务代码无需也不应手工设置这两个字段。
 *
 * <p>实际项目可按需扩展 creator/modifier 等字段，
 * 利用权限系统的当前登录用户填充</p>
 */
@Component
public class BaseEntityDraftInterceptor implements DraftInterceptor<BaseEntity, BaseEntityDraft> {

    @Override
    public void beforeSave(BaseEntityDraft draft, @Nullable BaseEntity original) {
        if (!ImmutableObjects.isLoaded(draft, BaseEntityProps.MODIFIED_TIME)) {
            draft.setModifiedTime(LocalDateTime.now());
        }
        // original == null 表示 INSERT
        if (original == null && !ImmutableObjects.isLoaded(draft, BaseEntityProps.CREATED_TIME)) {
            draft.setCreatedTime(LocalDateTime.now());
        }
    }
}
