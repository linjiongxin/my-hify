package com.hify.rag.service.impl;

import com.hify.common.web.entity.PageResult;
import com.hify.rag.api.KnowledgeBaseApi;
import com.hify.rag.dto.KnowledgeBaseCreateDTO;
import com.hify.rag.dto.KnowledgeBaseUpdateDTO;
import com.hify.rag.entity.KnowledgeBase;
import com.hify.rag.mapper.KnowledgeBaseMapper;
import com.hify.rag.vo.KnowledgeBaseVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 知识库服务实现
 */
@Slf4j
@Service
public class KnowledgeBaseService implements KnowledgeBaseApi {

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Override
    public Long create(KnowledgeBaseCreateDTO dto) {
        KnowledgeBase kb = new KnowledgeBase();
        BeanUtils.copyProperties(dto, kb);
        // 设置默认值
        if (kb.getEmbeddingModel() == null) {
            kb.setEmbeddingModel("text-embedding-v2");
        }
        if (kb.getChunkSize() == null) {
            kb.setChunkSize(512);
        }
        if (kb.getChunkOverlap() == null) {
            kb.setChunkOverlap(50);
        }
        kb.setEnabled(true);
        knowledgeBaseMapper.insert(kb);
        return kb.getId();
    }

    @Override
    public void update(Long id, KnowledgeBaseUpdateDTO dto) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(id);
        if (kb == null) {
            throw new RuntimeException("知识库不存在: " + id);
        }
        if (StringUtils.hasText(dto.getName())) {
            kb.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            kb.setDescription(dto.getDescription());
        }
        if (StringUtils.hasText(dto.getEmbeddingModel())) {
            kb.setEmbeddingModel(dto.getEmbeddingModel());
        }
        if (dto.getChunkSize() != null) {
            kb.setChunkSize(dto.getChunkSize());
        }
        if (dto.getChunkOverlap() != null) {
            kb.setChunkOverlap(dto.getChunkOverlap());
        }
        if (dto.getEnabled() != null) {
            kb.setEnabled(dto.getEnabled());
        }
        knowledgeBaseMapper.updateById(kb);
    }

    @Override
    public void delete(Long id) {
        // 逻辑删除
        KnowledgeBase kb = knowledgeBaseMapper.selectById(id);
        if (kb != null) {
            kb.setDeleted(true);
            knowledgeBaseMapper.updateById(kb);
        }
    }

    @Override
    public KnowledgeBase getById(Long id) {
        return knowledgeBaseMapper.selectById(id);
    }

    @Override
    public KnowledgeBaseVO getVoById(Long id) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(id);
        if (kb == null) return null;
        KnowledgeBaseVO vo = new KnowledgeBaseVO();
        BeanUtils.copyProperties(kb, vo);
        return vo;
    }

    @Override
    public PageResult<KnowledgeBaseVO> list(KnowledgeBaseQueryDTO query) {
        Page<KnowledgeBase> page = new Page<>(query.getPage(), query.getPageSize());
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getName())) {
            wrapper.like(KnowledgeBase::getName, query.getName());
        }
        if (query.getEnabled() != null) {
            wrapper.eq(KnowledgeBase::getEnabled, query.getEnabled());
        }
        wrapper.orderByDesc(KnowledgeBase::getCreatedAt);

        IPage<KnowledgeBase> result = knowledgeBaseMapper.selectPage(page, wrapper);
        Page<KnowledgeBaseVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<KnowledgeBaseVO> voList = result.getRecords().stream().map(kb -> {
            KnowledgeBaseVO vo = new KnowledgeBaseVO();
            BeanUtils.copyProperties(kb, vo);
            return vo;
        }).toList();
        voPage.setRecords(voList);

        return new PageResult<>(voList, result.getCurrent(), result.getSize(), result.getTotal());
    }
}