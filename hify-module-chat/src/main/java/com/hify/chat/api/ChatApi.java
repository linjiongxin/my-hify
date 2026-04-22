package com.hify.chat.api;

import com.hify.chat.dto.CreateSessionRequest;
import com.hify.chat.vo.ChatMessageVO;
import com.hify.chat.vo.ChatSessionVO;

import java.util.List;

/**
 * 对话模块对外 API
 */
public interface ChatApi {

    /**
     * 创建会话
     */
    ChatSessionVO createSession(Long userId, CreateSessionRequest request);

    /**
     * 获取用户会话列表
     */
    List<ChatSessionVO> listUserSessions(Long userId);

    /**
     * 获取会话消息列表
     */
    List<ChatMessageVO> listSessionMessages(Long userId, Long sessionId);
}
