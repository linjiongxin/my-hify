package com.hify.workflow.api;

import com.hify.common.web.entity.PageResult;
import com.hify.workflow.api.dto.*;
import lombok.Data;

import java.util.List;

/**
 * 工作流 API 接口
 */
public interface WorkflowApi {

    /**
     * 创建工作流
     */
    Long create(WorkflowCreateRequest dto);

    /**
     * 更新工作流
     */
    void update(Long id, WorkflowUpdateRequest dto);

    /**
     * 删除工作流
     */
    void delete(Long id);

    /**
     * 获取工作流详情
     */
    WorkflowDTO getById(Long id);

    /**
     * 分页查询工作流
     */
    PageResult<WorkflowDTO> list(WorkflowQueryDTO query);

    /**
     * 触发执行
     *
     * @return instanceId
     */
    String start(WorkflowStartRequest dto);

    /**
     * 查询实例状态
     */
    WorkflowInstanceDTO getInstanceById(Long instanceId);

    /**
     * 获取工作流节点
     */
    List<WorkflowNodeDTO> getNodes(Long workflowId);

    /**
     * 获取工作流连线
     */
    List<WorkflowEdgeDTO> getEdges(Long workflowId);

    /**
     * 审批通过
     *
     * @param approvalId 审批记录 ID
     * @param remark     备注
     */
    void approve(Long approvalId, String remark);

    /**
     * 审批拒绝
     *
     * @param approvalId 审批记录 ID
     * @param remark     备注
     */
    void reject(Long approvalId, String remark);

    /**
     * 保存工作流节点（全量替换）
     *
     * @param workflowId 工作流 ID
     * @param nodes      节点列表
     * @return 保存后的节点列表
     */
    List<WorkflowNodeDTO> saveNodes(Long workflowId, List<WorkflowNodeDTO> nodes);

    /**
     * 保存工作流连线（全量替换）
     *
     * @param workflowId 工作流 ID
     * @param edges      连线列表
     * @return 保存后的连线列表
     */
    List<WorkflowEdgeDTO> saveEdges(Long workflowId, List<WorkflowEdgeDTO> edges);

    /**
     * 查询待审批列表
     *
     * @param instanceId 实例 ID
     * @return 待审批记录列表
     */
    List<WorkflowApprovalDTO> getPendingApprovals(Long instanceId);

    /**
     * 查询条件 DTO
     */
    @Data
    class WorkflowQueryDTO {
        private String name;
        private String status;
        private Integer page = 1;
        private Integer pageSize = 20;
    }
}