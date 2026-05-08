#!/bin/bash
set -e

BASE_URL="http://localhost:8080/api"

echo "========================================"
echo "Agent + Workflow 双模式集成测试"
echo "========================================"

# 0. 登录获取 token
echo ""
echo "[0/7] 登录获取 token"
LOGIN_RESP=$(curl -s -X POST "${BASE_URL}/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')
TOKEN=$(echo "$LOGIN_RESP" | jq -r '.data.accessToken // empty')
if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
    echo "登录失败: $LOGIN_RESP"
    exit 1
fi
echo "登录成功，获取 token"

# 1. 创建一个工作流（退款流程）
echo ""
echo "[1/7] 创建工作流: 退款流程"
WORKFLOW_JSON='{
  "name": "退款流程",
  "description": "用户申请退款后的自动处理流程",
  "config": "{}"
}'
WORKFLOW_RAW=$(curl -s -X POST "${BASE_URL}/workflow" \
  -H "Content-Type: application/json" \
  -d "$WORKFLOW_JSON")
# 工作流创建返回的是裸字符串（带引号），需要去掉引号
WORKFLOW_ID=$(echo "$WORKFLOW_RAW" | sed 's/^"//;s/"$//')

if [ -z "$WORKFLOW_ID" ] || [ "$WORKFLOW_ID" = "null" ]; then
    echo "创建工作流失败，尝试查询已有工作流..."
    WORKFLOW_LIST=$(curl -s "${BASE_URL}/workflow?page=1&pageSize=1")
    WORKFLOW_ID=$(echo "$WORKFLOW_LIST" | jq -r '.data.records[0].id // empty')
    if [ -z "$WORKFLOW_ID" ] || [ "$WORKFLOW_ID" = "null" ]; then
        echo "错误: 无法获取工作流 ID"
        exit 1
    fi
fi
echo "工作流 ID: $WORKFLOW_ID"

# 2. 保存工作流节点（START -> END，无 LLM 调用，适合本地无 API Key 环境）
echo ""
echo "[2/7] 保存工作流节点"
curl -s -X PUT "${BASE_URL}/workflow/${WORKFLOW_ID}/nodes" \
  -H "Content-Type: application/json" \
  -d '[
    {"nodeId":"node_start","type":"START","name":"开始","config":"{}","positionX":100,"positionY":100},
    {"nodeId":"node_end","type":"END","name":"结束","config":"{}","positionX":300,"positionY":100}
  ]' | jq -r '. | length'

# 3. 保存工作流连线
echo ""
echo "[3/7] 保存工作流连线"
curl -s -X PUT "${BASE_URL}/workflow/${WORKFLOW_ID}/edges" \
  -H "Content-Type: application/json" \
  -d '[
    {"sourceNode":"node_start","targetNode":"node_end","condition":null,"edgeIndex":0}
  ]' | jq -r '. | length'

# 4. 创建 workflow 模式 Agent
echo ""
echo "[4/7] 创建 workflow 模式 Agent"
AGENT_JSON=$(cat <<EOF
{
  "name": "退款客服(workflow模式)",
  "modelId": "gpt-4o",
  "systemPrompt": "你是退款客服助手",
  "workflowId": ${WORKFLOW_ID},
  "executionMode": "workflow"
}
EOF
)
AGENT_ID=$(curl -s -X POST "${BASE_URL}/agent" \
  -H "Content-Type: application/json" \
  -d "$AGENT_JSON" | jq -r '.data // empty')

if [ -z "$AGENT_ID" ] || [ "$AGENT_ID" = "null" ]; then
    echo "创建 Agent 失败"
    exit 1
fi
echo "Agent ID: $AGENT_ID"

# 5. 验证 Agent 详情
echo ""
echo "[5/7] 验证 Agent 详情"
AGENT_DETAIL=$(curl -s "${BASE_URL}/agent/${AGENT_ID}")
echo "$AGENT_DETAIL" | jq -r '.data | {id,name,executionMode,workflowId}'

# 6. 创建会话
echo ""
echo "[6/7] 创建会话"
SESSION_JSON=$(cat <<EOF
{
  "agentId": ${AGENT_ID}
}
EOF
)
SESSION_ID=$(curl -s -X POST "${BASE_URL}/chat/session" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d "$SESSION_JSON" | jq -r '.data.id // empty')

if [ -z "$SESSION_ID" ] || [ "$SESSION_ID" = "null" ]; then
    echo "创建会话失败"
    exit 1
fi
echo "会话 ID: $SESSION_ID"

# 7. workflow 模式对话（SSE）
echo ""
echo "[7/7] workflow 模式对话测试"
ENCODED_MSG=$(echo "我要退款" | jq -sRr @uri)
curl -N -s "${BASE_URL}/chat/stream/${SESSION_ID}?message=${ENCODED_MSG}&token=${TOKEN}" 2>&1 | while read -r line; do
    echo "$line"
done

echo ""
echo "========================================"
echo "测试完成"
echo "========================================"
