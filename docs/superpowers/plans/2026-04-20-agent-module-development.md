# Agent 模块实现计划

> **面向执行者：** 本计划由 OpenSpec 规格驱动。执行前请先阅读 `openspec/changes/agent-module-development/design.md` 了解上下文。
> **执行方式：** `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans`
> **步骤标记：** `- [ ]` 未执行，`- [x]` 已完成

**目标：** 实现 Agent 配置的 CRUD + 工具绑定管理

**架构：** agent 模块遵循多模块单体架构，采用 api/service/mapper/entity/dto/vo/controller 分层

**技术栈：** Spring Boot 3.x + MyBatis-Plus + PostgreSQL + JUnit 5 + Mockito

**范围护栏：**
- 目标：Agent CRUD + 工具绑定管理
- 非目标：ReAct 推理循环、MCP 调用、知识库绑定

---

## Task 1: 创建 Agent 实体类

**类型：** 结构变更

**文件：**
- 创建：`hify-module-agent/src/main/java/com/hify/agent/entity/Agent.java`

**依赖：** 无

**描述：** 创建 Agent 实体类，继承 BaseEntity，映射 agent 表

- [ ] **Step 1: 编写 Agent 实体类**

```java
package com.hify.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent")
public class Agent extends BaseEntity {

    private String name;
    private String description;
    private String modelId;
    private String systemPrompt;
    private BigDecimal temperature;
    private Integer maxTokens;
    private BigDecimal topP;
    private String welcomeMessage;
    private Boolean enabled;
}
```

- [ ] **Step 2: 提交**

```bash
git add hify-module-agent/src/main/java/com/hify/agent/entity/Agent.java
git commit -m "feat(agent): create Agent entity"
```

---

## Task 2: 创建 AgentTool 实体类

**类型：** 结构变更

**文件：**
- 创建：`hify-module-agent/src/main/java/com/hify/agent/entity/AgentTool.java`

**依赖：** 无

**描述：** 创建 AgentTool 实体类，映射 agent_tool 表

- [ ] **Step 1: 编写 AgentTool 实体类**

```java
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
```

- [ ] **Step 2: 提交**

```bash
git add hify-module-agent/src/main/java/com/hify/agent/entity/AgentTool.java
git commit -m "feat(agent): create AgentTool entity"
```

---

## Task 3: 创建 AgentMcpBinding 实体类

**类型：** 结构变更

**文件：**
- 创建：`hify-module-agent/src/main/java/com/hify/agent/entity/AgentMcpBinding.java`

**依赖：** 无

**描述：** 创建 AgentMcpBinding 实体类，映射 agent_mcp_binding 表

- [ ] **Step 1: 编写 AgentMcpBinding 实体类**

```java
package com.hify.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_mcp_binding")
public class AgentMcpBinding extends BaseEntity {

    private Long agentId;
    private Long mcpServerId;
    private Boolean enabled;
}
```

- [ ] **Step 2: 提交**

```bash
git add hify-module-agent/src/main/java/com/hify/agent/entity/AgentMcpBinding.java
git commit -m "feat(agent): create AgentMcpBinding entity"
```

---

## Task 4: 创建 AgentConstants 常量类

**类型：** 结构变更

**文件：**
- 创建：`hify-module-agent/src/main/java/com/hify/agent/constant/AgentConstants.java`

**依赖：** Task 1, 2, 3

**描述：** 创建工具类型常量定义

- [ ] **Step 1: 编写 AgentConstants**

```java
package com.hify.agent.constant;

public class AgentConstants {

    private AgentConstants() {}

    public static final String TOOL_TYPE_BUILTIN = "builtin";
    public static final String TOOL_TYPE_MCP = "mcp";
}
```

- [ ] **Step 2: 提交**

```bash
git add hify-module-agent/src/main/java/com/hify/agent/constant/AgentConstants.java
git commit -m "feat(agent): add tool type constants"
```

---

## Task 5: 创建 AgentMapper 接口

**类型：** 结构变更

**文件：**
- 创建：`hify-module-agent/src/main/java/com/hify/agent/mapper/AgentMapper.java`

**依赖：** Task 1

**描述：** 创建 AgentMapper 接口，继承 MyBatis-Plus BaseMapper

- [ ] **Step 1: 编写 AgentMapper**

```java
package com.hify.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.agent.entity.Agent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentMapper extends BaseMapper<Agent> {
}
```

