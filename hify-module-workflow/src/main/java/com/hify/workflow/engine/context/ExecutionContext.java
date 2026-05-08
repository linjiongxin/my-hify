package com.hify.workflow.engine.context;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 执行上下文
 * <p>存储执行过程中的变量，支持节点级命名空间隔离</p>
 */
public class ExecutionContext {

    private static final Pattern RESOLVE_PATTERN = Pattern.compile("\\{\\{([^}]+)}}");

    private final Long workflowInstanceId;
    private final Map<String, Object> variables = new LinkedHashMap<>();

    /**
     * 无参构造（向后兼容）
     */
    public ExecutionContext() {
        this.workflowInstanceId = null;
    }

    /**
     * 主构造器
     *
     * @param workflowInstanceId 工作流实例 ID
     * @param inputs             初始输入变量，会按 "start.varName" 格式预写入
     */
    public ExecutionContext(Long workflowInstanceId, Map<String, Object> inputs) {
        this.workflowInstanceId = workflowInstanceId;
        if (inputs != null) {
            for (Map.Entry<String, Object> entry : inputs.entrySet()) {
                // 按命名空间写入，同时保留扁平 key 向后兼容
                this.variables.put("start." + entry.getKey(), entry.getValue());
                this.variables.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public Long getWorkflowInstanceId() {
        return workflowInstanceId;
    }

    /**
     * 按节点命名空间写入变量
     *
     * @param nodeKey 节点标识（如 nodeId）
     * @param varName 变量名
     * @param value   值
     */
    public void set(String nodeKey, String varName, Object value) {
        variables.put(nodeKey + "." + varName, value);
    }

    /**
     * 按节点命名空间读取变量
     *
     * @param nodeKey 节点标识
     * @param varName 变量名
     * @return 值，不存在时返回 null
     */
    public Object get(String nodeKey, String varName) {
        return variables.get(nodeKey + "." + varName);
    }

    /**
     * 扁平写入（向后兼容，用于内部状态如 _visitedNodes）
     */
    public void put(String key, Object value) {
        variables.put(key, value);
    }

    /**
     * 扁平读取（向后兼容）
     */
    public Object get(String key) {
        return variables.get(key);
    }

    public String getString(String key) {
        Object value = variables.get(key);
        return value != null ? value.toString() : null;
    }

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

    public boolean containsKey(String key) {
        return variables.containsKey(key);
    }

    public void remove(String key) {
        variables.remove(key);
    }

    public void clear() {
        variables.clear();
    }

    /**
     * 解析 {{nodeKey.varName}} 模板，变量不存在时保留原始占位符
     */
    public String resolve(String template) {
        if (template == null) {
            return null;
        }
        Matcher matcher = RESOLVE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String fullKey = matcher.group(1);
            Object value = variables.get(fullKey);
            if (value != null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(value.toString()));
            }
            // value == null 时不替换，保留原始占位符
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 返回所有变量的只读视图，用于执行记录落库
     */
    public Map<String, Object> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(variables));
    }

    /**
     * 获取所有变量（向后兼容，返回可修改副本）
     */
    public Map<String, Object> getAll() {
        return new LinkedHashMap<>(variables);
    }
}
