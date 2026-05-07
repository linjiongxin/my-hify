package com.hify.workflow.engine.context;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 执行上下文
 * <p>存储执行过程中的变量</p>
 */
@Data
@NoArgsConstructor
public class ExecutionContext {

    private Map<String, Object> variables = new HashMap<>();

    /**
     * 存储变量
     */
    public void put(String key, Object value) {
        variables.put(key, value);
    }

    /**
     * 获取变量
     */
    public Object get(String key) {
        return variables.get(key);
    }

    /**
     * 获取字符串类型变量
     */
    public String getString(String key) {
        Object value = variables.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 获取整数类型变量
     */
    public Integer getInt(String key) {
        Object value = variables.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }

    /**
     * 获取长整数类型变量
     */
    public Long getLong(String key) {
        Object value = variables.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    /**
     * 获取布尔类型变量
     */
    public Boolean getBoolean(String key) {
        Object value = variables.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }

    /**
     * 获取双精度类型变量
     */
    public Double getDouble(String key) {
        Object value = variables.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(value.toString());
    }

    /**
     * 判断变量是否存在
     */
    public boolean containsKey(String key) {
        return variables.containsKey(key);
    }

    /**
     * 移除变量
     */
    public void remove(String key) {
        variables.remove(key);
    }

    /**
     * 清空所有变量
     */
    public void clear() {
        variables.clear();
    }

    /**
     * 获取所有变量
     */
    public Map<String, Object> getAll() {
        return new HashMap<>(variables);
    }
}
