package com.hify.agent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication(scanBasePackages = "com.hify.agent")
@ComponentScan(
    basePackages = "com.hify.agent",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.hify\\.agent\\.(controller|service|api)\\..*"
    )
)
@MapperScan("com.hify.agent.mapper")
public class AgentTestApplication {
}