import { test, expect, Page } from '@playwright/test'

const API_BASE = 'http://localhost:8080/api'
const UI_BASE = 'http://localhost:5173'
const AGENT_ID = '2052677690619441154'

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

test.describe('工作流对话 E2E 测试', () => {
  test('用户说订单号，工作流提取并走查询分支', async ({ page }) => {
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

    // 在输入框中输入消息
    const input = page.locator('.chat-input-area textarea')
    await input.fill('订单号是dt123')

    // 点击发送按钮
    await page.locator('.chat-input-area button:has-text("发送")').click()

    // 等待用户消息出现在聊天区域
    await expect(page.locator('.message-bubble.user')).toContainText('订单号是dt123', { timeout: 10000 })

    // 等待 assistant 回复出现（workflow 模式会轮询，可能需要一些时间）
    const assistantBubble = page.locator('.message-bubble.assistant').last()
    await expect(assistantBubble).not.toBeEmpty({ timeout: 35000 })

    // 获取回复文本
    const replyText = await assistantBubble.textContent()
    console.log('Workflow reply:', replyText)

    // 验证回复不为空且不是错误提示
    expect(replyText).toBeTruthy()
    expect(replyText).not.toContain('WORKFLOW_ERROR')
    expect(replyText).not.toContain('WORKFLOW_FAILED')
  })

  test('用户没提供订单号，走缺少订单号分支', async ({ page }) => {
    await loginAndNavigate(page, `${UI_BASE}/chat`)

    await createSessionViaAPI(page, AGENT_ID)

    await page.goto(`${UI_BASE}/chat`)
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(1000)

    const sessionItem = page.locator('.session-list .session-item').first()
    await sessionItem.click()
    await page.waitForTimeout(500)

    const input = page.locator('.chat-input-area textarea')
    await input.fill('我想查订单')

    await page.locator('.chat-input-area button:has-text("发送")').click()

    await expect(page.locator('.message-bubble.user')).toContainText('我想查订单', { timeout: 10000 })

    const assistantBubble = page.locator('.message-bubble.assistant').last()
    await expect(assistantBubble).not.toBeEmpty({ timeout: 35000 })

    const replyText = await assistantBubble.textContent()
    console.log('Workflow reply (no order):', replyText)

    expect(replyText).toBeTruthy()
    expect(replyText).not.toContain('WORKFLOW_ERROR')
    expect(replyText).not.toContain('WORKFLOW_FAILED')
  })
})
