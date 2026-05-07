package com.hify.workflow.engine.config;

import java.util.Map;

/**
 * TOOL 节点配置
 */
public record ToolNodeConfig(
        String toolName,
        Map<String, Object> params,
        String outputVar,
        String errorBranch
) implements NodeConfig {}
