package com.hify.workflow.engine.impl;

import com.hify.workflow.engine.NodeResult;
import com.hify.workflow.engine.context.ExecutionContext;
import com.hify.workflow.entity.WorkflowNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StartNodeExecutorTest {

    private final StartNodeExecutor executor = new StartNodeExecutor();

    @Test
    void shouldReturnSuccess_withNullNextNode() {
        // Given
        WorkflowNode node = new WorkflowNode();
        node.setWorkflowId(1L);
        node.setNodeId("node_start");
        node.setType("START");

        ExecutionContext context = new ExecutionContext();

        // When
        NodeResult result = executor.execute(node, context);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getNextNodeId()).isNull();
    }
}
