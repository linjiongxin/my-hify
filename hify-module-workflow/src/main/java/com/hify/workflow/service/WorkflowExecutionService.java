package com.hify.workflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hify.common.web.entity.PageResult;
import com.hify.workflow.api.WorkflowApi;
import com.hify.workflow.api.dto.*;
import com.hify.workflow.engine.WorkflowEngine;
import com.hify.workflow.entity.Workflow;
import com.hify.workflow.entity.WorkflowApproval;
import com.hify.workflow.entity.WorkflowEdge;
import com.hify.workflow.entity.WorkflowInstance;
import com.hify.workflow.entity.WorkflowNode;
import com.hify.workflow.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工作流执行服务
 * <p>实现 WorkflowApi 接口，提供工作流的 CRUD 和执行能力</p>
 */
@Slf4j
@Service
public class WorkflowExecutionService implements WorkflowApi {

    @Autowired
    private WorkflowMapper workflowMapper;

    @Autowired
    private WorkflowNodeMapper workflowNodeMapper;

    @Autowired
    private WorkflowEdgeMapper workflowEdgeMapper;

    @Autowired
    private WorkflowInstanceMapper workflowInstanceMapper;

    @Autowired
    private WorkflowApprovalMapper workflowApprovalMapper;

    @Autowired
    private WorkflowEngine workflowEngine;

    @Override
    @Transactional
    public Long create(WorkflowCreateRequest dto) {
        Workflow workflow = new Workflow();
        workflow.setName(dto.getName());
        workflow.setDescription(dto.getDescription());
        workflow.setStatus("draft");
        workflow.setVersion(1);
        workflow.setRetryConfig(dto.getRetryConfig());
        workflow.setConfig(dto.getConfig());
        workflowMapper.insert(workflow);
        return workflow.getId();
    }

    @Override
    @Transactional
    public void update(Long id, WorkflowUpdateRequest dto) {
        Workflow workflow = workflowMapper.selectById(id);
        if (workflow == null) {
            throw new IllegalArgumentException("Workflow not found: " + id);
        }

        if (dto.getName() != null) {
            workflow.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            workflow.setDescription(dto.getDescription());
        }
        if (dto.getStatus() != null) {
            workflow.setStatus(dto.getStatus());
        }
        if (dto.getRetryConfig() != null) {
            workflow.setRetryConfig(dto.getRetryConfig());
        }
        if (dto.getConfig() != null) {
            workflow.setConfig(dto.getConfig());
        }

        workflowMapper.updateById(workflow);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        // 删除节点和连线
        workflowNodeMapper.delete(new LambdaQueryWrapper<WorkflowNode>()
                .eq(WorkflowNode::getWorkflowId, id));
        workflowEdgeMapper.delete(new LambdaQueryWrapper<WorkflowEdge>()
                .eq(WorkflowEdge::getWorkflowId, id));
        // 删除实例
        workflowInstanceMapper.delete(new LambdaQueryWrapper<WorkflowInstance>()
                .eq(WorkflowInstance::getWorkflowId, id));
        // 删除工作流
        workflowMapper.deleteById(id);
    }

    @Override
    public WorkflowDTO getById(Long id) {
        Workflow workflow = workflowMapper.selectById(id);
        if (workflow == null) {
            return null;
        }
        return toDTO(workflow);
    }

