package com.hify.rag.api;

import com.hify.common.web.entity.PageResult;
import com.hify.rag.dto.KnowledgeBaseCreateDTO;
import com.hify.rag.dto.KnowledgeBaseUpdateDTO;
import com.hify.rag.entity.KnowledgeBase;
import com.hify.rag.vo.KnowledgeBaseVO;

import java.util.List;

/**
 * 知识库 API 接口
 */
public interface KnowledgeBaseApi {

    /**
     * 创建知识库
     */
    Long create(KnowledgeBaseCreateDTO dto);

    /**
     * 更新知识库
     */
    void update(Long id, KnowledgeBaseUpdateDTO dto);

    /**
     * 删除知识库
     */
    void delete(Long id);

    /**
     * 获取知识库详情
     */
    KnowledgeBase getById(Long id);

    /**
     * 获取知识库 VO
     */
    KnowledgeBaseVO getVoById(Long id);

    /**
     * 分页查询知识库
     */
    PageResult<KnowledgeBaseVO> list(KnowledgeBaseQueryDTO query);

    /**
     * 查询条件 DTO
     */
    @Data
    class KnowledgeBaseQueryDTO {
        private String name;
        private Boolean enabled;
        private Integer page = 1;
        private Integer pageSize = 20;
    }
}