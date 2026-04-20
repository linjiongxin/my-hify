package com.hify.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent MCP Binding 实体类
 *
 * @author hify
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_mcp_binding")
public class AgentMcpBinding extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * Agent ID
     */
    private Long agentId;

    /**
     * MCP 服务器 ID
     */
    private Long mcpServerId;

    /**
     * 是否启用
     */
    private Boolean enabled;
}
