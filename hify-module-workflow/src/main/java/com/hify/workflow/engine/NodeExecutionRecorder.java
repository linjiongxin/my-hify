package com.hify.workflow.engine;

import com.hify.workflow.entity.WorkflowNode;
import com.hify.workflow.entity.WorkflowNodeExecution;
import com.hify.workflow.mapper.WorkflowNodeExecutionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 节点执行记录器
 * <p>在节点执行前后写入 workflow_node_execution 表</p>
 */
@Slf4j
@Component
public class NodeExecutionRecorder {

    private final WorkflowNodeExecutionMapper mapper;

    public NodeExecutionRecorder(WorkflowNodeExecutionMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 记录节点执行开始
     *
     * @param instanceId 实例 ID
     * @param node       当前节点
     * @param inputJson  输入参数 JSON
     * @return 执行记录 ID
     */
    public Long recordStart(Long instanceId, WorkflowNode node, String inputJson) {
        WorkflowNodeExecution execution = new WorkflowNodeExecution();
        execution.setExecutionId(instanceId);
        execution.setNodeId(node.getNodeId());
        execution.setNodeType(node.getType());
        execution.setStatus("running");
        execution.setInputJson(inputJson);
        execution.setStartedAt(LocalDateTime.now());
        mapper.insert(execution);
        return execution.getId();
    }

    /**
     * 记录节点执行成功
     *
     * @param recordId   执行记录 ID
     * @param outputJson 输出结果 JSON
     */
    public void recordSuccess(Long recordId, String outputJson) {
        WorkflowNodeExecution execution = new WorkflowNodeExecution();
        execution.setId(recordId);
        execution.setStatus("completed");
        execution.setOutputJson(outputJson);
        execution.setEndedAt(LocalDateTime.now());
        mapper.updateById(execution);
    }

    /**
     * 记录节点执行失败
     *
     * @param recordId 执行记录 ID
     * @param errorMsg 错误信息
     */
    public void recordFailure(Long recordId, String errorMsg) {
        WorkflowNodeExecution execution = new WorkflowNodeExecution();
        execution.setId(recordId);
        execution.setStatus("failed");
        execution.setErrorMsg(errorMsg);
        execution.setEndedAt(LocalDateTime.now());
        mapper.updateById(execution);
    }
}
