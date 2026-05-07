package com.hify.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.rag.entity.Document;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档 Mapper
 */
@Mapper
public interface DocumentMapper extends BaseMapper<Document> {
}