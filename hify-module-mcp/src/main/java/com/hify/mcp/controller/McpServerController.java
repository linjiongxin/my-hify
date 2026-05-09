package com.hify.mcp.controller;

import com.hify.common.web.entity.PageParam;
import com.hify.common.web.entity.PageResult;
import com.hify.common.web.entity.Result;
import com.hify.mcp.entity.McpServer;
import com.hify.mcp.service.McpService;
import com.hify.mcp.service.McpToolService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * MCP Server 控制器
 */
@RestController
@RequestMapping("/mcp-server")
@RequiredArgsConstructor
public class McpServerController {

    private final McpService mcpService;
    private final McpToolService mcpToolService;

    @PostMapping
    public Result<Long> create(@Valid @RequestBody McpServer server) {
        return Result.success(mcpService.createServer(server));
    }

    @GetMapping("/{id}")
    public Result<McpServer> getById(@PathVariable("id") Long id) {
        return Result.success(mcpService.getById(id));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id, @Valid @RequestBody McpServer server) {
        mcpService.updateServer(id, server);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        mcpService.deleteServer(id);
        return Result.success();
    }

    @GetMapping
    public Result<PageResult<McpServer>> page(@Valid PageParam pageParam) {
        return Result.success(mcpService.pageServers(pageParam));
    }

    /**
     * 同步 MCP Server 的工具列表
     */
    @PostMapping("/{id}/sync-tools")
    public Result<Void> syncTools(@PathVariable("id") Long id) {
        mcpToolService.syncTools(id);
        return Result.success();
    }
}
