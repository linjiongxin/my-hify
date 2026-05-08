package com.hify.chat.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.agent.api.AgentApi;
import com.hify.agent.api.dto.AgentDTO;
import com.hify.chat.entity.ChatMessage;
import com.hify.chat.entity.ChatSession;
import com.hify.chat.mapper.ChatMessageMapper;
import com.hify.chat.mapper.ChatSessionMapper;
import com.hify.common.core.enums.ResultCode;
import com.hify.common.core.exception.BizException;
import com.hify.model.api.LlmGatewayApi;
import com.hify.model.api.dto.LlmChatRequest;
import com.hify.model.api.dto.LlmMessage;
import com.hify.model.api.dto.LlmStreamChunk;
import com.hify.model.api.dto.LlmUsage;
import com.hify.rag.api.AgentKnowledgeBaseApi;
import com.hify.rag.api.RagSearchApi;
import com.hify.workflow.api.WorkflowApi;
import com.hify.workflow.api.dto.WorkflowInstanceDTO;
import com.hify.workflow.api.dto.WorkflowStartRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    private ChatMessageMapper chatMessageMapper;

    @Mock
    private ChatSessionMapper chatSessionMapper;

    @Mock
    private ChatSessionService chatSessionService;

    @Mock
    private LlmGatewayApi llmGatewayApi;

    @Mock
    private AgentApi agentApi;

    @Mock
    private AgentKnowledgeBaseApi agentKnowledgeBaseApi;

    @Mock
    private RagSearchApi ragSearchApi;

    @Mock
    private WorkflowApi workflowApi;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ChatMessageService chatMessageService;

    private ChatSession session;
    private AgentDTO agent;
    private SseEmitter emitter;

    @BeforeEach
    void setUp() {
        session = new ChatSession();
        session.setId(1L);
        session.setUserId(100L);
        session.setAgentId(10L);
        session.setModelId("gpt-4o");
        session.setTitle("新对话");
        session.setMessageCount(0);

        agent = new AgentDTO();
        agent.setId(10L);
        agent.setName("TestAgent");
        agent.setModelId("gpt-4o");
        agent.setSystemPrompt("You are a helpful assistant.");
        agent.setTemperature(new BigDecimal("0.7"));
        agent.setMaxTokens(2048);
        agent.setTopP(new BigDecimal("1.0"));
        agent.setEnabled(true);

        emitter = new SseEmitter(0L);

        // 默认 RAG 无绑定，避免所有 react 模式测试都需要重复 mock
        lenient().when(agentKnowledgeBaseApi.getByAgentId(anyLong())).thenReturn(Collections.emptyList());
    }

    @Test
    void shouldThrow_whenAgentNotFound() {
        when(chatSessionService.getSessionOrThrow(100L, 1L)).thenReturn(session);
        when(agentApi.getAgentById(10L)).thenReturn(null);

        assertThatThrownBy(() -> chatMessageService.streamChat(100L, 1L, "hello", emitter))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getCode()).isEqualTo(ResultCode.DATA_NOT_FOUND.getCode());
                });
    }

    @Test
    void shouldThrow_whenAgentDisabled() {
        when(chatSessionService.getSessionOrThrow(100L, 1L)).thenReturn(session);
        AgentDTO disabledAgent = new AgentDTO();
        disabledAgent.setId(10L);
        disabledAgent.setEnabled(false);
        when(agentApi.getAgentById(10L)).thenReturn(disabledAgent);

        assertThatThrownBy(() -> chatMessageService.streamChat(100L, 1L, "hello", emitter))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getCode()).isEqualTo(ResultCode.DATA_NOT_FOUND.getCode());
                });
    }

    @Test
    void shouldSaveUserMessageAndInvokeLlm_whenStreamChat() {
        when(chatSessionService.getSessionOrThrow(100L, 1L)).thenReturn(session);
        when(agentApi.getAgentById(10L)).thenReturn(agent);
        when(chatMessageMapper.selectMaxSeqBySessionId(1L)).thenReturn(5);
        when(chatMessageMapper.insert(any(ChatMessage.class))).thenAnswer(inv -> {
            ChatMessage m = inv.getArgument(0);
            m.setId(100L);
            return 1;
        });
        when(chatMessageMapper.selectHistoryBySessionId(1L)).thenReturn(Collections.emptyList());

        doAnswer(inv -> {
            Consumer<LlmStreamChunk> cb = inv.getArgument(2);
            cb.accept(LlmStreamChunk.builder().content("Hello").build());
            LlmStreamChunk done = new LlmStreamChunk();
            done.setContent(" world");
            done.setFinish(true);
            done.setFinishReason("stop");
            done.setUsage(new LlmUsage(10L, 5L, 15L));
            cb.accept(done);
            return null;
        }).when(llmGatewayApi).streamChat(anyString(), any(LlmChatRequest.class), any());

        chatMessageService.streamChat(100L, 1L, "hi", emitter);

        ArgumentCaptor<ChatMessage> msgCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageMapper, times(2)).insert(msgCaptor.capture());
        List<ChatMessage> saved = msgCaptor.getAllValues();

        // 第一条是用户消息
        assertThat(saved.get(0).getRole()).isEqualTo("user");
        assertThat(saved.get(0).getContent()).isEqualTo("hi");
        assertThat(saved.get(0).getSeq()).isEqualTo(6);

        // 第二条是 assistant 占位
        assertThat(saved.get(1).getRole()).isEqualTo("assistant");
        assertThat(saved.get(1).getSeq()).isEqualTo(7);

        // 验证 LLM 调用参数
        ArgumentCaptor<LlmChatRequest> reqCaptor = ArgumentCaptor.forClass(LlmChatRequest.class);
        verify(llmGatewayApi).streamChat(eq("gpt-4o"), reqCaptor.capture(), any());
        LlmChatRequest request = reqCaptor.getValue();
        assertThat(request.getMessages()).hasSize(2); // system + user
        assertThat(request.getMessages().get(0).getRole()).isEqualTo("system");
        assertThat(request.getMessages().get(0).getContent()).isEqualTo("You are a helpful assistant.");
        assertThat(request.getMessages().get(1).getRole()).isEqualTo("user");
        assertThat(request.getTemperature()).isEqualTo(0.7);
        assertThat(request.getMaxTokens()).isEqualTo(2048);
    }

    @Test
    void shouldTruncateHistory_whenTokenBudgetExceeded() {
        when(chatSessionService.getSessionOrThrow(100L, 1L)).thenReturn(session);
        when(agentApi.getAgentById(10L)).thenReturn(agent);
        when(chatMessageMapper.selectMaxSeqBySessionId(1L)).thenReturn(0);
        when(chatMessageMapper.insert(any(ChatMessage.class))).thenAnswer(inv -> {
            ChatMessage m = inv.getArgument(0);
            m.setId(100L);
            return 1;
        });

        // 构造超长的历史消息（每条约 1000 token），共 10 条
        String longText = "a".repeat(3000);
        List<ChatMessage> history = new java.util.ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            ChatMessage msg = new ChatMessage();
            msg.setSessionId(1L);
            msg.setSeq(i);
            msg.setRole(i % 2 == 1 ? "user" : "assistant");
            msg.setContent(longText);
            msg.setStatus("completed");
            history.add(msg);
        }
        when(chatMessageMapper.selectHistoryBySessionId(1L)).thenReturn(history);

        doAnswer(inv -> {
            Consumer<LlmStreamChunk> cb = inv.getArgument(2);
            cb.accept(LlmStreamChunk.builder()
                    .content("ok")
                    .finish(true)
                    .finishReason("stop")
                    .build());
            return null;
        }).when(llmGatewayApi).streamChat(anyString(), any(LlmChatRequest.class), any());

        chatMessageService.streamChat(100L, 1L, "hi", emitter);

        ArgumentCaptor<LlmChatRequest> reqCaptor = ArgumentCaptor.forClass(LlmChatRequest.class);
        verify(llmGatewayApi).streamChat(anyString(), reqCaptor.capture(), any());
        LlmChatRequest request = reqCaptor.getValue();

        // system prompt + 部分历史 + 当前用户消息
        // system prompt 约 5 token，预算 4000，每条 1000 token，最多保留 3 条历史
        // 所以 messages 数量 = 1(system) + 3(history) + 1(current user) = 5
        assertThat(request.getMessages().size()).isLessThanOrEqualTo(5);
        assertThat(request.getMessages().get(0).getRole()).isEqualTo("system");
        assertThat(request.getMessages().get(request.getMessages().size() - 1).getRole()).isEqualTo("user");
        assertThat(request.getMessages().get(request.getMessages().size() - 1).getContent()).isEqualTo("hi");
    }

    @Test
    void shouldLoadFromRedis_whenCacheHit() throws Exception {
        when(chatSessionService.getSessionOrThrow(100L, 1L)).thenReturn(session);
        when(agentApi.getAgentById(10L)).thenReturn(agent);
        when(chatMessageMapper.selectMaxSeqBySessionId(1L)).thenReturn(0);
        when(chatMessageMapper.insert(any(ChatMessage.class))).thenAnswer(inv -> {
            ChatMessage m = inv.getArgument(0);
            m.setId(100L);
            return 1;
        });

        String cachedJson = "[{\"sessionId\":1,\"seq\":1,\"role\":\"user\",\"content\":\"cached msg\",\"status\":\"completed\"}]";
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get("chat:session:1:messages")).thenReturn(cachedJson);

        List<ChatMessage> cachedList = List.of(
                createMessage(1L, 1, "user", "cached msg")
        );
        when(objectMapper.readValue(eq(cachedJson), any(TypeReference.class)))
                .thenReturn(cachedList);

        doAnswer(inv -> {
            Consumer<LlmStreamChunk> cb = inv.getArgument(2);
            cb.accept(LlmStreamChunk.builder()
                    .content("ok")
                    .finish(true)
                    .finishReason("stop")
                    .build());
            return null;
        }).when(llmGatewayApi).streamChat(anyString(), any(LlmChatRequest.class), any());

        chatMessageService.streamChat(100L, 1L, "hi", emitter);

        // Redis 命中时，loadHistory 不走 DB，但 cacheMessages 最后仍会查一次 DB
        verify(chatMessageMapper, times(1)).selectHistoryBySessionId(1L);
        verify(objectMapper).readValue(eq(cachedJson), any(TypeReference.class));
        ArgumentCaptor<LlmChatRequest> reqCaptor = ArgumentCaptor.forClass(LlmChatRequest.class);
        verify(llmGatewayApi).streamChat(anyString(), reqCaptor.capture(), any());
        // system + cached user + current user
        assertThat(reqCaptor.getValue().getMessages().size()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void shouldLoadFromDb_whenCacheMiss() {
        when(chatSessionService.getSessionOrThrow(100L, 1L)).thenReturn(session);
        when(agentApi.getAgentById(10L)).thenReturn(agent);
        when(chatMessageMapper.selectMaxSeqBySessionId(1L)).thenReturn(0);
        when(chatMessageMapper.insert(any(ChatMessage.class))).thenAnswer(inv -> {
            ChatMessage m = inv.getArgument(0);
            m.setId(100L);
            return 1;
        });

        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get("chat:session:1:messages")).thenReturn(null);

        when(chatMessageMapper.selectHistoryBySessionId(1L)).thenReturn(Collections.emptyList());

        doAnswer(inv -> {
            Consumer<LlmStreamChunk> cb = inv.getArgument(2);
            cb.accept(LlmStreamChunk.builder()
                    .content("ok")
                    .finish(true)
                    .finishReason("stop")
                    .build());
            return null;
        }).when(llmGatewayApi).streamChat(anyString(), any(LlmChatRequest.class), any());

        chatMessageService.streamChat(100L, 1L, "hi", emitter);

        // cacheMiss: loadHistory 查 1 次 + cacheMessages 查 1 次 = 2 次
        verify(chatMessageMapper, times(2)).selectHistoryBySessionId(1L);
    }

    @Test
    void shouldUpdateSessionTitle_whenFirstResponseAndDefaultTitle() {
        when(chatSessionService.getSessionOrThrow(100L, 1L)).thenReturn(session);
        when(agentApi.getAgentById(10L)).thenReturn(agent);
        when(chatMessageMapper.selectMaxSeqBySessionId(1L)).thenReturn(0);
        when(chatMessageMapper.insert(any(ChatMessage.class))).thenAnswer(inv -> {
            ChatMessage m = inv.getArgument(0);
            m.setId(100L);
            return 1;
        });
        when(chatMessageMapper.selectHistoryBySessionId(1L)).thenReturn(Collections.emptyList());

        doAnswer(inv -> {
            Consumer<LlmStreamChunk> cb = inv.getArgument(2);
            cb.accept(LlmStreamChunk.builder()
                    .content("This is a helpful response")
                    .finish(true)
                    .finishReason("stop")
                    .build());
            return null;
        }).when(llmGatewayApi).streamChat(anyString(), any(LlmChatRequest.class), any());

        chatMessageService.streamChat(100L, 1L, "hello", emitter);

        ArgumentCaptor<ChatSession> sessionCaptor = ArgumentCaptor.forClass(ChatSession.class);
        verify(chatSessionMapper).updateById(sessionCaptor.capture());
        ChatSession updated = sessionCaptor.getValue();
        assertThat(updated.getTitle()).isEqualTo("This is a helpful re");
        assertThat(updated.getMessageCount()).isEqualTo(2);
    }

    @Test
    void shouldNotChangeTitle_whenNotDefault() {
        session.setTitle("Custom Title");
        when(chatSessionService.getSessionOrThrow(100L, 1L)).thenReturn(session);
        when(agentApi.getAgentById(10L)).thenReturn(agent);
        when(chatMessageMapper.selectMaxSeqBySessionId(1L)).thenReturn(0);
        when(chatMessageMapper.insert(any(ChatMessage.class))).thenAnswer(inv -> {
            ChatMessage m = inv.getArgument(0);
            m.setId(100L);
            return 1;
        });
        when(chatMessageMapper.selectHistoryBySessionId(1L)).thenReturn(Collections.emptyList());

        doAnswer(inv -> {
            Consumer<LlmStreamChunk> cb = inv.getArgument(2);
            cb.accept(LlmStreamChunk.builder()
                    .content("response")
                    .finish(true)
                    .finishReason("stop")
                    .build());
            return null;
        }).when(llmGatewayApi).streamChat(anyString(), any(LlmChatRequest.class), any());

        chatMessageService.streamChat(100L, 1L, "hello", emitter);

        ArgumentCaptor<ChatSession> sessionCaptor = ArgumentCaptor.forClass(ChatSession.class);
        verify(chatSessionMapper).updateById(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getTitle()).isEqualTo("Custom Title");
    }

    @Test
    void shouldReturnMessages_whenListSessionMessages() {
        ChatMessage msg1 = createMessage(1L, 1, "user", "hello");
        ChatMessage msg2 = createMessage(1L, 2, "assistant", "hi");
        when(chatSessionService.getSessionOrThrow(100L, 1L)).thenReturn(session);
        when(chatMessageMapper.selectHistoryBySessionId(1L)).thenReturn(List.of(msg1, msg2));

        var result = chatMessageService.listSessionMessages(100L, 1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getContent()).isEqualTo("hello");
        assertThat(result.get(1).getContent()).isEqualTo("hi");
    }

    @Test
    void shouldStartWorkflow_whenAgentInWorkflowMode() throws Exception {
        agent.setExecutionMode("workflow");
        agent.setWorkflowId(10L);

        when(chatSessionService.getSessionOrThrow(100L, 1L)).thenReturn(session);
        when(agentApi.getAgentById(10L)).thenReturn(agent);
        when(chatMessageMapper.selectMaxSeqBySessionId(1L)).thenReturn(5);
        when(chatMessageMapper.insert(any(ChatMessage.class))).thenAnswer(inv -> {
            ChatMessage m = inv.getArgument(0);
            m.setId(100L);
            return 1;
        });

        when(workflowApi.start(any(WorkflowStartRequest.class))).thenReturn("100");

        chatMessageService.streamChat(100L, 1L, "我要退款", emitter);

        // 验证 workflowApi.start 被调用
        ArgumentCaptor<WorkflowStartRequest> reqCaptor = ArgumentCaptor.forClass(WorkflowStartRequest.class);
        verify(workflowApi).start(reqCaptor.capture());
        assertThat(reqCaptor.getValue().getWorkflowId()).isEqualTo(10L);

        // 验证 LLM 未被调用
        verify(llmGatewayApi, never()).streamChat(anyString(), any(LlmChatRequest.class), any());
    }

    @Test
    void shouldStartWorkflow_whenAgentHasWorkflowIdButNoMode() throws Exception {
        // executionMode 为 null，默认走 react，即使 workflowId 有值
        agent.setExecutionMode(null);
        agent.setWorkflowId(10L);

        when(chatSessionService.getSessionOrThrow(100L, 1L)).thenReturn(session);
        when(agentApi.getAgentById(10L)).thenReturn(agent);
        when(chatMessageMapper.selectMaxSeqBySessionId(1L)).thenReturn(5);
        when(chatMessageMapper.insert(any(ChatMessage.class))).thenAnswer(inv -> {
            ChatMessage m = inv.getArgument(0);
            m.setId(100L);
            return 1;
        });
        when(chatMessageMapper.selectHistoryBySessionId(1L)).thenReturn(Collections.emptyList());

        doAnswer(inv -> {
            Consumer<LlmStreamChunk> cb = inv.getArgument(2);
            cb.accept(LlmStreamChunk.builder().content("ok").finish(true).finishReason("stop").build());
            return null;
        }).when(llmGatewayApi).streamChat(anyString(), any(LlmChatRequest.class), any());

        chatMessageService.streamChat(100L, 1L, "hello", emitter);

        // react 模式不应调 workflow
        verify(workflowApi, never()).start(any(WorkflowStartRequest.class));
        verify(llmGatewayApi).streamChat(anyString(), any(LlmChatRequest.class), any());
    }

    private ChatMessage createMessage(Long sessionId, int seq, String role, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setSeq(seq);
        msg.setRole(role);
        msg.setContent(content);
        msg.setStatus("completed");
        return msg;
    }
}
