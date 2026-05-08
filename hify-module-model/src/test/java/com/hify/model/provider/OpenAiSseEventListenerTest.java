package com.hify.model.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.model.api.dto.LlmStreamChunk;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OpenAiSseEventListenerTest {

    private List<LlmStreamChunk> chunks;
    private Consumer<LlmStreamChunk> callback;
    private ObjectMapper objectMapper;
    private OpenAiSseEventListener listener;
    private EventSource eventSource;

    @BeforeEach
    void setUp() {
        chunks = new ArrayList<>();
        callback = chunks::add;
        objectMapper = new ObjectMapper();
        listener = new OpenAiSseEventListener(callback, objectMapper);
        eventSource = mock(EventSource.class);
    }

    @Test
    void shouldEmitFinishChunk_whenOnEvent_givenDoneData() {
        listener.onEvent(eventSource, "1", "message", "[DONE]");

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).getFinish()).isTrue();
        assertThat(chunks.get(0).getContent()).isNull();
    }

    @Test
    void shouldEmitContentChunk_whenOnEvent_givenNormalDelta() {
        String data = "{\"choices\":[{\"delta\":{\"content\":\"Hello\"}}]}";

        listener.onEvent(eventSource, "1", "message", data);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).getContent()).isEqualTo("Hello");
        assertThat(chunks.get(0).getFinish()).isFalse();
        verify(eventSource, never()).cancel();
    }

    @Test
    void shouldEmitContentAndCancel_whenOnEvent_givenFinishReason() {
        String data = "{\"choices\":[{\"delta\":{\"content\":\" world\"},\"finish_reason\":\"stop\"}]}";

        listener.onEvent(eventSource, "1", "message", data);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).getContent()).isEqualTo(" world");
        assertThat(chunks.get(0).getFinish()).isTrue();
        assertThat(chunks.get(0).getFinishReason()).isEqualTo("stop");
        verify(eventSource).cancel();
    }

    @Test
    void shouldHandleEmptyChoices_whenOnEvent_givenEmptyChoices() {
        String data = "{\"choices\":[]}";

        listener.onEvent(eventSource, "1", "message", data);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).getContent()).isEqualTo("");
        assertThat(chunks.get(0).getFinish()).isNull();
    }

    @Test
    void shouldHandleNullContent_whenOnEvent_givenDeltaWithNoContent() {
        String data = "{\"choices\":[{\"delta\":{\"role\":\"assistant\"}}]}";

        listener.onEvent(eventSource, "1", "message", data);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).getContent()).isEqualTo("");
    }

    @Test
    void shouldHandleMultipleEvents_whenOnEvent_givenSequentialChunks() {
        listener.onEvent(eventSource, "1", "message",
                "{\"choices\":[{\"delta\":{\"content\":\"Hello\"}}]}");
        listener.onEvent(eventSource, "2", "message",
                "{\"choices\":[{\"delta\":{\"content\":\" world\"}}]}");
        listener.onEvent(eventSource, "3", "message", "[DONE]");

        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0).getContent()).isEqualTo("Hello");
        assertThat(chunks.get(1).getContent()).isEqualTo(" world");
        assertThat(chunks.get(2).getFinish()).isTrue();
    }

    @Test
    void shouldNotCrash_whenOnEvent_givenInvalidJson() {
        listener.onEvent(eventSource, "1", "message", "not valid json");

        // 不应抛出异常，也不应产生 chunk
        assertThat(chunks).isEmpty();
    }

    @Test
    void shouldEmitFinishChunk_whenOnFailure() {
        Throwable error = new RuntimeException("Connection reset");

        listener.onFailure(eventSource, error, null);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).getFinish()).isTrue();
    }

    @Test
    void shouldEmitFinishChunk_whenOnFailure_givenResponse() {
        Response response = mock(Response.class);
        Throwable error = new RuntimeException("HTTP 500");

        listener.onFailure(eventSource, error, response);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).getFinish()).isTrue();
    }

    @Test
    void shouldHandleFinishReasonNull_whenOnEvent_givenExplicitNull() {
        String data = "{\"choices\":[{\"delta\":{\"content\":\"test\"},\"finish_reason\":null}]}";

        listener.onEvent(eventSource, "1", "message", data);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).getContent()).isEqualTo("test");
        assertThat(chunks.get(0).getFinish()).isFalse();
    }

    @Test
    void shouldAccumulateToolCalls_whenOnEvent_givenIncrementalToolCallChunks() {
        // Chunk 1: tool call id and name
        listener.onEvent(eventSource, "1", "message",
                "{\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,\"id\":\"call_abc\",\"type\":\"function\",\"function\":{\"name\":\"query_order\",\"arguments\":\"\"}}]}}]}");

        // Chunk 2: partial arguments
        listener.onEvent(eventSource, "2", "message",
                "{\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,\"function\":{\"arguments\":\"{\\\"order_id\\\":\\\"dt\"}}]}}]}");

        // Chunk 3: remaining arguments + finish
        listener.onEvent(eventSource, "3", "message",
                "{\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,\"function\":{\"arguments\":\"123\\\"}\"}}]},\"finish_reason\":\"tool_calls\"}]}");

        assertThat(chunks).hasSize(3);
        // 前两条没有 finish，tool_calls 不附加到 chunk 上
        assertThat(chunks.get(0).getToolCalls()).isNull();
        assertThat(chunks.get(1).getToolCalls()).isNull();
        // 最后一条 finish 时，累积的 tool_calls 被附加
        assertThat(chunks.get(2).getFinish()).isTrue();
        assertThat(chunks.get(2).getFinishReason()).isEqualTo("tool_calls");
        assertThat(chunks.get(2).getToolCalls()).hasSize(1);
        assertThat(chunks.get(2).getToolCalls().get(0).getId()).isEqualTo("call_abc");
        assertThat(chunks.get(2).getToolCalls().get(0).getName()).isEqualTo("query_order");
        assertThat(chunks.get(2).getToolCalls().get(0).getArguments()).isEqualTo("{\"order_id\":\"dt123\"}");
    }
}