- [ ] **Step 2: 提交**

```bash
git add hify-module-agent/src/main/java/com/hify/agent/mapper/AgentMapper.java
git commit -m "feat(agent): create AgentMapper interface"
```

---

## Task 6: 创建 AgentToolMapper 接口

**类型：** 结构变更

**文件：**
- 创建：`hify-module-agent/src/main/java/com/hify/agent/mapper/AgentToolMapper.java`

**依赖：** Task 2

**描述：** 创建 AgentToolMapper 接口

- [ ] **Step 1: 编写 AgentToolMapper**

```java
package com.hify.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.agent.entity.AgentTool;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentToolMapper extends BaseMapper<AgentTool> {
}
```

- [ ] **Step 2: 提交**

```bash
git add hify-module-agent/src/main/java/com/hify/agent/mapper/AgentToolMapper.java
git commit -m "feat(agent): create AgentToolMapper interface"
```

---

## Task 7: 创建 AgentMcpBindingMapper 接口

**类型：** 结构变更

**文件：**
- 创建：`hify-module-agent/src/main/java/com/hify/agent/mapper/AgentMcpBindingMapper.java`

**依赖：** Task 3

**描述：** 创建 AgentMcpBindingMapper 接口

- [ ] **Step 1: 编写 AgentMcpBindingMapper**

```java
package com.hify.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.agent.entity.AgentMcpBinding;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentMcpBindingMapper extends BaseMapper<AgentMcpBinding> {
}
```

- [ ] **Step 2: 提交**

```bash
git add hify-module-agent/src/main/java/com/hify/agent/mapper/AgentMcpBindingMapper.java
git commit -m "feat(agent): create AgentMcpBindingMapper interface"
```

---

## Task 8: 创建 DTO 类

**类型：** 结构变更

**文件：**
- 创建：`hify-module-agent/src/main/java/com/hify/agent/api/dto/AgentDTO.java`
- 创建：`hify-module-agent/src/main/java/com/hify/agent/api/dto/AgentToolDTO.java`
- 创建：`hify-module-agent/src/main/java/com/hify/agent/dto/AgentCreateRequest.java`
- 创建：`hify-module-agent/src/main/java/com/hify/agent/dto/AgentUpdateRequest.java`
- 创建：`hify-module-agent/src/main/java/com/hify/agent/dto/AgentToolBatchRequest.java`

**依赖：** Task 1, 2, 4

**描述：** 创建所有 DTO 和 Request 类

- [ ] **Step 1: 编写 AgentDTO**

```java
package com.hify.agent.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AgentDTO implements Serializable {
    private Long id;
    private String name;
    private String description;
    private String modelId;
    private String systemPrompt;
    private BigDecimal temperature;
    private Integer maxTokens;
    private BigDecimal topP;
    private String welcomeMessage;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private List<AgentToolDTO> tools;
}
```

- [ ] **Step 2: 编写 AgentToolDTO**

```java
package com.hify.agent.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.Map;

@Data
public class AgentToolDTO implements Serializable {
    private Long id;
    private String toolName;
    private String toolType;
    private String toolImpl;
    private Map<String, Object> configJson;
    private Boolean enabled;
    private Integer sortOrder;
}
```

- [ ] **Step 3: 编写 AgentCreateRequest**

```java
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
}
```

- [ ] **Step 4: 编写 AgentUpdateRequest**

```java
package com.hify.agent.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AgentUpdateRequest {
    private String name;
    private String description;
    private String modelId;
    private String systemPrompt;
    private BigDecimal temperature;
    private Integer maxTokens;
    private BigDecimal topP;
    private String welcomeMessage;
    private Boolean enabled;
}
```

- [ ] **Step 5: 编写 AgentToolBatchRequest**

```java
package com.hify.agent.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AgentToolBatchRequest {
    private List<ToolItem> tools;
    
    @Data
    public static class ToolItem {
        private String toolName;
        private String toolType;
        private String toolImpl;
        private Map<String, Object> configJson;
        private Boolean enabled = true;
        private Integer sortOrder = 0;
    }
}
```

- [ ] **Step 6: 提交**

