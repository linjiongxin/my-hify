-- H2 数据库 Schema（本地开发测试用）
-- 注意：H2 不支持 pgvector 扩展，RAG 向量功能受限

-- 自动更新触发器
CREATE ALIAS IF NOT EXISTS update_updated_at_column AS $$
import java.sql.Connection;
import java.sql.PreparedStatement;
@CODE
public static void update_updated_at_column(Connection conn, String tableName, Long id) throws Exception {
    PreparedStatement stmt = conn.prepareStatement(
        "UPDATE " + tableName + " SET updated_at = CURRENT_TIMESTAMP WHERE id = ?"
    );
    stmt.setLong(1, id);
    stmt.executeUpdate();
}
$$;

-- 示例：模型提供商表
CREATE TABLE IF NOT EXISTS model_provider (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL COMMENT '提供商名称',
    code VARCHAR(32) NOT NULL COMMENT '提供商代码',
    api_base_url VARCHAR(256) NOT NULL COMMENT 'API基础地址',
    api_key_required BOOLEAN DEFAULT TRUE COMMENT '是否需要API Key',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    created_by BIGINT,
    updated_by BIGINT
);

-- 示例：模型表
CREATE TABLE IF NOT EXISTS model (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    provider_id BIGINT NOT NULL COMMENT '提供商ID',
    name VARCHAR(64) NOT NULL COMMENT '模型名称',
    model_id VARCHAR(64) NOT NULL COMMENT '模型标识',
    max_tokens INT DEFAULT 4096 COMMENT '最大Token数',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    created_by BIGINT,
    updated_by BIGINT
);

-- 示例：用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(32) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(128) NOT NULL COMMENT '密码',
    nickname VARCHAR(32) COMMENT '昵称',
    email VARCHAR(64) COMMENT '邮箱',
    status TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    created_by BIGINT,
    updated_by BIGINT
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_model_provider_code ON model_provider(code);
CREATE INDEX IF NOT EXISTS idx_model_provider_id ON model(provider_id);
CREATE INDEX IF NOT EXISTS idx_sys_user_username ON sys_user(username);
