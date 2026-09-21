package com.jezetek.core.runtime.log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志标记：标注在写操作端点上，由 system 模块的切面环绕采集
 * （操作人/URI/入参/结果/耗时/异常）并异步落库。
 *
 * <p>纯标记注解，零依赖——可选模块只依赖 core 即可被记录；
 * 切面与落库在必选的 system 模块，故对所有模块生效</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {

    /** 业务模块名(如"用户管理") */
    String module();

    /** 动作名(如"新增用户") */
    String action();
}
