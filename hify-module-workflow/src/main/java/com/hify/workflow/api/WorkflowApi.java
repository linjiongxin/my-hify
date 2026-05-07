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