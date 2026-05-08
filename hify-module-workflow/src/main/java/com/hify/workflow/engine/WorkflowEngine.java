package com.hify.workflow.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hify.workflow.config.RetryConfig;
import com.hify.workflow.engine.config.ApprovalNodeConfig;
import com.hify.workflow.engine.config.NodeConfig;
import com.hify.workflow.engine.config.NodeConfigParser;
import com.hify.workflow.engine.context.ExecutionContext;
import com.hify.workflow.engine.executor.NodeExecutor;
import com.hify.workflow.engine.executor.NodeExecutorRegistry;
import com.hify.workflow.engine.util.PlaceholderUtils;
import com.hify.workflow.entity.*;
import com.hify.workflow.mapper.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流执行引擎
 * <p>核心编排逻辑：异步执行循环、节点调度、失败处理</p>
 */
@Slf4j
@Component
public class WorkflowEngine {

    /**
     * 全局默认重试配置
     */
    private static final RetryConfig DEFAULT_RETRY_CONFIG = new RetryConfig();

    @Autowired
    private WorkflowMapper workflowMapper;

    @Autowired
    private WorkflowNodeMapper workflowNodeMapper;

    @Autowired
    private WorkflowEdgeMapper workflowEdgeMapper;

    @Autowired
    private WorkflowInstanceMapper workflowInstanceMapper;

    @Autowired
    private WorkflowNodeExecutionMapper workflowNodeExecutionMapper;

    @Autowired
    private NodeExecutorRegistry nodeExecutorRegistry;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NodeConfigParser nodeConfigParser;

    @Autowired
    private NodeExecutionRecorder nodeExecutionRecorder;

    private final SpelExpressionParser spelParser = new SpelExpressionParser();

    /**
     * 应用启动时恢复未完成的实例
     */
    @PostConstruct
    public void resumePendingInstances() {
        log.info("Resuming pending workflow instances...");

        // 查询 PENDING 和 RUNNING 状态的实例
        List<WorkflowInstance> pendingInstances = workflowInstanceMapper.selectList(
                new LambdaQueryWrapper<WorkflowInstance>()
                        .in(WorkflowInstance::getStatus, List.of("PENDING", "RUNNING"))
        );

        for (WorkflowInstance instance : pendingInstances) {
            log.info("Resuming workflow instance: id={}, workflowId={}, currentNode={}",
                    instance.getId(), instance.getWorkflowId(), instance.getCurrentNodeId());

            // 恢复执行，从当前节点继续
            String currentNodeId = instance.getCurrentNodeId();
            if (currentNodeId != null) {
                executeAsync(instance.getId(), currentNodeId);
            }
        }

        log.info("Resumed {} pending instances", pendingInstances.size());
    }

    /**
     * 启动工作流
     *
     * @param workflowId 工作流 ID
     * @param inputs     输入参数
     * @return 实例 ID
     */
    public String start(Long workflowId, Map<String, Object> inputs) {
        // 获取工作流定义
        Workflow workflow = workflowMapper.selectById(workflowId);
        if (workflow == null) {
            throw new IllegalArgumentException("Workflow not found: " + workflowId);
        }

        // 获取开始节点
        List<WorkflowNode> startNodes = workflowNodeMapper.selectList(
                new LambdaQueryWrapper<WorkflowNode>()
                        .eq(WorkflowNode::getWorkflowId, workflowId)
                        .eq(WorkflowNode::getType, "START")
        );

        if (startNodes.isEmpty()) {
            throw new IllegalStateException("No START node found for workflow: " + workflowId);
        }

        String startNodeId = startNodes.get(0).getNodeId();

        // 创建执行实例
        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowId(workflowId);
        instance.setStatus("RUNNING");
        instance.setCurrentNodeId(startNodeId);
        instance.setStartedAt(LocalDateTime.now());

        // 初始化上下文
        ExecutionContext context = new ExecutionContext(instance.getId(), inputs);
        instance.setContext(toJson(context.getAll()));

        workflowInstanceMapper.insert(instance);

        log.info("Workflow instance created: id={}, workflowId={}, startNode={}",
                instance.getId(), workflowId, startNodeId);

        // 异步执行
        executeAsync(instance.getId(), startNodeId);

        return String.valueOf(instance.getId());
    }

