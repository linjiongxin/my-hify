package com.hify.workflow.engine.config;

/**
 * START 节点配置
 */
public record StartNodeConfig(String errorBranch) implements NodeConfig {}
