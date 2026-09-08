package com.jezetek.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 启动完成后打印各入口地址。
 * 监听 WebServerInitializedEvent 以拿到实际端口(支持 server.port 随机端口)
 */
@Component
public class StartupUrlLogger implements ApplicationListener<WebServerInitializedEvent> {

    private static final Logger log = LoggerFactory.getLogger(StartupUrlLogger.class);

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        String base = "http://localhost:" + event.getWebServer().getPort();
        log.info("""

                ----------------------------------------------------------
                启动完成，可访问以下地址:
                前端页面:    {}/
                API 文档:    {}/swagger-ui.html
                OpenAPI:     {}/openapi.yml
                TS 客户端:   {}/ts.zip
                H2 控制台:   {}/h2-console
                ----------------------------------------------------------""",
                base, base, base, base, base);
    }
}
