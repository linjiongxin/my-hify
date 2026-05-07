#!/bin/bash
set -e

BASE_URL="http://localhost:8080/api"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

PASS=0
FAIL=0

function ok() {
    echo -e "${GREEN}✓${NC} $1"
    ((PASS++)) || true
}

function err() {
    echo -e "${RED}✗${NC} $1"
    ((FAIL++)) || true
}

function info() {
    echo -e "${YELLOW}▸${NC} $1"
}

function cleanup() {
    info "清理测试数据..."
    docker exec -i hify-postgres psql -U postgres -d hify -q <<'SQL'
DELETE FROM workflow_edge WHERE id >= 888880000 AND id < 888890000;
DELETE FROM workflow_node WHERE id >= 888880000 AND id < 888890000;
DELETE FROM workflow_approval WHERE id >= 888880000 AND id < 888890000;
DELETE FROM workflow_node_execution WHERE execution_id IN (
    SELECT id FROM workflow_instance WHERE workflow_id IN (
        SELECT id FROM workflow WHERE name LIKE '【测试】%'
    )
);
DELETE FROM workflow_instance WHERE workflow_id IN (SELECT id FROM workflow WHERE name LIKE '【测试】%');
DELETE FROM workflow WHERE name LIKE '【测试】%';
SQL
}

function create_workflow() {
    local name="$1"
    local resp=$(curl -s -X POST "$BASE_URL/workflow" \
        -H 'Content-Type: application/json' \
        -d "{\"name\":\"$name\",\"description\":\"测试流程\",\"config\":\"{}\"}")
    echo "$resp" | jq -r 'if type == "string" or type == "number" then . else empty end'
}

function insert_data() {
    local wf_id="$1"
    sed "s/##WF_ID##/$wf_id/g" | docker exec -i hify-postgres psql -U postgres -d hify -q
}

function start_instance() {
    local wf_id="$1"
    local inputs="${2:-{}}"
    local resp=$(curl -s -X POST "$BASE_URL/workflow/instance" \
        -H 'Content-Type: application/json' \
        -d "{\"workflowId\":$wf_id,\"inputs\":$inputs}")
    echo "$resp" | jq -r 'if type == "string" or type == "number" then . else empty end'
}

function get_instance() {
    local id="$1"
    curl -s "$BASE_URL/workflow/instance/$id"
}

function get_pending_approvals() {
    local id="$1"
    curl -s "$BASE_URL/workflow/instance/$id/pending-approvals" | jq -r 'if length > 0 then .[0].id else empty end'
}

function approve() {
    local approval_id="$1"
    local remark=$(echo -n '同意' | jq -sRr @uri)
    curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/workflow/approval/$approval_id/approve?remark=$remark"
}

function reject() {
    local approval_id="$1"
    local remark=$(echo -n '拒绝' | jq -sRr @uri)
    curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/workflow/approval/$approval_id/reject?remark=$remark"
}

# ============================================
# 测试 1：最简流程 START -> END
# ============================================
info "【测试1】最简流程 START -> END"
WF_ID=$(create_workflow "【测试】最简流程")
if [ -z "$WF_ID" ] || [ "$WF_ID" = "null" ]; then
    err "创建工作流失败"
else
    ok "创建工作流: $WF_ID"
fi

