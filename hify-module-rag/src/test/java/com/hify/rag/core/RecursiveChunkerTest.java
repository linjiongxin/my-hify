package com.hify.rag.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RecursiveChunker 单元测试
 */
class RecursiveChunkerTest {

    private final RecursiveChunker chunker = new RecursiveChunker(512, 50);

    @Test
    void shouldReturnEmpty_whenTextIsNull() {
        List<String> chunks = chunker.chunk(null);
        assertTrue(chunks.isEmpty());
    }

    @Test
    void shouldReturnEmpty_whenTextIsBlank() {
        List<String> chunks = chunker.chunk("   \n\n   ");
        assertTrue(chunks.isEmpty());
    }

    @Test
    void shouldChunkByParagraph_whenShortParagraphs() {
        String text = "第一段内容。\n\n第二段内容。";
        List<String> chunks = chunker.chunk(text);

        // 短段落（<=30 字符）会合并到同一块，避免标题单独成块
        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).contains("第一段内容"));
        assertTrue(chunks.get(0).contains("第二段内容"));
    }

    @Test
    void shouldPreserveParagraphContent() {
        String text = "这是第一段，有多个句子。这是第二句。\n\n这是第二段。";
        List<String> chunks = chunker.chunk(text);

        assertFalse(chunks.isEmpty());
        // 每个 chunk 应该包含原始内容
        String result = String.join(" ", chunks);
        assertTrue(result.contains("第一段"));
        assertTrue(result.contains("第二段"));
    }

    @Test
    void shouldEstimateTokensCorrectly() {
        // 英文单词估算: 3 words * 1.5 = 4.5 -> (int)4
        assertEquals(4, chunker.estimateTokens("hello world test"));

        // 中文字符估算: 4 chars + 1 word (整串被 split 为 1 段) * 1.5 = 5.5 -> (int)5
        assertEquals(5, chunker.estimateTokens("你好世界"));

        // 混合估算
        int mixed = chunker.estimateTokens("hello你好world世界test");
        assertTrue(mixed > 0);
    }

    @Test
    void shouldHandleMultipleBlankLines() {
        String text = "段落1\n\n\n\n段落2";
        List<String> chunks = chunker.chunk(text);

        // 多个空行会被正则合并为段落分隔，短段落继续合并
        assertEquals(1, chunks.size());
    }
}