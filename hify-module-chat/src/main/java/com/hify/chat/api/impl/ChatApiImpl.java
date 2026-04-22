package com.hify.chat.api.impl;

import com.hify.chat.api.ChatApi;
import com.hify.chat.dto.CreateSessionRequest;
import com.hify.chat.service.ChatMessageService;
import com.hify.chat.service.ChatSessionService;
import com.hify.chat.vo.ChatMessageVO;
import com.hify.chat.vo.ChatSessionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 对话模块 API 实现
 */
@Service
@RequiredArgsConstructor
public class ChatApiImpl implements ChatApi {

    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;

    @Override
    public ChatSessionVO createSession(Long userId, CreateSessionRequest request) {
        return chatSessionService.createSession(userId, request);
    }

    @Override
    public List<ChatSessionVO> listUserSessions(Long userId) {
        return chatSessionService.listUserSessions(userId);
    }

    @Override
    public List<ChatMessageVO> listSessionMessages(Long userId, Long sessionId) {
        return chatMessageService.listSessionMessages(userId, sessionId);
    }
}
