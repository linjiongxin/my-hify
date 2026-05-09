package com.hify.agent.controller;

import com.hify.agent.dto.AgentCreateRequest;
import com.hify.agent.dto.AgentToolBatchRequest;
import com.hify.agent.dto.AgentUpdateRequest;
import com.hify.agent.service.AgentService;
import com.hify.agent.vo.AgentToolVO;
import com.hify.agent.vo.AgentVO;
import com.hify.common.web.entity.PageParam;
import com.hify.common.web.entity.PageResult;
import com.hify.common.web.entity.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Agent 控制器
 *
 * @author hify
 */
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    /**
     * 创建 Agent
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody AgentCreateRequest request) {
        return Result.success(agentService.createAgent(request));
    }

    /**
     * 详情
     */
    @GetMapping("/{id}")
    public Result<AgentVO> detail(@PathVariable("id") Long id) {
        return Result.success(agentService.getAgentDetail(id));
    }

    /**
     * 更新
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id, @Valid @RequestBody AgentUpdateRequest request) {
        agentService.updateAgent(id, request);
        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        agentService.deleteAgent(id);
        return Result.success();
    }

    /**
     * 分页列表
     */
    @GetMapping
    public Result<PageResult<AgentVO>> page(@Valid PageParam pageParam) {
        return Result.success(agentService.pageAgents(pageParam));
    }

    /**
     * 获取 Agent 绑定的工具
     */
    @GetMapping("/{id}/tools")
    public Result<List<AgentToolVO>> getTools(@PathVariable("id") Long id) {
        AgentVO agent = agentService.getAgentDetail(id);
        return Result.success(agent.getTools() != null ? agent.getTools() : List.of());
    }

    /**
     * 绑定工具
     */
    @PostMapping("/{id}/tools")
    public Result<Void> bindTools(@PathVariable("id") Long id, @Valid @RequestBody AgentToolBatchRequest request) {
        agentService.bindTools(id, request);
        return Result.success();
    }

    /**
     * 替换工具
     */
    @PutMapping("/{id}/tools")
    public Result<Void> replaceTools(@PathVariable("id") Long id, @Valid @RequestBody AgentToolBatchRequest request) {
        agentService.replaceTools(id, request);
        return Result.success();
    }

    /**
     * 解绑工具
     */
    @DeleteMapping("/{id}/tools/{toolId}")
    public Result<Void> unbindTool(@PathVariable("id") Long id, @PathVariable("toolId") Long toolId) {
        agentService.unbindTool(id, toolId);
        return Result.success();
    }

    /**
     * 获取 Agent 绑定的 MCP 服务器
     */
    @GetMapping("/{id}/mcp-servers")
    public Result<List<Long>> getMcpServers(@PathVariable("id") Long id) {
        return Result.success(agentService.listMcpServerIds(id));
    }

    /**
     * 绑定 MCP 服务器
     */
    @PostMapping("/{id}/mcp-servers")
    public Result<Void> bindMcpServers(@PathVariable("id") Long id, @RequestBody List<Long> serverIds) {
        agentService.bindMcpServers(id, serverIds);
        return Result.success();
    }

    /**
     * 解绑 MCP 服务器
     */
    @DeleteMapping("/{id}/mcp-servers/{serverId}")
    public Result<Void> unbindMcpServer(@PathVariable("id") Long id, @PathVariable("serverId") Long serverId) {
        agentService.unbindMcpServer(id, serverId);
        return Result.success();
    }
}