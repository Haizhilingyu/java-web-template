package com.jezetek.core.runtime;

/**
 * 业务规则拒绝(如删除被引用的数据)，与参数校验失败(HTTP 400)同级；
 * 由 {@code BusinessExceptionHandler} 统一转 400 + {message}，
 * 前端直接读取 message 提示
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
