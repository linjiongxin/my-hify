package com.hify.workflow.engine.executor;

import com.hify.workflow.engine.NodeResult;
import com.hify.workflow.engine.config.ApiCallNodeConfig;
import com.hify.workflow.engine.config.NodeConfig;
import com.hify.workflow.engine.context.ExecutionContext;
import com.hify.workflow.entity.WorkflowNode;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * API_CALL 节点执行器（v2）
 * <p>发起 HTTP 请求并将响应写入上下文</p>
 */
@Component
public class ApiCallNodeExecutor implements NodeExecutor {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String nodeType() {
        return "API_CALL";
    }

    @Override
    public NodeResult execute(WorkflowNode node, NodeConfig config, ExecutionContext context) {
        try {
            ApiCallNodeConfig apiConfig = (ApiCallNodeConfig) config;

            if (apiConfig.url() == null || apiConfig.method() == null) {
                return NodeResult.failure("API_CALL node config missing url or method");
            }

            String resolvedUrl = context.resolve(apiConfig.url());
            String resolvedBody = apiConfig.body() != null ? context.resolve(apiConfig.body()) : null;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (apiConfig.headers() != null) {
                apiConfig.headers().forEach((k, v) -> headers.add(k, context.resolve(v)));
            }

            HttpEntity<String> entity = new HttpEntity<>(resolvedBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    resolvedUrl,
                    HttpMethod.valueOf(apiConfig.method().toUpperCase()),
                    entity,
                    String.class
            );

            String body = response.getBody();
            if (body == null) {
                body = "";
            }

            if (apiConfig.outputVar() != null && !apiConfig.outputVar().isEmpty()) {
                context.set(node.getNodeId(), apiConfig.outputVar(), body);
                context.put(apiConfig.outputVar(), body);
            }

            return NodeResult.success(null);

        } catch (RestClientException e) {
            return NodeResult.failure("API call failed: " + e.getMessage());
        } catch (Exception e) {
            return NodeResult.failure("API_CALL execution failed: " + e.getMessage());
        }
    }
}
