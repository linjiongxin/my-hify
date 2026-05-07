package com.hify.rag.core;

import com.hify.rag.core.impl.AliEmbeddingService;
import com.hify.rag.core.impl.OllamaEmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Embedding 服务工厂，根据模型名称选择对应的服务
 */
@Slf4j
@Component
public class EmbeddingServiceFactory {

    private final AliEmbeddingService aliEmbeddingService;
    private final OllamaEmbeddingService ollamaEmbeddingService;

    public EmbeddingServiceFactory(AliEmbeddingService aliEmbeddingService,
                                   OllamaEmbeddingService ollamaEmbeddingService) {
        this.aliEmbeddingService = aliEmbeddingService;
        this.ollamaEmbeddingService = ollamaEmbeddingService;
    }

    /**
     * 根据模型名称获取对应的 Embedding 服务
     */
    public EmbeddingService getService(String modelName) {
        if (modelName == null) {
            log.warn("Model name is null, using default Ollama service");
            return ollamaEmbeddingService;
        }

        switch (modelName) {
            case "nomic-embed-text":
                return ollamaEmbeddingService;
            case "text-embedding-v2":
            case "text-embedding-3-large":
            case "text-embedding-3-small":
                return aliEmbeddingService;
            default:
                // 未知模型，尝试使用 Ollama
                log.warn("Unknown embedding model: {}, using Ollama as fallback", modelName);
                return ollamaEmbeddingService;
        }
    }
}
