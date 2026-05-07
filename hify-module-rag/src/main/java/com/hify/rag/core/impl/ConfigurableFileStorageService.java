package com.hify.rag.core.impl;

import com.hify.rag.core.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * 可配置的文件存储实现（云存储占位）
 *
 * <p>当前为本地存储模式，通过配置切换不同的存储后端</p>
 * <p>后期可扩展：OSS、S3、MinIO 等</p>
 */
@Slf4j
@Component
@Primary
public class ConfigurableFileStorageService implements FileStorageService {

    @Value("${hify.storage.type:local}")
    private String storageType;

    @Value("${hify.storage.local.base-path:/tmp/hify-rag}")
    private String localBasePath;

    @Value("${hify.storage.local.url-prefix:/api/rag/files}")
    private String localUrlPrefix;

    // OSS 配置占位
    @Value("${hify.storage.oss.endpoint:}")
    private String ossEndpoint;

    @Value("${hify.storage.oss.bucket:}")
    private String ossBucket;

    @Value("${hify.storage.oss.access-key:}")
    private String ossAccessKey;

    @Value("${hify.storage.oss.secret-key:}")
    private String ossSecretKey;

    private final FileStorageService localStorage;

    @Autowired
    public ConfigurableFileStorageService(@Qualifier("ragFileStorageService") FileStorageService localStorage) {
        this.localStorage = localStorage;
    }

    @Override
    public String upload(Long kbId, String fileName, byte[] content) {
        return switch (storageType.toLowerCase()) {
            case "local" -> localStorage.upload(kbId, fileName, content);
            case "oss" -> uploadToOss(kbId, fileName, content);
            default -> {
                log.warn("未知的存储类型: {}, 使用本地存储", storageType);
                yield localStorage.upload(kbId, fileName, content);
            }
        };
    }

    @Override
    public String upload(Long kbId, MultipartFile file) {
        try {
            return upload(kbId, file.getOriginalFilename(), file.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] download(String filePath) {
        return switch (storageType.toLowerCase()) {
            case "local" -> localStorage.download(filePath);
            case "oss" -> downloadFromOss(filePath);
            default -> localStorage.download(filePath);
        };
    }

    @Override
    public InputStream getInputStream(String filePath) {
        return switch (storageType.toLowerCase()) {
            case "local" -> localStorage.getInputStream(filePath);
            case "oss" -> new ByteArrayInputStream(downloadFromOss(filePath));
            default -> localStorage.getInputStream(filePath);
        };
    }

    @Override
    public void delete(String filePath) {
        switch (storageType.toLowerCase()) {
            case "local" -> localStorage.delete(filePath);
            case "oss" -> deleteFromOss(filePath);
            default -> localStorage.delete(filePath);
        }
    }

    @Override
    public boolean exists(String filePath) {
        return switch (storageType.toLowerCase()) {
            case "local" -> localStorage.exists(filePath);
            case "oss" -> existsInOss(filePath);
            default -> localStorage.exists(filePath);
        };
    }

    @Override
    public String getUrl(String filePath) {
        return switch (storageType.toLowerCase()) {
            case "local" -> localStorage.getUrl(filePath);
            case "oss" -> getOssUrl(filePath);
            default -> localStorage.getUrl(filePath);
        };
    }

    // ========== OSS 相关方法（TODO: 后期完善）==========

    private String uploadToOss(Long kbId, String fileName, byte[] content) {
        // TODO: 实现 OSS 上传
        // Aliyun OSS SDK 示例:
        // OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        // ossClient.putObject(bucketName, objectKey, new ByteArrayInputStream(content));
        throw new UnsupportedOperationException("OSS 存储待实现，请联系管理员配置");
    }

    private byte[] downloadFromOss(String filePath) {
        // TODO: 实现 OSS 下载
        throw new UnsupportedOperationException("OSS 存储待实现，请联系管理员配置");
    }

    private void deleteFromOss(String filePath) {
        // TODO: 实现 OSS 删除
        throw new UnsupportedOperationException("OSS 存储待实现，请联系管理员配置");
    }

    private boolean existsInOss(String filePath) {
        // TODO: 实现 OSS 存在检查
        throw new UnsupportedOperationException("OSS 存储待实现，请联系管理员配置");
    }

    private String getOssUrl(String filePath) {
        // TODO: 实现 OSS URL 生成
        // return String.format("https://%s.%s/%s", bucket, endpoint, filePath);
        throw new UnsupportedOperationException("OSS 存储待实现，请联系管理员配置");
    }
}