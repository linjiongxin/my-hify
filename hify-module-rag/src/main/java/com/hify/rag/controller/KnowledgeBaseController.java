package com.hify.rag.controller;

import com.hify.common.core.enums.ResultCode;
import com.hify.common.core.exception.BizException;
import com.hify.common.web.entity.PageResult;
import com.hify.rag.api.KnowledgeBaseApi;
import com.hify.rag.dto.KnowledgeBaseCreateDTO;
import com.hify.rag.dto.KnowledgeBaseUpdateDTO;
import com.hify.rag.entity.KnowledgeBase;
import com.hify.rag.vo.KnowledgeBaseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 知识库管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/rag/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseApi knowledgeBaseApi;

    @PostMapping
    public Long create(@RequestBody @Validated KnowledgeBaseCreateDTO dto) {
        log.info("创建知识库: {}", dto.getName());
        return knowledgeBaseApi.create(dto);
    }

    @GetMapping
    public PageResult<KnowledgeBaseVO> list(KnowledgeBaseApi.KnowledgeBaseQueryDTO query) {
        return knowledgeBaseApi.list(query);
    }

    @GetMapping("/{id}")
    public KnowledgeBaseVO getById(@PathVariable("id") Long id) {
        KnowledgeBaseVO vo = knowledgeBaseApi.getVoById(id);
        if (vo == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "知识库不存在");
        }
        return vo;
    }

    @PutMapping("/{id}")
    public void update(@PathVariable("id") Long id, @RequestBody @Validated KnowledgeBaseUpdateDTO dto) {
        log.info("更新知识库: {}", id);
        knowledgeBaseApi.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        log.info("删除知识库: {}", id);
        knowledgeBaseApi.delete(id);
    }
}