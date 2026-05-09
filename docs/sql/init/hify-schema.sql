-- ========================================
-- Hify 数据库初始化脚本（PostgreSQL）
-- ========================================

-- 1. 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. 创建自动更新触发器函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ========================================
-- 系统模块（前缀: sys_）
-- ========================================

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(32) NOT NULL UNIQUE,
    password VARCHAR(128) NOT NULL,
    nickname VARCHAR(32),
    email VARCHAR(64),
    avatar VARCHAR(256),
    status SMALLINT DEFAULT 1,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TRIGGER update_sys_user_updated_at
    BEFORE UPDATE ON sys_user
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX idx_sys_user_status ON sys_user(status);

-- ========================================
-- 模型模块（前缀: model_）
-- ========================================

CREATE TABLE IF NOT EXISTS model_provider (
    id BIGINT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(32) NOT NULL,
    protocol_type VARCHAR(32) DEFAULT 'openai_compatible',
    api_base_url VARCHAR(256) NOT NULL,
    auth_type VARCHAR(32) DEFAULT 'BEARER',
    api_key VARCHAR(512),
    auth_config JSONB DEFAULT '{}',
    enabled BOOLEAN DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TRIGGER update_model_provider_updated_at
    BEFORE UPDATE ON model_provider
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE UNIQUE INDEX uk_model_provider_code ON model_provider(code) WHERE deleted = FALSE;

CREATE TABLE IF NOT EXISTS model_config (
    id BIGINT PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    model_id VARCHAR(64) NOT NULL,
    max_tokens INT DEFAULT 4096,
    context_window INT DEFAULT 8192,
    capabilities JSONB DEFAULT '{"chat":true,"streaming":true,"vision":false,"toolCalling":false,"reasoning":false,"jsonMode":false}',
    input_price_per_1m DECIMAL(10,6),
    output_price_per_1m DECIMAL(10,6),
    default_model BOOLEAN DEFAULT FALSE,
    enabled BOOLEAN DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TRIGGER update_model_config_updated_at
    BEFORE UPDATE ON model_config
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX idx_model_config_provider_id ON model_config(provider_id);
CREATE UNIQUE INDEX uk_model_config_model_id ON model_config(model_id) WHERE deleted = FALSE;
CREATE INDEX idx_model_config_capabilities ON model_config USING GIN (capabilities);

CREATE TABLE IF NOT EXISTS model_provider_status (
    provider_id BIGINT PRIMARY KEY REFERENCES model_provider(id) ON DELETE CASCADE,
    health_status VARCHAR(16) DEFAULT 'unknown',
    health_checked_at TIMESTAMP,
    health_latency_ms INTEGER,
    health_error_msg TEXT,
    total_requests BIGINT DEFAULT 0,
    failed_requests BIGINT DEFAULT 0,
    last_error_at TIMESTAMP,
    last_error_code VARCHAR(64),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER update_model_provider_status_updated_at
    BEFORE UPDATE ON model_provider_status
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX idx_provider_status_health ON model_provider_status(health_status);

-- ========================================
-- Agent 模块（前缀: agent_）
-- ========================================

CREATE TABLE IF NOT EXISTS agent (
    id BIGINT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    description TEXT,
    model_id VARCHAR(64) NOT NULL,
    system_prompt TEXT,
    temperature DECIMAL(3,2) DEFAULT 0.7,
    max_tokens INT DEFAULT 2048,
    top_p DECIMAL(3,2) DEFAULT 1.0,
    welcome_message TEXT,
    enabled BOOLEAN DEFAULT TRUE,
    workflow_id BIGINT,
    execution_mode VARCHAR(20) DEFAULT 'react',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TRIGGER update_agent_updated_at
    BEFORE UPDATE ON agent
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX idx_agent_model_id ON agent(model_id);
CREATE INDEX idx_agent_enabled ON agent(enabled);

CREATE TABLE IF NOT EXISTS agent_tool (
    id BIGINT PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    tool_name VARCHAR(64) NOT NULL,
    tool_type VARCHAR(32) NOT NULL,
    tool_impl VARCHAR(128),
    config_json JSONB,
    enabled BOOLEAN DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TRIGGER update_agent_tool_updated_at
    BEFORE UPDATE ON agent_tool
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX idx_agent_tool_agent_id ON agent_tool(agent_id);

-- Agent × 知识库绑定表
CREATE TABLE IF NOT EXISTS agent_knowledge_base (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    kb_id BIGINT NOT NULL,
    top_k INT NOT NULL DEFAULT 5,
    similarity_threshold DECIMAL(3,2) NOT NULL DEFAULT 0.7,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    updated_by BIGINT,

    CONSTRAINT fk_akb_agent FOREIGN KEY (agent_id) REFERENCES agent(id),
    CONSTRAINT uk_akb_agent_kb UNIQUE (agent_id, kb_id)
);

CREATE TRIGGER update_agent_knowledge_base_updated_at
    BEFORE UPDATE ON agent_knowledge_base
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX idx_agent_knowledge_base_agent_id ON agent_knowledge_base(agent_id);
CREATE INDEX idx_agent_knowledge_base_kb_id ON agent_knowledge_base(kb_id);

-- ========================================
-- MCP 绑定模块
-- ========================================

CREATE TABLE IF NOT EXISTS agent_mcp_binding (
    id BIGINT PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    mcp_server_id BIGINT NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE INDEX idx_agent_mcp_binding_agent_id ON agent_mcp_binding(agent_id);

-- ========================================
-- 对话模块（前缀: chat_）
-- ========================================

CREATE TABLE IF NOT EXISTS chat_session (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    agent_id BIGINT,
    title VARCHAR(200),
    model_id VARCHAR(64),
    status VARCHAR(20) DEFAULT 'active',
    message_count INT DEFAULT 0,
    last_message_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TRIGGER update_chat_session_updated_at
    BEFORE UPDATE ON chat_session
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX idx_chat_session_user_id ON chat_session(user_id);
CREATE INDEX idx_chat_session_agent_id ON chat_session(agent_id);
CREATE INDEX idx_chat_session_last_message_at ON chat_session(last_message_at DESC);

CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    seq INT NOT NULL DEFAULT 0,
    role VARCHAR(20) NOT NULL,
    content TEXT,
    status VARCHAR(20) DEFAULT 'streaming',
    finish_reason VARCHAR(20),
    duration_ms INT,
    input_tokens INT,
    output_tokens INT,
    model VARCHAR(100),
    metadata JSONB,
    trace_id VARCHAR(32),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TRIGGER update_chat_message_updated_at
    BEFORE UPDATE ON chat_message
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX idx_chat_message_session_id ON chat_message(session_id);
CREATE INDEX idx_chat_message_seq ON chat_message(session_id, seq);
CREATE INDEX idx_chat_message_created_at ON chat_message(created_at);
CREATE INDEX idx_chat_message_trace_id ON chat_message(trace_id);
CREATE INDEX gin_chat_message_metadata ON chat_message USING GIN (metadata);

-- ========================================
-- RAG 模块（知识库 + 文档分块）
-- ========================================

CREATE TABLE IF NOT EXISTS knowledge_base (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    embedding_model VARCHAR(64) NOT NULL DEFAULT 'text-embedding-v2',
    chunk_size INT NOT NULL DEFAULT 512,
    chunk_overlap INT NOT NULL DEFAULT 50,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TRIGGER update_knowledge_base_updated_at
    BEFORE UPDATE ON knowledge_base
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE UNIQUE INDEX uk_knowledge_base_name ON knowledge_base(name);
CREATE INDEX idx_knowledge_base_created_by ON knowledge_base(created_by);

CREATE TABLE IF NOT EXISTS document (
    id BIGSERIAL PRIMARY KEY,
    kb_id BIGINT NOT NULL,
    file_name VARCHAR(256) NOT NULL,
    file_type VARCHAR(32) NOT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    file_path VARCHAR(512),
    status VARCHAR(16) NOT NULL DEFAULT 'pending',
    parsed_content TEXT,
    total_chunks INT NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    updated_by BIGINT,

    CONSTRAINT fk_document_kb FOREIGN KEY (kb_id) REFERENCES knowledge_base(id),
    CONSTRAINT chk_document_status CHECK (status IN ('pending', 'processing', 'completed', 'failed'))
);

CREATE TRIGGER update_document_updated_at
    BEFORE UPDATE ON document
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX idx_document_kb_id ON document(kb_id);
CREATE INDEX idx_document_status ON document(status);
CREATE INDEX idx_document_created_by ON document(created_by);

CREATE TABLE IF NOT EXISTS document_chunk (
    id BIGSERIAL PRIMARY KEY,
    kb_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    embedding VECTOR(768),
    chunk_index INT NOT NULL DEFAULT 0,
    meta_json JSONB,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    updated_by BIGINT,

    CONSTRAINT fk_chunk_kb FOREIGN KEY (kb_id) REFERENCES knowledge_base(id),
    CONSTRAINT fk_chunk_document FOREIGN KEY (document_id) REFERENCES document(id)
);

CREATE TRIGGER update_document_chunk_updated_at
    BEFORE UPDATE ON document_chunk
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX idx_document_chunk_kb_id ON document_chunk(kb_id);
CREATE INDEX idx_document_chunk_document_id ON document_chunk(document_id);
CREATE INDEX idx_document_chunk_enabled ON document_chunk(enabled);
CREATE INDEX idx_document_chunk_meta ON document_chunk USING gin (meta_json);
CREATE INDEX vec_document_chunk_embedding ON document_chunk USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- ========================================
-- RAG 检索日志
-- ========================================

CREATE TABLE IF NOT EXISTS rag_retrieval_log (
    id BIGSERIAL PRIMARY KEY,
    kb_id BIGINT,
    query TEXT,
    result_count INT,
    top_chunks JSONB,
    duration_ms INT,
    trace_id VARCHAR(32),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TRIGGER update_rag_retrieval_log_updated_at
    BEFORE UPDATE ON rag_retrieval_log
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX idx_rag_log_trace_id ON rag_retrieval_log(trace_id);

-- ========================================
-- 工作流模块（前缀: workflow_）
-- ========================================

CREATE TABLE IF NOT EXISTS workflow (
    id BIGINT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    description TEXT,
    status VARCHAR(16) DEFAULT 'draft',
    version INTEGER DEFAULT 1,
    retry_config TEXT,
    config TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TRIGGER update_workflow_updated_at
    BEFORE UPDATE ON workflow
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE IF NOT EXISTS workflow_node (
    id BIGINT PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    node_id VARCHAR(64) NOT NULL,
    type VARCHAR(32) NOT NULL,
    name VARCHAR(64) NOT NULL,
    config TEXT,
    position_x INTEGER,
    position_y INTEGER,
    retry_config TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TRIGGER update_workflow_node_updated_at
    BEFORE UPDATE ON workflow_node
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX idx_workflow_node_workflow_id ON workflow_node(workflow_id);

CREATE TABLE IF NOT EXISTS workflow_edge (
    id BIGINT PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    source_node VARCHAR(64) NOT NULL,
    target_node VARCHAR(64) NOT NULL,
    condition VARCHAR(256),
    edge_index INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TRIGGER update_workflow_edge_updated_at
    BEFORE UPDATE ON workflow_edge
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX idx_workflow_edge_workflow_id ON workflow_edge(workflow_id);

CREATE TABLE IF NOT EXISTS workflow_instance (
    id BIGINT PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    status VARCHAR(16) DEFAULT 'running',
    current_node_id VARCHAR(64),
    context JSONB,
    error_msg TEXT,
    trace_id VARCHAR(32),
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TRIGGER update_workflow_instance_updated_at
    BEFORE UPDATE ON workflow_instance
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX idx_workflow_instance_workflow_id ON workflow_instance(workflow_id);
CREATE INDEX idx_workflow_instance_status ON workflow_instance(status);
CREATE INDEX idx_workflow_instance_started_at ON workflow_instance(started_at);
CREATE INDEX idx_wf_instance_trace_id ON workflow_instance(trace_id);

CREATE TABLE IF NOT EXISTS workflow_node_execution (
    id BIGINT PRIMARY KEY,
    execution_id BIGINT NOT NULL,
    node_id VARCHAR(64) NOT NULL,
    node_type VARCHAR(32) NOT NULL,
    status VARCHAR(16) DEFAULT 'running',
    input_json JSONB,
    output_json JSONB,
    error_msg TEXT,
    trace_id VARCHAR(32),
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TRIGGER update_workflow_node_execution_updated_at
    BEFORE UPDATE ON workflow_node_execution
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX idx_workflow_node_execution_execution_id ON workflow_node_execution(execution_id);
CREATE INDEX idx_workflow_node_execution_node_id ON workflow_node_execution(node_id);
CREATE INDEX idx_wf_node_exec_trace_id ON workflow_node_execution(trace_id);

-- ========================================
-- MCP 模块（前缀: mcp_）
-- ========================================

CREATE TABLE IF NOT EXISTS mcp_server (
    id BIGINT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(32) NOT NULL UNIQUE,
    transport_type VARCHAR(16) NOT NULL,
    base_url VARCHAR(256),
    command VARCHAR(256),
    args_json JSONB,
    env_json JSONB,
    enabled BOOLEAN DEFAULT TRUE,
    status VARCHAR(16) DEFAULT 'active',
    last_heartbeat_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TRIGGER update_mcp_server_updated_at
    BEFORE UPDATE ON mcp_server
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX idx_mcp_server_status ON mcp_server(status);

CREATE TABLE IF NOT EXISTS mcp_tool (
    id BIGINT PRIMARY KEY,
    server_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    description TEXT,
    schema_json JSONB NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TRIGGER update_mcp_tool_updated_at
    BEFORE UPDATE ON mcp_tool
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX idx_mcp_tool_server_id ON mcp_tool(server_id);

-- ========================================
-- MCP 调用日志
-- ========================================

CREATE TABLE IF NOT EXISTS mcp_call_log (
    id BIGINT PRIMARY KEY,
    server_url VARCHAR(256) NOT NULL,
    tool_name VARCHAR(64) NOT NULL,
    request_json JSONB,
    response_json JSONB,
    status VARCHAR(16) NOT NULL,
    duration_ms INT,
    error_msg TEXT,
    trace_id VARCHAR(32),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TRIGGER update_mcp_call_log_updated_at
    BEFORE UPDATE ON mcp_call_log
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX idx_mcp_call_log_server_url ON mcp_call_log(server_url);
CREATE INDEX idx_mcp_call_log_tool_name ON mcp_call_log(tool_name);
CREATE INDEX idx_mcp_call_log_status ON mcp_call_log(status);
CREATE INDEX idx_mcp_call_log_created_at ON mcp_call_log(created_at);

-- ========================================
-- 初始数据
-- ========================================

-- 管理员用户（密码: admin123）
INSERT INTO sys_user (id, username, password, nickname, email, status)
VALUES (1, 'admin', '$2a$10$Vxa61cLimMWxsa5gaFe7IOThDczdH0KpNGxTPBD9pfYK7hWtuTzvS', '管理员', 'admin@hify.local', 1)
ON CONFLICT (username) DO NOTHING;

-- 测试用户（密码: test123）
INSERT INTO sys_user (id, username, password, nickname, email, status)
VALUES (2, 'test', '$2a$10$Vxa61cLimMWxsa5gaFe7IOThDczdH0KpNGxTPBD9pfYK7hWtuTzvS', '测试用户', 'test@hify.local', 1)
ON CONFLICT (username) DO NOTHING;

-- 开发用户（密码: dev123）
INSERT INTO sys_user (id, username, password, nickname, email, status)
VALUES (3, 'dev', '$2a$10$Vxa61cLimMWxsa5gaFe7IOThDczdH0KpNGxTPBD9pfYK7hWtuTzvS', '开发人员', 'dev@hify.local', 1)
ON CONFLICT (username) DO NOTHING;

-- 模型提供商
INSERT INTO model_provider (id, name, code, protocol_type, api_base_url, auth_type, api_key, auth_config, enabled, sort_order)
VALUES
(1, 'OpenAI', 'openai', 'openai_compatible', 'https://api.openai.com/v1', 'BEARER', '', '{}', true, 1),
(2, 'DeepSeek', 'deepseek', 'openai_compatible', 'https://api.deepseek.com/v1', 'BEARER', '', '{}', true, 2),
(3, 'Ollama', 'ollama', 'openai_compatible', 'http://localhost:11434/v1', 'NONE', '', '{}', true, 99),
(4, '通义千问', 'qwen', 'openai_compatible', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 'BEARER', '', '{}', true, 3),
(5, 'Azure OpenAI', 'azure_openai', 'openai_compatible', 'https://your-resource.openai.azure.com/openai/deployments', 'API_KEY', '', '{"headerName":"api-key","prefix":""}', true, 4),
(6, 'Claude (Anthropic)', 'anthropic', 'anthropic', 'https://api.anthropic.com', 'API_KEY', '', '{"headerName":"x-api-key","prefix":""}', true, 5),
(7, 'MiniMax', 'minimax', 'openai_compatible', 'https://api.minimax.io/v1', 'BEARER', '', '{}', true, 6);

-- 模型
INSERT INTO model_config (id, provider_id, name, model_id, max_tokens, context_window, capabilities, default_model, enabled)
VALUES
(1, 1, 'GPT-4o', 'gpt-4o', 4096, 128000, '{"chat":true,"streaming":true,"vision":true,"toolCalling":true,"reasoning":false,"jsonMode":true}', true, true),
(2, 1, 'GPT-4o Mini', 'gpt-4o-mini', 4096, 128000, '{"chat":true,"streaming":true,"vision":true,"toolCalling":true,"reasoning":false,"jsonMode":true}', false, true),
(3, 1, 'o3 Mini', 'o3-mini', 4096, 200000, '{"chat":true,"streaming":true,"vision":false,"toolCalling":true,"reasoning":true,"jsonMode":true}', false, true),
(4, 2, 'DeepSeek V3', 'deepseek-chat', 8192, 64000, '{"chat":true,"streaming":true,"vision":false,"toolCalling":true,"reasoning":false,"jsonMode":true}', true, true),
(5, 2, 'DeepSeek R1', 'deepseek-reasoner', 8192, 64000, '{"chat":true,"streaming":true,"vision":false,"toolCalling":true,"reasoning":true,"jsonMode":true}', false, true),
(6, 3, 'Llama 3.1', 'llama3.1', 4096, 128000, '{"chat":true,"streaming":true,"vision":false,"toolCalling":false,"reasoning":false,"jsonMode":false}', true, true),
(7, 4, 'Qwen2.5-72B', 'qwen2.5-72b-instruct', 8192, 131072, '{"chat":true,"streaming":true,"vision":false,"toolCalling":true,"reasoning":false,"jsonMode":true}', true, true),
(8, 4, 'Qwen-VL-Max', 'qwen-vl-max', 2048, 32768, '{"chat":true,"streaming":true,"vision":true,"toolCalling":true,"reasoning":false,"jsonMode":true}', false, true),
(9, 5, 'GPT-4o (Azure)', 'gpt-4o-azure', 4096, 128000, '{"chat":true,"streaming":true,"vision":true,"toolCalling":true,"reasoning":false,"jsonMode":true}', true, true),
(10, 6, 'Claude Sonnet 4.6', 'claude-sonnet-4-6', 8192, 200000, '{"chat":true,"streaming":true,"vision":true,"toolCalling":true,"reasoning":true,"jsonMode":true}', true, true),
(11, 6, 'Claude Opus 4.7', 'claude-opus-4-7', 8192, 200000, '{"chat":true,"streaming":true,"vision":true,"toolCalling":true,"reasoning":true,"jsonMode":true}', false, true),
(12, 7, 'MiniMax-M2.7', 'MiniMax-M2.7', 8192, 204800, '{"chat":true,"streaming":true,"vision":false,"toolCalling":true,"reasoning":true,"jsonMode":true}', true, true),
(13, 7, 'MiniMax-M2.5', 'MiniMax-M2.5', 8192, 204800, '{"chat":true,"streaming":true,"vision":false,"toolCalling":true,"reasoning":true,"jsonMode":true}', false, true),
(14, 7, 'MiniMax-M2.1', 'MiniMax-M2.1', 8192, 204800, '{"chat":true,"streaming":true,"vision":false,"toolCalling":true,"reasoning":false,"jsonMode":true}', false, true);

-- 验证安装
SELECT 'pgvector version: ' || extversion AS info
FROM pg_extension
WHERE extname = 'vector';
