package com.hify.mcp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hify.common.web.entity.PageParam;
import com.hify.common.web.entity.PageResult;
import com.hify.mcp.api.McpApi;
import com.hify.mcp.api.dto.McpServerDTO;
import com.hify.mcp.api.dto.McpToolDTO;
import com.hify.mcp.entity.McpServer;
import com.hify.mcp.entity.McpTool;
import com.hify.mcp.mapper.McpServerMapper;
import com.hify.mcp.mapper.McpToolMapper;
import com.hify.mcp.service.McpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpServiceImpl implements McpService, McpApi {

    private final McpServerMapper serverMapper;
    private final McpToolMapper toolMapper;
    private final com.hify.mcp.config.McpClientManager clientManager;

    @Override
    public Long createServer(McpServer server) {
        serverMapper.insert(server);
        return server.getId();
    }

    @Override
    public void updateServer(Long id, McpServer server) {
        server.setId(id);
        serverMapper.updateById(server);
    }

    @Override
    public void deleteServer(Long id) {
        serverMapper.deleteById(id);
    }

    @Override
    public McpServer getById(Long id) {
        return serverMapper.selectById(id);
    }

    @Override
    public PageResult<McpServer> pageServers(PageParam pageParam) {
        Page<McpServer> page = serverMapper.selectPage(pageParam.toPage(McpServer.class), null);
        return PageResult.of(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    // ===== McpApi 实现 =====

    @Override
    public McpServerDTO getServerById(Long id) {
        McpServer server = serverMapper.selectById(id);
        if (server == null) {
            return null;
        }
        McpServerDTO dto = new McpServerDTO();
        BeanUtils.copyProperties(server, dto);
        return dto;
    }

    @Override
    public List<McpToolDTO> listToolsByServerId(Long serverId) {
        LambdaQueryWrapper<McpTool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(McpTool::getServerId, serverId)
               .eq(McpTool::getEnabled, true);
        List<McpTool> tools = toolMapper.selectList(wrapper);
        return tools.stream().map(this::toToolDto).toList();
    }

    @Override
    public String callTool(String serverUrl, String toolName, java.util.Map<String, Object> arguments) {
        return clientManager.callTool(serverUrl, toolName, arguments);
    }

    private McpToolDTO toToolDto(McpTool tool) {
        McpToolDTO dto = new McpToolDTO();
        BeanUtils.copyProperties(tool, dto);
        return dto;
    }
}
