package com.hify.agent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class AgentCreateRequest {
    @NotBlank(message = "名称不能为空")
    private String name;
    private String description;
    @NotBlank(message = "模型不能为空")
    private String modelId;
    private String systemPrompt;
    private BigDecimal temperature = new BigDecimal("0.7");
    private Integer maxTokens = 2048;
    private BigDecimal topP = new BigDecimal("1.0");
    private String welcomeMessage;
    private Boolean enabled = true;
    private Long workflowId;
    private String executionMode = "react";
}
