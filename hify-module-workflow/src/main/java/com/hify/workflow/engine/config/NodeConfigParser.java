package com.hify.workflow.engine.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.workflow.config.RetryConfig;
import com.hify.workflow.entity.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 节点配置解析器
 * <p>将 JSON 配置反序列化为类型安全的 record，按 node type 统一分发</p>
 */
@Slf4j
@Component
public class NodeConfigParser {

    private final ObjectMapper objectMapper;

    public NodeConfigParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 解析节点配置
     *
     * @param node 工作流节点
     * @return 类型安全的 NodeConfig record
     */
    public NodeConfig parse(WorkflowNode node) {
        String type = node.getType();
        String configJson = node.getConfig();

        try {
            return switch (type) {
                case "START" -> parseOrEmpty(configJson, StartNodeConfig.class);
                case "END" -> parseOrEmpty(configJson, EndNodeConfig.class);
                case "LLM" -> objectMapper.readValue(configJson, LlmNodeConfig.class);
                case "TOOL" -> objectMapper.readValue(configJson, ToolNodeConfig.class);
                case "CONDITION" -> objectMapper.readValue(configJson, ConditionNodeConfig.class);
                case "APPROVAL" -> objectMapper.readValue(configJson, ApprovalNodeConfig.class);
                case "API_CALL" -> objectMapper.readValue(configJson, ApiCallNodeConfig.class);
                case "KNOWLEDGE" -> objectMapper.readValue(configJson, KnowledgeNodeConfig.class);
                default -> throw new IllegalArgumentException("Unknown node type: " + type);
            };
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse node config for type " + type, e);
        }
    }

    /**
     * 解析重试配置
     *
     * @param retryConfigJson JSON 字符串
     * @return RetryConfig，解析失败返回 null
     */
    public RetryConfig parseRetryConfig(String retryConfigJson) {
        if (retryConfigJson == null || retryConfigJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(retryConfigJson, RetryConfig.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse retry config: {}", e.getMessage());
            return null;
        }
    }

    private <T extends NodeConfig> T parseOrEmpty(String json, Class<T> type) throws JsonProcessingException {
        if (json == null || json.isBlank()) {
            return objectMapper.readValue("{}", type);
        }
        return objectMapper.readValue(json, type);
    }
}
