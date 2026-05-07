package com.hify.workflow.engine.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hify.workflow.engine.ExecutionContext;
import com.hify.workflow.engine.NodeExecutor;
import com.hify.workflow.engine.NodeResult;
import com.hify.workflow.entity.WorkflowEdge;
import com.hify.workflow.entity.WorkflowNode;
import com.hify.workflow.mapper.WorkflowEdgeMapper;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * START 节点执行器
 * <p>找到 START 节点的下一条连线对应的节点</p>
 */
@Component
public class StartNodeExecutor implements NodeExecutor {

    private final WorkflowEdgeMapper workflowEdgeMapper;

    public StartNodeExecutor(WorkflowEdgeMapper workflowEdgeMapper) {
        this.workflowEdgeMapper = workflowEdgeMapper;
    }

    @Override
    public NodeResult execute(WorkflowNode node, ExecutionContext context) {
        // 查询 START 节点的下一条连线
        LambdaQueryWrapper<WorkflowEdge> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WorkflowEdge::getWorkflowId, node.getWorkflowId())
                .eq(WorkflowEdge::getSourceNode, node.getNodeId())
                .orderByAsc(WorkflowEdge::getEdgeIndex);

        List<WorkflowEdge> outgoingEdges = workflowEdgeMapper.selectList(queryWrapper);

        if (outgoingEdges == null || outgoingEdges.isEmpty()) {
            return NodeResult.failure("START node has no outgoing edges");
        }

        // 取第一条连线（按 edgeIndex 排序）
        WorkflowEdge firstEdge = outgoingEdges.get(0);
        String nextNodeId = firstEdge.getTargetNode();

        return NodeResult.success(nextNodeId);
    }
}
