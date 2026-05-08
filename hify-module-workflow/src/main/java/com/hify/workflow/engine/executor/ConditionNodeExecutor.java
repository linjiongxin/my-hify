package com.hify.workflow.engine.executor;

import com.hify.workflow.engine.NodeResult;
import com.hify.workflow.engine.config.ConditionNodeConfig;
import com.hify.workflow.engine.config.NodeConfig;
import com.hify.workflow.engine.context.ExecutionContext;
import com.hify.workflow.entity.WorkflowNode;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

/**
 * CONDITION 节点执行器（v2）
 * <p>保留 SpEL 表达式能力</p>
 */
@Component
public class ConditionNodeExecutor implements NodeExecutor {

    private final SpelExpressionParser spelParser = new SpelExpressionParser();

    @Override
    public String nodeType() {
        return "CONDITION";
    }

    @Override
    public NodeResult execute(WorkflowNode node, NodeConfig config, ExecutionContext context) {
        try {
            ConditionNodeConfig conditionConfig = (ConditionNodeConfig) config;

            if (conditionConfig.expression() == null) {
                return NodeResult.failure("Condition node config missing expression");
            }

            String resolvedExpression = resolveExpression(conditionConfig.expression(), context);
            boolean result = spelParser.parseExpression(resolvedExpression).getValue(Boolean.class);

            String nextNodeId = result ? conditionConfig.trueBranch() : conditionConfig.falseBranch();
            return NodeResult.success(nextNodeId);

        } catch (Exception e) {
            return NodeResult.failure("Condition evaluation failed: " + e.getMessage());
        }
    }

    /**
     * 替换表达式中的占位符（字符串值加引号，供 SpEL 使用）
     */
    private String resolveExpression(String expression, ExecutionContext context) {
        if (expression == null) {
            return null;
        }
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\$\\{([^}]+)}");
        java.util.regex.Matcher matcher = pattern.matcher(expression);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = context.get(varName);
            if (value == null && varName.contains(".")) {
                // 尝试解析 nodeKey.varName 格式
                String[] parts = varName.split("\\.", 2);
                value = context.get(parts[0], parts[1]);
            }
            String replacement = formatForSpel(value);
            matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String formatForSpel(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String s) {
            if (isNumeric(s) || isBoolean(s)) {
                return s;
            }
            return "'" + s.replace("'", "\\'") + "'";
        }
        return value.toString();
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
