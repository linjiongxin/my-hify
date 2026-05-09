package com.hify.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.rag.entity.RagRetrievalLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * RAG 检索日志 Mapper
 */
@Mapper
public interface RagRetrievalLogMapper extends BaseMapper<RagRetrievalLog> {
}