```bash
git add hify-module-agent/src/main/java/com/hify/agent/api/dto/AgentDTO.java \
        hify-module-agent/src/main/java/com/hify/agent/api/dto/AgentToolDTO.java \
        hify-module-agent/src/main/java/com/hify/agent/dto/AgentCreateRequest.java \
        hify-module-agent/src/main/java/com/hify/agent/dto/AgentUpdateRequest.java \
        hify-module-agent/src/main/java/com/hify/agent/dto/AgentToolBatchRequest.java
git commit -m "feat(agent): create DTO and Request classes"
```

---

## Task 9: 创建 VO 类

**类型：** 结构变更

**文件：**
- 创建：`hify-module-agent/src/main/java/com/hify/agent/vo/AgentVO.java`
- 创建：`hify-module-agent/src/main/java/com/hify/agent/vo/AgentToolVO.java`

**依赖：** Task 1, 2

**描述：** 创建 VO 类用于详情响应

- [ ] **Step 1: 编写 AgentVO**

```java
package com.hify.agent.vo;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AgentVO implements Serializable {
    private Long id;
    private String name;
    private String description;
    private String modelId;
    private String systemPrompt;
    private BigDecimal temperature;
    private Integer maxTokens;
    private BigDecimal topP;
    private String welcomeMessage;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AgentToolVO> tools;
}
```

- [ ] **Step 2: 编写 AgentToolVO**

```java
package com.hify.agent.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.Map;

@Data
public class AgentToolVO implements Serializable {
    private Long id;
    private String toolName;
    private String toolType;
    private String toolImpl;
    private Map<String, Object> configJson;
    private Boolean enabled;
    private Integer sortOrder;
}
```

- [ ] **Step 3: 提交**

```bash
git add hify-module-agent/src/main/java/com/hify/agent/vo/AgentVO.java \
        hify-module-agent/src/main/java/com/hify/agent/vo/AgentToolVO.java
git commit -m "feat(agent): create VO classes"
```

---

## Task 10: 创建 AgentService 接口和实现

**类型：** 单元测试 TDD

**文件：**
- 创建：`hify-module-agent/src/main/java/com/hify/agent/service/AgentService.java`
- 创建：`hify-module-agent/src/main/java/com/hify/agent/service/impl/AgentServiceImpl.java`
- 创建：`hify-module-agent/src/test/java/com/hify/agent/service/impl/AgentServiceImplTest.java`

**依赖：** Task 5, 6, 7, 8, 9

**描述：** 创建 AgentService 接口和实现类，包含 CRUD 和工具绑定管理核心逻辑

- [ ] **Step 1: 编写 AgentService 接口**

```java
package com.hify.agent.service;

import com.hify.agent.dto.AgentCreateRequest;
import com.hify.agent.dto.AgentToolBatchRequest;
import com.hify.agent.dto.AgentUpdateRequest;
import com.hify.agent.vo.AgentVO;
import com.hify.common.web.entity.PageParam;
import com.hify.common.web.entity.PageResult;

public interface AgentService {
    
    Long createAgent(AgentCreateRequest request);
    
    void updateAgent(Long id, AgentUpdateRequest request);
    
    void deleteAgent(Long id);
    
    AgentVO getAgentDetail(Long id);
    
    PageResult<AgentVO> pageAgents(PageParam pageParam);
    
    void bindTools(Long agentId, AgentToolBatchRequest request);
    
    void replaceTools(Long agentId, AgentToolBatchRequest request);
    
    void unbindTool(Long agentId, Long toolId);
}
```

- [ ] **Step 2: 编写 AgentServiceImpl 骨架**

```java
package com.hify.agent.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hify.agent.entity.Agent;
import com.hify.agent.entity.AgentTool;
import com.hify.agent.mapper.AgentMapper;
import com.hify.agent.mapper.AgentToolMapper;
import com.hify.agent.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class AgentServiceImpl extends ServiceImpl<AgentMapper, Agent> implements AgentService {

    private final AgentToolMapper agentToolMapper;
    
    @Override
    public Long createAgent(AgentCreateRequest request) {
        // TODO: 实现
        return null;
    }
    
    @Override
    public void updateAgent(Long id, AgentUpdateRequest request) {
        // TODO: 实现
    }
    
    @Override
    public void deleteAgent(Long id) {
        // TODO: 实现
    }
    
    @Override
    @Transactional(readOnly = true)
    public AgentVO getAgentDetail(Long id) {
        // TODO: 实现
        return null;
    }
    
    @Override
    @Transactional(readOnly = true)
    public PageResult<AgentVO> pageAgents(PageParam pageParam) {
        // TODO: 实现
        return null;
    }
    
    @Override
    public void bindTools(Long agentId, AgentToolBatchRequest request) {
        // TODO: 实现
    }
    
    @Override
    public void replaceTools(Long agentId, AgentToolBatchRequest request) {
        // TODO: 实现
    }
    
    @Override
    public void unbindTool(Long agentId, Long toolId) {
        // TODO: 实现
    }
}
```

