package com.hify.rag.core;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 递归分块器
 * <p>
 * 按段落分块，长段落按句子再合并到 maxTokens
 */
@Component
public class RecursiveChunker {

    private static final int DEFAULT_MAX_TOKENS = 512;
    private static final int DEFAULT_OVERLAP_TOKENS = 50;

    private final int maxTokens;
    private final int overlapTokens;

    public RecursiveChunker() {
        this(DEFAULT_MAX_TOKENS, DEFAULT_OVERLAP_TOKENS);
    }

    public RecursiveChunker(int maxTokens, int overlapTokens) {
        this.maxTokens = maxTokens;
        this.overlapTokens = overlapTokens;
    }

    /**
     * 对文本进行递归分块
     */
    public List<String> chunk(String text) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return result;
        }

        // 按空行分段（段落分割）
        String[] paragraphs = text.split("\n\\s*\n");

        StringBuilder pending = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) continue;

            String trimmed = paragraph.trim();

            // 短段落（如标题）先缓存，和下一个段落合并，避免标题单独成块
            if (trimmed.length() <= 30 && pending.length() == 0) {
                pending.append(trimmed);
                continue;
            }

            // 如果有缓存的短段落，合并到当前段落
            if (pending.length() > 0) {
                trimmed = pending.toString() + "\n\n" + trimmed;
                pending.setLength(0);
            }

            int tokens = estimateTokens(trimmed);
            if (tokens <= maxTokens) {
                result.add(trimmed);
            } else {
                result.addAll(splitLongParagraph(trimmed));
            }
        }

        // 处理末尾剩余的短段落
        if (pending.length() > 0) {
            result.add(pending.toString());
        }

        return result;
    }

    /**
     * 拆分长段落
     */
    private List<String> splitLongParagraph(String paragraph) {
        List<String> chunks = new ArrayList<>();

        // 按句子拆分
        List<String> sentences = splitSentences(paragraph);
        List<String> currentChunk = new ArrayList<>();

        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty()) continue;

            int currentTokens = estimateTokens(String.join("", currentChunk));
            int sentenceTokens = estimateTokens(trimmed);

            if (currentTokens + sentenceTokens > maxTokens && !currentChunk.isEmpty()) {
                chunks.add(String.join("", currentChunk));
                // overlap: 保留上一个 chunk 的尾部句子
                currentChunk = new ArrayList<>();
            }
            currentChunk.add(trimmed);
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(String.join("", currentChunk));
        }

        return chunks;
    }

    /**
     * 按句子拆分
     */
    private List<String> splitSentences(String text) {
        // 按句末标点拆分：中英文句号、感叹号、问号
        Pattern sentencePattern = Pattern.compile("(?<=[。！？.!?])");
        return Arrays.stream(sentencePattern.split(text))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 估算 token 数量
     * <p>
     * 粗略估算：中文按字符数，英文按单词数 * 1.5
     */
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // 中文字符
        long chineseChars = text.chars().filter(c -> c > 127 && !Character.isWhitespace(c)).count();
        // 英文单词
        long englishWords = text.split("\\s+").length;
        return (int) (chineseChars + englishWords * 1.5);
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public int getOverlapTokens() {
        return overlapTokens;
    }
}