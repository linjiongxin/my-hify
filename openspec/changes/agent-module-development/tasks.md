# Agent 模块任务清单

> 变更：agent-module-development
> 创建时间：2026-04-20

---

## 任务列表

### Phase 1：基础设施

- [ ] 创建 Agent 实体类 `Agent.java`
- [ ] 创建 AgentTool 实体类 `AgentTool.java`
- [ ] 创建 AgentMcpBinding 实体类 `AgentMcpBinding.java`
- [ ] 创建 AgentConstants 常量类

### Phase 2：Mapper 层

- [ ] 创建 AgentMapper.xml（分页查询 + 工具列表查询）
- [ ] 创建 AgentToolMapper（CRUD）
- [ ] 创建 AgentMcpBindingMapper（CRUD）

### Phase 3：DTO & VO

- [ ] 创建 AgentDTO（跨模块传输）
- [ ] 创建 AgentToolDTO
- [ ] 创建 AgentVO（详情响应）
- [ ] 创建 AgentToolVO
- [ ] 创建 AgentCreateRequest
- [ ] 创建 AgentUpdateRequest
- [ ] 创建 AgentToolBatchRequest

### Phase 4：Service 层

- [ ] 创建 AgentService 接口
- [ ] 实现 AgentServiceImpl（CRUD + 工具绑定管理）
- [ ] 实现删除级联（agent_tool、agent_mcp_binding 软删除）

### Phase 5：API 层

- [ ] 创建 AgentApi 接口（getAgentById、listEnabledAgents）
- [ ] 实现 AgentServiceImpl 实现 AgentApi

### Phase 6：Controller 层

- [ ] 创建 AgentController（Agent CRUD + 工具绑定接口）
- [ ] 实现 MCP 绑定预留接口（空实现，TODO 标记）

### Phase 7：测试

- [ ] 单元测试：AgentServiceImplTest（覆盖率 ≥ 80%）
- [ ] 集成测试：AgentMapperIT
- [ ] 集成测试：AgentServiceImplIT

---

## 任务顺序

```
Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5 → Phase 6 → Phase 7
```

**Phase 1-3 可并行开发（entity/mapper/dto 互相独立）**
