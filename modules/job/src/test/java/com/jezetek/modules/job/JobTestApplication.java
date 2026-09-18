package com.jezetek.modules.job;

import org.babyfish.jimmer.client.EnableImplicitApi;
import org.babyfish.jimmer.sql.EnableDtoGeneration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * job 模块测试专用启动类，扫描范围与主应用一致(com.jezetek)；
 * 类路径上只有 core + job，天然验证模块可独立装配
 */
@EnableDtoGeneration
@EnableImplicitApi
@SpringBootApplication(scanBasePackages = "com.jezetek")
class JobTestApplication {
}
