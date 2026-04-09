package com.hify.common.web.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 *
 * @author hify
 */
@Slf4j
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 跨域配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        log.info("配置 CORS 跨域支持");

        registry.addMapping("/**")
                // 允许的源（生产环境应配置具体域名）
                .allowedOriginPatterns("*")
                // 允许的 HTTP 方法
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
                // 允许的请求头
                .allowedHeaders("*")
                // 暴露的响应头（SSE 需要）
                .exposedHeaders("Content-Type", "X-Request-ID", "Last-Event-ID")
                // 是否允许携带凭证
                .allowCredentials(true)
                // 预检请求缓存时间
                .maxAge(3600);
    }

}
