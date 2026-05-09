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

// 通过 API 创建 MCP Server
async function createTestMcpServerViaAPI(page: Page, name: string, code: string): Promise<string> {
  const token = await getAuthToken(page)
  const response = await page.request.post(`${API_BASE}/mcp-server`, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    data: {
      name,
      code,
      transportType: 'sse',
      baseUrl: 'http://localhost:3000/sse',
      enabled: true,
    },
  })
  const body = await response.json()
  return body.data
}

// 通过 API 删除 MCP Server
async function deleteTestMcpServerByName(page: Page, name: string): Promise<void> {
  const token = await getAuthToken(page)
  const response = await page.request.get(`${API_BASE}/mcp-server?pageNum=1&pageSize=100`, {
    headers: { 'Authorization': `Bearer ${token}` },
  })
  const data = await response.json()
  const server = data.data?.records?.find((r: any) => r.name === name)
  if (server) {
    await page.request.delete(`${API_BASE}/mcp-server/${server.id}`, {
      headers: { 'Authorization': `Bearer ${token}` },
    })
  }
}

// 刷新页面并等待表格中出现指定文本
async function reloadAndWaitForMcpData(page: Page, text: string): Promise<void> {
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

test.describe('MCP Server 管理 E2E 测试', () => {
  test('1. 页面加载 - MCP Server 列表页正确渲染', async ({ page }) => {
    await loginAndNavigate(page, `${UI_BASE}/mcp-servers`)
    await expect(page.locator('h1.page-title')).toHaveText('MCP Server 管理')
    await expect(page.locator('button:has-text("新增 MCP Server")')).toBeVisible()
    await expect(page.locator('.el-table')).toBeVisible()
  })

  test('2. 创建 SSE 类型的 MCP Server', async ({ page }) => {
    const serverName = `E2E_MCP_${Date.now()}`
    await loginAndNavigate(page, `${UI_BASE}/mcp-servers`)

    // 点击新增
    await page.click('button:has-text("新增 MCP Server")')
    await page.waitForTimeout(500)
    await expect(page.locator('.el-dialog')).toBeVisible()

    // 填写表单
    await page.locator('.el-dialog .el-form-item:has-text("名称") input').fill(serverName)
    await page.locator('.el-dialog .el-form-item:has-text("编码") input').fill(`code_${Date.now()}`)
    await page.locator('.el-dialog .el-form-item:has-text("传输类型") .el-select').click()
    await page.waitForTimeout(200)
    await page.locator('.el-select-dropdown__item:has-text("SSE")').click()
    await page.waitForTimeout(200)
    await page.locator('.el-dialog .el-form-item:has-text("基础 URL") input').fill('http://localhost:3000/sse')

    // 提交
    await page.locator('.el-dialog__footer button.el-button--primary').click()
    await page.waitForTimeout(800)

    // 验证表格中出现
    await reloadAndWaitForMcpData(page, serverName)
    await expect(page.locator(`.el-table__body tr:has-text("${serverName}")`)).toBeVisible()

    // 清理
    await deleteTestMcpServerByName(page, serverName)
  })

  test('3. 编辑 MCP Server', async ({ page }) => {
    const serverName = `E2E_MCP_Edit_${Date.now()}`
    const code = `edit_${Date.now()}`
    await loginAndNavigate(page, `${UI_BASE}/mcp-servers`)

    // 通过 API 创建
    await createTestMcpServerViaAPI(page, serverName, code)
    await reloadAndWaitForMcpData(page, serverName)

    // 点击编辑
    const row = page.locator(`.el-table__body tr:has-text("${serverName}")`)
    await row.locator('button:has-text("编辑")').click()
    await page.waitForTimeout(500)

    // 修改名称
    const nameInput = page.locator('.el-dialog input').first()
    await nameInput.fill(`${serverName}_Updated`)

    // 保存
    await page.locator('.el-dialog__footer button.el-button--primary').click()
    await page.waitForTimeout(800)

    // 验证更新
    await reloadAndWaitForMcpData(page, `${serverName}_Updated`)
    await expect(page.locator(`.el-table__body tr:has-text("${serverName}_Updated")`)).toBeVisible()

    // 清理
    await deleteTestMcpServerByName(page, `${serverName}_Updated`)
  })

  test('4. 删除 MCP Server', async ({ page }) => {
    const serverName = `E2E_MCP_Delete_${Date.now()}`
    const code = `delete_${Date.now()}`
    await loginAndNavigate(page, `${UI_BASE}/mcp-servers`)

    await createTestMcpServerViaAPI(page, serverName, code)
    await reloadAndWaitForMcpData(page, serverName)

    // 点击删除
    const row = page.locator(`.el-table__body tr:has-text("${serverName}")`)
    await row.locator('button:has-text("删除")').click()
    await page.waitForTimeout(300)

    // 确认删除
    await page.locator('.el-message-box__btns button.el-button--primary').click()
    await page.waitForTimeout(800)

    // 验证已删除
    await page.reload()
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(500)
    await expect(page.locator(`.el-table__body tr:has-text("${serverName}")`)).not.toBeVisible()
  })
})