- [ ] **Step 3: 编写单元测试（覆盖核心分支）**

```java
package com.hify.agent.service.impl;

import com.hify.agent.dto.AgentCreateRequest;
import com.hify.agent.dto.AgentUpdateRequest;
import com.hify.agent.vo.AgentVO;
import com.hify.common.core.enums.ResultCode;
import com.hify.common.core.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentServiceImplTest {

    @Mock
    private AgentMapper agentMapper;
    
    @Mock
    private AgentToolMapper agentToolMapper;
    
    @InjectMocks
    private AgentServiceImpl agentService;

    @Test
    void shouldCreateAgent_whenGivenValidRequest() {
        AgentCreateRequest request = new AgentCreateRequest();
        request.setName("TestAgent");
        request.setModelId("gpt-4o");
        request.setTemperature(new BigDecimal("0.7"));
        
        when(agentMapper.insert(any(Agent.class))).thenAnswer(invocation -> {
            Agent agent = invocation.getArgument(0);
            return 1L;
        });
        
        Long id = agentService.createAgent(request);
        
        assertThat(id).isNotNull();
        verify(agentMapper).insert(any(Agent.class));
    }

    @Test
    void shouldThrowException_whenDeleteNonExistingAgent() {
        when(agentMapper.selectById(999L)).thenReturn(null);
        
        assertThatThrownBy(() -> agentService.deleteAgent(999L))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ResultCode.DATA_NOT_FOUND.getCode()));
    }

    @Test
    void shouldThrowException_whenUpdateNonExistingAgent() {
        AgentUpdateRequest request = new AgentUpdateRequest();
        request.setName("UpdatedAgent");
        
        when(agentMapper.selectById(999L)).thenReturn(null);
        
        assertThatThrownBy(() -> agentService.updateAgent(999L, request))
                .isInstanceOf(BizException.class);
    }
}
```

- [ ] **Step 4: 实现完整 AgentServiceImpl**

根据测试需求实现所有方法，确保：
- 创建时验证 modelId 存在
- 更新/删除时验证 Agent 存在
- 删除时级联软删除 agent_tool 和 agent_mcp_binding
- 工具绑定/替换/删除逻辑

- [ ] **Step 5: 运行测试确认通过**

运行：`mvn test -Dtest=AgentServiceImplTest -pl hify-module-agent`
预期：PASS

- [ ] **Step 6: 提交**

```bash
git add hify-module-agent/src/main/java/com/hify/agent/service/AgentService.java \
        hify-module-agent/src/main/java/com/hify/agent/service/impl/AgentServiceImpl.java \
        hify-module-agent/src/test/java/com/hify/agent/service/impl/AgentServiceImplTest.java
git commit -m "feat(agent): implement AgentService with CRUD and tool binding"
```

---

## Task 11: 创建 AgentApi 接口

**类型：** 结构变更

**文件：**
- 创建：`hify-module-agent/src/main/java/com/hify/agent/api/AgentApi.java`

**依赖：** Task 10

**描述：** 创建 AgentApi 接口供其他模块调用

- [ ] **Step 1: 编写 AgentApi 接口**

```java
package com.hify.agent.api;

import com.hify.agent.api.dto.AgentDTO;

import java.util.List;

public interface AgentApi {
    
    AgentDTO getAgentById(Long id);
    
    List<AgentDTO> listEnabledAgents();
}
```

- [ ] **Step 2: 在 AgentServiceImpl 中实现 AgentApi**

```java
// 在 AgentServiceImpl 中添加实现
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class AgentServiceImpl 
    extends ServiceImpl<AgentMapper, Agent> 
    implements AgentService, AgentApi {
    
    // ... 现有方法 ...
    
    @Override
    @Transactional(readOnly = true)
    public AgentDTO getAgentById(Long id) {
        // 实现
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AgentDTO> listEnabledAgents() {
        // 实现
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add hify-module-agent/src/main/java/com/hify/agent/api/AgentApi.java
git add hify-module-agent/src/main/java/com/hify/agent/service/impl/AgentServiceImpl.java
git commit -m "feat(agent): add AgentApi interface for cross-module calls"
```

