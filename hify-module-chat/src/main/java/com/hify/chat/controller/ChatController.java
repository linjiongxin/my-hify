package com.hify.chat.controller;

import com.hify.chat.dto.ChatRequest;
import com.hify.chat.dto.CreateSessionRequest;
import com.hify.chat.service.ChatMessageService;
import com.hify.chat.service.ChatSessionService;
import com.hify.chat.vo.ChatMessageVO;
import com.hify.chat.vo.ChatSessionVO;
import com.hify.common.web.entity.Result;
import com.hify.common.web.security.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 对话控制器
 */
@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Validated
public class ChatController {

    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;

    /**
     * 创建会话
     */
    @PostMapping("/session")
    public Result<ChatSessionVO> createSession(@Valid @RequestBody CreateSessionRequest request) {
        return Result.success(chatSessionService.createSession(UserContext.getUserId(), request));
    }

    /**
     * 获取当前用户的会话列表
     */
    @GetMapping("/sessions")
    public Result<List<ChatSessionVO>> listSessions() {
        return Result.success(chatSessionService.listUserSessions(UserContext.getUserId()));
    }

    /**
     * 获取会话历史消息
     */
    @GetMapping("/session/{sessionId}/messages")
    public Result<List<ChatMessageVO>> listMessages(@PathVariable("sessionId") Long sessionId) {
        return Result.success(chatMessageService.listSessionMessages(UserContext.getUserId(), sessionId));
    }

    /**
     * 归档会话
     */
    @PostMapping("/session/{sessionId}/archive")
    public Result<Void> archiveSession(@PathVariable("sessionId") Long sessionId) {
        chatSessionService.archiveSession(UserContext.getUserId(), sessionId);
        return Result.success();
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/session/{sessionId}")
    public Result<Void> deleteSession(@PathVariable("sessionId") Long sessionId) {
        chatSessionService.deleteSession(UserContext.getUserId(), sessionId);
        return Result.success();
    }

    /**
     * 流式对话（SSE）
     */
    @GetMapping(value = "/stream/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@PathVariable("sessionId") Long sessionId, @RequestParam("message") String message) {
        SseEmitter emitter = new SseEmitter(0L);

        emitter.onCompletion(() -> log.debug("SSE completed, sessionId={}", sessionId));
        emitter.onTimeout(() -> log.warn("SSE timeout, sessionId={}", sessionId));
        emitter.onError(e -> log.error("SSE error, sessionId={}", sessionId, e));

        chatMessageService.streamChat(UserContext.getUserId(), sessionId, message, emitter);
        return emitter;
    }
}
