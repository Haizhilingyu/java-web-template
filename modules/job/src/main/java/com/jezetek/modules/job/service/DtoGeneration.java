package com.jezetek.modules.job.service;

import org.babyfish.jimmer.client.EnableImplicitApi;
import org.babyfish.jimmer.sql.EnableDtoGeneration;

/**
 * 实体接口定义在本模块 model 包，jimmer-apt 需要该注解
 * 手动启用 src/main/dto 下 DTO 文件的编译(仅 Java 需要，Kotlin 不需要)
 */
@EnableDtoGeneration
@EnableImplicitApi
public interface DtoGeneration {
}
