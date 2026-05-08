package com.hify.workflow.engine.executor;

import com.hify.common.core.enums.ResultCode;
import com.hify.common.core.exception.BizException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 节点执行器注册中心
 * <p>Spring 自动收集所有 {@link NodeExecutor} 实现类，按 {@link NodeExecutor#nodeType()} 分发</p>
 */
@Component
public class NodeExecutorRegistry {

    private final Map<String, NodeExecutor> executorMap;

    public NodeExecutorRegistry(List<NodeExecutor> executors) {
        this.executorMap = executors.stream()
                .collect(Collectors.toMap(
                        NodeExecutor::nodeType,
                        e -> e,
                        (existing, replacement) -> replacement // 后加载的覆盖前者
                ));
    }

    /**
     * 根据节点类型获取执行器
     *
     * @param nodeType 节点类型
     * @return 对应的执行器
     * @throws BizException 未知节点类型
     */
    public NodeExecutor get(String nodeType) {
        NodeExecutor executor = executorMap.get(nodeType);
        if (executor == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "Unknown node type: " + nodeType);
        }
        return executor;
    }
}
