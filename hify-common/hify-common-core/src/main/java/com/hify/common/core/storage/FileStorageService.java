package com.hify.common.core.storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文件存储服务接口
 *
 * @author hify
 */
public interface FileStorageService {

    /**
     * 上传文件
     *
     * @param directory   存储目录（如 "rag/documents"）
     * @param filename    文件名
     * @param inputStream 文件输入流
     * @return 文件访问路径或 URL
     */
    String upload(String directory, String filename, InputStream inputStream) throws IOException;

    /**
     * 下载文件
     *
     * @param path 文件路径
     * @return 文件输入流
     */
    InputStream download(String path) throws IOException;

    /**
     * 删除文件
     *
     * @param path 文件路径
     * @return 是否删除成功
     */
    boolean delete(String path);

    /**
     * 获取文件访问 URL
     *
     * @param path 文件路径
     * @return 访问 URL
     */
    String getUrl(String path);
}
