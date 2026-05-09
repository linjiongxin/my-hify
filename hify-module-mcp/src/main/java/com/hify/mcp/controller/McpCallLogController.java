package com.hify.mcp.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hify.common.web.entity.PageParam;
import com.hify.common.web.entity.PageResult;
import com.hify.common.web.entity.Result;
import com.hify.mcp.entity.McpCallLog;
import com.hify.mcp.mapper.McpCallLogMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * MCP 调用日志 Controller
 */
@RestController
@RequestMapping("/mcp/call-logs")
@RequiredArgsConstructor
public class McpCallLogController {

    private final McpCallLogMapper callLogMapper;

    @GetMapping
    public Result<PageResult<McpCallLog>> list(
            @RequestParam(name = "serverUrl", required = false) String serverUrl,
            @RequestParam(name = "toolName", required = false) String toolName,
            @RequestParam(name = "status", required = false) String status,
            @Valid PageParam pageParam) {

        LambdaQueryWrapper<McpCallLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(McpCallLog::getDeleted, false);

        if (StringUtils.hasText(serverUrl)) {
            wrapper.eq(McpCallLog::getServerUrl, serverUrl);
        }
        if (StringUtils.hasText(toolName)) {
            wrapper.like(McpCallLog::getToolName, toolName);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(McpCallLog::getStatus, status);
        }

        wrapper.orderByDesc(McpCallLog::getCreatedAt);

        Page<McpCallLog> page = callLogMapper.selectPage(pageParam.toPage(McpCallLog.class), wrapper);
        return Result.success(PageResult.of(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal()));
    }
}
