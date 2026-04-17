#!/usr/bin/env bash
set -euo pipefail

# Hify 一键启动脚本
# 注意：本项目的后端依赖是 PostgreSQL 和 Redis（非 MySQL），定义在项目 CLAUDE.md 中

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HEALTH_URL="http://localhost:8080/api/actuator/health"
MAX_WAIT_S=120
POLL_INTERVAL_S=2

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

cleanup() {
    if [[ "${START_SUCCESS:-}" == "true" ]]; then
        return 0
    fi
    log_warn "启动流程异常中断，正在清理..."
    if [[ -n "${BACKEND_PID:-}" ]] && kill -0 "${BACKEND_PID}" 2>/dev/null; then
        kill "${BACKEND_PID}" 2>/dev/null || true
        wait "${BACKEND_PID}" 2>/dev/null || true
    fi
    if [[ -n "${FRONTEND_PID:-}" ]] && kill -0 "${FRONTEND_PID}" 2>/dev/null; then
        kill "${FRONTEND_PID}" 2>/dev/null || true
        wait "${FRONTEND_PID}" 2>/dev/null || true
    fi
    :
}
trap cleanup EXIT INT TERM

# 检查必要命令
for cmd in mvn npm curl java nc; do
    if ! command -v "${cmd}" &>/dev/null; then
        log_error "缺少必要命令: ${cmd}"
        exit 1
    fi
done

# 1. 检查后端的依赖服务（PostgreSQL 和 Redis）
log_info "检查后端的依赖服务 (PostgreSQL & Redis)..."

try_start_container() {
    local svc=$1
    local port=$2
    if nc -z localhost "${port}" >/dev/null 2>&1; then
        log_info "${svc} 端口 ${port} 已可用，跳过容器启动"
        return 0
    fi
    if command -v docker &>/dev/null; then
        if ! docker ps --filter "name=hify-${svc,,}" --format '{{.Names}}' | grep -q .; then
            log_warn "${svc} 容器未运行，尝试启动..."
            if (cd "${PROJECT_ROOT}" && docker compose up -d "${svc,,}"); then
                return 0
            else
                log_warn "Docker 启动 ${svc} 失败，继续尝试等待端口就绪..."
            fi
        fi
    fi
    return 0
}

try_start_container "postgres" 5432
try_start_container "redis" 6379

log_info "等待 PostgreSQL (5432) 和 Redis (6379) 就绪..."
for port in 5432 6379; do
    elapsed=0
    while ! nc -z localhost "${port}" >/dev/null 2>&1; do
        if [[ ${elapsed} -ge 30 ]]; then
            log_error "端口 ${port} 未在 30 秒内就绪"
            exit 1
        fi
        sleep 1
        elapsed=$((elapsed + 1))
    done
    log_info "端口 ${port} 已就绪"
done

# 2. 构建后端
log_info "构建后端 (mvn clean install -DskipTests)..."
cd "${PROJECT_ROOT}"
if ! mvn clean install -DskipTests; then
    log_error "后端构建失败"
    exit 1
fi

SERVER_JAR=$(ls "${PROJECT_ROOT}"/hify-server/target/hify-server-*.jar 2>/dev/null | head -n1 || true)
if [[ -z "${SERVER_JAR}" ]]; then
    log_error "未找到后端 jar 包"
    exit 1
fi

# 3. 后台启动后端
log_info "启动后端服务 (profile: local)..."
cd "${PROJECT_ROOT}/hify-server"
nohup java -jar "${SERVER_JAR}" --spring.profiles.active=local > "${PROJECT_ROOT}/hify-server/backend.log" 2>&1 &
BACKEND_PID=$!
log_info "后端进程 PID: ${BACKEND_PID}"

# 4. 轮询等待健康检查
log_info "轮询等待后端健康检查通过..."
elapsed=0
while ! curl -sf "${HEALTH_URL}" >/dev/null 2>&1; do
    if ! kill -0 "${BACKEND_PID}" 2>/dev/null; then
        log_error "后端进程异常退出"
        log_error "查看日志: ${PROJECT_ROOT}/hify-server/backend.log"
        exit 1
    fi
    if [[ ${elapsed} -ge ${MAX_WAIT_S} ]]; then
        log_error "后端在 ${MAX_WAIT_S} 秒内未通过健康检查"
        log_error "查看日志: ${PROJECT_ROOT}/hify-server/backend.log"
        exit 1
    fi
    sleep "${POLL_INTERVAL_S}"
    elapsed=$((elapsed + POLL_INTERVAL_S))
    echo -n "."
done
echo ""

log_info "后端健康检查通过"

# 5. 后台启动前端开发服务器
log_info "启动前端开发服务器..."
cd "${PROJECT_ROOT}/hify-web"
nohup npm run dev > "${PROJECT_ROOT}/hify-web/frontend.log" 2>&1 &
FRONTEND_PID=$!
log_info "前端进程 PID: ${FRONTEND_PID}"

log_info "所有服务已启动"
START_SUCCESS=true
