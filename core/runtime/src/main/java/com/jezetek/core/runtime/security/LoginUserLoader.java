package com.jezetek.core.runtime.security;

import org.jetbrains.annotations.Nullable;

/**
 * 按用户 id 加载登录上下文。
 *
 * <p>core 只定义契约；实现由引入了用户体系的业务模块提供
 * (本模板中为 modules/system 的 UserDetailsServiceImpl)。
 * 若应用没有任何模块实现该接口，JWT 过滤器对携带令牌的请求
 * 也不会构造 Authentication，受保护接口一律 401</p>
 */
public interface LoginUserLoader {

    /**
     * @return 用户不存在或已禁用时返回 null(请求按未认证处理)
     */
    @Nullable
    LoginUser load(long userId);
}
