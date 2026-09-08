package com.jezetek.core.service;

import org.babyfish.jimmer.client.EnableImplicitApi;
import org.babyfish.jimmer.sql.EnableDtoGeneration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * service 模块测试专用启动类：core 各模块(repository/service/runtime)
 * 无自己的 Boot 应用类，由它提供测试上下文。
 * 扫描范围与 app 模块的 MainApplication 保持一致(com.jezetek.core)
 */
@EnableDtoGeneration
@EnableImplicitApi
@SpringBootApplication(scanBasePackages = "com.jezetek.core")
class ServiceTestApplication {
}
