package com.hify.workflow.engine.config;

/**
 * END 节点配置
 */
public record EndNodeConfig(String errorBranch) implements NodeConfig {}
