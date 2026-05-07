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
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentApi documentApi;

    @PostMapping("/knowledge-bases/{kbId}/documents")
    public Long uploadDocument(@PathVariable Long kbId,
                               @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "文件不能为空");
        }

        String fileName = file.getOriginalFilename();
        String fileType = extractFileType(fileName);
        Long fileSize = file.getSize();

        log.info("上传文档到知识库 {}, 文件名: {}, 大小: {}", kbId, fileName, fileSize);
        Long docId = documentApi.uploadDocument(kbId, fileName, fileType, fileSize);

        // TODO: 保存文件到存储，然后异步处理
        // 这里需要集成文件存储服务

        return docId;
    }

    @GetMapping("/knowledge-bases/{kbId}/documents")
    public PageResult<DocumentVO> listDocuments(@PathVariable Long kbId,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int pageSize) {
        return documentApi.listByKbId(kbId, page, pageSize);
    }

    @GetMapping("/documents/{id}")
    public DocumentVO getDocument(@PathVariable Long id) {
        DocumentVO vo = documentApi.getVoById(id);
        if (vo == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "文档不存在");
        }
        return vo;
    }

    @DeleteMapping("/documents/{id}")
    public void deleteDocument(@PathVariable Long id) {
        log.info("删除文档: {}", id);
        documentApi.delete(id);
    }

    @PostMapping("/documents/{id}/retry")
    public void retryProcess(@PathVariable Long id) {
        log.info("重试处理文档: {}", id);
        documentApi.retryProcess(id);
    }

    private String extractFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "unknown";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}