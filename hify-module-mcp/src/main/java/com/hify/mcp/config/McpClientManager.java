package com.hify.mcp.config;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MCP Client 管理器
 * <p>按 Server URL 全局缓存复用 McpSyncClient，避免 HttpClient 资源泄漏</p>
 */
@Slf4j
@Component
public class McpClientManager {

    private final Map<String, McpSyncClient> clients = new ConcurrentHashMap<>();

    /**
     * 调用 MCP 工具
     */
    public String callTool(String serverUrl, String toolName, Map<String, Object> arguments) {
        McpSyncClient client = clients.computeIfAbsent(serverUrl, this::createClient);
        try {
            McpSchema.CallToolResult result = client.callTool(
                    new McpSchema.CallToolRequest(toolName, arguments)
            );
            return extractResult(result);
        } catch (Exception e) {
            log.warn("MCP 工具调用失败，移除缓存 client: serverUrl={}, toolName={}", serverUrl, toolName, e);
            clients.remove(serverUrl);
            throw e;
        }
    }

    /**
     * 列出 Server 的所有工具
     */
    public List<McpSchema.Tool> listTools(String serverUrl) {
        McpSyncClient client = clients.computeIfAbsent(serverUrl, this::createClient);
        try {
            McpSchema.ListToolsResult result = client.listTools();
            return result.tools();
        } catch (Exception e) {
            log.warn("MCP 工具发现失败，移除缓存 client: serverUrl={}", serverUrl, e);
            clients.remove(serverUrl);
            throw e;
        }
    }

    private McpSyncClient createClient(String serverUrl) {
        log.info("创建 MCP Client: serverUrl={}", serverUrl);
        var transport = HttpClientSseClientTransport.builder(serverUrl).build();
        var client = McpClient.sync(transport).build();
        client.initialize();
        return client;
    }

    private String extractResult(McpSchema.CallToolResult result) {
        if (result == null || result.content() == null || result.content().isEmpty()) {
            return "";
        }
        if (Boolean.TRUE.equals(result.isError())) {
            String errorText = result.content().stream()
                    .map(c -> {
                        if (c instanceof McpSchema.TextContent tc) {
                            return tc.text();
                        }
                        return c.toString();
                    })
                    .collect(Collectors.joining());
            log.warn("MCP 工具返回错误: {}", errorText);
            return "工具执行出错: " + errorText;
        }
        return result.content().stream()
                .map(c -> {
                    if (c instanceof McpSchema.TextContent tc) {
                        return tc.text();
                    }
                    return c.toString();
                })
                .collect(Collectors.joining());
    }

    @PreDestroy
    public void shutdown() {
        log.info("关闭所有 MCP Client, 数量={}", clients.size());
        clients.forEach((url, client) -> {
            try {
                client.close();
            } catch (Exception e) {
                log.warn("关闭 MCP Client 失败: url={}", url, e);
            }
        });
        clients.clear();
    }
}
