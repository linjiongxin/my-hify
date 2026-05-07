package com.hify.rag.core.impl;

import com.hify.rag.core.DocumentParser;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * TXT 文档解析器
 */
@Component
public class TxtDocumentParser implements DocumentParser {

    @Override
    public String parse(InputStream input) {
        try {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse TXT file", e);
        }
    }

    @Override
    public boolean supports(String fileType) {
        return "txt".equalsIgnoreCase(fileType);
    }
}