---

## Task 12: 创建 AgentController

**类型：** 结构变更

**文件：**
- 创建：`hify-module-agent/src/main/java/com/hify/agent/controller/AgentController.java`

**依赖：** Task 10, 11

**描述：** 创建 AgentController 实现 HTTP 接口

- [ ] **Step 1: 编写 AgentController**

```java
package com.hify.agent.controller;

import com.hify.agent.dto.AgentCreateRequest;
import com.hify.agent.dto.AgentToolBatchRequest;
import com.hify.agent.dto.AgentUpdateRequest;
import com.hify.agent.service.AgentService;
import com.hify.agent.vo.AgentVO;
import com.hify.common.web.entity.PageParam;
import com.hify.common.web.entity.PageResult;
import com.hify.common.web.entity.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @PostMapping
    public Result<Long> create(@Valid @RequestBody AgentCreateRequest request) {
        return Result.success(agentService.createAgent(request));
    }

    @GetMapping("/{id}")
    public Result<AgentVO> detail(@PathVariable Long id) {
        return Result.success(agentService.getAgentDetail(id));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody AgentUpdateRequest request) {
        agentService.updateAgent(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        agentService.deleteAgent(id);
        return Result.success();
    }

    @GetMapping
    public Result<PageResult<AgentVO>> page(PageParam pageParam) {
        return Result.success(agentService.pageAgents(pageParam));
    }

    @GetMapping("/{id}/tools")
    public Result<Void> getTools(@PathVariable Long id) {
        // TODO: 后续实现
        return Result.success();
    }

    @PostMapping("/{id}/tools")
    public Result<Void> bindTools(@PathVariable Long id, @RequestBody AgentToolBatchRequest request) {
        agentService.bindTools(id, request);
        return Result.success();
    }

    @PutMapping("/{id}/tools")
    public Result<Void> replaceTools(@PathVariable Long id, @RequestBody AgentToolBatchRequest request) {
        agentService.replaceTools(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}/tools/{toolId}")
    public Result<Void> unbindTool(@PathVariable Long id, @PathVariable Long toolId) {
        agentService.unbindTool(id, toolId);
        return Result.success();
    }

    @GetMapping("/{id}/mcp-servers")
    public Result<Void> getMcpServers(@PathVariable Long id) {
        // TODO: MCP 后续实现
        return Result.success();
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add hify-module-agent/src/main/java/com/hify/agent/controller/AgentController.java
git commit -m "feat(agent): add AgentController with HTTP endpoints"
```

---

## Task 13: 集成测试 - AgentMapperIT

**类型：** 集成测试 TDD

**文件：**
- 创建：`hify-module-agent/src/test/java/com/hify/agent/mapper/AgentMapperIT.java`
- 创建：`hify-module-agent/src/test/java/com/hify/agent/AgentTestApplication.java`
- 创建：`hify-module-agent/src/test/resources/sql/agent-test-data.sql`

**依赖：** Task 5, 12

**描述：** 为 AgentMapper 编写集成测试

- [ ] **Step 1: 创建测试数据库初始化脚本**

```sql
-- hify-module-agent/src/test/resources/sql/agent-test-data.sql
TRUNCATE TABLE agent CASCADE;
TRUNCATE TABLE agent_tool CASCADE;
TRUNCATE TABLE agent_mcp_binding CASCADE;

INSERT INTO agent (id, name, description, model_id, system_prompt, temperature, max_tokens, top_p, enabled, created_at, updated_at, deleted)
VALUES 
(1, 'TestAgent1', 'Description1', 'gpt-4o', 'You are helpful.', 0.7, 2048, 1.0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
(2, 'TestAgent2', 'Description2', 'gpt-4o-mini', 'You are smart.', 0.5, 1024, 0.9, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false);

INSERT INTO agent_tool (id, agent_id, tool_name, tool_type, tool_impl, config_json, enabled, sort_order, created_at, updated_at, deleted)
VALUES (1, 1, 'web_search', 'builtin', 'WEB_SEARCH', '{}', true, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false);
```

- [ ] **Step 2: 编写 AgentTestApplication**

```java
package com.hify.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AgentTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgentTestApplication.class, args);
    }
}
```

