package com.hify.workflow.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hify.workflow.engine.config.NodeConfigParser;
import com.hify.workflow.engine.context.ExecutionContext;
import com.hify.workflow.engine.executor.NodeExecutor;
import com.hify.workflow.engine.executor.NodeExecutorRegistry;
import com.hify.workflow.entity.WorkflowEdge;
import com.hify.workflow.entity.WorkflowInstance;
import com.hify.workflow.entity.WorkflowNode;
import com.hify.workflow.mapper.WorkflowEdgeMapper;
import com.hify.workflow.engine.config.ApprovalNodeConfig;
import com.hify.workflow.mapper.WorkflowInstanceMapper;
import com.hify.workflow.mapper.WorkflowMapper;
import com.hify.workflow.mapper.WorkflowNodeMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Map;

@ExtendWith(MockitoExtension.class)
class WorkflowEngineTest {

    @Mock
    private WorkflowMapper workflowMapper;

    @Mock
    private WorkflowNodeMapper workflowNodeMapper;

    @Mock
    private WorkflowEdgeMapper workflowEdgeMapper;

    @Mock
    private WorkflowInstanceMapper workflowInstanceMapper;

    @Mock
    private NodeExecutorRegistry nodeExecutorRegistry;

    @Mock
    private NodeConfigParser nodeConfigParser;

    @Mock
    private NodeExecutionRecorder nodeExecutionRecorder;

    private WorkflowEngine workflowEngine;

