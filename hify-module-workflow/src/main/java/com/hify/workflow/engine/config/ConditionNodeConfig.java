package com.hify.workflow.engine.config;

/**
 * CONDITION 节点配置
 */
public record ConditionNodeConfig(
        String expression,
        String trueBranch,
        String falseBranch,
        String errorBranch
) implements NodeConfig {}
