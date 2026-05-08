package com.hify.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hify.agent.dto.AgentCreateRequest;
import com.hify.agent.dto.AgentToolBatchRequest;
import com.hify.agent.dto.AgentUpdateRequest;
import com.hify.agent.entity.Agent;
import com.hify.agent.entity.AgentMcpBinding;
import com.hify.agent.entity.AgentTool;
import com.hify.agent.mapper.AgentMapper;
import com.hify.agent.mapper.AgentMcpBindingMapper;
import com.hify.agent.mapper.AgentToolMapper;
import com.hify.agent.api.AgentApi;
import com.hify.agent.api.dto.AgentDTO;
import com.hify.agent.api.dto.AgentToolDTO;
import com.hify.agent.service.AgentService;
import com.hify.agent.vo.AgentToolVO;
import com.hify.agent.vo.AgentVO;
import com.hify.common.core.enums.ResultCode;
import com.hify.common.core.exception.BizException;
import com.hify.common.web.entity.PageParam;
import com.hify.common.web.entity.PageResult;
import com.hify.model.api.ModelConfigApi;
import com.hify.model.api.dto.ModelConfigDTO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Agent Service 实现
 *
 * @author hify
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class AgentServiceImpl implements AgentService, AgentApi {

    private final AgentMapper agentMapper;
    private final AgentToolMapper agentToolMapper;
    private final AgentMcpBindingMapper agentMcpBindingMapper;
    private final ModelConfigApi modelConfigApi;

    public AgentServiceImpl(AgentMapper agentMapper,
                           AgentToolMapper agentToolMapper,
                           AgentMcpBindingMapper agentMcpBindingMapper,
                           ModelConfigApi modelConfigApi) {
        this.agentMapper = agentMapper;
        this.agentToolMapper = agentToolMapper;
        this.agentMcpBindingMapper = agentMcpBindingMapper;
        this.modelConfigApi = modelConfigApi;
    }

    @Override
    public Long createAgent(AgentCreateRequest request) {
        // 校验模型是否存在
        ModelConfigDTO model = modelConfigApi.getModelByModelId(request.getModelId());
        if (model == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "模型不存在");
        }

        Agent agent = new Agent();
        BeanUtils.copyProperties(request, agent);
        // 设置默认值
        if (agent.getTemperature() == null) {
            agent.setTemperature(new java.math.BigDecimal("0.7"));
        }
        if (agent.getMaxTokens() == null) {
            agent.setMaxTokens(2048);
        }
        if (agent.getTopP() == null) {
            agent.setTopP(new java.math.BigDecimal("1.0"));
        }
        if (agent.getEnabled() == null) {
            agent.setEnabled(true);
        }
        if (agent.getExecutionMode() == null) {
            agent.setExecutionMode("react");
        }
        agentMapper.insert(agent);
        return agent.getId();
    }

    @Override
    public void updateAgent(Long id, AgentUpdateRequest request) {
        Agent agent = agentMapper.selectById(id);
        if (agent == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "Agent 不存在");
        }
        if (request.getModelId() != null) {
            ModelConfigDTO model = modelConfigApi.getModelByModelId(request.getModelId());
            if (model == null) {
                throw new BizException(ResultCode.DATA_NOT_FOUND, "模型不存在");
            }
        }
        BeanUtils.copyProperties(request, agent);
        agentMapper.updateById(agent);
    }

    @Override
    public void deleteAgent(Long id) {
        Agent agent = agentMapper.selectById(id);
        if (agent == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "Agent 不存在");
        }
        // 软删除 Agent
        agentMapper.deleteById(id);
        // 软删除关联的工具
        LambdaQueryWrapper<AgentTool> toolWrapper = new LambdaQueryWrapper<>();
        toolWrapper.eq(AgentTool::getAgentId, id);
        List<AgentTool> tools = agentToolMapper.selectList(toolWrapper);
        if (!tools.isEmpty()) {
            tools.forEach(t -> agentToolMapper.deleteById(t.getId()));
        }
        // 软删除关联的 MCP 绑定
        LambdaQueryWrapper<AgentMcpBinding> mcpWrapper = new LambdaQueryWrapper<>();
        mcpWrapper.eq(AgentMcpBinding::getAgentId, id);
        List<AgentMcpBinding> bindings = agentMcpBindingMapper.selectList(mcpWrapper);
        if (!bindings.isEmpty()) {
            bindings.forEach(b -> agentMcpBindingMapper.deleteById(b.getId()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AgentVO getAgentDetail(Long id) {
        Agent agent = agentMapper.selectById(id);
        if (agent == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "Agent 不存在");
        }
        AgentVO vo = toVO(agent);
        // 加载工具列表
        LambdaQueryWrapper<AgentTool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentTool::getAgentId, id).orderByAsc(AgentTool::getSortOrder);
        List<AgentTool> tools = agentToolMapper.selectList(wrapper);
        vo.setTools(tools.stream().map(this::toToolVO).toList());
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AgentVO> pageAgents(PageParam pageParam) {
        Page<Agent> page = agentMapper.selectPage(pageParam.toPage(Agent.class), null);
        List<AgentVO> voList = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(voList, page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Override
    public void bindTools(Long agentId, AgentToolBatchRequest request) {
        Agent agent = agentMapper.selectById(agentId);
        if (agent == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "Agent 不存在");
        }
        if (request.getTools() == null || request.getTools().isEmpty()) {
            return;
        }
        for (AgentToolBatchRequest.ToolItem item : request.getTools()) {
            AgentTool tool = new AgentTool();
            tool.setAgentId(agentId);
            tool.setToolName(item.getToolName());
            tool.setToolType(item.getToolType());
            tool.setToolImpl(item.getToolImpl());
            tool.setConfigJson(item.getConfigJson());
            tool.setEnabled(item.getEnabled() != null ? item.getEnabled() : true);
            tool.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : 0);
            agentToolMapper.insert(tool);
        }
    }

    @Override
    public void replaceTools(Long agentId, AgentToolBatchRequest request) {
        Agent agent = agentMapper.selectById(agentId);
        if (agent == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "Agent 不存在");
        }
        // 软删除所有现有工具
        LambdaQueryWrapper<AgentTool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentTool::getAgentId, agentId);
        List<AgentTool> existingTools = agentToolMapper.selectList(wrapper);
        if (!existingTools.isEmpty()) {
            existingTools.forEach(t -> agentToolMapper.deleteById(t.getId()));
        }
        // 插入新工具
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            for (AgentToolBatchRequest.ToolItem item : request.getTools()) {
                AgentTool tool = new AgentTool();
                tool.setAgentId(agentId);
                tool.setToolName(item.getToolName());
                tool.setToolType(item.getToolType());
                tool.setToolImpl(item.getToolImpl());
                tool.setConfigJson(item.getConfigJson());
                tool.setEnabled(item.getEnabled() != null ? item.getEnabled() : true);
                tool.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : 0);
                agentToolMapper.insert(tool);
            }
        }
    }

    @Override
    public void unbindTool(Long agentId, Long toolId) {
        Agent agent = agentMapper.selectById(agentId);
        if (agent == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "Agent 不存在");
        }
        AgentTool tool = agentToolMapper.selectById(toolId);
        if (tool == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "工具不存在");
        }
        if (!tool.getAgentId().equals(agentId)) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "工具不属于该 Agent");
        }
        agentToolMapper.deleteById(toolId);
    }

    @Override
    @Transactional(readOnly = true)
    public AgentDTO getAgentById(Long id) {
        AgentVO vo = getAgentDetail(id);
        if (vo == null) {
            return null;
        }
        AgentDTO dto = new AgentDTO();
        BeanUtils.copyProperties(vo, dto);
        if (vo.getTools() != null) {
            dto.setTools(vo.getTools().stream().map(this::toToolDtoFromVo).toList());
        }
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentDTO> listEnabledAgents() {
        LambdaQueryWrapper<Agent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Agent::getEnabled, true);
        List<Agent> agents = agentMapper.selectList(wrapper);
        return agents.stream().map(this::toDto).toList();
    }

    private AgentDTO toDto(Agent agent) {
        AgentDTO dto = new AgentDTO();
        BeanUtils.copyProperties(agent, dto);
        // 加载工具列表
        LambdaQueryWrapper<AgentTool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentTool::getAgentId, agent.getId()).orderByAsc(AgentTool::getSortOrder);
        List<AgentTool> tools = agentToolMapper.selectList(wrapper);
        dto.setTools(tools.stream().map(this::toToolDto).toList());
        return dto;
    }

    private AgentToolDTO toToolDto(AgentTool tool) {
        AgentToolDTO dto = new AgentToolDTO();
        BeanUtils.copyProperties(tool, dto);
        return dto;
    }

    private AgentToolDTO toToolDtoFromVo(AgentToolVO vo) {
        AgentToolDTO dto = new AgentToolDTO();
        BeanUtils.copyProperties(vo, dto);
        return dto;
    }

    private AgentVO toVO(Agent agent) {
        AgentVO vo = new AgentVO();
        BeanUtils.copyProperties(agent, vo);
        return vo;
    }

    private AgentToolVO toToolVO(AgentTool tool) {
        AgentToolVO vo = new AgentToolVO();
        BeanUtils.copyProperties(tool, vo);
        return vo;
    }
}