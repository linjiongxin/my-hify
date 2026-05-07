package com.hify.rag.controller;

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
@RequestMapping("/api/rag/agent-kb")
@RequiredArgsConstructor
public class AgentKbBindingController {

    private final AgentKnowledgeBaseApi agentKnowledgeBaseApi;

    @PostMapping("/bind")
    public void bind(@RequestBody AgentKbBindingDTO dto) {
        log.info("绑定 Agent {} 到知识库 {}", dto.getAgentId(), dto.getKbId());
        agentKnowledgeBaseApi.bind(dto);
    }

    @DeleteMapping("/unbind")
    public void unbind(@RequestParam Long agentId, @RequestParam Long kbId) {
        log.info("解绑 Agent {} 从知识库 {}", agentId, kbId);
        agentKnowledgeBaseApi.unbind(agentId, kbId);
    }

    @GetMapping("/agent/{agentId}")
    public List<AgentKnowledgeBaseVO> getByAgentId(@PathVariable Long agentId) {
        return agentKnowledgeBaseApi.getByAgentId(agentId);
    }

    @PutMapping("/update")
    public void updateBinding(@RequestParam Long agentId,
                              @RequestParam Long kbId,
                              @RequestParam(required = false) Integer topK,
                              @RequestParam(required = false) java.math.BigDecimal similarityThreshold) {
        log.info("更新 Agent {} 知识库 {} 绑定配置", agentId, kbId);
        agentKnowledgeBaseApi.updateBinding(agentId, kbId, topK, similarityThreshold);
    }
}