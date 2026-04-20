package com.hify.agent.api;

import com.hify.agent.api.dto.AgentDTO;

import java.util.List;

public interface AgentApi {

    AgentDTO getAgentById(Long id);

    List<AgentDTO> listEnabledAgents();
}
