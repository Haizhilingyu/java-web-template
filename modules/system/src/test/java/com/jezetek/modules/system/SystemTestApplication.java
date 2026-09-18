package com.jezetek.modules.system;

import org.babyfish.jimmer.client.EnableImplicitApi;
import org.babyfish.jimmer.sql.EnableDtoGeneration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * system 模块测试专用启动类。
 * 扫描范围与主应用的 MainApplication 保持一致(com.jezetek)，
 * 让 core 的安全设施(SecurityConfig/JWT 过滤器)一并进入测试上下文
 */
@EnableDtoGeneration
@EnableImplicitApi
@SpringBootApplication(scanBasePackages = "com.jezetek")
class SystemTestApplication {
}
