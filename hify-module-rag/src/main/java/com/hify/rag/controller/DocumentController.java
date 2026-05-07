package com.hify.rag.controller;

import com.hify.common.core.enums.ResultCode;
import com.hify.common.core.exception.BizException;
import com.hify.rag.api.DocumentApi;
import com.hify.rag.vo.DocumentVO;
import com.hify.common.web.entity.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentApi documentApi;

    @PostMapping("/knowledge-bases/{kbId}/documents")
    public Long uploadDocument(@PathVariable("kbId") Long kbId,
                               @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, "文件不能为空");
        }

        String fileName = file.getOriginalFilename();
        Long fileSize = file.getSize();
        log.info("上传文档到知识库 {}, 文件名: {}, 大小: {}", kbId, fileName, fileSize);

        // 保存文件到存储并触发异步处理（解析 → 分块 → 向量化）
        return documentApi.uploadAndSave(kbId, file);
    }

    @GetMapping("/knowledge-bases/{kbId}/documents")
    public PageResult<DocumentVO> listDocuments(@PathVariable("kbId") Long kbId,
                                                 @RequestParam(value = "page", defaultValue = "1") int page,
                                                 @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        return documentApi.listByKbId(kbId, page, pageSize);
    }

    @GetMapping("/documents/{id}")
    public DocumentVO getDocument(@PathVariable("id") Long id) {
        DocumentVO vo = documentApi.getVoById(id);
        if (vo == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "文档不存在");
        }
        return vo;
    }

    @DeleteMapping("/documents/{id}")
    public void deleteDocument(@PathVariable("id") Long id) {
        log.info("删除文档: {}", id);
        documentApi.delete(id);
    }

    @PostMapping("/documents/{id}/retry")
    public void retryProcess(@PathVariable("id") Long id) {
        log.info("重试处理文档: {}", id);
        documentApi.retryProcess(id);
    }
}