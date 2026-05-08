package com.hify.agent;

import com.hify.workflow.api.WorkflowApi;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import static org.mockito.Mockito.mock;

@SpringBootApplication(scanBasePackages = "com.hify.agent")
@ComponentScan(
    basePackages = "com.hify.agent",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.hify\\.agent\\.controller\\..*"
    )
)
@MapperScan("com.hify.agent.mapper")
public class AgentTestApplication {

    @TestConfiguration
    static class MockWorkflowApiConfig {
        @Bean
        public WorkflowApi workflowApi() {
            return mock(WorkflowApi.class);
        }
    }
}