    /**
     * 异步执行节点
     *
     * @param instanceId 实例 ID
     * @param nodeId      节点 ID
     */
    @Async("commonExecutor")
    public void executeAsync(Long instanceId, String nodeId) {
        log.info("Executing node: instanceId={}, nodeId={}", instanceId, nodeId);

        WorkflowInstance instance = workflowInstanceMapper.selectById(instanceId);
        if (instance == null) {
            log.error("Instance not found: {}", instanceId);
            return;
        }

        // 检查实例状态
        if ("COMPLETED".equals(instance.getStatus()) || "FAILED".equals(instance.getStatus())) {
            log.warn("Instance already finished: id={}, status={}", instanceId, instance.getStatus());
            return;
        }

        // 获取节点定义
        WorkflowNode node = getNodeByNodeId(instance.getWorkflowId(), nodeId);
        if (node == null) {
            handleFailure(null, instance, "Node not found: " + nodeId);
            return;
        }

        // 恢复上下文
        ExecutionContext context = loadContext(instance);
        context.put("_instanceId", instanceId);
        context.put("_workflowId", instance.getWorkflowId());

        // 循环检测
        if (isNodeVisited(context, nodeId)) {
            failInstance(instance, "Cycle detected: node " + nodeId + " has already been visited");
            return;
        }
        markNodeVisited(context, nodeId);
        saveContext(instance, context);

        // 解析节点配置
        NodeConfig config;
        try {
            config = nodeConfigParser.parse(node);
        } catch (Exception e) {
            handleFailure(node, instance, "Failed to parse node config: " + e.getMessage());
            return;
        }

        // 获取节点执行器
        NodeExecutor executor;
        try {
            executor = nodeExecutorRegistry.get(node.getType());
        } catch (Exception e) {
            handleFailure(node, instance, "Unknown node type: " + node.getType());
            return;
        }

        // 执行节点（带重试）
        NodeResult result = executeWithRetry(node, config, context, executor, instance);

        // 处理执行结果
        if (result.isRequiresApproval()) {
            // 需要人工审批，暂停等待
            instance.setCurrentNodeId(nodeId);
            workflowInstanceMapper.updateById(instance);
            log.info("Approval required: instanceId={}, nodeId={}", instanceId, nodeId);
            return;
        }

        if (!result.isSuccess()) {
            handleFailure(node, instance, result.getErrorMessage());
            return;
        }

        // 找到下一节点
        String nextNodeId = findNextNode(node, context, result);
        if (nextNodeId == null) {
            // 流程结束
            completeInstance(instance);
        } else {
            // 继续执行下一节点
            instance.setCurrentNodeId(nextNodeId);
            workflowInstanceMapper.updateById(instance);

            // 异步执行下一节点
            executeAsync(instanceId, nextNodeId);
        }
    }

