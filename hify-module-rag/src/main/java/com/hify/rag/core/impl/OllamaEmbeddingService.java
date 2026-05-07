package com.hify.rag.core.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.rag.core.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Ollama embedding 实现
 */
@Slf4j
@Component
public class OllamaEmbeddingService implements EmbeddingService {

    @Value("${hify.embedding.ollama.api-base:http://localhost:11434/v1}")
    private String apiBase;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int EMBEDDING_DIM = 768;

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return new float[EMBEDDING_DIM];
        }

        try {
            String url = apiBase + "/embeddings";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("model", "nomic-embed-text");
            body.put("input", text);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                @SuppressWarnings("unchecked")
                List<Number> embedding = (List<Number>) responseBody.get("embedding");
                if (embedding != null) {
                    float[] result = new float[embedding.size()];
                    for (int i = 0; i < embedding.size(); i++) {
                        result[i] = embedding.get(i).floatValue();
                    }
                    return result;
                }
            }

            log.warn("Unexpected Ollama embedding response format");
            return new float[EMBEDDING_DIM];

        } catch (Exception e) {
            log.error("Failed to get embedding from Ollama API: {}", e.getMessage());
            return new float[EMBEDDING_DIM];
        }
    }

    @Override
    public List<float[]> batchEmbed(List<String> texts) {
        List<float[]> results = new ArrayList<>();
        for (String text : texts) {
            results.add(embed(text));
        }
        return results;
    }

    public int getEmbeddingDim() {
        return EMBEDDING_DIM;
    }
}
