package com.hify.agent.controller;

import com.hify.agent.dto.AgentCreateRequest;
import com.hify.agent.dto.AgentToolBatchRequest;
import com.hify.agent.dto.AgentUpdateRequest;
import com.hify.agent.service.AgentService;
import com.hify.agent.vo.AgentVO;
import com.hify.common.web.entity.PageParam;
import com.hify.common.web.entity.PageResult;
import com.hify.common.web.entity.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
    public Result<Void> getTools(@PathVariable("id") Long id) {
        // TODO: 后续实现
        return Result.success();
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
    public Result<Void> getMcpServers(@PathVariable("id") Long id) {
        // TODO: MCP 后续实现
        return Result.success();
    }
}