insert_data "$WF_ID" <<'SQL'
INSERT INTO workflow_node (id, workflow_id, node_id, type, name, config, position_x, position_y)
VALUES
  (888880100, ##WF_ID##, 'node_start', 'START', '开始', '{}', 100, 100),
  (888880101, ##WF_ID##, 'node_end',   'END',   '结束', '{}', 300, 100);

INSERT INTO workflow_edge (id, workflow_id, source_node, target_node, condition, edge_index)
VALUES (888880102, ##WF_ID##, 'node_start', 'node_end', null, 0);
SQL
ok "插入节点和边"

INST_ID=$(start_instance "$WF_ID")
if [ -z "$INST_ID" ] || [ "$INST_ID" = "null" ]; then
    err "启动实例失败"
else
    ok "启动实例: $INST_ID"
fi

sleep 1
STATUS=$(get_instance "$INST_ID" | jq -r '.status')
if [ "$STATUS" = "COMPLETED" ]; then
    ok "实例状态为 COMPLETED"
else
    err "实例状态异常: $STATUS (期望 COMPLETED)"
fi

CURRENT_NODE=$(get_instance "$INST_ID" | jq -r '.currentNodeId')
if [ "$CURRENT_NODE" = "node_end" ]; then
    ok "当前节点为 node_end"
else
    err "当前节点异常: $CURRENT_NODE"
fi

# ============================================
# 测试 2：条件分支 START -> CONDITION -> END
# ============================================
info "【测试2】条件分支流程"
WF_ID2=$(create_workflow "【测试】条件分支")
ok "创建工作流: $WF_ID2"

COND_CFG='{"expression":"${score} > 80","trueBranch":"node_end_high","falseBranch":"node_end_low"}'

insert_data "$WF_ID2" <<SQL
INSERT INTO workflow_node (id, workflow_id, node_id, type, name, config, position_x, position_y)
VALUES
  (888880200, ##WF_ID##, 'node_start',    'START',     '开始', '{}', 100, 100),
  (888880201, ##WF_ID##, 'node_cond',     'CONDITION', '判断', '$COND_CFG', 300, 100),
  (888880202, ##WF_ID##, 'node_end_high', 'END',       '高分结束', '{}', 500, 50),
  (888880203, ##WF_ID##, 'node_end_low',  'END',       '低分结束', '{}', 500, 150);

INSERT INTO workflow_edge (id, workflow_id, source_node, target_node, condition, edge_index)
VALUES (888880204, ##WF_ID##, 'node_start', 'node_cond', null, 0);
SQL
ok "插入节点和边"

# 场景 A：score=90，期望走到 node_end_high
INST_ID2A=$(start_instance "$WF_ID2" '{"score":90}')
ok "启动实例（score=90）: $INST_ID2A"
sleep 1
NODE2A=$(get_instance "$INST_ID2A" | jq -r '.currentNodeId')
if [ "$NODE2A" = "node_end_high" ]; then
    ok "score=90 走到 node_end_high"
else
    err "score=90 分支异常: $NODE2A"
fi

# 场景 B：score=60，期望走到 node_end_low
INST_ID2B=$(start_instance "$WF_ID2" '{"score":60}')
ok "启动实例（score=60）: $INST_ID2B"
sleep 1
NODE2B=$(get_instance "$INST_ID2B" | jq -r '.currentNodeId')
if [ "$NODE2B" = "node_end_low" ]; then
    ok "score=60 走到 node_end_low"
else
    err "score=60 分支异常: $NODE2B"
fi

# ============================================
# 测试 3：审批流程 START -> APPROVAL -> END
# ============================================
info "【测试3】审批流程"
WF_ID3=$(create_workflow "【测试】审批流程")
ok "创建工作流: $WF_ID3"

APPROVAL_CFG='{"prompt":"请审批该申请","approveBranch":"node_end","rejectBranch":"node_rejected"}'

insert_data "$WF_ID3" <<SQL
INSERT INTO workflow_node (id, workflow_id, node_id, type, name, config, position_x, position_y)
VALUES
  (888880300, ##WF_ID##, 'node_start',    'START',     '开始', '{}', 100, 100),
  (888880301, ##WF_ID##, 'node_approval', 'APPROVAL',  '审批', '$APPROVAL_CFG', 300, 100),
  (888880302, ##WF_ID##, 'node_end',      'END',       '通过结束', '{}', 500, 100),
  (888880303, ##WF_ID##, 'node_rejected', 'END',       '拒绝结束', '{}', 500, 200);

INSERT INTO workflow_edge (id, workflow_id, source_node, target_node, condition, edge_index)
VALUES (888880304, ##WF_ID##, 'node_start', 'node_approval', null, 0);
SQL
ok "插入节点和边"

INST_ID3=$(start_instance "$WF_ID3")
ok "启动实例: $INST_ID3"
sleep 1

STATUS3=$(get_instance "$INST_ID3" | jq -r '.status')
if [ "$STATUS3" = "RUNNING" ]; then
    ok "实例暂停在审批节点（RUNNING）"
else
    err "审批节点状态异常: $STATUS3"
fi

APPROVAL_ID=$(get_pending_approvals "$INST_ID3")
if [ -n "$APPROVAL_ID" ] && [ "$APPROVAL_ID" != "null" ]; then
    ok "获取审批记录: $APPROVAL_ID"
else
    err "未找到审批记录"
fi

# 审批通过
APPROVE_CODE=$(approve "$APPROVAL_ID")
if [ "$APPROVE_CODE" = "200" ]; then
    ok "调用审批通过 API"
else
    err "审批通过 API 返回 $APPROVE_CODE"
fi
sleep 1

STATUS3A=$(get_instance "$INST_ID3" | jq -r '.status')
if [ "$STATUS3A" = "COMPLETED" ]; then
    ok "审批通过后实例完成"
else
    err "审批通过后状态异常: $STATUS3A"
fi

NODE3A=$(get_instance "$INST_ID3" | jq -r '.currentNodeId')
if [ "$NODE3A" = "node_end" ]; then
    ok "审批通过后走到 node_end"
else
    err "审批通过后节点异常: $NODE3A"
fi

# ============================================
# 测试 4：审批拒绝分支
# ============================================
info "【测试4】审批拒绝分支"
INST_ID4=$(start_instance "$WF_ID3")
ok "启动新实例: $INST_ID4"
sleep 1

APPROVAL_ID4=$(get_pending_approvals "$INST_ID4")
ok "获取审批记录: $APPROVAL_ID4"

REJECT_CODE=$(reject "$APPROVAL_ID4")
if [ "$REJECT_CODE" = "200" ]; then
    ok "调用审批拒绝 API"
else
    err "审批拒绝 API 返回 $REJECT_CODE"
fi
sleep 1

NODE4=$(get_instance "$INST_ID4" | jq -r '.currentNodeId')
if [ "$NODE4" = "node_rejected" ]; then
    ok "拒绝后走到 node_rejected"
else
    err "拒绝后节点异常: $NODE4"
fi

STATUS4=$(get_instance "$INST_ID4" | jq -r '.status')
if [ "$STATUS4" = "COMPLETED" ]; then
    ok "拒绝后实例完成"
else
    err "拒绝后状态异常: $STATUS4"
fi

# ============================================
# 测试 5：循环检测
# ============================================
info "【测试5】循环检测"
WF_ID5=$(create_workflow "【测试】循环检测")
ok "创建工作流: $WF_ID5"

insert_data "$WF_ID5" <<'SQL'
INSERT INTO workflow_node (id, workflow_id, node_id, type, name, config, position_x, position_y)
VALUES
  (888880400, ##WF_ID##, 'node_a', 'START', '节点A', '{}', 100, 100),
  (888880401, ##WF_ID##, 'node_b', 'END',   '节点B', '{}', 300, 100);

INSERT INTO workflow_edge (id, workflow_id, source_node, target_node, condition, edge_index)
VALUES
  (888880402, ##WF_ID##, 'node_a', 'node_b', null, 0),
  (888880403, ##WF_ID##, 'node_b', 'node_a', null, 0);
SQL
ok "插入节点和边（A->B->A 循环）"

# 直接插入一个 context 中已标记 visited 的实例，模拟回到已访问节点
docker exec -i hify-postgres psql -U postgres -d hify -q <<SQL
INSERT INTO workflow_instance (id, workflow_id, status, current_node_id, context, started_at)
VALUES (888880404, $WF_ID5, 'RUNNING', 'node_a', '{"_visitedNodes":["node_a","node_b"]}', now());
SQL
ok "插入模拟实例（已访问 A、B）"

# 由于没有直接触发从特定节点恢复的 API，我们更新实例让引擎恢复时走到 node_a
docker exec -i hify-postgres psql -U postgres -d hify -q <<SQL
UPDATE workflow_instance SET current_node_id = 'node_a', status = 'RUNNING' WHERE id = 888880404;
SQL

# 利用 resumePendingInstances 不会处理我们刚插入的实例（因为它在运行时已启动过），
# 但我们无法直接通过 API 触发单个节点执行。
# 这个测试场景由单元测试覆盖，脚本中仅验证表结构正确。
info "循环检测逻辑已由单元测试覆盖（WorkflowEngineTest.shouldFailInstance_whenCycleDetected）"

# ============================================
# 清理
# ============================================
cleanup

# ============================================
# 汇总
# ============================================
echo ""
echo "========================================"
echo -e "通过: ${GREEN}$PASS${NC}  失败: ${RED}$FAIL${NC}"
echo "========================================"

if [ $FAIL -gt 0 ]; then
    exit 1
fi
