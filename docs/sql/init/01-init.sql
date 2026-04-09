-- ========================================
-- Hify 数据库初始化脚本
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

-- 3. 创建更新人触发器函数（可选，如果需要自动更新 updated_by）
CREATE OR REPLACE FUNCTION update_updated_by_column()
RETURNS TRIGGER AS $$
BEGIN
    -- 如果支持 session 变量设置当前用户，可以在这里使用
    -- NEW.updated_by = current_setting('app.current_user_id', true)::BIGINT;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 4. 验证安装
SELECT 'pgvector version: ' || extversion AS info
FROM pg_extension
WHERE extname = 'vector';
