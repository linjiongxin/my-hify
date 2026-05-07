package com.hify.rag.core.impl;

import com.hify.rag.core.DocumentParser;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Markdown 文档解析器
 */
@Component
public class MarkdownDocumentParser implements DocumentParser {

    // 移除标题标记（# 标题）
    private static final Pattern HEADER_PATTERN = Pattern.compile("^#{1,6}\\s+", Pattern.MULTILINE);
    // 移除图片 ![alt](url)
    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[.*?\\]\\(.*?\\)");
    // 移除链接 [text](url) -> text
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^]]+)\\]\\([^)]+\\)");
    // 移除代码块标记
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```[\\s\\S]*?```");
    // 移除行内代码 `code`
    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`([^`]+)`");
    // 移除引用标记
    private static final Pattern BLOCKQUOTE_PATTERN = Pattern.compile("^>\\s*", Pattern.MULTILINE);
    // 移除水平线
    private static final Pattern HR_PATTERN = Pattern.compile("^-{3,}$", Pattern.MULTILINE);

    @Override
    public String parse(InputStream input) {
        try {
            String content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            // 移除 markdown 标记，保留纯文本
            return cleanMarkdown(content);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Markdown file", e);
        }
    }

    private String cleanMarkdown(String content) {
        // 移除代码块
        content = CODE_BLOCK_PATTERN.matcher(content).replaceAll("");
        // 移除行内代码
        content = INLINE_CODE_PATTERN.matcher(content).replaceAll("$1");
        // 移除图片
        content = IMAGE_PATTERN.matcher(content).replaceAll("");
        // 移除链接，保留链接文本
        content = LINK_PATTERN.matcher(content).replaceAll("$1");
        // 移除标题标记
        content = HEADER_PATTERN.matcher(content).replaceAll("");
        // 移除粗体/斜体标记
        content = content.replaceAll("\\*+", "").replaceAll("_+", "");
        // 移除引用标记
        content = BLOCKQUOTE_PATTERN.matcher(content).replaceAll("");
        // 移除水平线
        content = HR_PATTERN.matcher(content).replaceAll("");
        return content.trim();
    }

    @Override
    public boolean supports(String fileType) {
        return "md".equalsIgnoreCase(fileType) || "markdown".equalsIgnoreCase(fileType);
    }
}