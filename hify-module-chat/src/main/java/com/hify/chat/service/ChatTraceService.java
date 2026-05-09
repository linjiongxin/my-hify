package com.hify.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hify.chat.entity.ChatMessage;
import com.hify.chat.entity.trace.TraceMcpCallLog;
import com.hify.chat.entity.trace.TraceRagRetrievalLog;
import com.hify.chat.entity.trace.TraceWorkflowInstance;
import com.hify.chat.entity.trace.TraceWorkflowNodeExecution;
import com.hify.chat.mapper.ChatMessageMapper;
import com.hify.chat.mapper.trace.ChatTraceMcpCallLogMapper;
import com.hify.chat.mapper.trace.ChatTraceRagRetrievalLogMapper;
import com.hify.chat.mapper.trace.ChatTraceWorkflowInstanceMapper;
import com.hify.chat.mapper.trace.ChatTraceWorkflowNodeExecutionMapper;
import com.hify.chat.vo.ChatTraceEventVO;
import com.hify.chat.vo.ChatTraceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 对话链路追踪 Service
 */
@Service
@RequiredArgsConstructor
public class ChatTraceService {

    private final ChatMessageMapper chatMessageMapper;
    private final ChatTraceMcpCallLogMapper mcpCallLogMapper;
    private final ChatTraceRagRetrievalLogMapper ragRetrievalLogMapper;
    private final ChatTraceWorkflowInstanceMapper workflowInstanceMapper;
    private final ChatTraceWorkflowNodeExecutionMapper workflowNodeExecutionMapper;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public ChatTraceVO buildTrace(String traceId) {
        ChatTraceVO trace = new ChatTraceVO();
        trace.setTraceId(traceId);

        List<ChatTraceEventVO> events = new ArrayList<>();

        // 1. 对话消息
        List<ChatMessage> messages = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getTraceId, traceId)
                        .orderByAsc(ChatMessage::getCreatedAt)
        );

        String userMessage = null;
        Integer totalDurationMs = 0;
        for (ChatMessage msg : messages) {
            if ("user".equals(msg.getRole())) {
                userMessage = msg.getContent();
            }
            ChatTraceEventVO event = new ChatTraceEventVO();
            event.setType("user".equals(msg.getRole()) ? "user_message" : "llm_reply");
            event.setTitle("user".equals(msg.getRole()) ? "用户消息" : "LLM 回复");
            event.setTime(msg.getCreatedAt() != null ? msg.getCreatedAt().format(FORMATTER) : null);
            event.setDurationMs(msg.getDurationMs());
            event.setStatus(msg.getStatus());

            Map<String, Object> details = new LinkedHashMap<>();
            details.put("content", msg.getContent());
            if (msg.getModel() != null) details.put("model", msg.getModel());
            if (msg.getInputTokens() != null) details.put("inputTokens", msg.getInputTokens());
            if (msg.getOutputTokens() != null) details.put("outputTokens", msg.getOutputTokens());
            if (msg.getFinishReason() != null) details.put("finishReason", msg.getFinishReason());
            event.setDetails(details);

            events.add(event);

            if (msg.getDurationMs() != null) {
                totalDurationMs += msg.getDurationMs();
            }
        }

        trace.setUserMessage(userMessage != null ? userMessage : "未知消息");
        trace.setTotalDurationMs(totalDurationMs);

        // 2. MCP 调用
        List<TraceMcpCallLog> mcpLogs = mcpCallLogMapper.selectList(
                new LambdaQueryWrapper<TraceMcpCallLog>()
                        .eq(TraceMcpCallLog::getTraceId, traceId)
                        .orderByAsc(TraceMcpCallLog::getCreatedAt)
        );
        for (TraceMcpCallLog log : mcpLogs) {
            ChatTraceEventVO event = new ChatTraceEventVO();
            event.setType("mcp");
            event.setTitle("MCP 调用: " + log.getToolName());
            event.setTime(log.getCreatedAt() != null ? log.getCreatedAt().format(FORMATTER) : null);
            event.setDurationMs(log.getDurationMs());
            event.setStatus(log.getStatus());

            Map<String, Object> details = new LinkedHashMap<>();
            details.put("serverUrl", log.getServerUrl());
            details.put("toolName", log.getToolName());
            details.put("request", log.getRequestJson());
            details.put("response", log.getResponseJson());
            if (log.getErrorMsg() != null) details.put("error", log.getErrorMsg());
            event.setDetails(details);

            events.add(event);
        }

        // 3. RAG 检索
        List<TraceRagRetrievalLog> ragLogs = ragRetrievalLogMapper.selectList(
                new LambdaQueryWrapper<TraceRagRetrievalLog>()
                        .eq(TraceRagRetrievalLog::getTraceId, traceId)
                        .orderByAsc(TraceRagRetrievalLog::getCreatedAt)
        );
        for (TraceRagRetrievalLog log : ragLogs) {
            ChatTraceEventVO event = new ChatTraceEventVO();
            event.setType("rag");
            event.setTitle("RAG 检索");
            event.setTime(log.getCreatedAt() != null ? log.getCreatedAt().format(FORMATTER) : null);
            event.setDurationMs(log.getDurationMs());
            event.setStatus("completed");

            Map<String, Object> details = new LinkedHashMap<>();
            details.put("query", log.getQuery());
            details.put("knowledgeBase", log.getKbId());
            details.put("resultCount", log.getResultCount());
            details.put("topChunks", log.getTopChunks());
            event.setDetails(details);

            events.add(event);
        }

        // 4. 工作流
        List<TraceWorkflowInstance> wfInstances = workflowInstanceMapper.selectList(
                new LambdaQueryWrapper<TraceWorkflowInstance>()
                        .eq(TraceWorkflowInstance::getTraceId, traceId)
                        .orderByAsc(TraceWorkflowInstance::getCreatedAt)
        );
        for (TraceWorkflowInstance instance : wfInstances) {
            int duration = 0;
            if (instance.getStartedAt() != null && instance.getFinishedAt() != null) {
                duration = (int) java.time.Duration.between(instance.getStartedAt(), instance.getFinishedAt()).toMillis();
            }

            ChatTraceEventVO event = new ChatTraceEventVO();
            event.setType("workflow");
            event.setTitle("工作流执行");
            event.setTime(instance.getStartedAt() != null ? instance.getStartedAt().format(FORMATTER) : null);
            event.setEndTime(instance.getFinishedAt() != null ? instance.getFinishedAt().format(FORMATTER) : null);
            event.setDurationMs(duration > 0 ? duration : null);
            event.setStatus(instance.getStatus());

            // 查询节点执行记录
            List<TraceWorkflowNodeExecution> nodes = workflowNodeExecutionMapper.selectList(
                    new LambdaQueryWrapper<TraceWorkflowNodeExecution>()
                            .eq(TraceWorkflowNodeExecution::getExecutionId, instance.getId())
                            .orderByAsc(TraceWorkflowNodeExecution::getStartedAt)
            );
            List<Map<String, Object>> nodeList = new ArrayList<>();
            for (TraceWorkflowNodeExecution node : nodes) {
                Map<String, Object> n = new LinkedHashMap<>();
                n.put("nodeId", node.getNodeId());
                n.put("name", node.getNodeId());
                n.put("nodeType", node.getNodeType());
                n.put("type", node.getNodeType());
                n.put("status", node.getStatus());
                int nodeDuration = 0;
                if (node.getStartedAt() != null && node.getEndedAt() != null) {
                    nodeDuration = (int) java.time.Duration.between(node.getStartedAt(), node.getEndedAt()).toMillis();
                }
                n.put("durationMs", nodeDuration);
                nodeList.add(n);
            }

            Map<String, Object> details = new LinkedHashMap<>();
            details.put("workflowId", instance.getWorkflowId());
            details.put("instanceId", instance.getId());
            details.put("currentNodeId", instance.getCurrentNodeId());
            details.put("context", instance.getContext());
            if (instance.getErrorMsg() != null) details.put("error", instance.getErrorMsg());
            details.put("nodes", nodeList);
            event.setDetails(details);

            events.add(event);
        }

        // 按时间排序
        events.sort(Comparator.comparing(e -> e.getTime() != null ? e.getTime() : ""));

        trace.setEvents(events);
        return trace;
    }
}
