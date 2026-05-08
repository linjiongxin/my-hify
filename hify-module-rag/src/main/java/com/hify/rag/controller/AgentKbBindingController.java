package com.hify.rag.controller;

import com.hify.common.web.entity.Result;
import com.hify.rag.api.AgentKnowledgeBaseApi;
import com.hify.rag.dto.AgentKbBindingDTO;
import com.hify.rag.vo.AgentKnowledgeBaseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Agent × 知识库绑定 Controller
 */
@Slf4j
@RestController
@RequestMapping("/rag/agent-kb")
@RequiredArgsConstructor
public class AgentKbBindingController {

    private final AgentKnowledgeBaseApi agentKnowledgeBaseApi;

    @PostMapping("/bind")
    public Result<Void> bind(@RequestBody AgentKbBindingDTO dto) {
        log.info("绑定 Agent {} 到知识库 {}", dto.getAgentId(), dto.getKbId());
        agentKnowledgeBaseApi.bind(dto);
        return Result.success();
    }

    @DeleteMapping("/unbind")
    public Result<Void> unbind(@RequestParam("agentId") Long agentId, @RequestParam("kbId") Long kbId) {
        log.info("解绑 Agent {} 从知识库 {}", agentId, kbId);
        agentKnowledgeBaseApi.unbind(agentId, kbId);
        return Result.success();
    }

    @GetMapping("/agent/{agentId}")
    public Result<List<AgentKnowledgeBaseVO>> getByAgentId(@PathVariable("agentId") Long agentId) {
        return Result.success(agentKnowledgeBaseApi.getByAgentId(agentId));
    }

    @PutMapping("/update")
    public Result<Void> updateBinding(@RequestParam("agentId") Long agentId,
                              @RequestParam("kbId") Long kbId,
                              @RequestParam(value = "topK", required = false) Integer topK,
                              @RequestParam(value = "similarityThreshold", required = false) java.math.BigDecimal similarityThreshold) {
        log.info("更新 Agent {} 知识库 {} 绑定配置", agentId, kbId);
        agentKnowledgeBaseApi.updateBinding(agentId, kbId, topK, similarityThreshold);
        return Result.success();
    }
}