    /**
     * 带重试执行节点
     */
    private NodeResult executeWithRetry(WorkflowNode node, NodeConfig config, ExecutionContext context,
                                        NodeExecutor executor, WorkflowInstance instance) {
        Long recordId = nodeExecutionRecorder.recordStart(instance.getId(), node, node.getConfig());
        RetryConfig retryConfig = getRetryConfig(node, instance);

        int attempt = 0;
        while (attempt <= retryConfig.getMaxRetries()) {
            try {
                NodeResult result = executor.execute(node, config, context);

                // 保存上下文
                saveContext(instance, context);

                if (result.isSuccess()) {
                    nodeExecutionRecorder.recordSuccess(recordId, toJson(context.getAll()));
                    return result;
                }

                // 执行失败，尝试重试
                if (attempt < retryConfig.getMaxRetries()) {
                    attempt++;
                    log.warn("Node execution failed, retrying: instanceId={}, nodeId={}, attempt={}/{}",
                            instance.getId(), node.getNodeId(), attempt, retryConfig.getMaxRetries());

                    try {
                        Thread.sleep(retryConfig.getRetryIntervalSeconds() * 1000L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        NodeResult r = NodeResult.failure("Interrupted");
                        nodeExecutionRecorder.recordFailure(recordId, r.getErrorMessage());
                        return r;
                    }
                } else {
                    nodeExecutionRecorder.recordFailure(recordId, result.getErrorMessage());
                    return result;
                }
            } catch (Exception e) {
                log.error("Node execution exception: instanceId={}, nodeId={}, attempt={}",
                        instance.getId(), node.getNodeId(), attempt, e);

                if (attempt >= retryConfig.getMaxRetries()) {
                    NodeResult r = NodeResult.failure("Execution exception: " + e.getMessage());
                    nodeExecutionRecorder.recordFailure(recordId, r.getErrorMessage());
                    return r;
                }

                attempt++;
                try {
                    Thread.sleep(retryConfig.getRetryIntervalSeconds() * 1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    NodeResult r = NodeResult.failure("Interrupted");
                    nodeExecutionRecorder.recordFailure(recordId, r.getErrorMessage());
                    return r;
                }
            }
        }

        NodeResult r = NodeResult.failure("Max retries exceeded");
        nodeExecutionRecorder.recordFailure(recordId, r.getErrorMessage());
        return r;
    }

    /**
     * 查找下一节点
     *
     * @param node    当前节点
     * @param context 执行上下文
     * @param result  节点执行结果
     * @return 下一节点 ID，null 表示结束
     */
    public String findNextNode(WorkflowNode node, ExecutionContext context, NodeResult result) {
        // 如果结果显式指定了下一节点，直接使用
        if (result.getNextNodeId() != null) {
            return result.getNextNodeId();
        }

        // 获取从当前节点出发的所有连线
        List<WorkflowEdge> edges = workflowEdgeMapper.selectList(
                new LambdaQueryWrapper<WorkflowEdge>()
                        .eq(WorkflowEdge::getWorkflowId, node.getWorkflowId())
                        .eq(WorkflowEdge::getSourceNode, node.getNodeId())
                        .orderByAsc(WorkflowEdge::getEdgeIndex)
        );

        if (edges.isEmpty()) {
            return null; // 没有更多连线，流程结束
        }

        String defaultBranch = null;
        for (WorkflowEdge edge : edges) {
            String condition = edge.getCondition();
            if (condition == null || condition.isBlank()) {
                defaultBranch = edge.getTargetNode();
                continue;
            }
            try {
                String resolved = PlaceholderUtils.replaceForExpression(condition, context);
                if (evaluateExpression(resolved)) {
                    return edge.getTargetNode();
                }
            } catch (Exception e) {
                log.warn("Failed to evaluate edge condition: {}, error: {}", condition, e.getMessage());
            }
        }

        return defaultBranch;
    }

    /**
     * 使用 SpEL 计算表达式
     */
    private boolean evaluateExpression(String expression) {
        return Boolean.TRUE.equals(spelParser.parseExpression(expression).getValue(Boolean.class));
    }

    /**
     * 处理失败
     * <p>失败处理逻辑：errorBranch > 重试 > 标记失败</p>
     */
    private void handleFailure(WorkflowNode node, WorkflowInstance instance, String errorMsg) {
        log.error("Workflow failure: instanceId={}, error={}", instance.getId(), errorMsg);

        // 1. 检查节点的 errorBranch 配置
        if (node != null) {
            try {
                NodeConfig config = nodeConfigParser.parse(node);
                String errorBranch = config.errorBranch();
                if (errorBranch != null && !errorBranch.isEmpty()) {
                    // 从 edges 找到 errorBranch 对应的目标节点
                    List<WorkflowEdge> errorEdges = workflowEdgeMapper.selectList(
                            new LambdaQueryWrapper<WorkflowEdge>()
                                    .eq(WorkflowEdge::getWorkflowId, node.getWorkflowId())
                                    .eq(WorkflowEdge::getSourceNode, node.getNodeId())
                    );

                    for (WorkflowEdge edge : errorEdges) {
                        if (errorBranch.equals(edge.getTargetNode())) {
                            // 跳转到错误分支
                            log.info("Jumping to error branch: instanceId={}, from={}, to={}",
                                    instance.getId(), node.getNodeId(), errorBranch);
                            instance.setCurrentNodeId(errorBranch);
                            workflowInstanceMapper.updateById(instance);
                            executeAsync(instance.getId(), errorBranch);
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse node config for error branch: {}", e.getMessage());
            }
        }

        // 2. 标记实例失败
        failInstance(instance, errorMsg);
    }

    /**
     * 完成实例
     */
    private void completeInstance(WorkflowInstance instance) {
        instance.setStatus("COMPLETED");
        instance.setFinishedAt(LocalDateTime.now());
        workflowInstanceMapper.updateById(instance);
        log.info("Workflow completed: instanceId={}", instance.getId());
    }

    /**
     * 标记实例失败
     */
    private void failInstance(WorkflowInstance instance, String errorMsg) {
        instance.setStatus("FAILED");
        instance.setErrorMsg(errorMsg);
        instance.setFinishedAt(LocalDateTime.now());
        workflowInstanceMapper.updateById(instance);
        log.info("Workflow failed: instanceId={}, error={}", instance.getId(), errorMsg);
    }

    /**
     * 获取节点重试配置（优先级：节点级 > 工作流级 > 全局默认）
     */
    private RetryConfig getRetryConfig(WorkflowNode node, WorkflowInstance instance) {
        // 1. 检查节点级配置
        if (node != null && node.getRetryConfig() != null) {
            RetryConfig config = nodeConfigParser.parseRetryConfig(node.getRetryConfig());
            if (config != null) {
                return config;
            }
        }

        // 2. 检查工作流级配置
        Workflow workflow = workflowMapper.selectById(instance.getWorkflowId());
        if (workflow != null && workflow.getRetryConfig() != null) {
            RetryConfig config = nodeConfigParser.parseRetryConfig(workflow.getRetryConfig());
            if (config != null) {
                return config;
            }
        }

        // 3. 返回全局默认
        return DEFAULT_RETRY_CONFIG;
    }

    /**
     * 根据 nodeId 获取节点定义
     */
    private WorkflowNode getNodeByNodeId(Long workflowId, String nodeId) {
        return workflowNodeMapper.selectOne(
                new LambdaQueryWrapper<WorkflowNode>()
                        .eq(WorkflowNode::getWorkflowId, workflowId)
                        .eq(WorkflowNode::getNodeId, nodeId)
        );
    }

    /**
     * 审批通过后恢复执行
     * <p>根据审批结果选择分支，继续往下执行</p>
     *
     * @param instanceId 实例 ID
     * @param action     审批结果：approved 或 rejected
     */
    public void resumeAfterApproval(Long instanceId, String action) {
        log.info("Resuming after approval: instanceId={}, action={}", instanceId, action);

        WorkflowInstance instance = workflowInstanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new IllegalArgumentException("Instance not found: " + instanceId);
        }

        WorkflowNode currentNode = getNodeByNodeId(instance.getWorkflowId(), instance.getCurrentNodeId());
        if (currentNode == null) {
            failInstance(instance, "Current node not found after approval: " + instance.getCurrentNodeId());
            return;
        }

        log.info("Current node after approval: nodeId={}, type={}, config={}",
                currentNode.getNodeId(), currentNode.getType(), currentNode.getConfig());

        // 解析审批节点配置，按结果选择分支
        String nextNodeId = null;
        if (currentNode.getConfig() != null) {
            try {
                ApprovalNodeConfig config = (ApprovalNodeConfig) nodeConfigParser.parse(currentNode);
                log.info("Parsed approval config: approveBranch={}, rejectBranch={}",
                        config.approveBranch(), config.rejectBranch());
                if ("approved".equals(action)) {
                    nextNodeId = config.approveBranch();
                } else if ("rejected".equals(action)) {
                    nextNodeId = config.rejectBranch();
                }
            } catch (Exception e) {
                log.warn("Failed to parse approval config: {}", e.getMessage(), e);
            }
        }

        // 未配置分支时，走默认连线
        if (nextNodeId == null) {
            ExecutionContext context = loadContext(instance);
            nextNodeId = findNextNode(currentNode, context, NodeResult.success(null));
            log.info("Fallback to default edge: nextNodeId={}", nextNodeId);
        }

        log.info("Next node after approval: {}", nextNodeId);

        if (nextNodeId == null) {
            completeInstance(instance);
        } else {
            instance.setCurrentNodeId(nextNodeId);
            workflowInstanceMapper.updateById(instance);
            executeAsync(instanceId, nextNodeId);
        }
    }

    /**
     * 从实例加载上下文
     */
    private ExecutionContext loadContext(WorkflowInstance instance) {
        ExecutionContext context = new ExecutionContext(instance.getId(), null);
        if (instance.getContext() != null && !instance.getContext().isEmpty()) {
            try {
                Map<String, Object> vars = objectMapper.readValue(instance.getContext(),
                        new TypeReference<Map<String, Object>>() {});
                vars.forEach(context::put);
            } catch (Exception e) {
                log.warn("Failed to parse context: {}", e.getMessage());
            }
        }
        return context;
    }

    /**
     * 保存上下文到实例
     */
    private void saveContext(WorkflowInstance instance, ExecutionContext context) {
        instance.setContext(toJson(context.getAll()));
        workflowInstanceMapper.updateById(instance);
    }

    /**
     * 检查节点是否已访问（循环检测）
     */
    @SuppressWarnings("unchecked")
    private boolean isNodeVisited(ExecutionContext context, String nodeId) {
        Object visited = context.get("_visitedNodes");
        if (visited instanceof List) {
            return ((List<String>) visited).contains(nodeId);
        }
        return false;
    }

    /**
     * 标记节点为已访问
     */
    @SuppressWarnings("unchecked")
    private void markNodeVisited(ExecutionContext context, String nodeId) {
        Object visited = context.get("_visitedNodes");
        List<String> visitedNodes;
        if (visited instanceof List) {
            visitedNodes = new ArrayList<>((List<String>) visited);
        } else {
            visitedNodes = new ArrayList<>();
        }
        visitedNodes.add(nodeId);
        context.put("_visitedNodes", visitedNodes);
    }

    /**
     * 对象转 JSON
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Failed to serialize to JSON", e);
            return "{}";
        }
    }
}