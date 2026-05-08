package com.hify.workflow.engine.executor;

import com.hify.rag.api.RagSearchApi;
import com.hify.rag.vo.RagSearchResult;
import com.hify.workflow.engine.NodeResult;
import com.hify.workflow.engine.config.KnowledgeNodeConfig;
import com.hify.workflow.engine.config.NodeConfig;
import com.hify.workflow.engine.context.ExecutionContext;
import com.hify.workflow.entity.WorkflowNode;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * KNOWLEDGE 节点执行器（v2）
 * <p>在知识库中检索相关内容并写入上下文</p>
 */
@Component
public class KnowledgeNodeExecutor implements NodeExecutor {

    private final RagSearchApi ragSearchApi;

    public KnowledgeNodeExecutor(RagSearchApi ragSearchApi) {
        this.ragSearchApi = ragSearchApi;
    }

    @Override
    public String nodeType() {
        return "KNOWLEDGE";
    }

    @Override
    public NodeResult execute(WorkflowNode node, NodeConfig config, ExecutionContext context) {
        try {
            KnowledgeNodeConfig kbConfig = (KnowledgeNodeConfig) config;

            if (kbConfig.knowledgeBaseId() == null || kbConfig.query() == null) {
                return NodeResult.failure("KNOWLEDGE node config missing knowledgeBaseId or query");
            }

            String resolvedQuery = context.resolve(kbConfig.query());
            if (resolvedQuery == null || resolvedQuery.isBlank()) {
                return NodeResult.failure("KNOWLEDGE node resolved query is empty");
            }

            int topK = kbConfig.topK() != null ? kbConfig.topK() : 5;
            float threshold = kbConfig.threshold() != null ? kbConfig.threshold() : 0.7f;

            List<RagSearchResult> results = ragSearchApi.search(
                    kbConfig.knowledgeBaseId(), resolvedQuery, topK, threshold);

            String content = formatResults(results);

            if (kbConfig.outputVar() != null && !kbConfig.outputVar().isEmpty()) {
                context.set(node.getNodeId(), kbConfig.outputVar(), content);
                context.put(kbConfig.outputVar(), content);
            }

            return NodeResult.success(null);

        } catch (Exception e) {
            return NodeResult.failure("KNOWLEDGE execution failed: " + e.getMessage());
        }
    }

    private String formatResults(List<RagSearchResult> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            RagSearchResult r = results.get(i);
            sb.append("[").append(i + 1).append("] ").append(r.getContent()).append("\n");
        }
        return sb.toString().trim();
    }
}
