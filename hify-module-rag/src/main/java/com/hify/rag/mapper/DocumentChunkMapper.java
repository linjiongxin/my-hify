package com.hify.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.rag.entity.DocumentChunk;
import com.hify.rag.vo.ChunkSearchVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;

import java.util.List;

/**
 * 文档分块 Mapper（包含向量检索）
 */
@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunk> {

    /**
     * 相似度检索（余弦距离）
     *
     * @param embedding 向量（格式：'[0.1, 0.2, ...]'）
     * @param limit 返回数量
     * @return 检索结果
     */
    @Select("""
        SELECT id, kb_id AS kbId, document_id AS documentId, content,
               chunk_index AS chunkIndex, meta_json AS metaJson,
               (1 - (embedding <=> #{embedding}::vector)) AS similarity
        FROM document_chunk
        WHERE deleted = FALSE AND enabled = TRUE
        ORDER BY embedding <=> #{embedding}::vector
        LIMIT #{limit}
        """)
    List<ChunkSearchVO> searchSimilar(@Param("embedding") String embedding, @Param("limit") int limit);

    /**
     * 在指定知识库中检索
     *
     * @param embedding 向量
     * @param kbId 知识库 ID
     * @param limit 返回数量
     * @return 检索结果
     */
    @Select("""
        SELECT id, kb_id AS kbId, document_id AS documentId, content,
               chunk_index AS chunkIndex, meta_json AS metaJson,
               (1 - (embedding <=> #{embedding}::vector)) AS similarity
        FROM document_chunk
        WHERE deleted = FALSE AND enabled = TRUE AND kb_id = #{kbId}
        ORDER BY embedding <=> #{embedding}::vector
        LIMIT #{limit}
        """)
    List<ChunkSearchVO> searchSimilarInKb(@Param("embedding") String embedding,
                                          @Param("kbId") Long kbId,
                                          @Param("limit") int limit);

    /**
     * 插入向量数据（由 XML mapper 实现，支持 ::vector cast）
     */
    int insertVector(DocumentChunk chunk);

    /**
     * 批量插入向量数据（由 XML mapper 实现）
     */
    int insertVectorBatch(@Param("chunks") List<DocumentChunk> chunks);
}