    @BeforeEach
    void setUp() {
        workflowEngine = spy(new WorkflowEngine());
        ReflectionTestUtils.setField(workflowEngine, "workflowMapper", workflowMapper);
        ReflectionTestUtils.setField(workflowEngine, "workflowNodeMapper", workflowNodeMapper);
        ReflectionTestUtils.setField(workflowEngine, "workflowEdgeMapper", workflowEdgeMapper);
        ReflectionTestUtils.setField(workflowEngine, "workflowInstanceMapper", workflowInstanceMapper);
        ReflectionTestUtils.setField(workflowEngine, "nodeExecutorRegistry", nodeExecutorRegistry);
        ReflectionTestUtils.setField(workflowEngine, "nodeConfigParser", nodeConfigParser);
        ReflectionTestUtils.setField(workflowEngine, "nodeExecutionRecorder", nodeExecutionRecorder);
        ReflectionTestUtils.setField(workflowEngine, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(workflowEngine, "maxExecutionSteps", 50);
        ReflectionTestUtils.setField(workflowEngine, "syncTimeoutMs", 30000);
    }

    @Test
    void shouldThrowException_whenResumeWithNonExistingInstance() {
        when(workflowInstanceMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> workflowEngine.resumeAfterApproval(999L, "approved"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Instance not found");
    }

    @Test
    void shouldCompleteInstance_whenNoNextNodeAfterApproval() {
        // Given: 一个审批中的实例，当前节点是 APPROVAL
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(100L);
        instance.setWorkflowId(1L);
        instance.setCurrentNodeId("node_approval");
        instance.setStatus("RUNNING");

        WorkflowNode approvalNode = new WorkflowNode();
        approvalNode.setWorkflowId(1L);
        approvalNode.setNodeId("node_approval");
        approvalNode.setType("APPROVAL");

        when(workflowInstanceMapper.selectById(100L)).thenReturn(instance);
        when(workflowNodeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(approvalNode);
        when(workflowEdgeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(workflowInstanceMapper.updateById(any(WorkflowInstance.class))).thenReturn(1);

        // When
        workflowEngine.resumeAfterApproval(100L, "approved");

        // Then: 流程应该标记为完成
        verify(workflowInstanceMapper).updateById(argThat((WorkflowInstance i) -> "COMPLETED".equals(i.getStatus())));
    }

    @Test
    void shouldContinueToNextNode_whenNextNodeExistsAfterApproval() {
        // Given: 一个审批中的实例，APPROVAL 后面有 LLM 节点
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(100L);
        instance.setWorkflowId(1L);
        instance.setCurrentNodeId("node_approval");
        instance.setStatus("RUNNING");
        instance.setContext("{}");

        WorkflowNode approvalNode = new WorkflowNode();
        approvalNode.setWorkflowId(1L);
        approvalNode.setNodeId("node_approval");
        approvalNode.setType("APPROVAL");
        approvalNode.setConfig("{}");

        WorkflowNode llmNode = new WorkflowNode();
        llmNode.setWorkflowId(1L);
        llmNode.setNodeId("node_llm");
        llmNode.setType("LLM");
        llmNode.setConfig("{}");

        WorkflowEdge edge = new WorkflowEdge();
        edge.setSourceNode("node_approval");
        edge.setTargetNode("node_llm");
        edge.setEdgeIndex(0);

        when(workflowInstanceMapper.selectById(100L)).thenReturn(instance);
        // 第一次查 approvalNode（resumeAfterApproval），第二次查 llmNode（executeAsync）
        when(workflowNodeMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(approvalNode)
                .thenReturn(llmNode);
        when(workflowEdgeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(edge));
        when(workflowInstanceMapper.updateById(any(WorkflowInstance.class))).thenReturn(1);
        doNothing().when(workflowEngine).executeAsync(any(), any());

        // When
        workflowEngine.resumeAfterApproval(100L, "approved");

        // Then: instance 的 currentNodeId 应该更新为下一节点
        verify(workflowInstanceMapper).updateById(argThat((WorkflowInstance i) -> "node_llm".equals(i.getCurrentNodeId())));
    }

    @Test
    void shouldRouteToApproveBranch_whenApprovalIsApproved() {
        // Given: APPROVAL 节点配置了 approveBranch
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(100L);
        instance.setWorkflowId(1L);
        instance.setCurrentNodeId("node_approval");
        instance.setStatus("RUNNING");
        instance.setContext("{}");

        WorkflowNode approvalNode = new WorkflowNode();
        approvalNode.setWorkflowId(1L);
        approvalNode.setNodeId("node_approval");
        approvalNode.setType("APPROVAL");
        approvalNode.setConfig("{}");

        ApprovalNodeConfig config = new ApprovalNodeConfig(
                "Please approve", "node_approved", "node_rejected", null);

        when(workflowInstanceMapper.selectById(100L)).thenReturn(instance);
        when(workflowNodeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(approvalNode);
        when(nodeConfigParser.parse(approvalNode)).thenReturn(config);
        when(workflowInstanceMapper.updateById(any(WorkflowInstance.class))).thenReturn(1);
        doNothing().when(workflowEngine).executeAsync(any(), any());

        // When
        workflowEngine.resumeAfterApproval(100L, "approved");

        // Then: 应该走到 approveBranch
        verify(workflowInstanceMapper).updateById(argThat((WorkflowInstance i) -> "node_approved".equals(i.getCurrentNodeId())));
    }

    @Test
    void shouldRouteToRejectBranch_whenApprovalIsRejected() {
        // Given: APPROVAL 节点配置了 rejectBranch
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(100L);
        instance.setWorkflowId(1L);
        instance.setCurrentNodeId("node_approval");
        instance.setStatus("RUNNING");
        instance.setContext("{}");

        WorkflowNode approvalNode = new WorkflowNode();
        approvalNode.setWorkflowId(1L);
        approvalNode.setNodeId("node_approval");
        approvalNode.setType("APPROVAL");
        approvalNode.setConfig("{}");

        ApprovalNodeConfig config = new ApprovalNodeConfig(
                "Please approve", "node_approved", "node_rejected", null);

        when(workflowInstanceMapper.selectById(100L)).thenReturn(instance);
        when(workflowNodeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(approvalNode);
        when(nodeConfigParser.parse(approvalNode)).thenReturn(config);
        when(workflowInstanceMapper.updateById(any(WorkflowInstance.class))).thenReturn(1);
        doNothing().when(workflowEngine).executeAsync(any(), any());

        // When
        workflowEngine.resumeAfterApproval(100L, "rejected");

        // Then: 应该走到 rejectBranch
        verify(workflowInstanceMapper).updateById(argThat((WorkflowInstance i) -> "node_rejected".equals(i.getCurrentNodeId())));
    }

    @Test
    void shouldRecordNodeExecution_whenNodeExecutesSuccessfully() {
        // Given: LLM 节点执行成功
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(100L);
        instance.setWorkflowId(1L);
        instance.setCurrentNodeId("node_llm");
        instance.setStatus("RUNNING");
        instance.setContext("{}");

        WorkflowNode llmNode = new WorkflowNode();
        llmNode.setWorkflowId(1L);
        llmNode.setNodeId("node_llm");
        llmNode.setType("LLM");
        llmNode.setConfig("{\"model\":\"gpt-4\"}");

        WorkflowEdge edge = new WorkflowEdge();
        edge.setSourceNode("node_llm");
        edge.setTargetNode("node_end");
        edge.setEdgeIndex(0);

        when(workflowInstanceMapper.selectById(100L)).thenReturn(instance);
        when(workflowNodeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(llmNode);
        when(workflowEdgeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(workflowInstanceMapper.updateById(any(WorkflowInstance.class))).thenReturn(1);
        when(nodeExecutionRecorder.recordStart(any(), any(), any())).thenReturn(1L);

        NodeExecutor mockExecutor = mock(NodeExecutor.class);
        when(mockExecutor.execute(any(), any(), any())).thenReturn(NodeResult.success(null));
        when(nodeExecutorRegistry.get("LLM")).thenReturn(mockExecutor);

        // When: 直接执行节点（不 mock executeAsync）
        workflowEngine.executeAsync(100L, "node_llm");

        // Then: 记录器被调用
        verify(nodeExecutionRecorder).recordStart(eq(100L), eq(llmNode), any());
        verify(nodeExecutionRecorder).recordSuccess(eq(1L), any());
    }

    @Test
    void shouldRecordNodeExecutionFailure_whenNodeExecutesFailed() {
        // Given: LLM 节点执行失败
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(100L);
        instance.setWorkflowId(1L);
        instance.setCurrentNodeId("node_llm");
        instance.setStatus("RUNNING");
        instance.setContext("{}");

        WorkflowNode llmNode = new WorkflowNode();
        llmNode.setWorkflowId(1L);
        llmNode.setNodeId("node_llm");
        llmNode.setType("LLM");
        llmNode.setConfig("{\"model\":\"gpt-4\"}");

        WorkflowEdge edge = new WorkflowEdge();
        edge.setSourceNode("node_llm");
        edge.setTargetNode("node_end");
        edge.setEdgeIndex(0);

        when(workflowInstanceMapper.selectById(100L)).thenReturn(instance);
        when(workflowNodeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(llmNode);
        when(workflowInstanceMapper.updateById(any(WorkflowInstance.class))).thenReturn(1);
        when(nodeExecutionRecorder.recordStart(any(), any(), any())).thenReturn(1L);

        NodeExecutor mockExecutor = mock(NodeExecutor.class);
        when(mockExecutor.execute(any(), any(), any())).thenReturn(NodeResult.failure("LLM timeout"));
        when(nodeExecutorRegistry.get("LLM")).thenReturn(mockExecutor);

        // When
        workflowEngine.executeAsync(100L, "node_llm");

        // Then: 记录失败
        verify(nodeExecutionRecorder).recordStart(eq(100L), eq(llmNode), any());
        verify(nodeExecutionRecorder).recordFailure(eq(1L), eq("LLM timeout"));
    }

    @Test
    void shouldReturnFirstMatchingEdge_whenConditionIsTrue() {
        // Given: LLM 节点有两条条件连线，score=90 匹配第一条
        WorkflowNode node = new WorkflowNode();
        node.setWorkflowId(1L);
        node.setNodeId("node_llm");

        WorkflowEdge edge1 = new WorkflowEdge();
        edge1.setSourceNode("node_llm");
        edge1.setTargetNode("node_a");
        edge1.setCondition("${score} > 80");
        edge1.setEdgeIndex(0);

        WorkflowEdge edge2 = new WorkflowEdge();
        edge2.setSourceNode("node_llm");
        edge2.setTargetNode("node_b");
        edge2.setCondition("${score} <= 80");
        edge2.setEdgeIndex(1);

        when(workflowEdgeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(edge1, edge2));

        ExecutionContext context = new ExecutionContext();
        context.put("score", 90);

        // When
        String nextNodeId = workflowEngine.findNextNode(node, context, NodeResult.success(null));

        // Then
        assertThat(nextNodeId).isEqualTo("node_a");
    }

    @Test
    void shouldReturnSecondMatchingEdge_whenFirstConditionIsFalse() {
        // Given: 第一条不匹配，第二条匹配
        WorkflowNode node = new WorkflowNode();
        node.setWorkflowId(1L);
        node.setNodeId("node_llm");

        WorkflowEdge edge1 = new WorkflowEdge();
        edge1.setSourceNode("node_llm");
        edge1.setTargetNode("node_a");
        edge1.setCondition("${score} > 80");
        edge1.setEdgeIndex(0);

        WorkflowEdge edge2 = new WorkflowEdge();
        edge2.setSourceNode("node_llm");
        edge2.setTargetNode("node_b");
        edge2.setCondition("${score} <= 80");
        edge2.setEdgeIndex(1);

        when(workflowEdgeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(edge1, edge2));

        ExecutionContext context = new ExecutionContext();
        context.put("score", 60);

        String nextNodeId = workflowEngine.findNextNode(node, context, NodeResult.success(null));

        assertThat(nextNodeId).isEqualTo("node_b");
    }

    @Test
    void shouldReturnDefaultEdge_whenNoConditionMatches() {
        // Given: 条件都不匹配，有一条默认连线（condition 为空）
        WorkflowNode node = new WorkflowNode();
        node.setWorkflowId(1L);
        node.setNodeId("node_llm");

        WorkflowEdge edge1 = new WorkflowEdge();
        edge1.setSourceNode("node_llm");
        edge1.setTargetNode("node_a");
        edge1.setCondition("${score} > 80");
        edge1.setEdgeIndex(0);

        WorkflowEdge defaultEdge = new WorkflowEdge();
        defaultEdge.setSourceNode("node_llm");
        defaultEdge.setTargetNode("node_default");
        defaultEdge.setCondition(null);
        defaultEdge.setEdgeIndex(1);

        when(workflowEdgeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(edge1, defaultEdge));

        ExecutionContext context = new ExecutionContext();
        context.put("score", 60);

        String nextNodeId = workflowEngine.findNextNode(node, context, NodeResult.success(null));

        assertThat(nextNodeId).isEqualTo("node_default");
    }

    @Test
    void shouldReturnNull_whenNoConditionMatchesAndNoDefaultEdge() {
        // Given: 条件都不匹配，也没有默认连线
        WorkflowNode node = new WorkflowNode();
        node.setWorkflowId(1L);
        node.setNodeId("node_llm");

        WorkflowEdge edge1 = new WorkflowEdge();
        edge1.setSourceNode("node_llm");
        edge1.setTargetNode("node_a");
        edge1.setCondition("${score} > 80");
        edge1.setEdgeIndex(0);

        when(workflowEdgeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(edge1));

        ExecutionContext context = new ExecutionContext();
        context.put("score", 60);

        String nextNodeId = workflowEngine.findNextNode(node, context, NodeResult.success(null));

        assertThat(nextNodeId).isNull();
    }

    @Test
    void shouldFailInstance_whenCycleDetected() {
        // Given: context 中已记录访问过 node_a，再次执行 node_a 应触发循环检测
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(100L);
        instance.setWorkflowId(1L);
        instance.setCurrentNodeId("node_a");
        instance.setStatus("RUNNING");
        instance.setContext("{\"_visitedNodes\":[\"node_a\",\"node_b\"]}");

        WorkflowNode nodeA = new WorkflowNode();
        nodeA.setWorkflowId(1L);
        nodeA.setNodeId("node_a");
        nodeA.setType("LLM");

        when(workflowInstanceMapper.selectById(100L)).thenReturn(instance);
        when(workflowNodeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(nodeA);
        when(workflowInstanceMapper.updateById(any(WorkflowInstance.class))).thenReturn(1);

        // When
        workflowEngine.executeAsync(100L, "node_a");

        // Then: 标记为 FAILED，且错误信息包含 Cycle detected
        verify(workflowInstanceMapper).updateById(argThat((WorkflowInstance i) ->
                "FAILED".equals(i.getStatus()) && i.getErrorMsg().contains("Cycle detected")));
    }

    @Test
    void shouldAddVisitedNode_whenExecutingNewNode() {
        // Given: 正常流程，context 中没有 _visitedNodes
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(100L);
        instance.setWorkflowId(1L);
        instance.setCurrentNodeId("node_a");
        instance.setStatus("RUNNING");
        instance.setContext("{}");

        WorkflowNode nodeA = new WorkflowNode();
        nodeA.setWorkflowId(1L);
        nodeA.setNodeId("node_a");
        nodeA.setType("LLM");
        nodeA.setConfig("{\"model\":\"gpt-4\"}");

        WorkflowEdge edge = new WorkflowEdge();
        edge.setSourceNode("node_a");
        edge.setTargetNode("node_end");
        edge.setEdgeIndex(0);

        when(workflowInstanceMapper.selectById(100L)).thenReturn(instance);
        when(workflowNodeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(nodeA);
        when(workflowEdgeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(workflowInstanceMapper.updateById(any(WorkflowInstance.class))).thenReturn(1);
        when(nodeExecutionRecorder.recordStart(any(), any(), any())).thenReturn(1L);

        NodeExecutor mockExecutor = mock(NodeExecutor.class);
        when(mockExecutor.execute(any(), any(), any())).thenReturn(NodeResult.success(null));
        when(nodeExecutorRegistry.get("LLM")).thenReturn(mockExecutor);

        // When
        workflowEngine.executeAsync(100L, "node_a");

        // Then: 最终 context 中应包含 _visitedNodes
        verify(workflowInstanceMapper, atLeastOnce()).updateById(argThat((WorkflowInstance i) -> {
            if (i.getContext() == null) return false;
            return i.getContext().contains("_visitedNodes") && i.getContext().contains("node_a");
        }));
    }

    @Test
    void shouldFailInstance_whenExceedsMaxExecutionSteps() {
        // Given: 3 节点链 start->a->b->c(end)，MAX_STEPS 设为 2
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(100L);
        instance.setWorkflowId(1L);
        instance.setCurrentNodeId("node_start");
        instance.setStatus("RUNNING");
        instance.setContext("{}");

        WorkflowNode startNode = createNode(1L, "node_start", "START");
        WorkflowNode nodeA = createNode(1L, "node_a", "LLM");
        WorkflowNode nodeB = createNode(1L, "node_b", "LLM");

        WorkflowEdge edgeStartA = createEdge("node_start", "node_a");
        WorkflowEdge edgeAB = createEdge("node_a", "node_b");
        WorkflowEdge edgeBC = createEdge("node_b", "node_c");

        when(workflowInstanceMapper.selectById(100L)).thenReturn(instance);
        when(workflowNodeMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(startNode)
                .thenReturn(nodeA)
                .thenReturn(nodeB);
        when(workflowEdgeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(edgeStartA))
                .thenReturn(List.of(edgeAB))
                .thenReturn(List.of(edgeBC));
        when(workflowInstanceMapper.updateById(any(WorkflowInstance.class))).thenReturn(1);
        when(nodeExecutionRecorder.recordStart(any(), any(), any())).thenReturn(1L);

        NodeExecutor mockExecutor = mock(NodeExecutor.class);
        when(mockExecutor.execute(any(), any(), any())).thenReturn(NodeResult.success(null));
        when(nodeExecutorRegistry.get(anyString())).thenReturn(mockExecutor);

        ReflectionTestUtils.setField(workflowEngine, "maxExecutionSteps", 2);

        // When
        workflowEngine.executeAsync(100L, "node_start");

        // Then: 实例应该被标记为 FAILED，且错误信息包含 exceeded
        verify(workflowInstanceMapper, atLeastOnce()).updateById(argThat((WorkflowInstance i) ->
                "FAILED".equals(i.getStatus()) && i.getErrorMsg() != null && i.getErrorMsg().contains("exceeded")));
    }

    @Test
    void shouldReturnCompletedInstance_whenExecuteSyncFinishes() {
        // Given
        WorkflowInstance completed = new WorkflowInstance();
        completed.setId(100L);
        completed.setStatus("COMPLETED");
        completed.setContext("{\"reply\":\"hello\"}");

        doReturn("100").when(workflowEngine).start(eq(1L), any());
        when(workflowInstanceMapper.selectById(100L)).thenReturn(completed);

        // When
        WorkflowInstance result = workflowEngine.executeSync(1L, Map.of("userMessage", "hello"));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void shouldThrowBizException_whenExecuteSyncTimesOut() {
        // Given: 工作流一直处于 RUNNING 状态
        WorkflowInstance running = new WorkflowInstance();
        running.setId(100L);
        running.setStatus("RUNNING");

        doReturn("100").when(workflowEngine).start(eq(1L), any());
        when(workflowInstanceMapper.selectById(100L)).thenReturn(running);

        ReflectionTestUtils.setField(workflowEngine, "syncTimeoutMs", 100);

        // When / Then
        assertThatThrownBy(() -> workflowEngine.executeSync(1L, Map.of("userMessage", "hello")))
                .isInstanceOf(com.hify.common.core.exception.BizException.class)
                .hasMessageContaining("timeout");
    }

    private WorkflowNode createNode(Long workflowId, String nodeId, String type) {
        WorkflowNode node = new WorkflowNode();
        node.setWorkflowId(workflowId);
        node.setNodeId(nodeId);
        node.setType(type);
        node.setConfig("{}");
        return node;
    }

    private WorkflowEdge createEdge(String source, String target) {
        WorkflowEdge edge = new WorkflowEdge();
        edge.setSourceNode(source);
        edge.setTargetNode(target);
        edge.setEdgeIndex(0);
        return edge;
    }
}
