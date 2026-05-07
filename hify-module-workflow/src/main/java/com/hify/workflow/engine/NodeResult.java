package com.hify.workflow.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 节点执行结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeResult {

    /**
     * 是否执行成功
     */
    private boolean success;

    /**
     * 是否需要人工审批（暂停等待）
     */
    private boolean requiresApproval;

    /**
     * 输出变量名，结果存到 context 的 key
     */
    private String outputVar;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 下一个节点 ID（null 表示结束）
     */
    private String nextNodeId;

    /**
     * 创建成功结果
     */
    public static NodeResult success(String nextNodeId) {
        return NodeResult.builder()
                .success(true)
                .requiresApproval(false)
                .nextNodeId(nextNodeId)
                .build();
    }

    /**
     * 创建成功结果（带输出变量）
     */
    public static NodeResult success(String nextNodeId, String outputVar) {
        return NodeResult.builder()
                .success(true)
                .requiresApproval(false)
                .nextNodeId(nextNodeId)
                .outputVar(outputVar)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static NodeResult failure(String errorMessage) {
        return NodeResult.builder()
                .success(false)
                .requiresApproval(false)
                .errorMessage(errorMessage)
                .nextNodeId(null)
                .build();
    }

    /**
     * 创建需要审批的结果（暂停等待）
     */
    public static NodeResult approvalRequired(String outputVar) {
        return NodeResult.builder()
                .success(true)
                .requiresApproval(true)
                .outputVar(outputVar)
                .nextNodeId(null)
                .build();
    }

    /**
     * 结束流程
     */
    public static NodeResult end() {
        return NodeResult.builder()
                .success(true)
                .requiresApproval(false)
                .nextNodeId(null)
                .build();
    }
}
