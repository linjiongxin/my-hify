package com.hify.agent.service;

import com.hify.agent.dto.AgentCreateRequest;
import com.hify.agent.dto.AgentToolBatchRequest;
import com.hify.agent.dto.AgentUpdateRequest;
import com.hify.agent.vo.AgentVO;
import com.hify.common.web.entity.PageParam;
import com.hify.common.web.entity.PageResult;

public interface AgentService {

    Long createAgent(AgentCreateRequest request);

    void updateAgent(Long id, AgentUpdateRequest request);

    void deleteAgent(Long id);

    AgentVO getAgentDetail(Long id);

    PageResult<AgentVO> pageAgents(PageParam pageParam);

    void bindTools(Long agentId, AgentToolBatchRequest request);

    void replaceTools(Long agentId, AgentToolBatchRequest request);

    void unbindTool(Long agentId, Long toolId);
}