package com.hify.chat.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.agent.api.AgentApi;
import com.hify.agent.api.dto.AgentDTO;
import com.hify.chat.entity.ChatMessage;
import com.hify.chat.entity.ChatSession;
import com.hify.chat.mapper.ChatMessageMapper;
import com.hify.chat.mapper.ChatSessionMapper;
import com.hify.chat.vo.ChatMessageVO;
import com.hify.common.core.enums.ResultCode;
import com.hify.common.core.exception.BizException;
import com.hify.model.api.LlmGatewayApi;
import com.hify.model.api.dto.LlmChatRequest;
import com.hify.model.api.dto.LlmMessage;
import com.hify.model.api.dto.LlmStreamChunk;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

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

        // 构建 LLM 上下文（系统提示 + 历史消息滑动窗口 + Token 预算）
        List<LlmMessage> context = buildLlmContext(sessionId, agent);
        context.add(LlmMessage.user(message));

        // 创建 assistant 占位消息
        ChatMessage assistantMsg = createAssistantPlaceholder(sessionId, userMsg.getSeq() + 1, agent.getModelId());

        AtomicReference<StringBuilder> contentRef = new AtomicReference<>(new StringBuilder());

        try {
            LlmChatRequest request = buildLlmChatRequest(agent, context);
            llmGatewayApi.streamChat(agent.getModelId(), request, chunk -> {
                try {
                    String text = chunk.getContent();
                    if (text != null && !text.isEmpty()) {
                        contentRef.get().append(text);
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data(Map.of("content", text, "timestamp", System.currentTimeMillis())));
                    }

                    if (Boolean.TRUE.equals(chunk.getFinish())) {
                        String fullContent = contentRef.get().toString();
                        finalizeAssistantMessage(assistantMsg, fullContent, chunk, startTime);
                        updateSessionAfterChat(session, fullContent);
                        cacheMessages(sessionId);
                        sendDoneEvent(emitter, assistantMsg.getId());
                    }
                } catch (Exception e) {
                    log.error("SSE 发送失败, sessionId={}", sessionId, e);
                    emitter.completeWithError(e);
                }
            });
        } catch (Exception e) {
            log.error("LLM 调用失败, sessionId={}", sessionId, e);
            handleLlmError(assistantMsg, emitter, e);
        }
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
     * 构建 LLM 上下文：系统提示 + 历史消息（Token 预算截断）
     */
    private List<LlmMessage> buildLlmContext(Long sessionId, AgentDTO agent) {
        List<ChatMessage> history = loadHistory(sessionId);
        List<LlmMessage> context = new ArrayList<>();

        String systemPrompt = agent.getSystemPrompt();
        int systemTokens = estimateTokens(systemPrompt);
        int budget = DEFAULT_MAX_CONTEXT_TOKENS - systemTokens;

        // 从最新消息倒序贪婪累加，直到预算耗尽
        List<ChatMessage> included = new ArrayList<>();
        int used = 0;
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage msg = history.get(i);
            int tokens = estimateTokens(msg.getContent());
            if (used + tokens > budget) {
                log.debug("Token 预算截断: 已用 {}/{}, 舍弃 seq={} 及更早消息",
                        used + systemTokens, DEFAULT_MAX_CONTEXT_TOKENS, msg.getSeq());
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
            if ("user".equals(msg.getRole()) || "assistant".equals(msg.getRole())) {
                context.add(toLlmMessage(msg));
            }
        }
        return context;
    }

    private LlmMessage toLlmMessage(ChatMessage msg) {
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
        return LlmChatRequest.builder()
                .messages(messages)
                .temperature(agent.getTemperature() != null ? agent.getTemperature().doubleValue() : 0.7)
                .maxTokens(agent.getMaxTokens())
                .stream(true)
                .topP(agent.getTopP() != null ? agent.getTopP().doubleValue() : 1.0)
                .build();
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
}
