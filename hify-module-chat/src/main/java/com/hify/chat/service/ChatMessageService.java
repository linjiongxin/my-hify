package com.hify.chat.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.agent.api.AgentApi;
import com.hify.agent.api.dto.AgentDTO;
import com.hify.agent.api.dto.AgentToolDTO;
import com.hify.chat.entity.ChatMessage;
import com.hify.rag.api.AgentKnowledgeBaseApi;
import com.hify.rag.api.RagSearchApi;
import com.hify.rag.vo.AgentKnowledgeBaseVO;
import com.hify.rag.vo.RagSearchResult;
import com.hify.chat.entity.ChatSession;
import com.hify.chat.mapper.ChatMessageMapper;
import com.hify.chat.mapper.ChatSessionMapper;
import com.hify.chat.vo.ChatMessageVO;
import com.hify.common.core.enums.ResultCode;
import com.hify.common.core.exception.BizException;
import com.hify.common.core.util.LlmOutputCleaner;
import com.hify.mcp.api.McpApi;
import com.hify.model.api.LlmGatewayApi;
import com.hify.model.api.dto.LlmChatRequest;
import com.hify.model.api.dto.LlmChatResponse;
import com.hify.model.api.dto.LlmMessage;
import com.hify.model.api.dto.LlmStreamChunk;
import com.hify.model.api.dto.LlmToolCall;
import com.hify.model.api.dto.LlmToolDefinition;
import com.hify.workflow.api.WorkflowApi;
import com.hify.workflow.api.dto.WorkflowDTO;
import com.hify.workflow.api.dto.WorkflowInstanceDTO;
import com.hify.workflow.api.dto.WorkflowStartRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 对话消息 Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageMapper chatMessageMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatSessionService chatSessionService;
    private final LlmGatewayApi llmGatewayApi;
    private final AgentApi agentApi;
    private final AgentKnowledgeBaseApi agentKnowledgeBaseApi;
    private final RagSearchApi ragSearchApi;
    private final WorkflowApi workflowApi;
    private final McpApi mcpApi;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /** workflow 模式轮询间隔（毫秒） */
    private static final long WORKFLOW_POLL_INTERVAL_MS = 500;
    /** workflow 模式最大轮询次数（30 秒） */
    private static final int WORKFLOW_MAX_POLL_ATTEMPTS = 60;

    /** 从 DB/Redis 加载的最大原始消息条数（20 轮 × 2） */
    private static final int MAX_HISTORY_MESSAGES = 40;
    /** 默认上下文 Token 预算（含 system prompt） */
    private static final int DEFAULT_MAX_CONTEXT_TOKENS = 4000;
    /** 字符数转 token 估算系数：1 token ≈ 3 字符（混合中英文保守估计） */
    private static final double CHARS_PER_TOKEN = 3.0;
    /** Redis 消息缓存 key */
    private static final String REDIS_KEY_MESSAGES = "chat:session:%s:messages";
    /** Redis 缓存 TTL（分钟） */
    private static final long REDIS_TTL_MINUTES = 30;

    /**
     * 流式对话
     */
    public void streamChat(Long userId, Long sessionId, String message, SseEmitter emitter) {
        long startTime = System.currentTimeMillis();
        ChatSession session = chatSessionService.getSessionOrThrow(userId, sessionId);
        AgentDTO agent = getAgentOrThrow(session.getAgentId());

        // 保存用户消息
        ChatMessage userMsg = saveUserMessage(sessionId, message);

        // 构建 RAG 检索上下文
        String ragContext = buildRagContext(agent, message);

        // 构建 LLM 上下文（系统提示 + 历史消息滑动窗口 + Token 预算）
        List<LlmMessage> context = buildLlmContext(sessionId, agent, message, ragContext);

        // 将 RAG 上下文注入最后一条用户消息（模型对 user message 中的指令遵循度更高）
        String finalUserMessage = buildFinalUserMessage(message, ragContext);
        context.add(LlmMessage.user(finalUserMessage));

        // 创建 assistant 占位消息
        ChatMessage assistantMsg = createAssistantPlaceholder(sessionId, userMsg.getSeq() + 1, agent.getModelId());

        try {
            LlmChatRequest request = buildLlmChatRequest(agent, context);
            // 如果存在工具，先非流式探测一轮，规避部分模型流式+工具的兼容性问题（如 MiniMax CANCEL）
            if (request.getTools() != null && !request.getTools().isEmpty()) {
                handleChatWithTools(agent, context, assistantMsg, emitter, startTime, session, sessionId, request);
            } else {
                doStreamChat(agent, context, assistantMsg, emitter, startTime, session, sessionId);
            }
        } catch (Exception e) {
            log.error("LLM 调用失败, sessionId={}", sessionId, e);
            handleLlmError(assistantMsg, emitter, e);
        }
    }

    /**
     * 处理带工具的对话：先非流式探测 tool_calls，再流式生成最终回复
     */
    private void handleChatWithTools(AgentDTO agent, List<LlmMessage> context, ChatMessage assistantMsg,
                                     SseEmitter emitter, long startTime, ChatSession session, Long sessionId,
                                     LlmChatRequest request) throws Exception {
        LlmChatResponse probe = llmGatewayApi.chat(agent.getModelId(), request);

        if (probe.getToolCalls() != null && !probe.getToolCalls().isEmpty()) {
            List<LlmToolCall> toolCalls = probe.getToolCalls();
            emitter.send(SseEmitter.event()
                    .name("tool_calling")
                    .data(Map.of("tools", toolCalls.stream().map(LlmToolCall::getName).toList())));

            // 必须先添加 assistant 的 tool_calls 消息，再添加 tool 结果，否则 API 会报 tool_call_id 找不到
            context.add(LlmMessage.assistantWithToolCalls(toolCalls));

            for (LlmToolCall tc : toolCalls) {
                String result = executeTool(tc, agent);
                emitter.send(SseEmitter.event()
                        .name("tool_result")
                        .data(Map.of("tool", tc.getName(), "result", result)));
                context.add(LlmMessage.tool(tc.getId(), tc.getName(), result));
            }

            // 第二轮生成最终回复（非流式，规避部分模型流式+tool上下文的兼容性问题）
            LlmChatRequest finalRequest = buildLlmChatRequest(agent, context, true);
            finalRequest.setStream(false);
            LlmChatResponse finalResponse = llmGatewayApi.chat(agent.getModelId(), finalRequest);
            String finalContent = finalResponse.getContent() != null ? finalResponse.getContent() : "";
            String cleaned = LlmOutputCleaner.stripThinking(finalContent);
            if (!cleaned.isEmpty()) {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(Map.of("content", cleaned, "timestamp", System.currentTimeMillis())));
            }
            finalizeAssistantMessage(assistantMsg, cleaned,
                    LlmStreamChunk.builder().finish(true).finishReason("stop").build(), startTime);
            updateSessionAfterChat(session, cleaned);
            cacheMessages(sessionId);
            sendDoneEvent(emitter, assistantMsg.getId());
        } else {
            // 无需调用工具，直接返回非流式结果并模拟流式推送
            String content = probe.getContent() != null ? probe.getContent() : "";
            String cleaned = LlmOutputCleaner.stripThinking(content);
            if (!cleaned.isEmpty()) {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(Map.of("content", cleaned, "timestamp", System.currentTimeMillis())));
            }
            finalizeAssistantMessage(assistantMsg, cleaned,
                    LlmStreamChunk.builder().finish(true).finishReason("stop").build(), startTime);
            updateSessionAfterChat(session, cleaned);
            cacheMessages(sessionId);
            sendDoneEvent(emitter, assistantMsg.getId());
        }
    }

    /**
     * 执行流式对话，支持工具调用循环
     */
    private void doStreamChat(AgentDTO agent, List<LlmMessage> context, ChatMessage assistantMsg,
                              SseEmitter emitter, long startTime, ChatSession session, Long sessionId) throws Exception {
        doStreamChat(agent, context, assistantMsg, emitter, startTime, session, sessionId, false);
    }

    private void doStreamChat(AgentDTO agent, List<LlmMessage> context, ChatMessage assistantMsg,
                              SseEmitter emitter, long startTime, ChatSession session, Long sessionId,
                              boolean skipTools) throws Exception {
        AtomicReference<StringBuilder> contentRef = new AtomicReference<>(new StringBuilder());
        AtomicBoolean inThinkBlock = new AtomicBoolean(false);
        AtomicReference<List<LlmToolCall>> toolCallsRef = new AtomicReference<>();
        AtomicBoolean hasToolCalls = new AtomicBoolean(false);
        AtomicReference<LlmStreamChunk> lastChunkRef = new AtomicReference<>();

        LlmChatRequest request = buildLlmChatRequest(agent, context, skipTools);
        llmGatewayApi.streamChat(agent.getModelId(), request, chunk -> {
            try {
                if (chunk.getError() != null && !chunk.getError().isEmpty()) {
                    log.error("LLM 流式返回错误, sessionId={}, error={}", sessionId, chunk.getError());
                    handleLlmError(assistantMsg, emitter, new RuntimeException(chunk.getError()));
                    return;
                }

                if (chunk.getToolCalls() != null && !chunk.getToolCalls().isEmpty()) {
                    toolCallsRef.set(chunk.getToolCalls());
                }

                String text = chunk.getContent();
                if (text != null && !text.isEmpty()) {
                    String filtered = filterStreamingThink(text, inThinkBlock);
                    contentRef.get().append(text);
                    if (filtered != null && !filtered.isEmpty()) {
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data(Map.of("content", filtered, "timestamp", System.currentTimeMillis())));
                    }
                }

                if (Boolean.TRUE.equals(chunk.getFinish())) {
                    lastChunkRef.set(chunk);
                    if ("tool_calls".equals(chunk.getFinishReason())) {
                        hasToolCalls.set(true);
                    } else {
                        String fullContent = LlmOutputCleaner.stripThinking(contentRef.get().toString());
                        finalizeAssistantMessage(assistantMsg, fullContent, chunk, startTime);
                        updateSessionAfterChat(session, fullContent);
                        cacheMessages(sessionId);
                        sendDoneEvent(emitter, assistantMsg.getId());
                    }
                }
            } catch (Exception e) {
                log.error("SSE 发送失败, sessionId={}", sessionId, e);
                emitter.completeWithError(e);
            }
        });

        if (hasToolCalls.get() && toolCallsRef.get() != null) {
            List<LlmToolCall> toolCalls = toolCallsRef.get();
            emitter.send(SseEmitter.event()
                    .name("tool_calling")
                    .data(Map.of("tools", toolCalls.stream().map(LlmToolCall::getName).toList())));

            // 必须先添加 assistant 的 tool_calls 消息，再添加 tool 结果
            context.add(LlmMessage.assistantWithToolCalls(toolCalls));

            for (LlmToolCall tc : toolCalls) {
                String result = executeTool(tc, agent);
                emitter.send(SseEmitter.event()
                        .name("tool_result")
                        .data(Map.of("tool", tc.getName(), "result", result)));
                context.add(LlmMessage.tool(tc.getId(), tc.getName(), result));
            }

            contentRef.set(new StringBuilder());
            inThinkBlock.set(false);
            doStreamChat(agent, context, assistantMsg, emitter, startTime, session, sessionId);
        }
    }

    private String buildFinalUserMessage(String message, String ragContext) {
        if (ragContext != null && !ragContext.isBlank()) {
            return """
                    请严格根据以下参考信息回答问题。要求：
                    1. 优先使用参考信息中的原文内容，尽量完整呈现，不要概括或简化
                    2. 如果参考信息足够回答问题，不要添加你自己的知识
                    3. 如果参考信息不足，明确告知用户"根据现有资料，暂时无法找到完整答案"

                    <参考信息>
                    %s
                    </参考信息>

                    用户问题：%s
                    """.formatted(ragContext, message);
        }
        return message;
    }

    private AgentDTO getAgentOrThrow(Long agentId) {
        AgentDTO agent = agentApi.getAgentById(agentId);
        if (agent == null || !Boolean.TRUE.equals(agent.getEnabled())) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "Agent 不存在或已禁用");
        }
        return agent;
    }

    private ChatMessage saveUserMessage(Long sessionId, String message) {
        Integer maxSeq = chatMessageMapper.selectMaxSeqBySessionId(sessionId);
        int nextSeq = (maxSeq == null ? 0 : maxSeq) + 1;

        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setSeq(nextSeq);
        userMsg.setRole("user");
        userMsg.setContent(message);
        userMsg.setStatus("completed");
        chatMessageMapper.insert(userMsg);
        return userMsg;
    }

    private ChatMessage createAssistantPlaceholder(Long sessionId, int seq, String modelId) {
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setSeq(seq);
        msg.setRole("assistant");
        msg.setContent("");
        msg.setStatus("streaming");
        msg.setModel(modelId);
        chatMessageMapper.insert(msg);
        return msg;
    }

    /**
     * 构建 LLM 上下文：系统提示 + 历史消息（Token 预算截断）+ 带 RAG 的用户消息
     */
    private List<LlmMessage> buildLlmContext(Long sessionId, AgentDTO agent, String userQuery, String ragContext) {
        List<ChatMessage> history = loadHistory(sessionId);
        List<LlmMessage> context = new ArrayList<>();

        String basePrompt = agent.getSystemPrompt();
        String toolGuidance = buildToolGuidance(agent);
        String systemPrompt = buildSystemPrompt(basePrompt, toolGuidance);

        int basePromptTokens = estimateTokens(systemPrompt);
        int ragTokens = estimateTokens(ragContext);
        int userQueryTokens = estimateTokens(userQuery);
        int overheadTokens = estimateTokens("参考以下信息回答问题：\n\n<参考信息>\n</参考信息>\n\n用户问题：");
        int fixedTokens = basePromptTokens + ragTokens + userQueryTokens + overheadTokens;
        int budget = DEFAULT_MAX_CONTEXT_TOKENS - fixedTokens;

        // 从最新消息倒序贪婪累加，直到预算耗尽
        List<ChatMessage> included = new ArrayList<>();
        int used = 0;
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage msg = history.get(i);
            int tokens = estimateTokens(msg.getContent());
            if (used + tokens > budget) {
                log.debug("Token 预算截断: 已用 {}/{}, 舍弃 seq={} 及更早消息",
                        used + fixedTokens, DEFAULT_MAX_CONTEXT_TOKENS, msg.getSeq());
                break;
            }
            used += tokens;
            included.add(msg);
        }
        Collections.reverse(included); // 恢复正序

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            context.add(LlmMessage.system(systemPrompt));
        }
        for (ChatMessage msg : included) {
            if ("user".equals(msg.getRole()) || "assistant".equals(msg.getRole()) || "tool".equals(msg.getRole())) {
                context.add(toLlmMessage(msg));
            }
        }
        return context;
    }

    /**
     * 组装最终系统提示词：基础提示 + 工具使用指引
     */
    private String buildSystemPrompt(String basePrompt, String toolGuidance) {
        if (toolGuidance == null || toolGuidance.isEmpty()) {
            return basePrompt;
        }
        if (basePrompt == null || basePrompt.isBlank()) {
            return toolGuidance;
        }
        return basePrompt + "\n\n" + toolGuidance;
    }

    /**
     * 根据 Agent 绑定的工具生成使用指引，帮助 LLM 自主决策何时调用工具
     */
    private String buildToolGuidance(AgentDTO agent) {
        if (agent.getTools() == null || agent.getTools().isEmpty()) {
            return null;
        }
        List<AgentToolDTO> enabledTools = agent.getTools().stream()
                .filter(t -> Boolean.TRUE.equals(t.getEnabled()))
                .toList();
        if (enabledTools.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【工具使用指引】\n你拥有以下工具可以帮助用户，请根据用户意图自主判断是否使用：\n");
        for (AgentToolDTO tool : enabledTools) {
            if ("workflow".equals(tool.getToolType())) {
                sb.append("- ").append(tool.getToolName());
                Long workflowId = parseWorkflowId(tool.getToolImpl());
                if (workflowId != null) {
                    WorkflowDTO wf = workflowApi.getById(workflowId);
                    if (wf != null && wf.getDescription() != null && !wf.getDescription().isEmpty()) {
                        sb.append("：").append(wf.getDescription());
                    }
                }
                sb.append("\n");
            } else if ("mcp".equals(tool.getToolType())) {
                sb.append("- ").append(tool.getToolName());
                @SuppressWarnings("unchecked")
                String desc = tool.getConfigJson() != null
                        ? (String) tool.getConfigJson().get("description")
                        : null;
                if (desc != null && !desc.isEmpty()) {
                    sb.append("：").append(desc);
                }
                sb.append("\n");
            }
        }
        sb.append("\n使用规则：\n");
        sb.append("1. 仅在必要时调用工具，能直接回答的问题不要调用\n");
        sb.append("2. 调用工具时必须准确提供工具所需的参数\n");
        sb.append("3. 工具返回结果后，请用自然语言向用户解释结果");
        return sb.toString();
    }

    /**
     * 构建 RAG 检索上下文（仅返回检索到的内容，不含 basePrompt）
     */
    private String buildRagContext(AgentDTO agent, String userQuery) {
        // 查询 Agent 绑定的知识库
        List<AgentKnowledgeBaseVO> bindings = agentKnowledgeBaseApi.getByAgentId(agent.getId());
        if (bindings == null || bindings.isEmpty()) {
            return null;
        }

        // 在每个绑定的知识库中检索
        StringBuilder ragContext = new StringBuilder();
        int totalResults = 0;
        for (AgentKnowledgeBaseVO binding : bindings) {
            List<RagSearchResult> results = ragSearchApi.search(
                    binding.getKbId(),
                    userQuery,
                    binding.getTopK() != null ? binding.getTopK() : 10,
                    binding.getSimilarityThreshold() != null
                            ? binding.getSimilarityThreshold().floatValue() : 0.5f
            );
            if (!results.isEmpty()) {
                totalResults += results.size();
                for (int i = 0; i < results.size(); i++) {
                    RagSearchResult result = results.get(i);
                    ragContext.append("[").append(i + 1).append("] ")
                            .append(result.getContent().replace("\n", " "))
                            .append("\n");
                }
            }
        }

        if (totalResults == 0) {
            log.debug("Agent {} RAG 检索未返回结果, query='{}'", agent.getId(), userQuery);
            return null;
        }

        log.info("Agent {} RAG 检索返回 {} 条结果, query='{}'",
                agent.getId(), totalResults, userQuery.substring(0, Math.min(50, userQuery.length())));

        return ragContext.toString().trim();
    }

    private LlmMessage toLlmMessage(ChatMessage msg) {
        if ("tool".equals(msg.getRole())) {
            String toolCallId = null;
            String name = null;
            if (msg.getMetadata() != null && !msg.getMetadata().isEmpty()) {
                try {
                    Map<String, Object> meta = objectMapper.readValue(msg.getMetadata(), new TypeReference<>() {});
                    toolCallId = meta.get("toolCallId") != null ? meta.get("toolCallId").toString() : null;
                    name = meta.get("name") != null ? meta.get("name").toString() : null;
                } catch (Exception e) {
                    log.warn("Failed to parse tool metadata: {}", msg.getMetadata());
                }
            }
            return LlmMessage.tool(
                    toolCallId != null ? toolCallId : "placeholder",
                    name != null ? name : "tool",
                    msg.getContent() != null ? msg.getContent() : ""
            );
        }
        LlmMessage m = new LlmMessage();
        m.setRole(msg.getRole());
        m.setContent(msg.getContent() != null ? msg.getContent() : "");
        return m;
    }

    /**
     * 估算文本 token 数：字符数 / 3（混合中英文保守估计）
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (int) Math.ceil(text.length() / CHARS_PER_TOKEN);
    }

    /**
     * 加载历史消息：优先 Redis 缓存，否则查 DB
     */
    private List<ChatMessage> loadHistory(Long sessionId) {
        String key = String.format(REDIS_KEY_MESSAGES, sessionId);
        try {
            String cached = stringRedisTemplate.opsForValue().get(key);
            if (cached != null && !cached.isBlank()) {
                List<ChatMessage> messages = objectMapper.readValue(cached, new TypeReference<>() {});
                log.debug("从 Redis 加载历史消息, sessionId={}, size={}", sessionId, messages.size());
                return messages;
            }
        } catch (Exception e) {
            log.warn("Redis 历史消息反序列化失败, sessionId={}, 降级到 DB", sessionId, e);
        }

        List<ChatMessage> messages = chatMessageMapper.selectHistoryBySessionId(sessionId);
        if (messages.size() > MAX_HISTORY_MESSAGES) {
            return messages.subList(messages.size() - MAX_HISTORY_MESSAGES, messages.size());
        }
        return messages;
    }

    /**
     * 将最近消息写入 Redis 缓存
     */
    private void cacheMessages(Long sessionId) {
        try {
            List<ChatMessage> messages = chatMessageMapper.selectHistoryBySessionId(sessionId);
            List<ChatMessage> recent = messages.size() > MAX_HISTORY_MESSAGES
                    ? messages.subList(messages.size() - MAX_HISTORY_MESSAGES, messages.size())
                    : messages;
            String key = String.format(REDIS_KEY_MESSAGES, sessionId);
            String json = objectMapper.writeValueAsString(recent);
            stringRedisTemplate.opsForValue().set(key, json, REDIS_TTL_MINUTES, TimeUnit.MINUTES);
            log.debug("Redis 缓存更新成功, sessionId={}, size={}", sessionId, recent.size());
        } catch (Exception e) {
            log.warn("Redis 缓存更新失败, sessionId={}", sessionId, e);
        }
    }

    private LlmChatRequest buildLlmChatRequest(AgentDTO agent, List<LlmMessage> messages) {
        return buildLlmChatRequest(agent, messages, false);
    }

    private LlmChatRequest buildLlmChatRequest(AgentDTO agent, List<LlmMessage> messages, boolean skipTools) {
        List<LlmToolDefinition> tools = skipTools ? null : buildToolDefinitions(agent);
        return LlmChatRequest.builder()
                .messages(messages)
                .tools(tools)
                // 显式指定 tool_choice: auto，引导模型在需要时调用工具
                .toolChoice(tools != null && !tools.isEmpty() ? "auto" : null)
                .temperature(agent.getTemperature() != null ? agent.getTemperature().doubleValue() : 0.7)
                .maxTokens(agent.getMaxTokens())
                .stream(true)
                .topP(agent.getTopP() != null ? agent.getTopP().doubleValue() : 1.0)
                .build();
    }

    private List<LlmToolDefinition> buildToolDefinitions(AgentDTO agent) {
        if (agent.getTools() == null || agent.getTools().isEmpty()) {
            return null;
        }
        List<LlmToolDefinition> defs = new ArrayList<>();
        for (AgentToolDTO tool : agent.getTools()) {
            if (!Boolean.TRUE.equals(tool.getEnabled())) {
                continue;
            }
            if ("workflow".equals(tool.getToolType())) {
                LlmToolDefinition def = buildWorkflowToolDefinition(tool);
                if (def != null) {
                    defs.add(def);
                }
            } else if ("mcp".equals(tool.getToolType())) {
                LlmToolDefinition def = buildMcpToolDefinition(tool);
                if (def != null) {
                    defs.add(def);
                }
            }
        }
        return defs.isEmpty() ? null : defs;
    }

    @SuppressWarnings("unchecked")
    private LlmToolDefinition buildMcpToolDefinition(AgentToolDTO tool) {
        Map<String, Object> config = tool.getConfigJson();
        if (config == null) {
            log.warn("MCP 工具缺少 configJson: toolName={}", tool.getToolName());
            return null;
        }
        String description = config.get("description") != null ? config.get("description").toString() : "";
        Map<String, Object> schema = config.get("schema") instanceof Map
                ? (Map<String, Object>) config.get("schema")
                : Map.of("type", "object", "properties", Map.of());
        String toolName = sanitizeToolName(tool.getToolName());
        return LlmToolDefinition.builder()
                .type("function")
                .function(LlmToolDefinition.Function.builder()
                        .name(toolName)
                        .description(description)
                        .parameters(schema)
                        .build())
                .build();
    }

    private LlmToolDefinition buildWorkflowToolDefinition(AgentToolDTO tool) {
        Long workflowId = parseWorkflowId(tool.getToolImpl());
        if (workflowId == null) {
            log.warn("Invalid workflowId in agentTool: toolImpl={}", tool.getToolImpl());
            return null;
        }
        WorkflowDTO workflow = workflowApi.getById(workflowId);
        if (workflow == null) {
            log.warn("Workflow not found: workflowId={}", workflowId);
            return null;
        }
        String toolName = tool.getToolName();
        if (toolName == null || toolName.isEmpty()) {
            toolName = workflow.getName();
        }
        toolName = sanitizeToolName(toolName);

        // 生成详细描述：包含工作流用途和预期输入参数
        String description = buildWorkflowToolDescription(workflow, tool);

        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = tool.getConfigJson() != null
                ? (Map<String, Object>) tool.getConfigJson().get("parameters")
                : null;
        if (parameters == null) {
            parameters = buildParametersFromWorkflowNodes(workflowId);
        }
        return LlmToolDefinition.builder()
                .type("function")
                .function(LlmToolDefinition.Function.builder()
                        .name(toolName)
                        .description(description)
                        .parameters(parameters)
                        .build())
                .build();
    }

    /**
     * 根据工作流节点配置提取输入参数 JSON Schema
     */
    private Map<String, Object> buildParametersFromWorkflowNodes(Long workflowId) {
        try {
            List<com.hify.workflow.api.dto.WorkflowNodeDTO> nodes = workflowApi.getNodes(workflowId);
            if (nodes == null || nodes.isEmpty()) {
                return defaultParameters();
            }

            // 提取所有 ${var} 和 {{var}} 占位符中的变量名
            java.util.Set<String> varNames = new java.util.LinkedHashSet<>();
            java.util.Set<String> outputVars = new java.util.LinkedHashSet<>();
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\$\\{([^}]+)}");
            java.util.regex.Pattern pattern2 = java.util.regex.Pattern.compile("\\{\\{([^}]+)}}");
            for (com.hify.workflow.api.dto.WorkflowNodeDTO node : nodes) {
                if (node.getConfig() == null || node.getConfig().isEmpty()) {
                    continue;
                }
                // 收集 outputVar（输出变量不应作为输入参数）
                try {
                    Map<String, Object> config = objectMapper.readValue(node.getConfig(), new TypeReference<>() {});
                    if (config.get("outputVar") != null) {
                        outputVars.add(config.get("outputVar").toString());
                    }
                } catch (Exception e) {
                    // ignore parse error
                }
                java.util.regex.Matcher matcher = pattern.matcher(node.getConfig());
                while (matcher.find()) {
                    String varName = matcher.group(1).trim();
                    // 排除内置变量和数字索引
                    if (!varName.isEmpty() && !varName.matches("\\d+") && !varName.startsWith("__")) {
                        varNames.add(varName);
                    }
                }
                java.util.regex.Matcher matcher2 = pattern2.matcher(node.getConfig());
                while (matcher2.find()) {
                    String varName = matcher2.group(1).trim();
                    if (!varName.isEmpty() && !varName.matches("\\d+") && !varName.startsWith("__")) {
                        // 对于 {{node.var}} 格式，取变量部分
                        if (varName.contains(".")) {
                            varName = varName.substring(varName.lastIndexOf('.') + 1);
                        }
                        varNames.add(varName);
                    }
                }
            }

            // 移除输出变量，避免将内部状态暴露为必填参数
            varNames.removeAll(outputVars);

            if (varNames.isEmpty()) {
                return defaultParameters();
            }

            Map<String, Object> properties = new HashMap<>();
            for (String varName : varNames) {
                properties.put(varName, Map.of(
                        "type", "string",
                        "description", "工作流变量: " + varName
                ));
            }
            return Map.of(
                    "type", "object",
                    "properties", properties,
                    "required", new java.util.ArrayList<>(varNames)
            );
        } catch (Exception e) {
            log.warn("Failed to extract parameters from workflow nodes: workflowId={}", workflowId, e);
            return defaultParameters();
        }
    }

    private Map<String, Object> defaultParameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "input", Map.of(
                                "type", "string",
                                "description", "用户输入内容或需要传递给工作流的参数"
                        )
                )
        );
    }

    /**
     * 构建工作流工具的详细描述，帮助 LLM 理解何时调用该工具
     */
    private String buildWorkflowToolDescription(WorkflowDTO workflow, AgentToolDTO tool) {
        String baseDesc = workflow.getDescription();
        if (baseDesc == null || baseDesc.isEmpty() || baseDesc.length() < 3) {
            baseDesc = "执行 " + workflow.getName() + " 工作流";
        }
        StringBuilder sb = new StringBuilder(baseDesc);

        // 尝试提取参数信息补充到描述中
        try {
            List<com.hify.workflow.api.dto.WorkflowNodeDTO> nodes = workflowApi.getNodes(workflow.getId());
            if (nodes != null && !nodes.isEmpty()) {
                java.util.Set<String> varNames = new java.util.LinkedHashSet<>();
                java.util.Set<String> outputVars = new java.util.LinkedHashSet<>();
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\$\\{([^}]+)}");
                java.util.regex.Pattern pattern2 = java.util.regex.Pattern.compile("\\{\\{([^}]+)}}");
                for (com.hify.workflow.api.dto.WorkflowNodeDTO node : nodes) {
                    if (node.getConfig() == null || node.getConfig().isEmpty()) {
                        continue;
                    }
                    // 收集 outputVar
                    try {
                        Map<String, Object> config = objectMapper.readValue(node.getConfig(), new TypeReference<>() {});
                        if (config.get("outputVar") != null) {
                            outputVars.add(config.get("outputVar").toString());
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                    java.util.regex.Matcher matcher = pattern.matcher(node.getConfig());
                    while (matcher.find()) {
                        String varName = matcher.group(1).trim();
                        if (!varName.isEmpty() && !varName.matches("\\d+") && !varName.startsWith("__")) {
                            varNames.add(varName);
                        }
                    }
                    java.util.regex.Matcher matcher2 = pattern2.matcher(node.getConfig());
                    while (matcher2.find()) {
                        String varName = matcher2.group(1).trim();
                        if (!varName.isEmpty() && !varName.matches("\\d+") && !varName.startsWith("__")) {
                            if (varName.contains(".")) {
                                varName = varName.substring(varName.lastIndexOf('.') + 1);
                            }
                            varNames.add(varName);
                        }
                    }
                }
                // 排除输出变量
                varNames.removeAll(outputVars);
                if (!varNames.isEmpty()) {
                    sb.append("\n调用此工具时需要提供以下参数: ")
                      .append(String.join(", ", varNames));
                }
            }
        } catch (Exception e) {
            log.debug("Failed to enrich tool description: workflowId={}", workflow.getId());
        }
        return sb.toString();
    }

    private Long parseWorkflowId(String toolImpl) {
        if (toolImpl == null || toolImpl.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(toolImpl);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String sanitizeToolName(String name) {
        if (name == null) {
            return "workflow_tool";
        }
        String sanitized = name.replaceAll("[^a-zA-Z0-9_-]", "_");
        if (sanitized.matches("^\\d.*")) {
            sanitized = "wf_" + sanitized;
        }
        if (sanitized.length() > 64) {
            sanitized = sanitized.substring(0, 64);
        }
        if (sanitized.isEmpty()) {
            sanitized = "workflow_tool";
        }
        return sanitized;
    }

    private String executeTool(LlmToolCall toolCall, AgentDTO agent) {
        String name = toolCall.getName();
        String arguments = toolCall.getArguments();
        AgentToolDTO matched = null;
        for (AgentToolDTO tool : agent.getTools()) {
            if (!Boolean.TRUE.equals(tool.getEnabled())) {
                continue;
            }
            String expectedName = sanitizeToolName(tool.getToolName());
            if (name.equals(expectedName)) {
                matched = tool;
                break;
            }
        }
        if (matched == null) {
            return "工具未找到: " + name;
        }
        if ("workflow".equals(matched.getToolType())) {
            Long workflowId = parseWorkflowId(matched.getToolImpl());
            if (workflowId == null) {
                return "工作流 ID 无效";
            }
            Map<String, Object> inputs = new HashMap<>();
            if (arguments != null && !arguments.isEmpty()) {
                try {
                    inputs = objectMapper.readValue(arguments, new TypeReference<>() {});
                } catch (Exception e) {
                    log.warn("Failed to parse tool arguments: {}", arguments);
                    inputs.put("input", arguments);
                }
            }
            return invokeWorkflowSync(workflowId, inputs);
        } else if ("mcp".equals(matched.getToolType())) {
            @SuppressWarnings("unchecked")
            Map<String, Object> config = matched.getConfigJson();
            if (config == null) {
                return "MCP 工具配置缺失";
            }
            String serverUrl = config.get("serverUrl") != null ? config.get("serverUrl").toString() : null;
            if (serverUrl == null || serverUrl.isBlank()) {
                return "MCP Server URL 未配置";
            }
            Map<String, Object> args = new HashMap<>();
            if (arguments != null && !arguments.isEmpty()) {
                try {
                    args = objectMapper.readValue(arguments, new TypeReference<>() {});
                } catch (Exception e) {
                    log.warn("Failed to parse MCP tool arguments: {}", arguments);
                    args.put("input", arguments);
                }
            }
            try {
                return mcpApi.callTool(serverUrl, matched.getToolName(), args);
            } catch (Exception e) {
                log.error("MCP 工具调用失败: serverUrl={}, toolName={}", serverUrl, matched.getToolName(), e);
                return "MCP 工具调用失败: " + e.getMessage();
            }
        }
        return "不支持的工具类型: " + matched.getToolType();
    }

    private String invokeWorkflowSync(Long workflowId, Map<String, Object> inputs) {
        WorkflowStartRequest request = new WorkflowStartRequest();
        request.setWorkflowId(workflowId);
        request.setInputs(inputs);
        String instanceId = workflowApi.start(request);
        if (instanceId == null || instanceId.isEmpty()) {
            return "工作流启动失败";
        }
        for (int i = 0; i < WORKFLOW_MAX_POLL_ATTEMPTS; i++) {
            try {
                Thread.sleep(WORKFLOW_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "工作流执行被中断";
            }
            WorkflowInstanceDTO instance = workflowApi.getInstanceById(Long.valueOf(instanceId));
            if (instance == null) {
                return "工作流实例不存在";
            }
            if ("COMPLETED".equalsIgnoreCase(instance.getStatus())) {
                return extractWorkflowReply(instance.getContext());
            }
            if ("FAILED".equalsIgnoreCase(instance.getStatus())) {
                return "工作流执行失败: " + (instance.getErrorMsg() != null ? instance.getErrorMsg() : "未知错误");
            }
        }
        return "工作流执行超时";
    }

    /**
     * 从工作流 context 中提取最终回复。优先取 reply 字段，避免把内部状态 JSON 丢给 LLM。
     */
    private String extractWorkflowReply(String contextJson) {
        if (contextJson == null || contextJson.isEmpty()) {
            return "工作流执行完成，无输出";
        }
        try {
            Map<String, Object> ctx = objectMapper.readValue(contextJson, new TypeReference<>() {
            });
            if (ctx.containsKey("reply")) {
                Object reply = ctx.get("reply");
                return reply != null ? reply.toString() : "工作流执行完成，无输出";
            }
            return contextJson;
        } catch (Exception e) {
            log.warn("解析工作流 context 失败，返回原始内容: {}",
                    contextJson.substring(0, Math.min(100, contextJson.length())));
            return contextJson;
        }
    }

    private void finalizeAssistantMessage(ChatMessage assistantMsg, String fullContent,
                                          LlmStreamChunk lastChunk, long startTime) {
        assistantMsg.setContent(fullContent);
        assistantMsg.setStatus("completed");
        assistantMsg.setFinishReason(lastChunk.getFinishReason());
        assistantMsg.setDurationMs((int) (System.currentTimeMillis() - startTime));
        if (lastChunk.getUsage() != null) {
            assistantMsg.setInputTokens(lastChunk.getUsage().getPromptTokens() != null
                    ? lastChunk.getUsage().getPromptTokens().intValue() : null);
            assistantMsg.setOutputTokens(lastChunk.getUsage().getCompletionTokens() != null
                    ? lastChunk.getUsage().getCompletionTokens().intValue() : null);
        }
        chatMessageMapper.updateById(assistantMsg);
    }

    private void updateSessionAfterChat(ChatSession session, String lastContent) {
        session.setMessageCount((session.getMessageCount() != null ? session.getMessageCount() : 0) + 2);
        session.setLastMessageAt(LocalDateTime.now());
        if ("新对话".equals(session.getTitle()) && lastContent != null && !lastContent.isBlank()) {
            session.setTitle(lastContent.substring(0, Math.min(20, lastContent.length())));
        }
        chatSessionMapper.updateById(session);
    }

    private void sendDoneEvent(SseEmitter emitter, Long messageId) throws Exception {
        emitter.send(SseEmitter.event()
                .name("done")
                .data(Map.of("status", "completed", "messageId", messageId)));
        emitter.complete();
    }

    private void handleLlmError(ChatMessage assistantMsg, SseEmitter emitter, Exception e) {
        assistantMsg.setStatus("error");
        assistantMsg.setFinishReason("error");
        chatMessageMapper.updateById(assistantMsg);
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(Map.of("code", "LLM_ERROR", "message", e.getMessage())));
        } catch (Exception ex) {
            log.error("发送错误事件失败", ex);
        }
        emitter.complete();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageVO> listSessionMessages(Long userId, Long sessionId) {
        chatSessionService.getSessionOrThrow(userId, sessionId);
        List<ChatMessage> messages = chatMessageMapper.selectHistoryBySessionId(sessionId);
        return messages.stream().map(this::toVO).toList();
    }

    private ChatMessageVO toVO(ChatMessage message) {
        ChatMessageVO vo = new ChatMessageVO();
        BeanUtils.copyProperties(message, vo);
        return vo;
    }

    // ==================== Workflow 工具执行 ====================

    /**
     * 流式分块 thinking 内容过滤
     *
     * @return 过滤后的文本；如果整块都在 think 标签内则返回空字符串
     */
    private String filterStreamingThink(String text, AtomicBoolean inThinkBlock) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        if (inThinkBlock.get()) {
            int closeIdx = text.indexOf("</think>");
            if (closeIdx == -1) {
                closeIdx = text.indexOf("</thinking>");
            }
            if (closeIdx != -1) {
                inThinkBlock.set(false);
                return text.substring(closeIdx + (text.charAt(closeIdx + 2) == 't' ? 8 : 11));
            }
            return "";
        }
        int openIdx = text.indexOf("<think>");
        if (openIdx == -1) {
            openIdx = text.indexOf("<thinking>");
        }
        if (openIdx != -1) {
            inThinkBlock.set(true);
            String before = text.substring(0, openIdx);
            String after = text.substring(openIdx + (text.charAt(openIdx + 2) == 't' && text.charAt(openIdx + 3) == 'h' ? 10 : 7));
            int closeIdx = after.indexOf("</think>");
            if (closeIdx == -1) {
                closeIdx = after.indexOf("</thinking>");
            }
            if (closeIdx != -1) {
                inThinkBlock.set(false);
                return before + after.substring(closeIdx + (after.charAt(closeIdx + 2) == 't' ? 8 : 11));
            }
            return before;
        }
        return text;
    }
}
