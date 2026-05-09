package com.hify.workflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hify.common.web.entity.PageResult;
import com.hify.workflow.api.WorkflowApi;
import com.hify.workflow.api.dto.WorkflowApprovalDTO;
import com.hify.workflow.api.dto.WorkflowCreateRequest;
import com.hify.workflow.api.dto.WorkflowDTO;
import com.hify.workflow.api.dto.WorkflowEdgeDTO;
import com.hify.workflow.api.dto.WorkflowInstanceDTO;
import com.hify.workflow.api.dto.WorkflowNodeDTO;
import com.hify.workflow.api.dto.WorkflowStartRequest;
import com.hify.workflow.api.dto.WorkflowUpdateRequest;
import com.hify.workflow.engine.WorkflowEngine;
import com.hify.workflow.entity.Workflow;
import com.hify.workflow.entity.WorkflowApproval;
import com.hify.workflow.entity.WorkflowEdge;
import com.hify.workflow.entity.WorkflowInstance;
import com.hify.workflow.entity.WorkflowNode;
import com.hify.workflow.mapper.WorkflowApprovalMapper;
import com.hify.workflow.mapper.WorkflowEdgeMapper;
import com.hify.workflow.mapper.WorkflowInstanceMapper;
import com.hify.workflow.mapper.WorkflowMapper;
import com.hify.workflow.mapper.WorkflowNodeExecutionMapper;
import com.hify.workflow.mapper.WorkflowNodeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowExecutionServiceTest {

    @Mock
    private WorkflowMapper workflowMapper;

    @Mock
    private WorkflowNodeMapper workflowNodeMapper;

    @Mock
    private WorkflowEdgeMapper workflowEdgeMapper;

    @Mock
    private WorkflowInstanceMapper workflowInstanceMapper;

    @Mock
    private WorkflowApprovalMapper workflowApprovalMapper;

    @Mock
    private WorkflowNodeExecutionMapper workflowNodeExecutionMapper;

    @Mock
    private WorkflowEngine workflowEngine;

    @InjectMocks
    private WorkflowExecutionService workflowExecutionService;

    @Test
    void shouldCreateWorkflow_whenGivenValidRequest() {
        WorkflowCreateRequest request = new WorkflowCreateRequest();
        request.setName("Test Workflow");
        request.setDescription("A test workflow");
        request.setConfig("{\"nodes\":[],\"edges\":[]}");

        doAnswer(invocation -> {
            Workflow w = invocation.getArgument(0);
            w.setId(100L);
            return 1;
        }).when(workflowMapper).insert(any(Workflow.class));

        Long workflowId = workflowExecutionService.create(request);

        assertThat(workflowId).isEqualTo(100L);
        verify(workflowMapper).insert(any(Workflow.class));
    }

    @Test
    void shouldUpdateWorkflow_whenWorkflowExists() {
        Workflow existingWorkflow = new Workflow();
        existingWorkflow.setId(1L);
        existingWorkflow.setName("Old Name");

        WorkflowUpdateRequest request = new WorkflowUpdateRequest();
        request.setName("New Name");

        when(workflowMapper.selectById(1L)).thenReturn(existingWorkflow);
        when(workflowMapper.updateById(any(Workflow.class))).thenReturn(1);

        workflowExecutionService.update(1L, request);

        verify(workflowMapper).updateById(any(Workflow.class));
    }

    @Test
    void shouldThrowException_whenUpdateNonExistingWorkflow() {
        when(workflowMapper.selectById(999L)).thenReturn(null);

        WorkflowUpdateRequest request = new WorkflowUpdateRequest();
        request.setName("New Name");

        assertThatThrownBy(() -> workflowExecutionService.update(999L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Workflow not found");
    }

    @Test
    void shouldDeleteWorkflow_whenWorkflowExists() {
        when(workflowNodeMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(workflowEdgeMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(workflowInstanceMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(workflowMapper.deleteById(1L)).thenReturn(1);

        workflowExecutionService.delete(1L);

        verify(workflowMapper).deleteById(1L);
        verify(workflowNodeMapper).delete(any(LambdaQueryWrapper.class));
        verify(workflowEdgeMapper).delete(any(LambdaQueryWrapper.class));
        verify(workflowInstanceMapper).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    void shouldReturnWorkflowDetail_whenWorkflowExists() {
        Workflow existingWorkflow = new Workflow();
        existingWorkflow.setId(1L);
        existingWorkflow.setName("Test Workflow");
        existingWorkflow.setDescription("Description");
        existingWorkflow.setStatus("draft");
        existingWorkflow.setVersion(1);

        when(workflowMapper.selectById(1L)).thenReturn(existingWorkflow);

        WorkflowDTO dto = workflowExecutionService.getById(1L);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Test Workflow");
        assertThat(dto.getStatus()).isEqualTo("draft");
    }

    @Test
    void shouldReturnNull_whenWorkflowNotFound() {
        when(workflowMapper.selectById(999L)).thenReturn(null);

        WorkflowDTO dto = workflowExecutionService.getById(999L);

        assertThat(dto).isNull();
    }

    @Test
    void shouldReturnPagedWorkflows() {
        Workflow workflow1 = new Workflow();
        workflow1.setId(1L);
        workflow1.setName("Workflow 1");
        workflow1.setStatus("draft");

        Workflow workflow2 = new Workflow();
        workflow2.setId(2L);
        workflow2.setName("Workflow 2");
        workflow2.setStatus("published");

        Page<Workflow> page = new Page<>(1, 20);
        page.setRecords(List.of(workflow1, workflow2));
        page.setTotal(2L);
        page.setPages(1L);

        when(workflowMapper.selectPage(any(Page.class), any())).thenReturn(page);

        PageResult<WorkflowDTO> result = workflowExecutionService.list(
                new WorkflowApi.WorkflowQueryDTO()
        );

        assertThat(result).isNotNull();
        assertThat(result.getRecords()).hasSize(2);
        assertThat(result.getTotal()).isEqualTo(2L);
    }

    @Test
    void shouldStartWorkflow_andReturnInstanceId() {
        WorkflowStartRequest request = new WorkflowStartRequest();
        request.setWorkflowId(1L);
        request.setInputs(Map.of("userId", "123"));

        when(workflowEngine.start(1L, Map.of("userId", "123"))).thenReturn("instance-100");

        String instanceId = workflowExecutionService.start(request);

        assertThat(instanceId).isEqualTo("instance-100");
        verify(workflowEngine).start(1L, Map.of("userId", "123"));
    }

    @Test
    void shouldReturnInstanceDetail_whenInstanceExists() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(100L);
        instance.setWorkflowId(1L);
        instance.setStatus("running");
        instance.setCurrentNodeId("node_001");

        when(workflowInstanceMapper.selectById(100L)).thenReturn(instance);

        WorkflowInstanceDTO dto = workflowExecutionService.getInstanceById(100L);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(100L);
        assertThat(dto.getStatus()).isEqualTo("running");
        assertThat(dto.getCurrentNodeId()).isEqualTo("node_001");
    }

    @Test
    void shouldReturnNodes_forWorkflow() {
        WorkflowNode node1 = new WorkflowNode();
        node1.setId(1L);
        node1.setWorkflowId(1L);
        node1.setNodeId("node_start");
        node1.setType("START");

        WorkflowNode node2 = new WorkflowNode();
        node2.setId(2L);
        node2.setWorkflowId(1L);
        node2.setNodeId("node_end");
        node2.setType("END");

        when(workflowNodeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(node1, node2));

        List<?> nodes = workflowExecutionService.getNodes(1L);

        assertThat(nodes).hasSize(2);
    }

    @Test
    void shouldReturnEdges_forWorkflow() {
        WorkflowEdge edge = new WorkflowEdge();
        edge.setId(1L);
        edge.setWorkflowId(1L);
        edge.setSourceNode("node_start");
        edge.setTargetNode("node_end");

        when(workflowEdgeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(edge));

        List<?> edges = workflowExecutionService.getEdges(1L);

        assertThat(edges).hasSize(1);
    }

    @Test
    void shouldApprove_whenApprovalExists() {
        WorkflowApproval approval = new WorkflowApproval();
        approval.setId(1L);
        approval.setInstanceId(100L);
        approval.setStatus("pending");

        when(workflowApprovalMapper.selectById(1L)).thenReturn(approval);
        when(workflowApprovalMapper.updateById(any(WorkflowApproval.class))).thenReturn(1);

        workflowExecutionService.approve(1L, "Approved by manager");

        assertThat(approval.getStatus()).isEqualTo("approved");
        assertThat(approval.getRemark()).isEqualTo("Approved by manager");
        verify(workflowApprovalMapper).updateById(any(WorkflowApproval.class));
    }

    @Test
    void shouldThrowException_whenApproveNonExistingApproval() {
        when(workflowApprovalMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> workflowExecutionService.approve(999L, "Approved"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Approval not found");
    }

    @Test
    void shouldReject_whenApprovalExists() {
        WorkflowApproval approval = new WorkflowApproval();
        approval.setId(1L);
        approval.setInstanceId(100L);
        approval.setStatus("pending");

        when(workflowApprovalMapper.selectById(1L)).thenReturn(approval);
        when(workflowApprovalMapper.updateById(any(WorkflowApproval.class))).thenReturn(1);

        workflowExecutionService.reject(1L, "Rejected due to policy");

        assertThat(approval.getStatus()).isEqualTo("rejected");
        assertThat(approval.getRemark()).isEqualTo("Rejected due to policy");
        verify(workflowApprovalMapper).updateById(any(WorkflowApproval.class));
    }

    @Test
    void shouldReturnPendingApprovals_forInstance() {
        WorkflowApproval approval1 = new WorkflowApproval();
        approval1.setId(1L);
        approval1.setInstanceId(100L);
        approval1.setNodeId("node_approval");
        approval1.setStatus("pending");

        when(workflowApprovalMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(approval1));

        List<WorkflowApprovalDTO> approvals = workflowExecutionService.getPendingApprovals(100L);

        assertThat(approvals).hasSize(1);
        assertThat(approvals.get(0).getStatus()).isEqualTo("pending");
    }

    @Test
    void shouldReturnEmptyList_whenNoPendingApprovals() {
        when(workflowApprovalMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        List<WorkflowApprovalDTO> approvals = workflowExecutionService.getPendingApprovals(100L);

        assertThat(approvals).isEmpty();
    }

    @Test
    void shouldResumeExecution_whenApprovalIsApproved() {
        WorkflowApproval approval = new WorkflowApproval();
        approval.setId(1L);
        approval.setInstanceId(100L);
        approval.setNodeId("node_approval");
        approval.setStatus("pending");

        when(workflowApprovalMapper.selectById(1L)).thenReturn(approval);
        when(workflowApprovalMapper.updateById(any(WorkflowApproval.class))).thenReturn(1);

        workflowExecutionService.approve(1L, "Approved by manager");

        assertThat(approval.getStatus()).isEqualTo("approved");
        verify(workflowEngine).resumeAfterApproval(100L, "approved");
    }

    @Test
    void shouldResumeExecutionWithRejectedAction_whenApprovalIsRejected() {
        WorkflowApproval approval = new WorkflowApproval();
        approval.setId(1L);
        approval.setInstanceId(100L);
        approval.setNodeId("node_approval");
        approval.setStatus("pending");

        when(workflowApprovalMapper.selectById(1L)).thenReturn(approval);
        when(workflowApprovalMapper.updateById(any(WorkflowApproval.class))).thenReturn(1);

        workflowExecutionService.reject(1L, "Rejected by manager");

        assertThat(approval.getStatus()).isEqualTo("rejected");
        verify(workflowEngine).resumeAfterApproval(100L, "rejected");
    }

    @Test
    void shouldSaveNodes_whenWorkflowExists() {
        Workflow existingWorkflow = new Workflow();
        existingWorkflow.setId(1L);

        when(workflowMapper.selectById(1L)).thenReturn(existingWorkflow);
        when(workflowNodeMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(0);
        doAnswer(invocation -> {
            WorkflowNode node = invocation.getArgument(0);
            node.setId(100L);
            return 1;
        }).when(workflowNodeMapper).insert(any(WorkflowNode.class));

        WorkflowNodeDTO nodeDto = new WorkflowNodeDTO();
        nodeDto.setNodeId("node_start");
        nodeDto.setType("START");
        nodeDto.setName("开始");
        nodeDto.setConfig("{}");

        List<WorkflowNodeDTO> result = workflowExecutionService.saveNodes(1L, List.of(nodeDto));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNodeId()).isEqualTo("node_start");
        verify(workflowNodeMapper).delete(any(LambdaQueryWrapper.class));
        verify(workflowNodeMapper).insert(any(WorkflowNode.class));
    }

    @Test
    void shouldThrowException_whenSaveNodesForNonExistingWorkflow() {
        when(workflowMapper.selectById(999L)).thenReturn(null);

        WorkflowNodeDTO nodeDto = new WorkflowNodeDTO();
        nodeDto.setNodeId("node_start");

        assertThatThrownBy(() -> workflowExecutionService.saveNodes(999L, List.of(nodeDto)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Workflow not found");
    }

    @Test
    void shouldSaveEdges_whenWorkflowExists() {
        Workflow existingWorkflow = new Workflow();
        existingWorkflow.setId(1L);

        when(workflowMapper.selectById(1L)).thenReturn(existingWorkflow);
        when(workflowEdgeMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(0);
        doAnswer(invocation -> {
            WorkflowEdge edge = invocation.getArgument(0);
            edge.setId(200L);
            return 1;
        }).when(workflowEdgeMapper).insert(any(WorkflowEdge.class));

        WorkflowEdgeDTO edgeDto = new WorkflowEdgeDTO();
        edgeDto.setSourceNode("node_start");
        edgeDto.setTargetNode("node_end");
        edgeDto.setEdgeIndex(0);

        List<WorkflowEdgeDTO> result = workflowExecutionService.saveEdges(1L, List.of(edgeDto));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSourceNode()).isEqualTo("node_start");
        verify(workflowEdgeMapper).delete(any(LambdaQueryWrapper.class));
        verify(workflowEdgeMapper).insert(any(WorkflowEdge.class));
    }

    @Test
    void shouldThrowException_whenSaveEdgesForNonExistingWorkflow() {
        when(workflowMapper.selectById(999L)).thenReturn(null);

        WorkflowEdgeDTO edgeDto = new WorkflowEdgeDTO();
        edgeDto.setSourceNode("node_start");

        assertThatThrownBy(() -> workflowExecutionService.saveEdges(999L, List.of(edgeDto)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Workflow not found");
    }

    @Test
    void shouldReturnNodeExecutions_whenInstanceHasExecutions() {
        com.hify.workflow.entity.WorkflowNodeExecution exec1 = new com.hify.workflow.entity.WorkflowNodeExecution();
        exec1.setId(1L);
        exec1.setExecutionId(100L);
        exec1.setNodeId("node_start");
        exec1.setNodeType("START");
        exec1.setStatus("completed");
        exec1.setInputJson("{\"input\":\"hello\"}");
        exec1.setOutputJson("{\"output\":\"world\"}");
        exec1.setStartedAt(LocalDateTime.of(2025, 5, 8, 14, 0, 0));
        exec1.setEndedAt(LocalDateTime.of(2025, 5, 8, 14, 0, 1));

        com.hify.workflow.entity.WorkflowNodeExecution exec2 = new com.hify.workflow.entity.WorkflowNodeExecution();
        exec2.setId(2L);
        exec2.setExecutionId(100L);
        exec2.setNodeId("node_llm");
        exec2.setNodeType("LLM");
        exec2.setStatus("failed");
        exec2.setErrorMsg("Timeout");
        exec2.setStartedAt(LocalDateTime.of(2025, 5, 8, 14, 0, 2));
        exec2.setEndedAt(LocalDateTime.of(2025, 5, 8, 14, 0, 5));

        when(workflowNodeExecutionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(exec1, exec2));

        List<com.hify.workflow.api.dto.WorkflowNodeExecutionDTO> result =
                workflowExecutionService.getNodeExecutions(100L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getNodeId()).isEqualTo("node_start");
        assertThat(result.get(0).getStatus()).isEqualTo("completed");
        assertThat(result.get(0).getInputJson()).isEqualTo("{\"input\":\"hello\"}");
        assertThat(result.get(1).getNodeId()).isEqualTo("node_llm");
        assertThat(result.get(1).getStatus()).isEqualTo("failed");
        assertThat(result.get(1).getErrorMsg()).isEqualTo("Timeout");
    }

    @Test
    void shouldReturnEmptyList_whenInstanceHasNoExecutions() {
        when(workflowNodeExecutionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        List<com.hify.workflow.api.dto.WorkflowNodeExecutionDTO> result =
                workflowExecutionService.getNodeExecutions(100L);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnPagedInstances_whenQueryByWorkflowId() {
        WorkflowInstance instance1 = new WorkflowInstance();
        instance1.setId(100L);
        instance1.setWorkflowId(1L);
        instance1.setStatus("completed");

        WorkflowInstance instance2 = new WorkflowInstance();
        instance2.setId(101L);
        instance2.setWorkflowId(1L);
        instance2.setStatus("failed");

        Page<WorkflowInstance> page = new Page<>(1, 20);
        page.setRecords(List.of(instance1, instance2));
        page.setTotal(2L);
        page.setPages(1L);

        when(workflowInstanceMapper.selectPage(any(Page.class), any())).thenReturn(page);

        WorkflowApi.InstanceQueryDTO query = new WorkflowApi.InstanceQueryDTO();
        query.setWorkflowId(1L);
        PageResult<WorkflowInstanceDTO> result = workflowExecutionService.listInstances(query);

        assertThat(result).isNotNull();
        assertThat(result.getRecords()).hasSize(2);
        assertThat(result.getTotal()).isEqualTo(2L);
    }

    @Test
    void shouldReturnEmptyPage_whenNoInstancesMatch() {
        Page<WorkflowInstance> page = new Page<>(1, 20);
        page.setRecords(Collections.emptyList());
        page.setTotal(0L);
        page.setPages(0L);

        when(workflowInstanceMapper.selectPage(any(Page.class), any())).thenReturn(page);

        WorkflowApi.InstanceQueryDTO query = new WorkflowApi.InstanceQueryDTO();
        query.setWorkflowId(999L);
        PageResult<WorkflowInstanceDTO> result = workflowExecutionService.listInstances(query);

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getTotal()).isEqualTo(0L);
    }
}
