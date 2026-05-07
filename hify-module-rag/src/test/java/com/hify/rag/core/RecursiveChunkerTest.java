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

        assertEquals(2, chunks.size());
        assertEquals("第一段内容。", chunks.get(0));
        assertEquals("第二段内容。", chunks.get(1));
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
        // 英文单词估算
        assertEquals(3, chunker.estimateTokens("hello world test")); // 3 words * 1.5

        // 中文字符估算
        assertEquals(5, chunker.estimateTokens("你好世界")); // 4 chars

        // 混合估算
        int mixed = chunker.estimateTokens("hello你好world世界test");
        assertTrue(mixed > 0);
    }

    @Test
    void shouldHandleMultipleBlankLines() {
        String text = "段落1\n\n\n\n段落2";
        List<String> chunks = chunker.chunk(text);

        assertEquals(2, chunks.size());
    }
}