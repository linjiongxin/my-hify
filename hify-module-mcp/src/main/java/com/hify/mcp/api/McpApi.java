package com.hify.mcp.api;

import com.hify.mcp.api.dto.McpServerDTO;
import com.hify.mcp.api.dto.McpToolDTO;

import java.util.List;
import java.util.Map;

/**
 * MCP API 接口
 * <p>供其他模块调用的唯一入口</p>
 */
public interface McpApi {

    /**
     * 根据 ID 获取 MCP Server
     */
    McpServerDTO getServerById(Long id);

    /**
     * 获取指定 Server 下的工具列表
     */
    List<McpToolDTO> listToolsByServerId(Long serverId);

    /**
     * 调用 MCP 工具
     *
     * @param serverUrl MCP Server 地址
     * @param toolName  工具名称
     * @param arguments 工具参数
     * @return 工具执行结果文本
     */
    String callTool(String serverUrl, String toolName, Map<String, Object> arguments);
}
