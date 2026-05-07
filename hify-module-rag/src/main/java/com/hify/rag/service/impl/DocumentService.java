package com.hify.rag.service.impl;

import com.hify.rag.api.DocumentApi;
import com.hify.rag.core.DocumentParser;
import com.hify.rag.core.EmbeddingService;
import com.hify.rag.core.FileStorageService;
import com.hify.rag.core.RecursiveChunker;
import com.hify.rag.entity.Document;
import com.hify.rag.entity.DocumentChunk;
import com.hify.rag.mapper.DocumentChunkMapper;
import com.hify.rag.mapper.DocumentMapper;
import com.hify.rag.mapper.KnowledgeBaseMapper;
import com.hify.rag.vo.DocumentVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * 文档服务实现
 */
@Slf4j
@Service
public class DocumentService implements DocumentApi {

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private DocumentChunkMapper documentChunkMapper;

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private List<DocumentParser> documentParsers;

    @Autowired
    private RecursiveChunker recursiveChunker;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private FileStorageService fileStorageService;

    @Override
    public Long uploadDocument(Long kbId, String fileName, String fileType, Long fileSize) {
        Document doc = new Document();
        doc.setKbId(kbId);
        doc.setFileName(fileName);
        doc.setFileType(fileType);
        doc.setFileSize(fileSize);
        doc.setStatus("pending");
        doc.setTotalChunks(0);
        documentMapper.insert(doc);
        return doc.getId();
    }

    /**
     * 上传文件并保存（供 Controller 调用）
     */
    public Long uploadAndSave(Long kbId, MultipartFile file) {
        String fileName = file.getOriginalFilename();
        String fileType = extractFileType(fileName);

        // 1. 保存文档记录
        Document doc = new Document();
        doc.setKbId(kbId);
        doc.setFileName(fileName);
        doc.setFileType(fileType);
        doc.setFileSize(file.getSize());
        doc.setStatus("pending");
        doc.setTotalChunks(0);
        documentMapper.insert(doc);

        // 2. 保存文件到存储
        try {
            String filePath = fileStorageService.upload(kbId, file);
            log.info("文件保存成功: kbId={}, docId={}, path={}", kbId, doc.getId(), filePath);

            // 3. 触发异步处理
            processDocumentAsync(doc.getId());
        } catch (Exception e) {
            log.error("文件保存失败: kbId={}, fileName={}", kbId, fileName, e);
            doc.setStatus("failed");
            doc.setErrorMessage("文件保存失败: " + e.getMessage());
            documentMapper.updateById(doc);
        }

        return doc.getId();
    }

    @Override
    public Document getById(Long id) {
        return documentMapper.selectById(id);
    }

    @Override
    public DocumentVO getVoById(Long id) {
        Document doc = documentMapper.selectById(id);
        if (doc == null) return null;
        DocumentVO vo = new DocumentVO();
        BeanUtils.copyProperties(doc, vo);
        return vo;
    }

    @Override
    public com.hify.common.web.entity.PageResult<DocumentVO> listByKbId(Long kbId, int page, int pageSize) {
        Page<Document> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getKbId, kbId)
               .orderByDesc(Document::getCreatedAt);

        IPage<Document> result = documentMapper.selectPage(pageParam, wrapper);
        Page<DocumentVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<DocumentVO> voList = result.getRecords().stream().map(doc -> {
            DocumentVO vo = new DocumentVO();
            BeanUtils.copyProperties(doc, vo);
            return vo;
        }).toList();
        voPage.setRecords(voList);

        return new com.hify.common.web.entity.PageResult<>(voList, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Override
    public void delete(Long id) {
        Document doc = documentMapper.selectById(id);
        if (doc != null) {
            doc.setDeleted(true);
            documentMapper.updateById(doc);
        }
    }

    @Override
    public void retryProcess(Long id) {
        Document doc = documentMapper.selectById(id);
        if (doc != null && "failed".equals(doc.getStatus())) {
            doc.setStatus("pending");
            doc.setErrorMessage(null);
            documentMapper.updateById(doc);
            processDocumentAsync(id);
        }
    }

    /**
     * 异步处理文档：解析 → 分块 → 向量化 → 存储
     */
    @Async
    @Transactional
    public void processDocumentAsync(Long documentId) {
        Document doc = documentMapper.selectById(documentId);
        if (doc == null) {
            log.error("Document not found: {}", documentId);
            return;
        }

        doc.setStatus("processing");
        documentMapper.updateById(doc);

        try {
            // 1. 获取解析器
            DocumentParser parser = documentParsers.stream()
                    .filter(p -> p.supports(doc.getFileType()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("不支持的文件类型: " + doc.getFileType()));

            // 2. 从文件存储读取内容
            // 文件路径存储在 parsed_content 中（作为临时字段，实际应该新建 file_path 字段）
            // 这里简化处理：如果有 parsed_content 直接用，否则从文件存储读取
            String content;
            if (doc.getParsedContent() != null && !doc.getParsedContent().isBlank()) {
                // 已有解析内容（可能是之前上传时直接传的文本）
                content = doc.getParsedContent();
            } else {
                // 从文件存储读取
                // 注意：这里需要文件路径，当前设计缺失，建议增加 file_path 字段
                // 暂时抛出异常提示
                throw new RuntimeException("文件路径未配置，请先上传文件");
            }

            if (content == null || content.isBlank()) {
                throw new RuntimeException("文档内容为空");
            }

            // 3. 分块
            List<String> chunks = recursiveChunker.chunk(content);
            log.info("Document {} split into {} chunks", documentId, chunks.size());

            // 4. 向量化 + 存储
            for (int i = 0; i < chunks.size(); i++) {
                String chunkContent = chunks.get(i);
                float[] embedding = embeddingService.embed(chunkContent);

                DocumentChunk chunk = new DocumentChunk();
                chunk.setKbId(doc.getKbId());
                chunk.setDocumentId(doc.getId());
                chunk.setContent(chunkContent);
                chunk.setEmbedding(vectorToString(embedding));
                chunk.setChunkIndex(i);
                chunk.setEnabled(true);
                documentChunkMapper.insert(chunk);
            }

            // 5. 更新文档状态
            doc.setStatus("completed");
            doc.setTotalChunks(chunks.size());
            documentMapper.updateById(doc);

            log.info("Document {} processed successfully, {} chunks stored", documentId, chunks.size());

        } catch (Exception e) {
            log.error("Failed to process document: {}", documentId, e);
            doc.setStatus("failed");
            doc.setErrorMessage(e.getMessage());
            documentMapper.updateById(doc);
        }
    }

    private String extractFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "unknown";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    private String vectorToString(float[] vector) {
        if (vector == null || vector.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}