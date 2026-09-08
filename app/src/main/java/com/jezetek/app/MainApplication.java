package com.jezetek.app;

import org.babyfish.jimmer.client.EnableImplicitApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * scanBasePackages 放宽到 com.jezetek，
 * 让 core 模块的 service/repository/过滤器/拦截器被扫描注册
 */
@EnableImplicitApi
@SpringBootApplication(scanBasePackages = "com.jezetek")
public class MainApplication {

	public static void main(String[] args) {
		SpringApplication.run(MainApplication.class, args);
	}

}
