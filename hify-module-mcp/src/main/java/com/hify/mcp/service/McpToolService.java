package com.hify.mcp.service;

/**
 * MCP Tool 服务接口
 */
public interface McpToolService {

    /**
     * 同步指定 Server 的工具列表
     * <p>连接 MCP Server，调用 listTools，将结果保存到 mcp_tool 表</p>
     *
     * @param serverId MCP Server ID
     */
    void syncTools(Long serverId);
}
