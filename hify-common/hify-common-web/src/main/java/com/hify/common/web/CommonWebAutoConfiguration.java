package com.hify.common.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Common Web 自动配置
 *
 * @author hify
 */
@Slf4j
@Configuration
@ComponentScan(basePackages = "com.hify.common.web")
public class CommonWebAutoConfiguration {

    public CommonWebAutoConfiguration() {
        log.info("Common Web 模块已加载");
    }

}
