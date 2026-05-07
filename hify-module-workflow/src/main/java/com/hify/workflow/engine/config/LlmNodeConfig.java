package com.hify.workflow.engine.config;

/**
 * LLM 节点配置
 */
public record LlmNodeConfig(
        String model,
        String prompt,
        String outputVar,
        String errorBranch
) implements NodeConfig {}
