package com.hify.workflow.engine.config;

/**
 * APPROVAL 节点配置
 */
public record ApprovalNodeConfig(
        String prompt,
        String errorBranch
) implements NodeConfig {}
