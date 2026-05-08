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

// 通过 API 创建工作流（使用 Playwright request，不受页面导航影响）
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
async function reloadAndWaitForWorkflowData(page: Page, text: string): Promise<void> {
  await page.reload()
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(300)

  // 等待表格中出现指定文本
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

// 通过 HTML5 DataTransfer 模拟拖拽节点到画布
async function dragNodeToCanvas(page: Page, nodeLabel: string) {
  const canvasBox = await page.locator('.flow-canvas').boundingBox()
  if (!canvasBox) throw new Error('Canvas not found')

  await page.evaluate(({ label, cx, cy }) => {
    const items = Array.from(document.querySelectorAll('.palette-item'))
    const source = items.find((el) => el.textContent?.includes(label)) as HTMLElement | undefined
    const target = document.querySelector('.flow-canvas') as HTMLElement | undefined
    if (!source || !target) throw new Error('Source or target not found')

    const dt = new DataTransfer()
    dt.setData('application/vueflow-node-type', label)
    dt.effectAllowed = 'move'

    source.dispatchEvent(new DragEvent('dragstart', { bubbles: true, cancelable: true, dataTransfer: dt }))
    target.dispatchEvent(new DragEvent('dragover', { bubbles: true, cancelable: true, dataTransfer: dt, clientX: cx, clientY: cy }))
    target.dispatchEvent(new DragEvent('drop', { bubbles: true, cancelable: true, dataTransfer: dt, clientX: cx, clientY: cy }))
    source.dispatchEvent(new DragEvent('dragend', { bubbles: true, cancelable: true, dataTransfer: dt }))
  }, {
    label: nodeLabel,
    cx: canvasBox.x + canvasBox.width / 2,
    cy: canvasBox.y + canvasBox.height / 2,
  })

  await page.waitForTimeout(300)
}

test.describe('工作流列表页 E2E 测试', () => {
  test('1. 页面加载 - 工作流列表页正确渲染', async ({ page }) => {
    await loginAndNavigate(page, `${UI_BASE}/workflows`)
    await expect(page.locator('h1.page-title')).toHaveText('工作流管理')
    await expect(page.locator('button:has-text("新增工作流")')).toBeVisible()
    await expect(page.locator('.el-table')).toBeVisible()
  })

  test('2. 通过 API 创建工作流 - 然后在 UI 验证', async ({ page }) => {
    const wfName = `E2E_WF_${Date.now()}`
    await loginAndNavigate(page, `${UI_BASE}/workflows`)

    // 通过 API 创建
    await createTestWorkflowViaAPI(page, wfName)

    // 刷新页面并等待表格数据
    await reloadAndWaitForWorkflowData(page, wfName)

    // 验证工作流出现在列表中
    await expect(page.locator(`.el-table__body tr:has-text("${wfName}")`)).toBeVisible()

    // 清理
    await deleteTestWorkflowByName(page, wfName)
  })

  test('3. 表单验证 - 名称为空时显示错误', async ({ page }) => {
    await loginAndNavigate(page, `${UI_BASE}/workflows`)

    await page.click('button:has-text("新增工作流")')
    await expect(page.locator('.el-dialog')).toBeVisible()
    await page.waitForTimeout(500)

    // 不填名称直接提交
    await page.locator('.el-dialog__footer button.el-button--primary').click()
    await page.waitForTimeout(500)

    // 验证错误提示
    await expect(page.locator('.el-form-item__error').first()).toContainText('请输入工作流名称')
  })
})

test.describe('工作流编排器 E2E 测试', () => {
  test('4. 编排器页面加载 - 显示三栏布局', async ({ page }) => {
    const wfName = `E2E_Edit_${Date.now()}`
    await loginAndNavigate(page, `${UI_BASE}/workflows`)

    // 通过 API 创建工作流
    const wfId = await createTestWorkflowViaAPI(page, wfName)

    // 刷新页面并等待表格数据
    await reloadAndWaitForWorkflowData(page, wfName)

    // 点击编排按钮进入编辑器
    const row = page.locator(`.el-table__body tr:has-text("${wfName}")`)
    await row.locator('button:has-text("编排")').click()
    await page.waitForLoadState('networkidle')

    // 验证 URL
    await expect(page).toHaveURL(new RegExp(`/workflows/${wfId}/edit`))

    // 验证三栏布局元素
    await expect(page.locator('.node-palette')).toBeVisible()
    await expect(page.locator('.flow-canvas')).toBeVisible()
    await expect(page.locator('.node-config-panel')).toBeVisible()

    // 验证顶部工具栏
    await expect(page.locator('.editor-header')).toBeVisible()
    await expect(page.locator('button:has-text("保存")')).toBeVisible()
    await expect(page.locator('button:has-text("发布")')).toBeVisible()

    // 验证左侧节点面板有 8 种节点
    const paletteItems = page.locator('.palette-item')
    await expect(paletteItems).toHaveCount(8)

    // 清理
    await deleteTestWorkflowByName(page, wfName)
  })

  test('5. 拖拽添加节点到画布', async ({ page }) => {
    const wfName = `E2E_Drag_${Date.now()}`
    await loginAndNavigate(page, `${UI_BASE}/workflows`)

    const wfId = await createTestWorkflowViaAPI(page, wfName)
    await reloadAndWaitForWorkflowData(page, wfName)

    // 进入编排器
    const row = page.locator(`.el-table__body tr:has-text("${wfName}")`)
    await row.locator('button:has-text("编排")').click()
    await page.waitForLoadState('networkidle')

    // 等待编辑器加载完成（标题不再是"加载中..."）
    await expect(page.locator('.workflow-title .title-text')).not.toHaveText('加载中...', { timeout: 10000 })

    // 初始画布为空
    const initialCount = await page.locator('.vue-flow__node').count()

    // 从左侧拖拽 "LLM" 节点到画布中央
    await dragNodeToCanvas(page, 'LLM')

    // 验证画布上新增了一个节点
    const newCount = await page.locator('.vue-flow__node').count()
    expect(newCount).toBeGreaterThan(initialCount)

    // 点击保存
    await page.click('button:has-text("保存")')
    await page.waitForTimeout(1000)

    // 验证保存成功提示（Element Plus message）
    await expect(page.locator('.el-message--success')).toContainText('保存成功')

    // 清理
    await deleteTestWorkflowByName(page, wfName)
  })

  test('6. 选中节点后右侧显示配置面板', async ({ page }) => {
    const wfName = `E2E_Config_${Date.now()}`
    await loginAndNavigate(page, `${UI_BASE}/workflows`)

    const wfId = await createTestWorkflowViaAPI(page, wfName)
    await reloadAndWaitForWorkflowData(page, wfName)

    // 进入编排器
    const row = page.locator(`.el-table__body tr:has-text("${wfName}")`)
    await row.locator('button:has-text("编排")').click()
    await page.waitForLoadState('networkidle')

    // 等待编辑器加载完成
    await expect(page.locator('.workflow-title .title-text')).not.toHaveText('加载中...', { timeout: 10000 })

    // 拖拽一个 LLM 节点到画布
    await dragNodeToCanvas(page, 'LLM')

    // 点击画布上的节点
    await page.locator('.vue-flow__node').last().click()
    await page.waitForTimeout(300)

    // 验证右侧配置面板显示了节点配置表单
    await expect(page.locator('.node-config-panel')).toContainText('节点名称')
    await expect(page.locator('.node-config-panel')).toContainText('模型')
    await expect(page.locator('.node-config-panel')).toContainText('Prompt')
    await expect(page.locator('.node-config-panel')).toContainText('输出变量')

    // 填写配置（注意：input 顺序为 节点名称、节点ID(disabled)、模型、输出变量、错误分支）
    await page.locator('.node-config-panel input').nth(0).fill('问题分类')
    await page.locator('.node-config-panel input').nth(2).fill('gpt-4o-mini')
    await page.locator('.node-config-panel textarea').fill('判断用户问题类型')
    await page.locator('.node-config-panel input').nth(3).fill('intent')

    // 保存工作流
    await page.click('button:has-text("保存")')
    await page.waitForTimeout(1000)
    await expect(page.locator('.el-message--success')).toContainText('保存成功')

    // 刷新页面验证持久化
    await page.reload()
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(1000)

    // 点击节点验证配置已持久化
    await page.locator('.vue-flow__node').last().click()
    await page.waitForTimeout(300)
    await expect(page.locator('.node-config-panel input').nth(0)).toHaveValue('问题分类')
    await expect(page.locator('.node-config-panel input').nth(2)).toHaveValue('gpt-4o-mini')

    // 清理
    await deleteTestWorkflowByName(page, wfName)
  })
})
