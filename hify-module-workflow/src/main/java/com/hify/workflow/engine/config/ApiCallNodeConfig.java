package com.hify.workflow.engine.config;

import java.util.Map;

/**
 * API_CALL 节点配置
 */
public record ApiCallNodeConfig(
        String url,
        String method,
        Map<String, String> headers,
        String body,
        String outputVar,
        String errorBranch
) implements NodeConfig {
}
