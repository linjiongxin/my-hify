-- H2 数据库初始化数据

-- 模型提供商
INSERT INTO model_provider (id, name, code, api_base_url, api_key_required, enabled, sort_order) VALUES
(1, 'OpenAI', 'openai', 'https://api.openai.com/v1', true, true, 1),
(2, 'DeepSeek', 'deepseek', 'https://api.deepseek.com/v1', true, true, 2),
(3, 'Ollama', 'ollama', 'http://localhost:11434', false, true, 99);

-- 模型
INSERT INTO model (id, provider_id, name, model_id, max_tokens, enabled) VALUES
(1, 1, 'GPT-4o', 'gpt-4o', 8192, true),
(2, 1, 'GPT-4o Mini', 'gpt-4o-mini', 8192, true),
(3, 1, 'GPT-3.5 Turbo', 'gpt-3.5-turbo', 4096, true),
(4, 2, 'DeepSeek Chat', 'deepseek-chat', 8192, true),
(5, 2, 'DeepSeek Coder', 'deepseek-coder', 8192, true),
(6, 3, 'Llama 2', 'llama2', 4096, true);

-- 管理员用户（密码: admin123）
INSERT INTO sys_user (id, username, password, nickname, email, status) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', '管理员', 'admin@hify.local', 1);
