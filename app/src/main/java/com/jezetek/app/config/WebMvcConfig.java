package com.jezetek.app.config;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

	/**
	 * web 模块的前端产物位于 classpath:/static，前端使用 history 路由，
	 * 未命中静态资源的路径(如 /login)需回退到 index.html 交给前端路由处理，
	 * 否则刷新深层路由会 404；/api 前缀留给后端接口，不做回退。
	 * classpath:/META-INF/resources/ 同时让 webjars 资源(如 swagger-ui)可访问。
	 */
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**")
				.addResourceLocations("classpath:/static/", "classpath:/META-INF/resources/")
				.resourceChain(true)
				.addResolver(new PathResourceResolver() {
					@Override
					protected Resource getResource(String resourcePath, Resource location) throws IOException {
						if (resourcePath.startsWith("api/") || resourcePath.contains("..")) {
							return null;
						}
						Resource requestedResource = location.createRelative(resourcePath);
						if (requestedResource.exists() && requestedResource.isReadable()) {
							return requestedResource;
						}
						return location.createRelative("index.html");
					}
				});
	}
}
