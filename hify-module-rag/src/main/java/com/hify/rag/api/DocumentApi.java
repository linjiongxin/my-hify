package com.hify.rag.api;

import com.hify.common.web.entity.PageResult;
import com.hify.rag.entity.Document;
import com.hify.rag.vo.DocumentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档 API 接口
 */
public interface DocumentApi {

    /**
     * 上传文档（仅保存元数据）
     */
    Long uploadDocument(Long kbId, String fileName, String fileType, Long fileSize);

    /**
     * 上传文件并保存，触发异步处理
     */
    Long uploadAndSave(Long kbId, MultipartFile file);

    /**
     * 获取文档详情
     */
    Document getById(Long id);

    /**
     * 获取文档 VO
     */
    DocumentVO getVoById(Long id);

    /**
     * 按知识库查询文档列表
     */
    PageResult<DocumentVO> listByKbId(Long kbId, int page, int pageSize);

    /**
     * 删除文档
     */
    void delete(Long id);

    /**
     * 重试处理文档
     */
    void retryProcess(Long id);
}