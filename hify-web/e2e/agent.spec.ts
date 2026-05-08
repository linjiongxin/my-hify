import { test, expect, Page } from '@playwright/test'

const API_BASE = 'http://localhost:8080/api'
const UI_BASE = 'http://localhost:5173'

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

// 通过 API 创建 Agent（使用 Playwright request，不受页面导航影响）
async function createTestAgentViaAPI(page: Page, name: string): Promise<string> {
  const token = await getAuthToken(page)
  const response = await page.request.post(`${API_BASE}/agent`, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    data: {
      name,
      modelId: 'gpt-4o',
      description: 'E2E 测试描述',
      systemPrompt: '你是一个测试助手',
      temperature: 0.7,
      maxTokens: 2048,
      topP: 1.0,
      welcomeMessage: '欢迎',
      enabled: true,
    },
  })
  const body = await response.json()
  return body.data
}

// 通过 API 删除 Agent
async function deleteTestAgentByName(page: Page, name: string): Promise<void> {
  const token = await getAuthToken(page)
  const response = await page.request.get(`${API_BASE}/agent?pageNum=1&pageSize=100`, {
    headers: { 'Authorization': `Bearer ${token}` },
  })
  const data = await response.json()
  const agent = data.data?.records?.find((r: any) => r.name === name)
  if (agent) {
    await page.request.delete(`${API_BASE}/agent/${agent.id}`, {
      headers: { 'Authorization': `Bearer ${token}` },
    })
  }
}

// 刷新页面并等待表格中出现指定文本
async function reloadAndWaitForAgentData(page: Page, text: string): Promise<void> {
  await page.reload()
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(300)

  await page.waitForFunction(
    (t) => {
      const rows = document.querySelectorAll('.el-table__body tr')
      for (const row of rows) {
        if (row.textContent?.includes(t)) {
          return true
        }
      }
      return false
    },
    text,
    { timeout: 15000 }
  )
}

test.describe('Agent 管理 E2E 测试', () => {
  test('1. 页面加载 - Agent 列表页正确渲染', async ({ page }) => {
    await loginAndNavigate(page, `${UI_BASE}/agents`)
    await expect(page.locator('h1.page-title')).toHaveText('Agent 管理')
    await expect(page.locator('button:has-text("新增 Agent")')).toBeVisible()
    await expect(page.locator('.el-table')).toBeVisible()
  })

  test('2. 通过 API 创建 Agent - 然后在 UI 验证', async ({ page }) => {
    const agentName = `E2E_Test_${Date.now()}`
    await loginAndNavigate(page, `${UI_BASE}/agents`)

    // 通过 API 创建
    await createTestAgentViaAPI(page, agentName)

    // 刷新页面并等待表格数据
    await reloadAndWaitForAgentData(page, agentName)

    // 验证 Agent 出现在列表中
    await expect(page.locator(`.el-table__body tr:has-text("${agentName}")`)).toBeVisible()

    // 清理
    await deleteTestAgentByName(page, agentName)
  })

  test('3. 查看 Agent 详情并编辑', async ({ page }) => {
    const agentName = `E2E_Edit_${Date.now()}`
    await loginAndNavigate(page, `${UI_BASE}/agents`)

    const agentId = await createTestAgentViaAPI(page, agentName)
    await reloadAndWaitForAgentData(page, agentName)

    // 点击编辑按钮
    const row = page.locator(`.el-table__body tr:has-text("${agentName}")`)
    await row.locator('button:has-text("编辑")').click()
    await page.waitForTimeout(500)

    // 验证弹窗和表单
    await expect(page.locator('.el-dialog')).toBeVisible()
    await expect(page.locator('.el-dialog__title')).toContainText('编辑')

    // 修改名称
    const nameInput = page.locator('.el-dialog input').first()
    await nameInput.fill(`${agentName}_Updated`)

    // 保存
    await page.locator('.el-dialog__footer button.el-button--primary').click()
    await page.waitForTimeout(500)

    // 验证列表刷新
    await reloadAndWaitForAgentData(page, `${agentName}_Updated`)
    await expect(page.locator(`.el-table__body tr:has-text("${agentName}_Updated")`)).toBeVisible()

    // 清理
    await deleteTestAgentByName(page, `${agentName}_Updated`)
  })

  test('4. 删除 Agent', async ({ page }) => {
    const agentName = `E2E_Delete_${Date.now()}`
    await loginAndNavigate(page, `${UI_BASE}/agents`)

    await createTestAgentViaAPI(page, agentName)
    await reloadAndWaitForAgentData(page, agentName)

    // 点击删除按钮
    const row = page.locator(`.el-table__body tr:has-text("${agentName}")`)
    await row.locator('button:has-text("删除")').click()
    await page.waitForTimeout(300)

    // 确认删除（Element Plus MessageBox）
    await page.locator('.el-message-box__btns button.el-button--primary').click()
    await page.waitForTimeout(500)

    // 验证已删除
    await page.reload()
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(500)
    await expect(page.locator(`.el-table__body tr:has-text("${agentName}")`)).not.toBeVisible()
  })
})

test.describe('Agent 表单验证', () => {
  test('名称为空时显示验证错误', async ({ page }) => {
    await loginAndNavigate(page, `${UI_BASE}/agents`)

    await page.click('button:has-text("新增 Agent")')
    await expect(page.locator('.el-dialog')).toBeVisible()
    await page.waitForTimeout(500)

    // 不填名称，直接提交
    await page.locator('.el-dialog__footer button.el-button--primary').click()
    await page.waitForTimeout(500)

    // 验证有错误提示
    await expect(page.locator('.el-form-item__error').first()).toContainText('请输入名称')
  })
})
