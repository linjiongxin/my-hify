package com.hify.mcp.service;

import com.hify.common.web.entity.PageParam;
import com.hify.common.web.entity.PageResult;
import com.hify.mcp.entity.McpServer;

/**
 * MCP Server 服务接口
 */
public interface McpService {

    Long createServer(McpServer server);

    void updateServer(Long id, McpServer server);

    void deleteServer(Long id);

    McpServer getById(Long id);

    PageResult<McpServer> pageServers(PageParam pageParam);
}
