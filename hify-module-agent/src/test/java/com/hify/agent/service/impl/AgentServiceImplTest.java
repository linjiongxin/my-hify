package com.hify.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hify.agent.dto.AgentCreateRequest;
import com.hify.agent.dto.AgentToolBatchRequest;
import com.hify.agent.dto.AgentUpdateRequest;
import com.hify.agent.entity.Agent;
import com.hify.agent.entity.AgentTool;
import com.hify.agent.mapper.AgentMapper;
import com.hify.agent.mapper.AgentMcpBindingMapper;
import com.hify.agent.mapper.AgentToolMapper;
import com.hify.agent.vo.AgentVO;
import com.hify.common.core.enums.ResultCode;
import com.hify.common.core.exception.BizException;
import com.hify.common.web.entity.PageParam;
import com.hify.common.web.entity.PageResult;
import com.hify.model.api.ModelConfigApi;
import com.hify.model.api.dto.ModelConfigDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentServiceImplTest {

    @Mock
    private AgentMapper agentMapper;

    @Mock
    private AgentToolMapper agentToolMapper;

    @Mock
    private AgentMcpBindingMapper agentMcpBindingMapper;

    @Mock
    private ModelConfigApi modelConfigApi;

    @InjectMocks
    private AgentServiceImpl agentService;

    @Test
    void shouldCreateAgent_whenGivenValidRequest() {
        AgentCreateRequest request = new AgentCreateRequest();
        request.setName("Test Agent");
        request.setModelId("gpt-4o");
        request.setDescription("A test agent");
        request.setSystemPrompt("You are helpful");
        request.setTemperature(new BigDecimal("0.8"));
        request.setMaxTokens(1024);
        request.setTopP(new BigDecimal("0.9"));
        request.setEnabled(true);

        ModelConfigDTO modelConfigDTO = new ModelConfigDTO();
        modelConfigDTO.setId(1L);
        modelConfigDTO.setModelId("gpt-4o");
        modelConfigDTO.setName("GPT-4o");
        modelConfigDTO.setEnabled(true);
        when(modelConfigApi.getModelByModelId("gpt-4o")).thenReturn(modelConfigDTO);
        doAnswer(invocation -> {
            Agent agent = invocation.getArgument(0);
            agent.setId(100L);
            return 1;
        }).when(agentMapper).insert(any(Agent.class));

        Long agentId = agentService.createAgent(request);

        assertThat(agentId).isEqualTo(100L);
        verify(agentMapper).insert(any(Agent.class));
    }

    @Test
    void shouldThrowException_whenCreateAgent_withNonExistingModel() {
        AgentCreateRequest request = new AgentCreateRequest();
        request.setName("Test Agent");
        request.setModelId("non-existent-model");

        when(modelConfigApi.getModelByModelId("non-existent-model")).thenReturn(null);

        assertThatThrownBy(() -> agentService.createAgent(request))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getCode()).isEqualTo(ResultCode.DATA_NOT_FOUND.getCode());
                });
    }

    @Test
    void shouldUpdateAgent_whenAgentExists() {
        Agent existingAgent = new Agent();
        existingAgent.setId(1L);
        existingAgent.setName("Old Name");
        existingAgent.setModelId("gpt-4o");

        AgentUpdateRequest request = new AgentUpdateRequest();
        request.setName("New Name");

        when(agentMapper.selectById(1L)).thenReturn(existingAgent);
        when(agentMapper.updateById(any(Agent.class))).thenReturn(1);

        agentService.updateAgent(1L, request);

        verify(agentMapper).updateById(any(Agent.class));
    }

    @Test
    void shouldThrowException_whenUpdateNonExistingAgent() {
        when(agentMapper.selectById(999L)).thenReturn(null);

        AgentUpdateRequest request = new AgentUpdateRequest();
        request.setName("New Name");

        assertThatThrownBy(() -> agentService.updateAgent(999L, request))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getCode()).isEqualTo(ResultCode.DATA_NOT_FOUND.getCode());
                });
    }

    @Test
    void shouldDeleteAgent_whenAgentExists() {
        Agent existingAgent = new Agent();
        existingAgent.setId(1L);
        existingAgent.setName("Test Agent");

        when(agentMapper.selectById(1L)).thenReturn(existingAgent);
        when(agentToolMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(agentMcpBindingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(agentMapper.deleteById(1L)).thenReturn(1);

        agentService.deleteAgent(1L);

        verify(agentMapper).deleteById(1L);
    }

    @Test
    void shouldThrowException_whenDeleteNonExistingAgent() {
        when(agentMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> agentService.deleteAgent(999L))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getCode()).isEqualTo(ResultCode.DATA_NOT_FOUND.getCode());
                });
    }

    @Test
    void shouldReturnAgentDetail_whenAgentExists() {
        Agent existingAgent = new Agent();
        existingAgent.setId(1L);
        existingAgent.setName("Test Agent");
        existingAgent.setDescription("A test agent");
        existingAgent.setModelId("gpt-4o");
        existingAgent.setSystemPrompt("You are helpful");
        existingAgent.setTemperature(new BigDecimal("0.7"));
        existingAgent.setMaxTokens(2048);
        existingAgent.setTopP(new BigDecimal("1.0"));
        existingAgent.setEnabled(true);

        AgentTool tool = new AgentTool();
        tool.setId(10L);
        tool.setAgentId(1L);
        tool.setToolName("calculator");
        tool.setToolType("builtin");
        tool.setEnabled(true);
        tool.setSortOrder(1);

        when(agentMapper.selectById(1L)).thenReturn(existingAgent);
        when(agentToolMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(tool));

        AgentVO vo = agentService.getAgentDetail(1L);

        assertThat(vo).isNotNull();
        assertThat(vo.getId()).isEqualTo(1L);
        assertThat(vo.getName()).isEqualTo("Test Agent");
        assertThat(vo.getModelId()).isEqualTo("gpt-4o");
        assertThat(vo.getTools()).hasSize(1);
        assertThat(vo.getTools().get(0).getToolName()).isEqualTo("calculator");
    }

    @Test
    void shouldThrowException_whenGetDetailNonExistingAgent() {
        when(agentMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> agentService.getAgentDetail(999L))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getCode()).isEqualTo(ResultCode.DATA_NOT_FOUND.getCode());
                });
    }

    @Test
    void shouldReturnPagedAgents() {
        Agent agent1 = new Agent();
        agent1.setId(1L);
        agent1.setName("Agent 1");
        agent1.setModelId("gpt-4o");

        Agent agent2 = new Agent();
        agent2.setId(2L);
        agent2.setName("Agent 2");
        agent2.setModelId("claude-3");

        Page<Agent> page = new Page<>(1, 20);
        page.setRecords(List.of(agent1, agent2));
        page.setTotal(2L);
        page.setPages(1L);

        PageParam pageParam = new PageParam();
        pageParam.setPageNum(1L);
        pageParam.setPageSize(20L);

        when(agentMapper.selectPage(any(Page.class), any())).thenReturn(page);

        PageResult<AgentVO> result = agentService.pageAgents(pageParam);

        assertThat(result).isNotNull();
        assertThat(result.getRecords()).hasSize(2);
        assertThat(result.getTotal()).isEqualTo(2L);
    }

    @Test
    void shouldBindTools_whenAgentExists() {
        Agent existingAgent = new Agent();
        existingAgent.setId(1L);

        AgentToolBatchRequest.ToolItem toolItem = new AgentToolBatchRequest.ToolItem();
        toolItem.setToolName("calculator");
        toolItem.setToolType("builtin");
        toolItem.setToolImpl("com.hify.tool.CalculatorTool");
        toolItem.setEnabled(true);
        toolItem.setSortOrder(1);

        AgentToolBatchRequest request = new AgentToolBatchRequest();
        request.setTools(List.of(toolItem));

        when(agentMapper.selectById(1L)).thenReturn(existingAgent);
        when(agentToolMapper.insert(any(AgentTool.class))).thenReturn(1);

        agentService.bindTools(1L, request);

        verify(agentToolMapper).insert(any(AgentTool.class));
    }

    @Test
    void shouldReplaceTools_whenAgentExists() {
        Agent existingAgent = new Agent();
        existingAgent.setId(1L);

        AgentTool existingTool = new AgentTool();
        existingTool.setId(10L);
        existingTool.setAgentId(1L);
        existingTool.setToolName("old_tool");

        AgentToolBatchRequest.ToolItem newToolItem = new AgentToolBatchRequest.ToolItem();
        newToolItem.setToolName("new_tool");
        newToolItem.setToolType("builtin");
        newToolItem.setToolImpl("com.hify.tool.NewTool");
        newToolItem.setEnabled(true);
        newToolItem.setSortOrder(1);

        AgentToolBatchRequest request = new AgentToolBatchRequest();
        request.setTools(List.of(newToolItem));

        when(agentMapper.selectById(1L)).thenReturn(existingAgent);
        when(agentToolMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(existingTool));
        when(agentToolMapper.deleteById(10L)).thenReturn(1);
        when(agentToolMapper.insert(any(AgentTool.class))).thenReturn(1);

        agentService.replaceTools(1L, request);

        verify(agentToolMapper).deleteById(10L);
        verify(agentToolMapper).insert(any(AgentTool.class));
    }

    @Test
    void shouldUnbindTool_whenToolBelongsToAgent() {
        Agent existingAgent = new Agent();
        existingAgent.setId(1L);

        AgentTool existingTool = new AgentTool();
        existingTool.setId(10L);
        existingTool.setAgentId(1L);
        existingTool.setToolName("calculator");

        when(agentMapper.selectById(1L)).thenReturn(existingAgent);
        when(agentToolMapper.selectById(10L)).thenReturn(existingTool);
        when(agentToolMapper.deleteById(10L)).thenReturn(1);

        agentService.unbindTool(1L, 10L);

        verify(agentToolMapper).deleteById(10L);
    }

    @Test
    void shouldThrowException_whenUnbindTool_notBelongsToAgent() {
        Agent existingAgent = new Agent();
        existingAgent.setId(1L);

        AgentTool existingTool = new AgentTool();
        existingTool.setId(10L);
        existingTool.setAgentId(2L); // different agent

        when(agentMapper.selectById(1L)).thenReturn(existingAgent);
        when(agentToolMapper.selectById(10L)).thenReturn(existingTool);

        assertThatThrownBy(() -> agentService.unbindTool(1L, 10L))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getCode()).isEqualTo(ResultCode.DATA_NOT_FOUND.getCode());
                });
    }

    @Test
    void shouldCreateAgent_withWorkflowMode() {
        AgentCreateRequest request = new AgentCreateRequest();
        request.setName("Workflow Agent");
        request.setModelId("gpt-4o");
        request.setWorkflowId(10L);
        request.setExecutionMode("workflow");

        ModelConfigDTO modelConfigDTO = new ModelConfigDTO();
        modelConfigDTO.setId(1L);
        modelConfigDTO.setModelId("gpt-4o");
        when(modelConfigApi.getModelByModelId("gpt-4o")).thenReturn(modelConfigDTO);
        doAnswer(invocation -> {
            Agent agent = invocation.getArgument(0);
            agent.setId(101L);
            return 1;
        }).when(agentMapper).insert(any(Agent.class));

        Long agentId = agentService.createAgent(request);

        assertThat(agentId).isEqualTo(101L);
        org.mockito.ArgumentCaptor<Agent> captor = org.mockito.ArgumentCaptor.forClass(Agent.class);
        verify(agentMapper).insert(captor.capture());
        Agent saved = captor.getValue();
        assertThat(saved.getWorkflowId()).isEqualTo(10L);
        assertThat(saved.getExecutionMode()).isEqualTo("workflow");
    }

    @Test
    void shouldDefaultExecutionModeToReact_whenNotSpecified() {
        AgentCreateRequest request = new AgentCreateRequest();
        request.setName("Default Agent");
        request.setModelId("gpt-4o");
        // 不设置 executionMode

        ModelConfigDTO modelConfigDTO = new ModelConfigDTO();
        modelConfigDTO.setId(1L);
        modelConfigDTO.setModelId("gpt-4o");
        when(modelConfigApi.getModelByModelId("gpt-4o")).thenReturn(modelConfigDTO);
        doAnswer(invocation -> {
            Agent agent = invocation.getArgument(0);
            agent.setId(102L);
            return 1;
        }).when(agentMapper).insert(any(Agent.class));

        agentService.createAgent(request);

        org.mockito.ArgumentCaptor<Agent> captor = org.mockito.ArgumentCaptor.forClass(Agent.class);
        verify(agentMapper).insert(captor.capture());
        assertThat(captor.getValue().getExecutionMode()).isEqualTo("react");
    }

    @Test
    void shouldReturnWorkflowFields_whenGetAgentDetail() {
        Agent existingAgent = new Agent();
        existingAgent.setId(1L);
        existingAgent.setName("Workflow Agent");
        existingAgent.setModelId("gpt-4o");
        existingAgent.setWorkflowId(5L);
        existingAgent.setExecutionMode("workflow");
        existingAgent.setEnabled(true);

        when(agentMapper.selectById(1L)).thenReturn(existingAgent);
        when(agentToolMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        AgentVO vo = agentService.getAgentDetail(1L);

        assertThat(vo.getWorkflowId()).isEqualTo(5L);
        assertThat(vo.getExecutionMode()).isEqualTo("workflow");
    }

    @Test
    void shouldUpdateWorkflowFields_whenUpdateAgent() {
        Agent existingAgent = new Agent();
        existingAgent.setId(1L);
        existingAgent.setName("Old Name");
        existingAgent.setModelId("gpt-4o");

        AgentUpdateRequest request = new AgentUpdateRequest();
        request.setWorkflowId(20L);
        request.setExecutionMode("workflow");

        when(agentMapper.selectById(1L)).thenReturn(existingAgent);
        when(agentMapper.updateById(any(Agent.class))).thenReturn(1);

        agentService.updateAgent(1L, request);

        org.mockito.ArgumentCaptor<Agent> captor = org.mockito.ArgumentCaptor.forClass(Agent.class);
        verify(agentMapper).updateById(captor.capture());
        assertThat(captor.getValue().getWorkflowId()).isEqualTo(20L);
        assertThat(captor.getValue().getExecutionMode()).isEqualTo("workflow");
    }
}