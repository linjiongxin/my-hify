package com.hify.mcp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hify.mcp.config.McpClientManager;
import com.hify.mcp.entity.McpServer;
import com.hify.mcp.entity.McpTool;
import com.hify.mcp.mapper.McpServerMapper;
import com.hify.mcp.mapper.McpToolMapper;
import com.hify.mcp.service.McpToolService;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * MCP Tool 服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpToolServiceImpl implements McpToolService {

    private final McpServerMapper serverMapper;
    private final McpToolMapper toolMapper;
    private final McpClientManager clientManager;

    @Override
    public void syncTools(Long serverId) {
        McpServer server = serverMapper.selectById(serverId);
        if (server == null) {
            log.warn("MCP Server 不存在: serverId={}", serverId);
            return;
        }
        if (!Boolean.TRUE.equals(server.getEnabled())) {
            log.warn("MCP Server 已禁用: serverId={}", serverId);
            return;
        }
        if (server.getBaseUrl() == null || server.getBaseUrl().isBlank()) {
            log.warn("MCP Server 未配置 baseUrl: serverId={}", serverId);
            return;
        }

        List<McpSchema.Tool> remoteTools = clientManager.listTools(server.getBaseUrl());
        log.info("MCP Server 工具发现完成: serverId={}, name={}, 工具数量={}",
                serverId, server.getName(), remoteTools.size());

        // 软删除该 Server 下现有工具
        LambdaQueryWrapper<McpTool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(McpTool::getServerId, serverId);
        List<McpTool> existingTools = toolMapper.selectList(wrapper);
        for (McpTool t : existingTools) {
            toolMapper.deleteById(t.getId());
        }

        // 保存新工具
        for (McpSchema.Tool rt : remoteTools) {
            McpTool tool = new McpTool();
            tool.setServerId(serverId);
            tool.setName(rt.name());
            tool.setDescription(rt.description());
            tool.setEnabled(true);

            McpSchema.JsonSchema inputSchema = rt.inputSchema();
            if (inputSchema != null) {
                tool.setSchemaJson(Map.of(
                        "type", inputSchema.type() != null ? inputSchema.type() : "object",
                        "properties", inputSchema.properties() != null ? inputSchema.properties() : Map.of(),
                        "required", inputSchema.required() != null ? inputSchema.required() : List.of()
                ));
            } else {
                tool.setSchemaJson(Map.of("type", "object", "properties", Map.of()));
            }
            toolMapper.insert(tool);
        }
    }
}
