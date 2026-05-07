package com.hify.rag.api;

import com.hify.common.web.entity.PageResult;
import com.hify.rag.dto.AgentKbBindingDTO;
import com.hify.rag.vo.AgentKnowledgeBaseVO;

import java.util.List;

/**
 * Agent × 知识库绑定 API 接口
 */
public interface AgentKnowledgeBaseApi {

    /**
     * 绑定知识库到 Agent
     */
    void bind(AgentKbBindingDTO dto);

    /**
     * 解绑知识库
     */
    void unbind(Long agentId, Long kbId);

    /**
     * 获取 Agent 绑定的所有知识库
     */
    List<AgentKnowledgeBaseVO> getByAgentId(Long agentId);

    /**
     * 更新绑定配置
     */
    void updateBinding(Long agentId, Long kbId, Integer topK, java.math.BigDecimal similarityThreshold);
}