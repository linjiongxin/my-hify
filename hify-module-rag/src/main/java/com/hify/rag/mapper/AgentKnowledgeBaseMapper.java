package com.hify.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.rag.entity.AgentKnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Agent × 知识库绑定 Mapper
 */
@Mapper
public interface AgentKnowledgeBaseMapper extends BaseMapper<AgentKnowledgeBase> {

    /**
     * 查询 Agent 与知识库的绑定关系（包含已逻辑删除的记录）
     */
    @Select("SELECT * FROM agent_knowledge_base WHERE agent_id = #{agentId} AND kb_id = #{kbId} LIMIT 1")
    AgentKnowledgeBase selectAllByAgentAndKb(@Param("agentId") Long agentId, @Param("kbId") Long kbId);

    /**
     * 恢复已逻辑删除的绑定记录（绕过 MyBatis-Plus @TableLogic 限制）
     */
    @Update("""
        UPDATE agent_knowledge_base
        SET deleted = false,
            enabled = true,
            top_k = #{topK},
            similarity_threshold = #{similarityThreshold},
            updated_at = NOW()
        WHERE id = #{id}
        """)
    int restoreById(@Param("id") Long id,
                    @Param("topK") Integer topK,
                    @Param("similarityThreshold") java.math.BigDecimal similarityThreshold);
}