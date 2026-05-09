package com.hify.mcp.service;

import com.hify.common.web.filter.TraceIdFilter;
import com.hify.mcp.entity.McpCallLog;
import com.hify.mcp.mapper.McpCallLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MCP 调用记录器
 * <p>在 MCP 工具调用前后写入 mcp_call_log 表</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpCallRecorder {

    private final McpCallLogMapper mapper;

    /**
     * 记录调用开始
     *
     * @param serverUrl   MCP Server 地址
     * @param toolName    工具名称
     * @param requestJson 请求参数 JSON
     * @return 日志记录 ID
     */
    public Long recordStart(String serverUrl, String toolName, String requestJson) {
        McpCallLog logEntry = new McpCallLog();
        logEntry.setServerUrl(serverUrl);
        logEntry.setToolName(toolName);
        logEntry.setRequestJson(requestJson);
        logEntry.setStatus("running");
        logEntry.setTraceId(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY));
        mapper.insert(logEntry);
        return logEntry.getId();
    }

    /**
     * 记录调用成功
     *
     * @param logId        日志记录 ID
     * @param responseJson 响应结果 JSON
     * @param durationMs   耗时（毫秒）
     */
    public void recordSuccess(Long logId, String responseJson, long durationMs) {
        McpCallLog logEntry = new McpCallLog();
        logEntry.setId(logId);
        logEntry.setStatus("success");
        logEntry.setResponseJson(truncate(ensureJson(responseJson), 65535));
        logEntry.setDurationMs((int) durationMs);
        mapper.updateById(logEntry);
    }

    /**
     * 记录调用失败
     *
     * @param logId      日志记录 ID
     * @param errorMsg   错误信息
     * @param durationMs 耗时（毫秒）
     */
    public void recordFailure(Long logId, String errorMsg, long durationMs) {
        McpCallLog logEntry = new McpCallLog();
        logEntry.setId(logId);
        logEntry.setStatus("failed");
        logEntry.setErrorMsg(truncate(errorMsg, 65535));
        logEntry.setDurationMs((int) durationMs);
        mapper.updateById(logEntry);
    }

    /**
     * 截断字符串，避免超长内容写入数据库
     */
    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "\n...[truncated]";
    }

    /**
     * 确保内容可被 PostgreSQL JSONB 接受。
     * 若已是合法 JSON 则原样返回；否则包装为 {"text": "..."}。
     */
    private String ensureJson(String text) {
        if (text == null || text.isBlank()) {
            return "{}";
        }
        try {
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(text);
            return text;
        } catch (Exception e) {
            try {
                return new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(Map.of("text", text));
            } catch (Exception ex) {
                return "{\"text\":\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
            }
        }
    }
}
