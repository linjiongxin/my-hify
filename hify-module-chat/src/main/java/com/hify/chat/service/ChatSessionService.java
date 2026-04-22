package com.hify.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hify.chat.dto.CreateSessionRequest;
import com.hify.chat.entity.ChatSession;
import com.hify.chat.mapper.ChatSessionMapper;
import com.hify.chat.vo.ChatSessionVO;
import com.hify.common.core.enums.ResultCode;
import com.hify.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话 Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatSessionMapper chatSessionMapper;

    @Transactional(rollbackFor = Exception.class)
    public ChatSessionVO createSession(Long userId, CreateSessionRequest request) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setAgentId(request.getAgentId());
        session.setTitle(request.getFirstMessage() != null
                ? request.getFirstMessage().substring(0, Math.min(20, request.getFirstMessage().length()))
                : "新对话");
        session.setStatus("active");
        session.setMessageCount(0);
        session.setLastMessageAt(LocalDateTime.now());
        chatSessionMapper.insert(session);
        return toVO(session);
    }

    @Transactional(readOnly = true)
    public List<ChatSessionVO> listUserSessions(Long userId) {
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getDeleted, false)
                .orderByDesc(ChatSession::getLastMessageAt);
        return chatSessionMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatSessionVO getSession(Long userId, Long sessionId) {
        ChatSession session = getSessionOrThrow(userId, sessionId);
        return toVO(session);
    }

    @Transactional(rollbackFor = Exception.class)
    public void archiveSession(Long userId, Long sessionId) {
        ChatSession session = getSessionOrThrow(userId, sessionId);
        session.setStatus("archived");
        chatSessionMapper.updateById(session);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long userId, Long sessionId) {
        ChatSession session = getSessionOrThrow(userId, sessionId);
        chatSessionMapper.deleteById(session);
    }

    ChatSession getSessionOrThrow(Long userId, Long sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || Boolean.TRUE.equals(session.getDeleted())) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "会话不存在");
        }
        if (!session.getUserId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN, "无权访问该会话");
        }
        return session;
    }

    private ChatSessionVO toVO(ChatSession session) {
        ChatSessionVO vo = new ChatSessionVO();
        BeanUtils.copyProperties(session, vo);
        return vo;
    }
}