- [ ] **Step 3: 编写 AgentMapperIT**

```java
package com.hify.agent.mapper;

import com.hify.agent.AgentTestApplication;
import com.hify.agent.entity.Agent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AgentTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
@Sql(scripts = "/sql/agent-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AgentMapperIT {

    @Autowired
    private AgentMapper agentMapper;

    @Test
    void shouldSelectAllEnabledAgents() {
        List<Agent> agents = agentMapper.selectList(null);
        assertThat(agents).hasSize(2);
    }

    @Test
    void shouldSelectAgentById() {
        Agent agent = agentMapper.selectById(1L);
        assertThat(agent).isNotNull();
        assertThat(agent.getName()).isEqualTo("TestAgent1");
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

运行：`mvn test -Dtest=AgentMapperIT -pl hify-module-agent`
预期：PASS

- [ ] **Step 5: 提交**

```bash
git add hify-module-agent/src/test/java/com/hify/agent/mapper/AgentMapperIT.java \
        hify-module-agent/src/test/java/com/hify/agent/AgentTestApplication.java \
        hify-module-agent/src/test/resources/sql/agent-test-data.sql
git commit -m "test(agent): add AgentMapper integration tests"
```

---

## Task 14: 集成测试 - AgentServiceImplIT

**类型：** 集成测试 TDD

**文件：**
- 创建：`hify-module-agent/src/test/java/com/hify/agent/service/impl/AgentServiceImplIT.java`

**依赖：** Task 10, 13

**描述：** 为 AgentServiceImpl 编写集成测试，验证级联删除等事务行为

- [ ] **Step 1: 编写 AgentServiceImplIT**

```java
package com.hify.agent.service.impl;

import com.hify.agent.AgentTestApplication;
import com.hify.agent.dto.AgentCreateRequest;
import com.hify.agent.entity.Agent;
import com.hify.agent.entity.AgentTool;
import com.hify.agent.mapper.AgentMapper;
import com.hify.agent.mapper.AgentToolMapper;
import com.hify.agent.service.AgentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AgentTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
@Sql(scripts = "/sql/agent-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AgentServiceImplIT {

    @Autowired
    private AgentService agentService;

    @Autowired
    private AgentMapper agentMapper;

    @Autowired
    private AgentToolMapper agentToolMapper;

    @Test
    void shouldCascadeDeleteAgentTools_whenDeleteAgent() {
        agentService.deleteAgent(1L);
        
        Agent deletedAgent = agentMapper.selectById(1L);
        assertThat(deletedAgent).isNull();
        
        AgentTool remainingTool = agentToolMapper.selectById(1L);
        assertThat(remainingTool).isNull();
    }

    @Test
    void shouldCreateAgentWithTools_whenGivenValidRequest() {
        AgentCreateRequest request = new AgentCreateRequest();
        request.setName("NewAgent");
        request.setModelId("gpt-4o");
        request.setTemperature(new BigDecimal("0.8"));
        
        Long id = agentService.createAgent(request);
        
        assertThat(id).isNotNull();
        Agent agent = agentMapper.selectById(id);
        assertThat(agent).isNotNull();
        assertThat(agent.getName()).isEqualTo("NewAgent");
    }
}
```

- [ ] **Step 2: 运行测试确认通过**

运行：`mvn test -Dtest=AgentServiceImplIT -pl hify-module-agent`
预期：PASS

- [ ] **Step 3: 提交**

```bash
git add hify-module-agent/src/test/java/com/hify/agent/service/impl/AgentServiceImplIT.java
git commit -m "test(agent): add AgentService integration tests"
```

---

## 任务依赖图

```
Task 1 ─┬─→ Task 4 ─┬─→ Task 8 ─┬─→ Task 10 ─┬─→ Task 11 ─┬─→ Task 12
Task 2 ─┤            └─→ Task 9 ─┘            │
Task 3 ─┘                                     │
Task 5 ─┬─→ Task 6 ─┬─→ Task 7 ─┘            │
Task 6 ─┘                                     │
Task 7 ─┘                                     │
                                               │
Task 8 ─┘                                     │
Task 9 ─┘                                     │
                                               │
                              Task 10 ─┬─→ Task 13 ─┬─→ Task 14
                              Task 12 ─┘            │
                              Task 11 ─┘            │
```

**并行度：** Task 1-9 可部分并行（entity/mapper/dto/vo 互相独立）
