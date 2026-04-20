package com.hify.agent.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.web.entity.base.BaseEntity;
import com.hify.common.web.handler.JsonbTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_tool")
public class AgentTool extends BaseEntity {

    private Long agentId;
    private String toolName;
    private String toolType;
    private String toolImpl;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private Map<String, Object> configJson;

    private Boolean enabled;
    private Integer sortOrder;
}