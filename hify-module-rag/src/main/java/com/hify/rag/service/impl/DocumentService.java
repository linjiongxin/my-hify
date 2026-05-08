package com.hify.rag.service.impl;

import com.hify.rag.api.DocumentApi;
import com.hify.rag.core.DocumentParser;
import com.hify.rag.core.EmbeddingService;
import com.hify.rag.core.EmbeddingServiceFactory;
import com.hify.rag.core.FileStorageService;
import com.hify.rag.core.RecursiveChunker;
import com.hify.rag.entity.Document;
import com.hify.rag.entity.DocumentChunk;
import com.hify.rag.entity.KnowledgeBase;
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
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
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
    private EmbeddingServiceFactory embeddingServiceFactory;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    @Lazy
    private DocumentService self;

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
     * 上传文件并保存，触发异步处理
     * 注意：不加 @Transactional，确保 insert/update 立即提交，
     * 否则 @Async 在事务提交前执行，查不到刚插入的文档
     */
    @Override
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

            // 3. 保存文件路径
            doc.setFilePath(filePath);
            documentMapper.updateById(doc);

            // 4. 通过代理触发异步处理（确保 @Async 和 @Transactional 生效）
            self.processDocumentAsync(doc.getId());
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
    @Transactional
    public void delete(Long id) {
        // 级联删除文档分块（物理删除，避免残留 chunk 污染向量检索）
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DocumentChunk> chunkWrapper =
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        chunkWrapper.eq(DocumentChunk::getDocumentId, id);
        documentChunkMapper.delete(chunkWrapper);

        documentMapper.deleteById(id);
        log.info("Document {} deleted with chunks", id);
    }

    @Override
    public void retryProcess(Long id) {
        Document doc = documentMapper.selectById(id);
        if (doc != null && ("failed".equals(doc.getStatus()) || "pending".equals(doc.getStatus()))) {
            doc.setStatus("pending");
            doc.setErrorMessage(null);
            documentMapper.updateById(doc);
            self.processDocumentAsync(id);
        }
    }

    /**
     * 独立事务标记文档为失败（REQUIRES_NEW 确保即使主事务回滚也能更新状态）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long documentId, String errorMessage) {
        Document doc = documentMapper.selectById(documentId);
        if (doc != null) {
            doc.setStatus("failed");
            doc.setErrorMessage(errorMessage);
            documentMapper.updateById(doc);
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
            // 1. 获取知识库配置（包含 embedding 模型）
            KnowledgeBase kb = knowledgeBaseMapper.selectById(doc.getKbId());
            if (kb == null) {
                throw new RuntimeException("Knowledge base not found: " + doc.getKbId());
            }

            // 根据知识库的 embedding 模型获取对应的服务
            EmbeddingService embeddingService = embeddingServiceFactory.getService(kb.getEmbeddingModel());
            log.info("Using embedding service for model: {}", kb.getEmbeddingModel());

            // 2. 获取解析器
            DocumentParser parser = documentParsers.stream()
                    .filter(p -> p.supports(doc.getFileType()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("不支持的文件类型: " + doc.getFileType()));

            // 3. 从文件存储读取内容并解析
            String content;
            if (doc.getParsedContent() != null && !doc.getParsedContent().isBlank()) {
                // 已有解析内容（可能是之前上传时直接传的文本）
                content = doc.getParsedContent();
            } else {
                // 从文件存储读取并解析
                try (InputStream is = fileStorageService.getInputStream(doc.getFilePath())) {
                    content = parser.parse(is);
                }
            }

            if (content == null || content.isBlank()) {
                throw new RuntimeException("文档内容为空");
            }

            // 4. 分块
            List<String> chunks = recursiveChunker.chunk(content);
            log.info("Document {} split into {} chunks", documentId, chunks.size());

            // 5. 批量向量化
            List<float[]> embeddings = embeddingService.batchEmbed(chunks);

            // 6. 构建 chunk 实体列表
            List<DocumentChunk> chunkEntities = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk chunk = new DocumentChunk();
                chunk.setKbId(doc.getKbId());
                chunk.setDocumentId(doc.getId());
                chunk.setContent(chunks.get(i));
                chunk.setEmbedding(vectorToString(embeddings.get(i)));
                chunk.setChunkIndex(i);
                chunk.setEnabled(true);
                chunkEntities.add(chunk);
            }

            // 7. 批量插入（使用 XML mapper 中的 ::vector cast）
            documentChunkMapper.insertVectorBatch(chunkEntities);

            // 6. 更新文档状态
            doc.setStatus("completed");
            doc.setTotalChunks(chunks.size());
            documentMapper.updateById(doc);

            log.info("Document {} processed successfully, {} chunks stored", documentId, chunks.size());

        } catch (Exception e) {
            log.error("Failed to process document: {}", documentId, e);
            // 使用独立事务更新失败状态，避免主事务回滚导致 UPDATE 也失败
            self.markFailed(documentId, e.getMessage());
        }
    }

    private String extractFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "unknown";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    private static final int VECTOR_DIM = 1536;

    private String vectorToString(float[] vector) {
        if (vector == null || vector.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < VECTOR_DIM; i++) {
            if (i > 0) sb.append(",");
            sb.append(i < vector.length ? vector[i] : 0.0f);
        }
        sb.append("]");
        return sb.toString();
    }
}