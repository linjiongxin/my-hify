package com.hify.rag.core.impl;

import com.hify.rag.core.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 * 本地文件存储实现（RAG 模块专用）
 */
@Slf4j
@Component("ragFileStorageService")
public class LocalFileStorageService implements FileStorageService {

    @Value("${hify.storage.local.base-path:/tmp/hify-rag}")
    private String basePath;

    @Value("${hify.storage.local.url-prefix:/api/rag/files}")
    private String urlPrefix;

    @Override
    public String upload(Long kbId, String fileName, byte[] content) {
        String relativePath = buildPath(kbId, fileName);
        Path path = Paths.get(basePath, relativePath);

        try {
            // 确保目录存在
            Files.createDirectories(path.getParent());

            // 写入文件
            Files.write(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            log.info("文件上传成功: {}", path);
            return relativePath;

        } catch (IOException e) {
            log.error("文件上传失败: {}", path, e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String upload(Long kbId, MultipartFile file) {
        try {
            return upload(kbId, file.getOriginalFilename(), file.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] download(String filePath) {
        Path path = Paths.get(basePath, filePath);
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            log.error("文件读取失败: {}", path, e);
            throw new RuntimeException("文件读取失败: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream getInputStream(String filePath) {
        Path path = Paths.get(basePath, filePath);
        try {
            return new BufferedInputStream(Files.newInputStream(path));
        } catch (IOException e) {
            throw new RuntimeException("获取文件流失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String filePath) {
        Path path = Paths.get(basePath, filePath);
        try {
            Files.deleteIfExists(path);
            log.info("文件删除成功: {}", path);
        } catch (IOException e) {
            log.error("文件删除失败: {}", path, e);
            throw new RuntimeException("文件删除失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String filePath) {
        Path path = Paths.get(basePath, filePath);
        return Files.exists(path);
    }

    @Override
    public String getUrl(String filePath) {
        return urlPrefix + "/" + filePath;
    }

    /**
     * 构建存储路径：{kbId}/{uuid_filename}
     */
    private String buildPath(Long kbId, String fileName) {
        String ext = "";
        if (fileName != null && fileName.contains(".")) {
            ext = fileName.substring(fileName.lastIndexOf("."));
        }
        String uniqueName = UUID.randomUUID().toString().replace("-", "") + ext;
        return kbId + "/" + uniqueName;
    }

    public String getBasePath() {
        return basePath;
    }
}