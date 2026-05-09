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

// 通过 API 创建工作流
async function createTestWorkflowViaAPI(page: Page, name: string): Promise<number> {
  const token = await getAuthToken(page)
  const response = await page.request.post(`${API_BASE}/workflow`, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    data: {
      name,
      description: 'E2E 测试工作流',
    },
  })
  const body = await response.json()
  return body.data
}

// 通过 API 保存节点
async function saveWorkflowNodes(page: Page, workflowId: number, nodes: any[]): Promise<void> {
  const token = await getAuthToken(page)
  await page.request.put(`${API_BASE}/workflow/${workflowId}/nodes`, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    data: nodes,
  })
}

// 通过 API 保存连线
async function saveWorkflowEdges(page: Page, workflowId: number, edges: any[]): Promise<void> {
  const token = await getAuthToken(page)
  await page.request.put(`${API_BASE}/workflow/${workflowId}/edges`, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    data: edges,
  })
}

// 通过 API 启动工作流
async function startWorkflowInstance(page: Page, workflowId: number): Promise<string> {
  const token = await getAuthToken(page)
  const response = await page.request.post(`${API_BASE}/workflow/instances`, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    data: {
      workflowId,
      inputs: {},
    },
  })
  const body = await response.json()
  if (!body.data) {
    console.error('Start workflow failed:', body.message, 'code:', body.code, 'workflowId:', workflowId)
  }
  return body.data
}

// 通过 API 删除工作流
async function deleteTestWorkflowByName(page: Page, name: string): Promise<void> {
  const token = await getAuthToken(page)
  const response = await page.request.get(`${API_BASE}/workflow?page=1&pageSize=100`, {
    headers: { 'Authorization': `Bearer ${token}` },
  })
  const data = await response.json()
  const wf = data.data?.records?.find((r: any) => r.name === name)
  if (wf) {
    await page.request.delete(`${API_BASE}/workflow/${wf.id}`, {
      headers: { 'Authorization': `Bearer ${token}` },
    })
  }
}