    @Override
    public PageResult<WorkflowDTO> list(WorkflowQueryDTO query) {
        LambdaQueryWrapper<Workflow> wrapper = new LambdaQueryWrapper<>();
        if (query.getName() != null && !query.getName().isEmpty()) {
            wrapper.like(Workflow::getName, query.getName());
        }
        if (query.getStatus() != null && !query.getStatus().isEmpty()) {
            wrapper.eq(Workflow::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(Workflow::getCreatedAt);

        IPage<Workflow> page = workflowMapper.selectPage(
                new Page<>(query.getPage(), query.getPageSize()),
                wrapper
        );

        List<WorkflowDTO> records = page.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResult.of(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Override
    public String start(WorkflowStartRequest dto) {
        return workflowEngine.start(dto.getWorkflowId(), dto.getInputs());
    }

    @Override
    public WorkflowInstanceDTO getInstanceById(Long instanceId) {
        WorkflowInstance instance = workflowInstanceMapper.selectById(instanceId);
        if (instance == null) {
            return null;
        }
        return toInstanceDTO(instance);
    }

    @Override
    public List<WorkflowNodeDTO> getNodes(Long workflowId) {
        List<WorkflowNode> nodes = workflowNodeMapper.selectList(
                new LambdaQueryWrapper<WorkflowNode>()
                        .eq(WorkflowNode::getWorkflowId, workflowId)
                        .orderByAsc(WorkflowNode::getPositionX)
        );
        return nodes.stream().map(this::toNodeDTO).collect(Collectors.toList());
    }

    @Override
    public List<WorkflowEdgeDTO> getEdges(Long workflowId) {
        List<WorkflowEdge> edges = workflowEdgeMapper.selectList(
                new LambdaQueryWrapper<WorkflowEdge>()
                        .eq(WorkflowEdge::getWorkflowId, workflowId)
                        .orderByAsc(WorkflowEdge::getEdgeIndex)
        );
        return edges.stream().map(this::toEdgeDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void approve(Long approvalId, String remark) {
        WorkflowApproval approval = workflowApprovalMapper.selectById(approvalId);
        if (approval == null) {
            throw new IllegalArgumentException("Approval not found: " + approvalId);
        }
        approval.setStatus("approved");
        approval.setRemark(remark);
        approval.setProcessedAt(LocalDateTime.now());
        workflowApprovalMapper.updateById(approval);

        // 恢复工作流执行（通过分支）
        log.info("Resuming workflow after approval: instanceId={}, action=approved", approval.getInstanceId());
        workflowEngine.resumeAfterApproval(approval.getInstanceId(), "approved");
    }

    @Override
    @Transactional
    public void reject(Long approvalId, String remark) {
        WorkflowApproval approval = workflowApprovalMapper.selectById(approvalId);
        if (approval == null) {
            throw new IllegalArgumentException("Approval not found: " + approvalId);
        }
        approval.setStatus("rejected");
        approval.setRemark(remark);
        approval.setProcessedAt(LocalDateTime.now());
        workflowApprovalMapper.updateById(approval);

        // 恢复工作流执行（拒绝分支）
        workflowEngine.resumeAfterApproval(approval.getInstanceId(), "rejected");
    }

    @Override
    public List<WorkflowApprovalDTO> getPendingApprovals(Long instanceId) {
        List<WorkflowApproval> approvals = workflowApprovalMapper.selectList(
                new LambdaQueryWrapper<WorkflowApproval>()
                        .eq(WorkflowApproval::getInstanceId, instanceId)
                        .eq(WorkflowApproval::getStatus, "pending")
                        .orderByAsc(WorkflowApproval::getCreatedAt)
        );
        return approvals.stream().map(this::toApprovalDTO).collect(Collectors.toList());
    }

    private WorkflowApprovalDTO toApprovalDTO(WorkflowApproval approval) {
        WorkflowApprovalDTO dto = new WorkflowApprovalDTO();
        dto.setId(approval.getId());
        dto.setInstanceId(approval.getInstanceId());
        dto.setNodeId(approval.getNodeId());
        dto.setStatus(approval.getStatus());
        dto.setRemark(approval.getRemark());
        dto.setCreatedAt(approval.getCreatedAt());
        dto.setProcessedAt(approval.getProcessedAt());
        return dto;
    }

    /**
     * 将 Workflow 实体转换为 DTO
     */
    private WorkflowDTO toDTO(Workflow workflow) {
        WorkflowDTO dto = new WorkflowDTO();
        dto.setId(workflow.getId());
        dto.setName(workflow.getName());
        dto.setDescription(workflow.getDescription());
        dto.setStatus(workflow.getStatus());
        dto.setVersion(workflow.getVersion());
        dto.setRetryConfig(workflow.getRetryConfig());
        dto.setConfig(workflow.getConfig());
        dto.setCreatedAt(workflow.getCreatedAt());
        dto.setUpdatedAt(workflow.getUpdatedAt());
        return dto;
    }

    /**
     * 将 WorkflowInstance 实体转换为 DTO
     */
    private WorkflowInstanceDTO toInstanceDTO(WorkflowInstance instance) {
        WorkflowInstanceDTO dto = new WorkflowInstanceDTO();
        dto.setId(instance.getId());
        dto.setWorkflowId(instance.getWorkflowId());
        dto.setStatus(instance.getStatus());
        dto.setCurrentNodeId(instance.getCurrentNodeId());
        dto.setContext(instance.getContext());
        dto.setErrorMsg(instance.getErrorMsg());
        dto.setStartedAt(instance.getStartedAt());
        dto.setFinishedAt(instance.getFinishedAt());
        return dto;
    }

    /**
     * 将 WorkflowNode 实体转换为 DTO
     */
    private WorkflowNodeDTO toNodeDTO(WorkflowNode node) {
        WorkflowNodeDTO dto = new WorkflowNodeDTO();
        dto.setId(node.getId());
        dto.setWorkflowId(node.getWorkflowId());
        dto.setNodeId(node.getNodeId());
        dto.setType(node.getType());
        dto.setName(node.getName());
        dto.setConfig(node.getConfig());
        dto.setPositionX(node.getPositionX());
        dto.setPositionY(node.getPositionY());
        dto.setRetryConfig(node.getRetryConfig());
        return dto;
    }

    /**
     * 将 WorkflowEdge 实体转换为 DTO
     */
    private WorkflowEdgeDTO toEdgeDTO(WorkflowEdge edge) {
        WorkflowEdgeDTO dto = new WorkflowEdgeDTO();
        dto.setId(edge.getId());
        dto.setWorkflowId(edge.getWorkflowId());
        dto.setSourceNode(edge.getSourceNode());
        dto.setTargetNode(edge.getTargetNode());
        dto.setCondition(edge.getCondition());
        dto.setEdgeIndex(edge.getEdgeIndex());
        return dto;
    }
}