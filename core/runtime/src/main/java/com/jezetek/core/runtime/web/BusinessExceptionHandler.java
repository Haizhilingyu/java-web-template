package com.jezetek.core.runtime.web;

import com.jezetek.core.runtime.BusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 业务异常统一转 400 + {message}。
 * message 字段名与 AuthExceptionHandler 一致，
 * 前端 api 客户端统一读取 error.message 提示
 */
@RestControllerAdvice
public class BusinessExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> business(BusinessException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", e.getMessage() == null ? "业务规则拒绝" : e.getMessage()));
    }
}
