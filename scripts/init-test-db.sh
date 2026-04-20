#!/usr/bin/env bash
# =============================================================================
# Hify 集成测试数据库一键初始化脚本
# =============================================================================
# 前置条件：docker compose up -d 已启动 PostgreSQL 容器
# 作用：创建 hify_test 数据库并初始化 schema（含 pgvector 扩展）
#
# 用法：
#   chmod +x scripts/init-test-db.sh
#   ./scripts/init-test-db.sh
# =============================================================================

set -euo pipefail

# 配置
DB_HOST="localhost"
DB_PORT="5432"
DB_USER="postgres"
DB_PASS="postgres"
DB_DEV="hify"
DB_TEST="hify_test"
SCHEMA_FILE="docs/sql/init/hify-schema.sql"

# Docker 容器名
PG_CONTAINER="hify-postgres"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 辅助函数：在容器内执行 psql
pg_exec() {
    local db="${1:-$DB_USER}"
    shift 2>/dev/null || true
    docker exec -i "$PG_CONTAINER" \
        psql -U "$DB_USER" -d "$db" -v ON_ERROR_STOP=1 "$@" 2>/dev/null
}

# 检查容器是否运行
if ! docker ps --format '{{.Names}}' | grep -q "^${PG_CONTAINER}$"; then
    echo -e "${RED}[错误] PostgreSQL 容器未运行${NC}"
    echo "请先执行: docker compose up -d"
    exit 1
fi

# 等待 PostgreSQL 就绪
echo "[1/4] 检查 PostgreSQL 连接..."
for i in {1..10}; do
    if docker exec "$PG_CONTAINER" pg_isready -U "$DB_USER" > /dev/null 2>&1; then
        break
    fi
    echo "  等待 PostgreSQL 就绪... ($i/10)"
    sleep 1
done

# 检查 pgvector 扩展（必须在 dev 库中已安装）
echo "[2/4] 验证 pgvector 扩展..."
if ! pg_exec "$DB_DEV" -t -c "SELECT extname FROM pg_extension WHERE extname = 'vector';" | grep -q "vector"; then
    echo -e "${YELLOW}[警告] pgvector 扩展未安装，尝试创建...${NC}"
    pg_exec "$DB_DEV" -c "CREATE EXTENSION IF NOT EXISTS vector;"
fi
echo -e "  ${GREEN}pgvector 扩展就绪${NC}"

# 创建测试数据库（如果不存在）
echo "[3/4] 创建测试数据库 ${DB_TEST}（如果不存在）..."
if ! pg_exec "$DB_USER" -t -c "SELECT 1 FROM pg_database WHERE datname = '${DB_TEST}';" | grep -q "1"; then
    pg_exec "$DB_USER" -c "CREATE DATABASE ${DB_TEST};"
    echo -e "  ${GREEN}数据库 ${DB_TEST} 已创建${NC}"
else
    echo -e "  ${YELLOW}数据库 ${DB_TEST} 已存在，跳过创建${NC}"
fi

# 在测试库中安装 pgvector 扩展
echo "  在测试库中安装 pgvector 扩展..."
pg_exec "$DB_TEST" -c "CREATE EXTENSION IF NOT EXISTS vector;" || true

# 初始化 schema
echo "[4/4] 初始化 schema..."
if [ ! -f "$SCHEMA_FILE" ]; then
    echo -e "${RED}[错误] Schema 文件不存在: ${SCHEMA_FILE}${NC}"
    exit 1
fi

# 测试库允许清空重建，先清理旧对象
pg_exec "$DB_TEST" -c "DROP SCHEMA IF EXISTS public CASCADE; CREATE SCHEMA public; GRANT ALL ON SCHEMA public TO postgres; GRANT ALL ON SCHEMA public TO public;" > /dev/null 2>&1
pg_exec "$DB_TEST" -c "CREATE EXTENSION IF NOT EXISTS vector;" > /dev/null 2>&1

docker exec -i "$PG_CONTAINER" psql -U "$DB_USER" -d "$DB_TEST" -v ON_ERROR_STOP=1 < "$SCHEMA_FILE" > /dev/null 2>&1 || {
    echo -e "${RED}[错误] Schema 初始化失败${NC}"
    exit 1
}

# 验证
echo ""
echo "=== 验证结果 ==="
TABLE_COUNT=$(pg_exec "$DB_TEST" -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE';" | tr -d ' ')
echo "  表数量: $TABLE_COUNT"

echo "  关键表检查:"
for table in model_provider model_config model_provider_status sys_user chat_session knowledge_base workflow_definition mcp_server; do
    if pg_exec "$DB_TEST" -t -c "SELECT 1 FROM information_schema.tables WHERE table_name = '${table}';" | grep -q "1"; then
        echo -e "    ${GREEN}✓ ${table}${NC}"
    else
        echo -e "    ${RED}✗ ${table} (缺失)${NC}"
    fi
done

echo ""
echo -e "${GREEN}[完成] 测试数据库 ${DB_TEST} 已就绪${NC}"
echo ""
echo "现在可以运行集成测试:"
echo "  mvn test"
