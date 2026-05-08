package com.hify.workflow.engine.executor;

import com.hify.workflow.engine.NodeResult;
import com.hify.workflow.engine.config.ApprovalNodeConfig;
import com.hify.workflow.engine.config.NodeConfig;
import com.hify.workflow.engine.context.ExecutionContext;
import com.hify.workflow.entity.WorkflowNode;
import org.springframework.stereotype.Component;

/**
 * APPROVAL 节点执行器（v2）
 * <p>暂停流程，等待人工审批</p>
 */
@Component
public class ApprovalNodeExecutor implements NodeExecutor {

    @Override
    public String nodeType() {
        return "APPROVAL";
    }

    @Override
    public NodeResult execute(WorkflowNode node, NodeConfig config, ExecutionContext context) {
        ApprovalNodeConfig approvalConfig = (ApprovalNodeConfig) config;

        // 记录审批信息到上下文
        if (approvalConfig.prompt() != null) {
            context.set(node.getNodeId(), "approvalPrompt", approvalConfig.prompt());
            context.put("approvalPrompt", approvalConfig.prompt());
        }

        return NodeResult.approvalRequired(null);
    }
}
