package com.hify.workflow.engine.config;

/**
 * KNOWLEDGE 节点配置
 */
public record KnowledgeNodeConfig(
        Long knowledgeBaseId,
        String query,
        Integer topK,
        Float threshold,
        String outputVar,
        String errorBranch
) implements NodeConfig {
}
