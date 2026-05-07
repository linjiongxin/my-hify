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
 * 阿里 text-embedding-v2 实现
 */
@Slf4j
@Component
public class AliEmbeddingService implements EmbeddingService {

    @Value("${hify.embedding.ali.api-key:}")
    private String apiKey;

    @Value("${hify.embedding.ali.model:text-embedding-v2}")
    private String model;

    @Value("${hify.embedding.ali.api-base:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String apiBase;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int EMBEDDING_DIM = 1024;

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return new float[EMBEDDING_DIM];
        }

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Ali embedding API key not configured, returning zero vector");
            return new float[EMBEDDING_DIM];
        }

        try {
            String url = apiBase + "/embeddings";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("input", text);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
                if (data != null && !data.isEmpty()) {
                    Map<String, Object> embeddingData = data.get(0);
                    List<Number> embedding = (List<Number>) embeddingData.get("embedding");
                    if (embedding != null) {
                        float[] result = new float[embedding.size()];
                        for (int i = 0; i < embedding.size(); i++) {
                            result[i] = embedding.get(i).floatValue();
                        }
                        return result;
                    }
                }
            }

            log.warn("Unexpected embedding response format");
            return new float[EMBEDDING_DIM];

        } catch (Exception e) {
            log.error("Failed to get embedding from Ali API: {}", e.getMessage());
            return new float[EMBEDDING_DIM];
        }
    }

    @Override
    public List<float[]> batchEmbed(List<String> texts) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Ali embedding API key not configured, returning zero vectors");
            return texts.stream().map(t -> new float[EMBEDDING_DIM]).toList();
        }

        try {
            String url = apiBase + "/embeddings";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("input", texts);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");

                List<float[]> results = new ArrayList<>();
                if (data != null) {
                    for (Map<String, Object> item : data) {
                        List<Number> embedding = (List<Number>) item.get("embedding");
                        float[] vec = new float[embedding != null ? embedding.size() : EMBEDDING_DIM];
                        if (embedding != null) {
                            for (int i = 0; i < embedding.size(); i++) {
                                vec[i] = embedding.get(i).floatValue();
                            }
                        }
                        results.add(vec);
                    }
                }
                return results;
            }

            return texts.stream().map(t -> new float[EMBEDDING_DIM]).toList();

        } catch (Exception e) {
            log.error("Failed to batch embed from Ali API: {}", e.getMessage());
            return texts.stream().map(t -> new float[EMBEDDING_DIM]).toList();
        }
    }

    public int getEmbeddingDim() {
        return EMBEDDING_DIM;
    }
}