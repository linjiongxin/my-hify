package com.hify.rag.service.impl;

import com.hify.rag.api.AgentKnowledgeBaseApi;
import com.hify.rag.dto.AgentKbBindingDTO;
import com.hify.rag.entity.AgentKnowledgeBase;
import com.hify.rag.mapper.AgentKnowledgeBaseMapper;
import com.hify.rag.vo.AgentKnowledgeBaseVO;
import com.hify.rag.vo.KnowledgeBaseVO;
import com.hify.common.web.entity.PageResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Agent × 知识库绑定服务实现
 */
@Slf4j
@Service
public class AgentKnowledgeBaseService implements AgentKnowledgeBaseApi {

    @Autowired
    private AgentKnowledgeBaseMapper agentKnowledgeBaseMapper;

    @Override
    @Transactional
    public void bind(AgentKbBindingDTO dto) {
        // 检查是否已存在绑定
        LambdaQueryWrapper<AgentKnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentKnowledgeBase::getAgentId, dto.getAgentId())
               .eq(AgentKnowledgeBase::getKbId, dto.getKbId())
               .eq(AgentKnowledgeBase::getDeleted, false);

        AgentKnowledgeBase existing = agentKnowledgeBaseMapper.selectOne(wrapper);
        if (existing != null) {
            throw new RuntimeException("该 Agent 已绑定此知识库");
        }

        AgentKnowledgeBase binding = new AgentKnowledgeBase();
        binding.setAgentId(dto.getAgentId());
        binding.setKbId(dto.getKbId());
        binding.setTopK(dto.getTopK() != null ? dto.getTopK() : 5);
        binding.setSimilarityThreshold(dto.getSimilarityThreshold() != null
                ? dto.getSimilarityThreshold() : new java.math.BigDecimal("0.7"));
        binding.setEnabled(true);
        agentKnowledgeBaseMapper.insert(binding);

        log.info("Agent {} bound to knowledge base {}", dto.getAgentId(), dto.getKbId());
    }

    @Override
    @Transactional
    public void unbind(Long agentId, Long kbId) {
        LambdaQueryWrapper<AgentKnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentKnowledgeBase::getAgentId, agentId)
               .eq(AgentKnowledgeBase::getKbId, kbId)
               .eq(AgentKnowledgeBase::getDeleted, false);

        AgentKnowledgeBase binding = agentKnowledgeBaseMapper.selectOne(wrapper);
        if (binding != null) {
            binding.setDeleted(true);
            agentKnowledgeBaseMapper.updateById(binding);
            log.info("Agent {} unbound from knowledge base {}", agentId, kbId);
        }
    }

    @Override
    public List<AgentKnowledgeBaseVO> getByAgentId(Long agentId) {
        LambdaQueryWrapper<AgentKnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentKnowledgeBase::getAgentId, agentId)
               .eq(AgentKnowledgeBase::getDeleted, false)
               .eq(AgentKnowledgeBase::getEnabled, true);

        List<AgentKnowledgeBase> bindings = agentKnowledgeBaseMapper.selectList(wrapper);
        return bindings.stream().map(binding -> {
            AgentKnowledgeBaseVO vo = new AgentKnowledgeBaseVO();
            BeanUtils.copyProperties(binding, vo);
            return vo;
        }).toList();
    }

    @Override
    @Transactional
    public void updateBinding(Long agentId, Long kbId, Integer topK, java.math.BigDecimal similarityThreshold) {
        LambdaQueryWrapper<AgentKnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentKnowledgeBase::getAgentId, agentId)
               .eq(AgentKnowledgeBase::getKbId, kbId)
               .eq(AgentKnowledgeBase::getDeleted, false);

        AgentKnowledgeBase binding = agentKnowledgeBaseMapper.selectOne(wrapper);
        if (binding != null) {
            if (topK != null) {
                binding.setTopK(topK);
            }
            if (similarityThreshold != null) {
                binding.setSimilarityThreshold(similarityThreshold);
            }
            agentKnowledgeBaseMapper.updateById(binding);
            log.info("Updated binding for agent {} kb {}", agentId, kbId);
        }
    }
}