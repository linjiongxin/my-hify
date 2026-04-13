#!/usr/bin/env bash
set -euo pipefail

# Hify 优雅停止脚本
# 按 PID 文件查找进程，先 SIGTERM，超时后 SIGKILL

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TIMEOUT_S=15
POLL_INTERVAL_S=0.5

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

stop_process() {
    local name="$1"
    local pid_file="$2"

    if [[ ! -f "${pid_file}" ]]; then
        log_warn "PID 文件不存在: ${pid_file}，跳过 ${name}"
        return 0
    fi

    local pid
    pid="$(cat "${pid_file}")"

    if ! kill -0 "${pid}" 2>/dev/null; then
        log_info "${name} 进程 (PID: ${pid}) 已不存在"
        rm -f "${pid_file}"
        return 0
    fi

    log_info "正在停止 ${name} (PID: ${pid})，发送 SIGTERM..."
    kill -15 "${pid}" 2>/dev/null || true

    local waited=0
    while kill -0 "${pid}" 2>/dev/null; do
        if [[ "$(echo "${waited} >= ${TIMEOUT_S}" | bc)" -eq 1 ]]; then
            log_warn "${name} 在 ${TIMEOUT_S}s 内未退出，发送 SIGKILL..."
            kill -9 "${pid}" 2>/dev/null || true
            sleep "${POLL_INTERVAL_S}"
            break
        fi
        sleep "${POLL_INTERVAL_S}"
        waited="$(echo "${waited} + ${POLL_INTERVAL_S}" | bc)"
    done

    if kill -0 "${pid}" 2>/dev/null; then
        log_error "${name} 进程 (PID: ${pid}) 仍然存活，请手动检查"
        return 1
    else
        log_info "${name} 已停止"
        rm -f "${pid_file}"
    fi
}

# 停止后端
stop_process "后端" "${PROJECT_ROOT}/hify-server/hify-server.pid"

# 停止前端
stop_process "前端" "${PROJECT_ROOT}/hify-web/hify-web.pid"

log_info "所有服务已停止"
