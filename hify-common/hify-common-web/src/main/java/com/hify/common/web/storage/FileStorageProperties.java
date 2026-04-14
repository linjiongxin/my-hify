package com.hify.common.web.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件存储配置属性
 *
 * @author hify
 */
@Data
@Component
@ConfigurationProperties(prefix = "hify.storage")
public class FileStorageProperties {

    /**
     * 本地存储根目录
     */
    private String basePath = "./uploads";

    /**
     * 文件访问 URL 前缀
     */
    private String urlPrefix = "/uploads";
}
