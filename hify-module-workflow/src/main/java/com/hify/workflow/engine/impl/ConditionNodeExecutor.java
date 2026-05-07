package com.hify.workflow.engine.impl;

import com.hify.workflow.engine.context.ExecutionContext;
import com.hify.workflow.engine.NodeExecutor;
import com.hify.workflow.engine.NodeResult;
import com.hify.workflow.engine.config.ConditionNodeConfig;
import com.hify.workflow.engine.config.NodeConfigParser;
import com.hify.workflow.engine.util.PlaceholderUtils;
import com.hify.workflow.entity.WorkflowNode;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

/**
 * 条件节点执行器
 * <p>根据条件表达式计算结果，决定下一步走向</p>
 */
@Component
public class ConditionNodeExecutor implements NodeExecutor {

    private final NodeConfigParser nodeConfigParser;
    private final SpelExpressionParser spelParser = new SpelExpressionParser();

    public ConditionNodeExecutor(NodeConfigParser nodeConfigParser) {
        this.nodeConfigParser = nodeConfigParser;
    }

    @Override
    public NodeResult execute(WorkflowNode node, ExecutionContext context) {
        try {
            ConditionNodeConfig config = (ConditionNodeConfig) nodeConfigParser.parse(node);

            if (config.expression() == null) {
                return NodeResult.failure("Condition node config missing expression");
            }

            // 替换表达式中的占位符（字符串值加引号，供 SpEL 使用）
            String resolvedExpression = PlaceholderUtils.replaceForExpression(config.expression(), context);

            // 计算表达式
            boolean result = spelParser.parseExpression(resolvedExpression).getValue(Boolean.class);

            // 根据结果选择分支
            String nextNodeId = result ? config.trueBranch() : config.falseBranch();

            return NodeResult.success(nextNodeId);

        } catch (Exception e) {
            return NodeResult.failure("Condition evaluation failed: " + e.getMessage());
        }
    }
}
