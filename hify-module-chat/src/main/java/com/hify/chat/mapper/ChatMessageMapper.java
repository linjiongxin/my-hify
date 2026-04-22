package com.hify.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.chat.entity.ChatMessage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 消息 Mapper
 */
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 查询会话内的历史消息（用于组装 LLM 上下文）
     */
    List<ChatMessage> selectHistoryBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 查询会话内最大的 seq
     */
    Integer selectMaxSeqBySessionId(@Param("sessionId") Long sessionId);
}
