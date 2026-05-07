package com.hify.workflow.engine.impl;

import com.hify.workflow.engine.context.ExecutionContext;
import com.hify.workflow.engine.NodeExecutor;
import com.hify.workflow.engine.NodeResult;
import com.hify.workflow.engine.config.ApprovalNodeConfig;
import com.hify.workflow.engine.config.NodeConfigParser;
import com.hify.workflow.engine.util.PlaceholderUtils;
import com.hify.workflow.entity.WorkflowApproval;
import com.hify.workflow.entity.WorkflowNode;
import com.hify.workflow.mapper.WorkflowApprovalMapper;
import org.springframework.stereotype.Component;

/**
 * 审批节点执行器
 * <p>创建审批记录并暂停等待人工审批</p>
 */
@Component
public class ApprovalNodeExecutor implements NodeExecutor {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";

    private final NodeConfigParser nodeConfigParser;
    private final WorkflowApprovalMapper workflowApprovalMapper;

    public ApprovalNodeExecutor(NodeConfigParser nodeConfigParser, WorkflowApprovalMapper workflowApprovalMapper) {
        this.nodeConfigParser = nodeConfigParser;
        this.workflowApprovalMapper = workflowApprovalMapper;
    }

    @Override
    public NodeResult execute(WorkflowNode node, ExecutionContext context) {
        try {
            ApprovalNodeConfig config = (ApprovalNodeConfig) nodeConfigParser.parse(node);

            // 替换 prompt 中的占位符
            String resolvedPrompt = PlaceholderUtils.replace(config.prompt(), context);

            // 获取实例 ID（从上下文中获取）
            Long instanceId = context.getLong("instanceId");
            if (instanceId == null) {
                return NodeResult.failure("Approval node requires instanceId in context");
            }

            // 创建审批记录
            WorkflowApproval approval = new WorkflowApproval();
            approval.setInstanceId(instanceId);
            approval.setNodeId(node.getNodeId());
            approval.setStatus(STATUS_PENDING);
            // 审批内容可以存储替换后的 prompt
            approval.setRemark(resolvedPrompt);

            workflowApprovalMapper.insert(approval);

            // 返回需要审批的结果（暂停）
            return NodeResult.approvalRequired("approval_" + approval.getId());

        } catch (Exception e) {
            return NodeResult.failure("Approval creation failed: " + e.getMessage());
        }
    }
}
