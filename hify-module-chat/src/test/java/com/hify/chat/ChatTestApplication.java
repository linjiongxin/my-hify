package com.hify.chat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication(scanBasePackages = {"com.hify.chat", "com.hify.common.web"})
@ComponentScan(
    basePackages = {"com.hify.chat", "com.hify.common.web"},
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.hify\\.chat\\.controller\\..*"
    )
)
@MapperScan("com.hify.chat.mapper")
public class ChatTestApplication {
}
