package com.hify.common.web.storage;

import com.hify.common.core.storage.FileStorageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 本地文件系统存储实现
 *
 * @author hify
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {

    private final FileStorageProperties properties;

    @PostConstruct
    public void init() {
        File dir = new File(properties.getBasePath());
        if (!dir.exists() && !dir.mkdirs()) {
            log.warn("创建上传目录失败: {}", properties.getBasePath());
        }
    }

    @Override
    public String upload(String directory, String filename, InputStream inputStream) throws IOException {
        Path dirPath = Paths.get(properties.getBasePath(), directory);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        Path targetPath = dirPath.resolve(filename);
        Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);

        String relativePath = directory + "/" + filename;
        log.info("文件上传成功, path={}", relativePath);
        return relativePath;
    }

    @Override
    public InputStream download(String path) throws IOException {
        Path targetPath = Paths.get(properties.getBasePath(), path);
        return new FileInputStream(targetPath.toFile());
    }

    @Override
    public boolean delete(String path) {
        try {
            Path targetPath = Paths.get(properties.getBasePath(), path);
            return Files.deleteIfExists(targetPath);
        } catch (IOException e) {
            log.warn("删除文件失败, path={}", path, e);
            return false;
        }
    }

    @Override
    public String getUrl(String path) {
        return properties.getUrlPrefix() + "/" + path;
    }
}
