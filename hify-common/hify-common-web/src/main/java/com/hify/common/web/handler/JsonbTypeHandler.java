package com.hify.common.web.handler;

import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * PostgreSQL JSONB 类型处理器
 * <p>映射 JSONB 列与 Map<String, Object></p>
 *
 * @author hify
 */
public class JsonbTypeHandler extends AbstractJsonTypeHandler<Map<String, Object>> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> TYPE_REF = new TypeReference<>() {
    };

    public JsonbTypeHandler(Class<?> type) {
        super(type);
    }

    @Override
    public Map<String, Object> parse(String json) {
        try {
            if (json == null || json.isBlank()) {
                return Map.of();
            }
            return OBJECT_MAPPER.readValue(json, TYPE_REF);
        } catch (Exception e) {
            throw new RuntimeException("JSONB 反序列化失败: " + json, e);
        }
    }

    @Override
    public String toJson(Map<String, Object> obj) {
        try {
            if (obj == null) {
                return "{}";
            }
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSONB 序列化失败", e);
        }
    }
}
