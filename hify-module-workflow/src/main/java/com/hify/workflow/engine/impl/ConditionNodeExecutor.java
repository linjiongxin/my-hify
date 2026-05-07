package com.hify.workflow.engine.impl;

import com.hify.workflow.engine.ExecutionContext;
import com.hify.workflow.engine.NodeExecutor;
import com.hify.workflow.engine.NodeResult;
import com.hify.workflow.entity.WorkflowNode;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 条件节点执行器
 * <p>根据条件表达式计算结果，决定下一步走向</p>
 */
@Component
public class ConditionNodeExecutor implements NodeExecutor {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    private final ScriptEngineManager scriptEngineManager;

    public ConditionNodeExecutor() {
        this.scriptEngineManager = new ScriptEngineManager();
    }

    @Override
    public NodeResult execute(WorkflowNode node, ExecutionContext context) {
        try {
            // 解析配置
            JSONObject config = JSONObject.parseObject(node.getConfig());
            String expression = config.getString("expression");
            String trueBranch = config.getString("trueBranch");
            String falseBranch = config.getString("falseBranch");

            if (expression == null) {
                return NodeResult.failure("Condition node config missing expression");
            }

            // 替换表达式中的占位符
            String resolvedExpression = replacePlaceholders(expression, context);

            // 计算表达式
            boolean result = evaluateExpression(resolvedExpression);

            // 根据结果选择分支
            String nextNodeId = result ? trueBranch : falseBranch;

            return NodeResult.success(nextNodeId);

        } catch (Exception e) {
            return NodeResult.failure("Condition evaluation failed: " + e.getMessage());
        }
    }

    /**
     * 替换字符串中的 ${variable} 占位符
     */
    private String replacePlaceholders(String template, ExecutionContext context) {
        if (template == null) {
            return null;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = context.get(varName);
            String replacement = value != null ? value.toString() : "null";
            // 对于字符串值，需要加引号
            if (value instanceof String && !isNumeric(replacement) && !isBoolean(replacement)) {
                replacement = "'" + replacement.replace("'", "\\'") + "'";
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 使用 JavaScript 引擎计算表达式
     */
    private boolean evaluateExpression(String expression) throws ScriptException {
        ScriptEngine engine = scriptEngineManager.getEngineByName("JavaScript");
        Object result = engine.eval(expression);
        if (result instanceof Boolean) {
            return (Boolean) result;
        }
        return Boolean.parseBoolean(result.toString());
    }

    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isBoolean(String str) {
        return "true".equalsIgnoreCase(str) || "false".equalsIgnoreCase(str);
    }
}
