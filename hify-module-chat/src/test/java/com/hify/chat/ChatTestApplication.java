package com.hify.chat;

import com.hify.agent.api.AgentApi;
import com.hify.model.api.LlmGatewayApi;
import com.hify.rag.api.AgentKnowledgeBaseApi;
import com.hify.rag.api.RagSearchApi;
import com.hify.workflow.api.WorkflowApi;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import static org.mockito.Mockito.mock;

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

    @TestConfiguration
    static class MockConfig {
        @Bean
        public AgentKnowledgeBaseApi agentKnowledgeBaseApi() {
            return mock(AgentKnowledgeBaseApi.class);
        }

        @Bean
        public RagSearchApi ragSearchApi() {
            return mock(RagSearchApi.class);
        }

        @Bean
        public AgentApi agentApi() {
            return mock(AgentApi.class);
        }

        @Bean
        public LlmGatewayApi llmGatewayApi() {
            return mock(LlmGatewayApi.class);
        }

        @Bean
        public WorkflowApi workflowApi() {
            return mock(WorkflowApi.class);
        }
    }
}
