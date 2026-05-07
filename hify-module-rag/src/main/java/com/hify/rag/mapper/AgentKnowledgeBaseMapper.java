package com.hify.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.rag.entity.AgentKnowledgeBase;
import org.apache.ibatis.annotations.Mapper;

/**
 * Agent × 知识库绑定 Mapper
 */
@Mapper
public interface AgentKnowledgeBaseMapper extends BaseMapper<AgentKnowledgeBase> {
}