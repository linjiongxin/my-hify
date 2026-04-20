package com.hify.agent.service.impl;

import com.hify.agent.AgentTestApplication;
import com.hify.agent.dto.AgentCreateRequest;
import com.hify.agent.entity.Agent;
import com.hify.agent.mapper.AgentMapper;
import com.hify.agent.mapper.AgentToolMapper;
import com.hify.agent.service.AgentService;
import com.hify.model.api.ModelConfigApi;
import com.hify.model.api.dto.ModelConfigDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = AgentTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class AgentServiceImplIT {

    @Autowired
    private AgentService agentService;

    @Autowired
    private AgentMapper agentMapper;

    @Autowired
    private AgentToolMapper agentToolMapper;

    @MockBean
    private ModelConfigApi modelConfigApi;

    @BeforeEach
    void setUp() {
        // Mock model config API to return a valid model
        ModelConfigDTO mockModel = new ModelConfigDTO();
        mockModel.setId(10L);
        mockModel.setModelId("gpt-4o");
        mockModel.setName("GPT-4o");
        when(modelConfigApi.getModelByModelId(anyString())).thenReturn(mockModel);
    }

    @Test
    void shouldSoftDeleteAgent_whenDeleteAgent() {
        // Given: create an agent
        AgentCreateRequest request = new AgentCreateRequest();
        request.setName("TestAgent");
        request.setModelId("gpt-4o");
        request.setTemperature(new BigDecimal("0.7"));
        Long id = agentService.createAgent(request);

        // Verify agent exists
        Agent agent = agentMapper.selectById(id);
        assertThat(agent).isNotNull();
        assertThat(agent.getName()).isEqualTo("TestAgent");

        // When: delete the agent
        agentService.deleteAgent(id);

        // Then: agent should be soft deleted (selectById returns null due to @TableLogic)
        Agent deletedAgent = agentMapper.selectById(id);
        assertThat(deletedAgent).isNull();
    }

    @Test
    void shouldCreateAgent_whenGivenValidRequest() {
        AgentCreateRequest request = new AgentCreateRequest();
        request.setName("NewAgent");
        request.setModelId("gpt-4o");
        request.setTemperature(new BigDecimal("0.8"));

        Long id = agentService.createAgent(request);

        assertThat(id).isNotNull();
        Agent agent = agentMapper.selectById(id);
        assertThat(agent).isNotNull();
        assertThat(agent.getName()).isEqualTo("NewAgent");
        assertThat(agent.getTemperature()).isEqualByComparingTo("0.8");
    }

    @Test
    void shouldThrowException_whenCreateAgent_withNonExistingModel() {
        // Given: mock returns null for non-existing model
        when(modelConfigApi.getModelByModelId("non-existing-model")).thenReturn(null);

        AgentCreateRequest request = new AgentCreateRequest();
        request.setName("InvalidAgent");
        request.setModelId("non-existing-model");

        // When/Then
        org.junit.jupiter.api.Assertions.assertThrows(
            com.hify.common.core.exception.BizException.class,
            () -> agentService.createAgent(request)
        );
    }
}