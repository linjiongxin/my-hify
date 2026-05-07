package com.hify.agent.tool;

import com.hify.workflow.api.WorkflowApi;
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