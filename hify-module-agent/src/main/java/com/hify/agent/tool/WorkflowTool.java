package com.hify.agent.tool;

import com.hify.workflow.api.WorkflowApi;
import com.hify.workflow.api.dto.WorkflowInstanceDTO;
import com.hify.workflow.api.dto.WorkflowStartRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Workflow Tool - Allows Agent to invoke workflows as tools
 *
 * <p>This tool enables AI agents to execute workflows by calling the WorkflowApi.
 * It takes workflow ID and input parameters, starts the workflow execution,
 * and returns the instance ID for tracking.</p>
 *
 * <p>Usage in Agent system prompt:</p>
 * <pre>
 * When you need to execute a workflow, use the workflow tool with:
 * - workflowId: The ID of the workflow to execute
 * - inputs: A map of input parameters for the workflow
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowTool {

    private final WorkflowApi workflowApi;

    /**
     * Tool name constant for agent tool binding
     */
    public static final String TOOL_NAME = "workflow";

    /**
     * Tool type constant
     */
    public static final String TOOL_TYPE = "workflow";

    /**
     * Invoke a workflow by ID with the given inputs
     *
     * @param workflowId the workflow ID to invoke
     * @param inputs     input parameters for the workflow
     * @return the workflow instance ID
     */
    public String invoke(Long workflowId, Map<String, Object> inputs) {
        log.info("Agent invoking workflow: workflowId={}, inputs={}", workflowId, inputs);

        WorkflowStartRequest request = new WorkflowStartRequest();
        request.setWorkflowId(workflowId);
        request.setInputs(inputs);

        String instanceId = workflowApi.start(request);
        log.info("Workflow started successfully: instanceId={}", instanceId);

        return instanceId;
    }

    /**
     * 同步调用工作流并等待结果
     *
     * @param workflowId 工作流 ID
     * @param inputs     输入参数
     * @return 工作流执行结果（上下文 JSON）
     */
    public String invokeSync(Long workflowId, Map<String, Object> inputs) {
        log.info("Agent invoking workflow (sync): workflowId={}, inputs={}", workflowId, inputs);

        String instanceId = invoke(workflowId, inputs);
        if (instanceId == null || instanceId.isEmpty()) {
            return "工作流启动失败";
        }

        long pollIntervalMs = 500;
        int maxPolls = 60;
        String status = null;

        for (int i = 0; i < maxPolls; i++) {
            try {
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "工作流执行被中断";
            }

            WorkflowInstanceDTO instance = workflowApi.getInstanceById(Long.valueOf(instanceId));
            if (instance == null) {
                return "工作流实例不存在";
            }

            status = instance.getStatus();
            if ("completed".equals(status)) {
                String context = instance.getContext();
                log.info("Workflow completed: instanceId={}, context={}", instanceId, context);
                return context != null ? context : "工作流执行完成，无输出";
            }
            if ("failed".equals(status)) {
                log.warn("Workflow failed: instanceId={}, error={}", instanceId, instance.getErrorMsg());
                return "工作流执行失败: " + (instance.getErrorMsg() != null ? instance.getErrorMsg() : "未知错误");
            }
        }

        log.warn("Workflow poll timeout: instanceId={}, lastStatus={}", instanceId, status);
        return "工作流执行超时，请稍后重试";
    }

    /**
     * Tool result DTO returned to the Agent
     */
    @Data
    public static class ToolResult {
        private String instanceId;
        private String status;
        private String message;

        public static ToolResult success(String instanceId) {
            ToolResult result = new ToolResult();
            result.setInstanceId(instanceId);
            result.setStatus("RUNNING");
            result.setMessage("Workflow started successfully");
            return result;
        }

        public static ToolResult error(String message) {
            ToolResult result = new ToolResult();
            result.setStatus("ERROR");
            result.setMessage(message);
            return result;
        }
    }
}