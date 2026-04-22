package com.hify.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.chat.entity.ChatMessage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 消息 Mapper
 */
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 查询会话内的历史消息（用于组装 LLM 上下文）
     */
    @Select("SELECT * FROM chat_message WHERE session_id = #{sessionId} AND deleted = FALSE AND status = 'completed' ORDER BY seq ASC")
    List<ChatMessage> selectHistoryBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 查询会话内最大的 seq
     */
    @Select("SELECT COALESCE(MAX(seq), 0) FROM chat_message WHERE session_id = #{sessionId} AND deleted = FALSE")
    Integer selectMaxSeqBySessionId(@Param("sessionId") Long sessionId);
}
