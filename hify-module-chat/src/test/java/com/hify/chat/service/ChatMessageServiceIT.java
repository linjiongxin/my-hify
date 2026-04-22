package com.hify.chat.service;

import com.hify.agent.api.AgentApi;
import com.hify.agent.api.dto.AgentDTO;
import com.hify.chat.ChatTestApplication;
import com.hify.chat.entity.ChatMessage;
import com.hify.chat.entity.ChatSession;
import com.hify.chat.mapper.ChatMessageMapper;
import com.hify.chat.mapper.ChatSessionMapper;
import com.hify.chat.vo.ChatMessageVO;
import com.hify.model.api.LlmGatewayApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = ChatTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class ChatMessageServiceIT {

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private ChatSessionMapper chatSessionMapper;

    @MockBean
    private LlmGatewayApi llmGatewayApi;

    @MockBean
    private AgentApi agentApi;

    private ChatSession session;

    @BeforeEach
    void setUp() {
        chatMessageMapper.delete(null);
        chatSessionMapper.delete(null);

        session = new ChatSession();
        session.setId(1L);
        session.setUserId(100L);
        session.setAgentId(10L);
        session.setTitle("Test Session");
        session.setStatus("active");
        session.setMessageCount(0);
        session.setLastMessageAt(LocalDateTime.now());
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        session.setDeleted(false);
        chatSessionMapper.insert(session);

        // mock agent
        AgentDTO agent = new AgentDTO();
        agent.setId(10L);
        agent.setName("TestAgent");
        agent.setModelId("gpt-4o");
        agent.setSystemPrompt("You are helpful.");
        agent.setEnabled(true);
        when(agentApi.getAgentById(anyLong())).thenReturn(agent);

        // Insert messages
        ChatMessage msg1 = createMessage(1L, 1, "user", "hello", "completed");
        ChatMessage msg2 = createMessage(1L, 2, "assistant", "hi there", "completed");
        ChatMessage msg3 = createMessage(1L, 3, "user", "how are you", "completed");
        ChatMessage msg4 = createMessage(1L, 4, "assistant", "", "streaming");
        ChatMessage msg5 = createMessage(2L, 1, "user", "other session", "completed");

        chatMessageMapper.insert(msg1);
        chatMessageMapper.insert(msg2);
        chatMessageMapper.insert(msg3);
        chatMessageMapper.insert(msg4);
        chatMessageMapper.insert(msg5);
    }

    @Test
    void shouldReturnCompletedMessages_whenListSessionMessages() {
        List<ChatMessageVO> messages = chatMessageService.listSessionMessages(100L, 1L);

        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).getRole()).isEqualTo("user");
        assertThat(messages.get(0).getContent()).isEqualTo("hello");
        assertThat(messages.get(1).getRole()).isEqualTo("assistant");
        assertThat(messages.get(2).getContent()).isEqualTo("how are you");
    }

    @Test
    void shouldExcludeStreamingAndOtherSessions_whenListSessionMessages() {
        List<ChatMessageVO> messages = chatMessageService.listSessionMessages(100L, 1L);

        assertThat(messages).extracting(ChatMessageVO::getSeq).containsExactly(1, 2, 3);
        assertThat(messages).extracting(ChatMessageVO::getStatus).containsOnly("completed");
    }

    @Test
    void shouldReturnEmpty_whenSessionHasNoCompletedMessages() {
        // Insert a new session with no messages
        ChatSession session3 = new ChatSession();
        session3.setId(3L);
        session3.setUserId(100L);
        session3.setAgentId(10L);
        session3.setTitle("Empty Session");
        session3.setStatus("active");
        session3.setMessageCount(0);
        session3.setLastMessageAt(LocalDateTime.now());
        session3.setCreatedAt(LocalDateTime.now());
        session3.setUpdatedAt(LocalDateTime.now());
        session3.setDeleted(false);
        chatSessionMapper.insert(session3);

        List<ChatMessageVO> messages = chatMessageService.listSessionMessages(100L, 3L);
        assertThat(messages).isEmpty();
    }

    private ChatMessage createMessage(Long sessionId, int seq, String role, String content, String status) {
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setSeq(seq);
        msg.setRole(role);
        msg.setContent(content);
        msg.setStatus(status);
        msg.setCreatedAt(LocalDateTime.now());
        msg.setUpdatedAt(LocalDateTime.now());
        msg.setDeleted(false);
        return msg;
    }
}
