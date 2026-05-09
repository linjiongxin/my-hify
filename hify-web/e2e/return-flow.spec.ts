import { test, expect, Page } from '@playwright/test'

const API_BASE = 'http://localhost:8080/api'
const UI_BASE = 'http://localhost:5173'
const AGENT_ID = '2052677690619441154'
const RETURN_WORKFLOW_ID = '2052993202125647873'

// 获取真实登录 Token
async function getAuthToken(page: Page): Promise<string> {
  const response = await page.request.post(`${API_BASE}/auth/login`, {
    headers: { 'Content-Type': 'application/json' },
    data: {
      username: 'admin',
      password: 'admin123',
    },
  })
  const body = await response.json()
  return body.data.accessToken
}

// 登录并导航到目标页面
async function loginAndNavigate(page: Page, url: string) {
  const token = await getAuthToken(page)
  await page.goto(url)
  await page.waitForLoadState('networkidle')
  if (page.url().includes('/login')) {
    await page.evaluate(({ targetUrl, accessToken }) => {
      localStorage.setItem('token', accessToken)
      window.location.href = targetUrl
    }, { targetUrl: url, accessToken: token })
    await page.waitForLoadState('networkidle')
  }
}

// 通过 API 创建会话
async function createSessionViaAPI(page: Page, agentId: string): Promise<number> {
  const token = await getAuthToken(page)
  const response = await page.request.post(`${API_BASE}/chat/session`, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    data: { agentId: agentId },
  })
  const body = await response.json()
  return body.data.id
}

// 查询最近的工作流实例
async function getLatestWorkflowInstance(page: Page): Promise<any> {
  const token = await getAuthToken(page)
  const response = await page.request.get(
    `${API_BASE}/workflow/instances?page=1&size=1&workflowId=${RETURN_WORKFLOW_ID}`,
    {
      headers: {
        'Authorization': `Bearer ${token}`,
      },
    }
  )
  const body = await response.json()
  return body.data?.records?.[0] || null
}

test.describe('退货全流程 E2E 测试', () => {
  test('用户查询退货流程并提交退货申请', async ({ page }) => {
    test.setTimeout(120000)
    await loginAndNavigate(page, `${UI_BASE}/chat`)

    // 创建会话
    await createSessionViaAPI(page, AGENT_ID)

    // 刷新页面
    await page.goto(`${UI_BASE}/chat`)
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(1000)

    // 点击左侧第一个会话
    const sessionItem = page.locator('.session-list .session-item').first()
    await sessionItem.click()
    await page.waitForTimeout(500)

    // 记录测试开始时间，用于后续验证工作流实例
    const testStartTime = new Date()

    // 第一步：询问退货流程
    const input = page.locator('.chat-input-area textarea')
    await input.fill('怎么退货')

    // 点击发送按钮
    await page.locator('.chat-input-area button:has-text("发送")').click()

    // 等待用户消息出现在聊天区域
    await expect(page.locator('.message-bubble.user')).toContainText('怎么退货', { timeout: 10000 })

    // 等待 assistant 回复出现
    let assistantBubble = page.locator('.message-bubble.assistant').last()
    await expect(assistantBubble).not.toBeEmpty({ timeout: 90000 })

    const replyText1 = await assistantBubble.textContent()
    console.log('Return policy reply:', replyText1)

    // 验证回复不为空且不是错误提示
    expect(replyText1).toBeTruthy()
    expect(replyText1).not.toContain('错误')
    expect(replyText1).not.toContain('失败')

    // 验证 RAG 知识库生效：回复应包含退货流程相关内容
    expect(replyText1).toMatch(/退货|退款|申请/)

    // 第二步：提供订单号和退货原因，请求退货（直接触发 workflow）
    await input.fill('我要退货，订单号是ORD202405090001，原因是商品质量问题')
    await page.locator('.chat-input-area button:has-text("发送")').click()

    // 等待新的用户消息
    await expect(page.locator('.message-bubble.user').last()).toContainText('我要退货', { timeout: 10000 })

    // 等待 assistant 回复（workflow + MCP 工具调用可能需要较长时间）
    assistantBubble = page.locator('.message-bubble.assistant').last()
    await expect(assistantBubble).not.toBeEmpty({ timeout: 90000 })

    const replyText2 = await assistantBubble.textContent()
    console.log('Return application reply:', replyText2)

    // 验证回复包含退货相关信息
    expect(replyText2).toBeTruthy()
    expect(replyText2).not.toContain('WORKFLOW_ERROR')
    expect(replyText2).not.toContain('WORKFLOW_FAILED')

    // 验证回复包含订单号或退款/退货相关信息
    const hasOrderNo = replyText2!.includes('ORD202405090001')
    const hasRefundInfo = replyText2!.includes('退款') || replyText2!.includes('退货') || replyText2!.includes('申请')
    expect(hasOrderNo || hasRefundInfo).toBe(true)

    // 第三步：通过 API 验证工作流确实被执行（核心链路验证）
    const instance = await getLatestWorkflowInstance(page)
    expect(instance).not.toBeNull()
    console.log('Workflow instance:', instance.id, 'status:', instance.status)

    // 验证是本次测试触发的新实例
    const instanceStartTime = new Date(instance.startedAt)
    expect(instanceStartTime.getTime()).toBeGreaterThanOrEqual(testStartTime.getTime())

    // 验证工作流已执行完成
    expect(instance.status).toBe('COMPLETED')

    // 验证工作流访问了关键节点（证明 MCP 工具被调用）
    const context = JSON.parse(instance.context)
    const visitedNodes = context._visitedNodes as string[]
    expect(visitedNodes).toContain('query_order')
    expect(visitedNodes).toContain('apply_refund')

    // 验证订单查询结果已写入上下文（MCP query_order 调用成功）
    expect(context.orderResult).toContain('ORD202405090001')
    expect(context.orderResult).toContain('iPhone 15 Pro Max')

    // 验证退款申请已处理（MCP apply_refund 调用成功，可能是新申请或重复申请提示）
    expect(context.refundResult).toBeTruthy()
    expect(context.refundResult).toContain('ORD202405090001')
  })
})
