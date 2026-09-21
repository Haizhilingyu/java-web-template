package com.jezetek.modules.system.service;

import java.lang.reflect.Field;

/**
 * jimmer 生成 Input 的字段读取工具。
 *
 * <p>背景(见 AGENTS.md Jimmer 要点)：集合 id 视图的 getter 懒初始化空列表，
 * 无法用 {@code getXxx() != null} 判断"是否提交"；标量 getter 对未提交值
 * 直接抛异常。这里统一直读字段——调用方需以常量字符串提供字段名，
 * 与 .dto 属性同步(编译期无法校验，语义由测试锁定)</p>
 */
public final class InputFields {

    private InputFields() {
    }

    /** 字段是否被客户端提交过(setter 写过) */
    public static boolean isProvided(Class<?> dtoClass, Object dto, String field) {
        try {
            Field f = dtoClass.getDeclaredField(field);
            f.setAccessible(true);
            return f.get(dto) != null;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(dtoClass.getSimpleName() + " 缺少字段: " + field, e);
        }
    }

    /** 读取 Integer 标量字段原始值(未提交返回 null) */
    public static Integer intValue(Class<?> dtoClass, Object dto, String field) {
        try {
            Field f = dtoClass.getDeclaredField(field);
            f.setAccessible(true);
            return (Integer) f.get(dto);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(dtoClass.getSimpleName() + " 缺少字段: " + field, e);
        }
    }
}