// 刷新页面并等待表格中出现指定文本
async function reloadAndWaitForTableData(page: Page, text: string): Promise<void> {
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

test.describe('工作流执行记录 E2E 测试', () => {
  test('1. 页面加载 - 实例列表页正确渲染', async ({ page }) => {
    await loginAndNavigate(page, `${UI_BASE}/workflow-instances`)
    await expect(page.locator('h1.page-title')).toHaveText('工作流执行记录')
    await expect(page.locator('.el-table')).toBeVisible()
    await expect(page.locator('.el-select')).toBeVisible()
  })

  test('2. 从工作流列表进入实例列表', async ({ page }) => {
    const wfName = `E2E_InstanceNav_${Date.now()}`
    await loginAndNavigate(page, `${UI_BASE}/workflows`)

    // 通过 API 创建工作流
    const wfId = await createTestWorkflowViaAPI(page, wfName)

    // 刷新页面并等待表格数据
    await reloadAndWaitForTableData(page, wfName)

    // 点击执行记录按钮
    const row = page.locator(`.el-table__body tr:has-text("${wfName}")`)
    await row.locator('button:has-text("执行记录")').click()
    await page.waitForLoadState('networkidle')

    // 验证 URL 和标题
    await expect(page).toHaveURL(new RegExp(`/workflow-instances`))
    await expect(page.locator('h1.page-title')).toHaveText('工作流执行记录')

    // 清理
    await deleteTestWorkflowByName(page, wfName)
  })

  test('3. 启动工作流后在实例列表中查看详情', async ({ page }) => {
    const wfName = `E2E_InstanceDetail_${Date.now()}`
    await loginAndNavigate(page, `${UI_BASE}/workflows`)

    // 通过 API 创建工作流
    const wfId = await createTestWorkflowViaAPI(page, wfName)

    // 保存节点：START -> END
    await saveWorkflowNodes(page, wfId, [
      {
        nodeId: 'node_start',
        type: 'START',
        name: '开始',
        config: '{}',
        positionX: 100,
        positionY: 100,
      },
      {
        nodeId: 'node_end',
        type: 'END',
        name: '结束',
        config: '{}',
        positionX: 300,
        positionY: 100,
      },
    ])

    // 保存连线
    await saveWorkflowEdges(page, wfId, [
      {
        sourceNode: 'node_start',
        targetNode: 'node_end',
        condition: null,
        edgeIndex: 0,
      },
    ])

    // 启动工作流
    const instanceId = await startWorkflowInstance(page, wfId)
    expect(instanceId).toBeTruthy()

    // 等待一小段时间让引擎执行完成
    await page.waitForTimeout(1500)

    // 导航到实例列表页
    await page.goto(`${UI_BASE}/workflow-instances?workflowId=${wfId}`)
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(800)

    // 先通过 API 确认实例已创建
    const token = await getAuthToken(page)
    const instResponse = await page.request.get(`${API_BASE}/workflow/instances?page=1&pageSize=20&workflowId=${wfId}`, {
      headers: { 'Authorization': `Bearer ${token}` },
    })
    const instData = await instResponse.json()
    if (instData.code !== 200) {
      console.error('Instance API error:', instData.message, 'URL:', `${API_BASE}/workflow/instances?page=1&pageSize=20&workflowId=${wfId}`)
    }
    expect(instData.code).toBe(200)
    expect(instData.data?.records?.length ?? 0).toBeGreaterThanOrEqual(1)

    // 等待表格中出现刚创建的实例 ID
    const instanceIdStr = String(instanceId)
    await page.waitForFunction(
      (id) => {
        const rows = document.querySelectorAll('.el-table__body tr')
        for (const row of rows) {
          if (row.textContent?.includes(id)) return true
        }
        return false
      },
      instanceIdStr,
      { timeout: 15000 }
    )

    // 点击包含该实例 ID 的行的查看详情按钮
    const targetRow = page.locator(`.el-table__body tr:has-text("${instanceIdStr}")`)
    await targetRow.locator('button:has-text("查看详情")').click()
    await page.waitForTimeout(800)

    // 验证 Drawer 打开
    await expect(page.locator('.el-drawer__header span')).toHaveText('工作流执行详情')

    // 验证时间线内容（至少应有 START 节点）
    await expect(page.locator('.el-timeline')).toBeVisible({ timeout: 10000 })
    await expect(page.locator('.el-timeline-item').first()).toBeVisible()

    // 验证节点名称
    await expect(page.locator('.node-name').first()).toContainText('node_start')

    // 关闭 Drawer
    await page.locator('.el-drawer__header .el-drawer__close').click()

    // 清理
    await deleteTestWorkflowByName(page, wfName)
  })

  test('3b. 查询节点执行记录 API 直接验证', async ({ page }) => {
    const wfName = `E2E_NodeExec_${Date.now()}`
    await loginAndNavigate(page, `${UI_BASE}/workflows`)

    // 通过 API 创建工作流
    const wfId = await createTestWorkflowViaAPI(page, wfName)

    // 保存节点：START -> END
    await saveWorkflowNodes(page, wfId, [
      {
        nodeId: 'node_start',
        type: 'START',
        name: '开始',
        config: '{}',
        positionX: 100,
        positionY: 100,
      },
      {
        nodeId: 'node_end',
        type: 'END',
        name: '结束',
        config: '{}',
        positionX: 300,
        positionY: 100,
      },
    ])

    // 保存连线
    await saveWorkflowEdges(page, wfId, [
      {
        sourceNode: 'node_start',
        targetNode: 'node_end',
        condition: null,
        edgeIndex: 0,
      },
    ])

    // 启动工作流
    const instanceId = await startWorkflowInstance(page, wfId)
    expect(instanceId).toBeTruthy()

    // 等待一小段时间让引擎执行完成
    await page.waitForTimeout(2000)

    // 通过 API 直接查询节点执行记录
    const token = await getAuthToken(page)
    const response = await page.request.get(`${API_BASE}/workflow/instances/${instanceId}/node-executions`, {
      headers: { 'Authorization': `Bearer ${token}` },
    })
    const data = await response.json()
    expect(data.code).toBe(200)
    expect(data.data.length).toBeGreaterThanOrEqual(1)

    // 清理
    await deleteTestWorkflowByName(page, wfName)
  })

  test('4. 实例列表状态过滤', async ({ page }) => {
    await loginAndNavigate(page, `${UI_BASE}/workflow-instances`)

    // 验证状态选择器存在
    await expect(page.locator('.el-select')).toBeVisible()

    // 点击状态选择器展开选项
    await page.locator('.el-select').click()
    await page.waitForTimeout(300)

    // 验证选项存在
    await expect(page.locator('.el-select-dropdown__item:has-text("已完成")')).toBeVisible()
    await expect(page.locator('.el-select-dropdown__item:has-text("失败")')).toBeVisible()
  })
})
