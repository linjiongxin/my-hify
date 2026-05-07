package com.hify.rag.core;

import java.io.InputStream;

/**
 * 文档解析器接口
 */
public interface DocumentParser {

    /**
     * 解析文档，提取纯文本内容
     */
    String parse(InputStream input);

    /**
     * 是否支持该文件类型
     */
    boolean supports(String fileType);
}