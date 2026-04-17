#!/usr/bin/env bash
set -euo pipefail

# Hify 优雅停止脚本
# 按端口 + 工作目录精确查找进程，先 SIGTERM，超时后 SIGKILL

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TIMEOUT_S=15
POLL_INTERVAL_S=0.5

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { printf "${GREEN}[INFO]${NC} %s\n" "$1"; }
log_warn() { printf "${YELLOW}[WARN]${NC} %s\n" "$1"; }
log_error() { printf "${RED}[ERROR]${NC} %s\n" "$1"; }

get_cwd() {
    local pid="$1"
    if [[ -d "/proc/${pid}" ]]; then
        readlink "/proc/${pid}/cwd" 2>/dev/null || true
    else
        lsof -p "${pid}" 2>/dev/null | awk '$4=="cwd" {print $9}'
    fi
}

find_pids_by_port_and_cwd() {
    local port="$1"
    local expected_cwd="$2"

    lsof -ti :"${port}" 2>/dev/null | while read -r pid; do
        local cwd
        cwd="$(get_cwd "${pid}" 2>/dev/null || true)"
        if [[ "${cwd}" == "${expected_cwd}" ]]; then
            echo "${pid}"
        fi
    done
}

stop_by_pid() {
    local name="$1"
    local pid="$2"

    if ! kill -0 "${pid}" 2>/dev/null; then
        log_info "${name} 进程 (PID: ${pid}) 已不存在"
        return 0
    fi

    log_info "正在停止 ${name} (PID: ${pid})，发送 SIGTERM..."
    kill -15 "${pid}" 2>/dev/null || true

    local waited=0
    while kill -0 "${pid}" 2>/dev/null; do
        if awk "BEGIN {exit !(${waited} >= ${TIMEOUT_S})}"; then
            log_warn "${name} 在 ${TIMEOUT_S}s 内未退出，发送 SIGKILL..."
            kill -9 "${pid}" 2>/dev/null || true
            sleep "${POLL_INTERVAL_S}"
            break
        fi
        sleep "${POLL_INTERVAL_S}"
        waited="$(awk "BEGIN {print ${waited} + ${POLL_INTERVAL_S}}")"
    done

    if kill -0 "${pid}" 2>/dev/null; then
        log_error "${name} 进程 (PID: ${pid}) 仍然存活，请手动检查"
        return 1
    else
        log_info "${name} 已停止"
    fi
}

stop_process() {
    local name="$1"
    local find_cmd="$2"

    local found_pids
    found_pids="$(eval "${find_cmd}" 2>/dev/null || true)"

    if [[ -z "${found_pids}" ]]; then
        log_warn "未找到运行中的 ${name} 进程，跳过"
        return 0
    fi

    while IFS= read -r p; do
        [[ -n "${p}" ]] || continue
        stop_by_pid "${name}" "${p}"
    done <<< "${found_pids}"
}

# 停止后端：端口 8080 + 工作目录验证
stop_process "后端" "find_pids_by_port_and_cwd 8080 '${PROJECT_ROOT}/hify-server'"

# 停止前端：端口 5173 + 工作目录验证
stop_process "前端" "find_pids_by_port_and_cwd 5173 '${PROJECT_ROOT}/hify-web'"

log_info "所有服务已停止"
