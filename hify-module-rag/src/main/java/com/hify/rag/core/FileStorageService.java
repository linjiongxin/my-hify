package com.hify.rag.core;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 文件存储服务接口
 */
public interface FileStorageService {

    /**
     * 上传文件
     *
     * @param kbId     知识库 ID
     * @param fileName 文件名
     * @param inputStream 文件内容
     * @return 存储后的文件路径
     */
    String upload(Long kbId, String fileName, byte[] content);

    /**
     * 上传 MultipartFile
     */
    String upload(Long kbId, MultipartFile file);

    /**
     * 下载文件
     *
     * @param filePath 文件路径
     * @return 文件内容
     */
    byte[] download(String filePath);

    /**
     * 获取文件输入流
     */
    InputStream getInputStream(String filePath);

    /**
     * 删除文件
     */
    void delete(String filePath);

    /**
     * 检查文件是否存在
     */
    boolean exists(String filePath);

    /**
     * 获取文件的访问 URL
     */
    String getUrl(String filePath);
}