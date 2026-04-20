-- model 模块集成测试数据
-- 每次测试前由 @Sql 注入，测试后由 @Transactional 回滚

TRUNCATE TABLE model_provider CASCADE;
TRUNCATE TABLE model_config CASCADE;
TRUNCATE TABLE model_provider_status CASCADE;

-- 插入 2 个 provider
INSERT INTO model_provider (id, name, code, protocol_type, api_base_url, auth_type, api_key, auth_config, enabled, sort_order, created_at, updated_at, deleted)
VALUES
    (1, 'OpenAI', 'openai', 'openai_compatible', 'https://api.openai.com/v1', 'BEARER', 'sk-test', '{}', TRUE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
    (2, 'DeepSeek', 'deepseek', 'openai_compatible', 'https://api.deepseek.com/v1', 'BEARER', 'sk-test', '{}', FALSE, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE);

-- 插入 2 个 model_config（provider_id=1）
INSERT INTO model_config (id, provider_id, name, model_id, max_tokens, context_window, capabilities, input_price_per_1m, output_price_per_1m, default_model, enabled, sort_order, created_at, updated_at, deleted)
VALUES
    (10, 1, 'GPT-4o', 'gpt-4o', 4096, 8192, '{"chat":true,"streaming":true}', 5.00, 15.00, TRUE, TRUE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
    (11, 1, 'GPT-4o Mini', 'gpt-4o-mini', 4096, 8192, '{"chat":true,"streaming":true}', 0.15, 0.60, FALSE, TRUE, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE);

-- 插入 1 个 provider_status（与 enabled provider 关联）
INSERT INTO model_provider_status (provider_id, health_status, health_checked_at, health_latency_ms, total_requests, failed_requests, updated_at)
VALUES
    (1, 'healthy', CURRENT_TIMESTAMP, 120, 1000, 10, CURRENT_TIMESTAMP);
