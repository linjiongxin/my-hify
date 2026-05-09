package com.hify.chat.mapper.trace;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.chat.entity.trace.TraceRagRetrievalLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * RAG 检索日志 Mapper（chat 模块只读）
 */
@Mapper
public interface ChatTraceRagRetrievalLogMapper extends BaseMapper<TraceRagRetrievalLog> {
}
