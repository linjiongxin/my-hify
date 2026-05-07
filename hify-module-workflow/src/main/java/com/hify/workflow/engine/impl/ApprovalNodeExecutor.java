package com.hify.workflow.engine.impl;

import com.hify.workflow.engine.ExecutionContext;
import com.hify.workflow.engine.NodeExecutor;
import com.hify.workflow.engine.NodeResult;
import com.hify.workflow.entity.WorkflowApproval;
import com.hify.workflow.entity.WorkflowNode;
import com.hify.workflow.mapper.WorkflowApprovalMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 审批节点执行器
 * <p>创建审批记录并暂停等待人工审批</p>
 */
@Component
public class ApprovalNodeExecutor implements NodeExecutor {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";

    private final WorkflowApprovalMapper workflowApprovalMapper;

    public ApprovalNodeExecutor(WorkflowApprovalMapper workflowApprovalMapper) {
        this.workflowApprovalMapper = workflowApprovalMapper;
    }

    @Override
    public NodeResult execute(WorkflowNode node, ExecutionContext context) {
        try {
            // 解析配置
            JSONObject config = JSONObject.parseObject(node.getConfig());
            String prompt = config.getString("prompt");
            String variables = config.getString("variables");

            // 替换 prompt 中的占位符
            String resolvedPrompt = replacePlaceholders(prompt, context);

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
            matcher.appendReplacement(result, Matcher.quoteReplacement(value != null ? value.toString() : ""));
        }

        matcher.appendTail(result);
        return result.toString();
    }
}
