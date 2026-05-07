package com.hify.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.rag.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库 Mapper
 */
@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {
}