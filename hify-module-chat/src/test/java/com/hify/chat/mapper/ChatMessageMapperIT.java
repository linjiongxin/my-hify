package com.hify.chat.mapper;

import com.hify.agent.api.AgentApi;
import com.hify.chat.ChatTestApplication;
import com.hify.chat.entity.ChatMessage;
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

@SpringBootTest(classes = ChatTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class ChatMessageMapperIT {

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @MockBean
    private LlmGatewayApi llmGatewayApi;

    @MockBean
    private AgentApi agentApi;

    @BeforeEach
    void setUp() {
        chatMessageMapper.delete(null);

        // 插入 5 条测试消息（3 条 completed，1 条 streaming，1 条 deleted）
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
    void shouldSelectCompletedMessagesBySessionId() {
        List<ChatMessage> messages = chatMessageMapper.selectHistoryBySessionId(1L);

        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).getSeq()).isEqualTo(1);
        assertThat(messages.get(0).getRole()).isEqualTo("user");
        assertThat(messages.get(0).getContent()).isEqualTo("hello");
        assertThat(messages.get(1).getSeq()).isEqualTo(2);
        assertThat(messages.get(2).getSeq()).isEqualTo(3);
    }

    @Test
    void shouldReturnEmpty_whenSessionHasNoCompletedMessages() {
        List<ChatMessage> messages = chatMessageMapper.selectHistoryBySessionId(999L);
        assertThat(messages).isEmpty();
    }

    @Test
    void shouldExcludeStreamingMessages() {
        List<ChatMessage> messages = chatMessageMapper.selectHistoryBySessionId(1L);
        assertThat(messages).extracting(ChatMessage::getStatus).containsOnly("completed");
        assertThat(messages).extracting(ChatMessage::getSeq).doesNotContain(4);
    }

    @Test
    void shouldReturnMaxSeq_whenSessionHasMessages() {
        Integer maxSeq = chatMessageMapper.selectMaxSeqBySessionId(1L);
        assertThat(maxSeq).isEqualTo(4);
    }

    @Test
    void shouldReturnZero_whenSessionHasNoMessages() {
        Integer maxSeq = chatMessageMapper.selectMaxSeqBySessionId(999L);
        assertThat(maxSeq).isEqualTo(0);
    }

    @Test
    void shouldOrderBySeqAsc() {
        List<ChatMessage> messages = chatMessageMapper.selectHistoryBySessionId(1L);
        assertThat(messages).extracting(ChatMessage::getSeq).containsExactly(1, 2, 3);
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
