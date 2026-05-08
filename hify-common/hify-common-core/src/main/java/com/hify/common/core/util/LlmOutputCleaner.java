package com.hify.common.core.util;

import java.util.regex.Pattern;

/**
 * LLM 输出清洗工具
 * <p>过滤部分模型的 reasoning/thinking 内容</p>
 */
public final class LlmOutputCleaner {

    private LlmOutputCleaner() {
    }

    // <think>...</think> (DeepSeek R1 等)
    private static final Pattern THINK_PATTERN = Pattern.compile("<think>.*?</think>", Pattern.DOTALL);
    // <thinking>...</thinking> (部分模型)
    private static final Pattern THINKING_PATTERN = Pattern.compile("<thinking>.*?</thinking>", Pattern.DOTALL);

    /**
     * 去除 thinking/reasoning 标签及内容
     */
    public static String stripThinking(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String result = THINK_PATTERN.matcher(text).replaceAll("");
        result = THINKING_PATTERN.matcher(result).replaceAll("");
        return result.trim();
    }

    /**
     * 判断文本是否处于 thinking 标签内部（用于流式分块过滤）
     *
     * @return true 表示当前文本在 &lt;think&gt; 开启后、&lt;/think&gt; 关闭前
     */
    public static boolean isInsideThinkTag(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        int openCount = countOccurrences(text, "<think>");
        int closeCount = countOccurrences(text, "</think>");
        return openCount > closeCount;
    }

    private static int countOccurrences(String text, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}
