import { test, expect, Page } from '@playwright/test'

// 登录
async function login(page: Page) {
  await page.goto('http://localhost:5173/login')
  await page.waitForLoadState('networkidle')
  // 如果在登录页，设置 token 后跳转到知识库页面
  if (page.url().includes('/login')) {
    await page.evaluate(() => localStorage.setItem('token', 'test-token'))
    await page.evaluate(() => { window.location.href = 'http://localhost:5173/rag/knowledge-bases' })
    await page.waitForLoadState('networkidle')
  }
}

async function waitForTableData(page: Page, kbName: string, timeout = 10000): Promise<void> {
  await page.waitForFunction(
    (name) => {
      const rows = document.querySelectorAll('.el-table__body tr')
      for (const row of rows) {
        if (row.textContent?.includes(name)) {
          return true
        }
      }
      return false
    },
    kbName,
    { timeout }
  )
}

// 通过 API 创建知识库
async function createTestKBViaAPI(page: Page, name: string): Promise<number> {
  const resp = await page.request.post('http://localhost:8080/api/rag/knowledge-bases', {
    data: {
      name: name,
      description: 'E2E 测试知识库',
      embeddingModel: 'text-embedding-v2',
      chunkSize: 512,
      chunkOverlap: 50
    }
  })
  const json = await resp.json()
  return json
}

// 通过 API 删除知识库
async function deleteTestKBByName(page: Page, name: string): Promise<void> {
  const resp = await page.request.get('http://localhost:8080/api/rag/knowledge-bases?page=1&pageSize=100')
  const data = await resp.json()
  const kb = data.records?.find((r: any) => r.name === name)
  if (kb) {
    await page.request.delete(`http://localhost:8080/api/rag/knowledge-bases/${kb.id}`)
  }
}

test.describe('知识库管理 E2E 测试', () => {
  test('1. 页面加载 - 知识库列表页正确渲染', async ({ page }) => {
    await login(page)
    await expect(page.locator('h2')).toHaveText('知识库管理')
    await expect(page.locator('button:has-text("创建知识库")')).toBeVisible()
    await expect(page.locator('.el-table')).toBeVisible()
  })

  test('2. 通过 UI 创建知识库', async ({ page }) => {
    const kbName = `E2E_KB_${Date.now()}`

    await login(page)

    // 点击创建按钮
    await page.click('button:has-text("创建知识库")')
    await expect(page.locator('.el-dialog')).toBeVisible()

    // 填写表单
    await page.fill('input[placeholder="请输入知识库名称"]', kbName)
    await page.fill('textarea[placeholder="可选"]', 'E2E 测试描述')

    // 提交
    await page.click('.el-dialog__footer button.el-button--primary')
    await page.waitForTimeout(1000)

    // 验证成功提示
    await expect(page.locator('.el-message')).toContainText('成功')

    // 等待表格中出现新创建的知识库
    await waitForTableData(page, kbName)

    // 验证知识库出现在列表中
    await expect(page.locator(`.el-table__body tr:has-text("${kbName}")`)).toBeVisible()

    // 清理
    await deleteTestKBByName(page, kbName)
  })

  test('3. 通过 UI 编辑知识库', async ({ page }) => {
    const kbName = `E2E_KB_Edit_${Date.now()}`

    // 先创建测试数据
    await createTestKBViaAPI(page, kbName)

    await login(page)
    await waitForTableData(page, kbName)

    // 点击编辑按钮
    const row = page.locator(`.el-table__body tr:has-text("${kbName}")`)
    await row.locator('button:has-text("编辑")').click()

    // 等待编辑弹窗
    await expect(page.locator('.el-dialog:has-text("编辑知识库")')).toBeVisible()

    // 修改名称
    await page.fill('input[placeholder="请输入知识库名称"]', kbName + '_updated')

    // 保存
    await page.click('.el-dialog__footer button.el-button--primary')
    await page.waitForTimeout(1000)

    // 验证成功提示
    await expect(page.locator('.el-message')).toContainText('成功')

    // 清理
    await deleteTestKBByName(page, kbName + '_updated')
  })

  test('4. 通过 UI 删除知识库', async ({ page }) => {
    const kbName = `E2E_KB_Delete_${Date.now()}`

    // 先创建测试数据
    await createTestKBViaAPI(page, kbName)

    await login(page)
    await waitForTableData(page, kbName)

    // 点击删除按钮
    const row = page.locator(`.el-table__body tr:has-text("${kbName}")`)
    await row.locator('button:has-text("删除")').click()

    // 确认删除弹窗
    await expect(page.locator('.el-message-box')).toBeVisible()
    await page.click('.el-message-box__btns button.el-button--primary')

    await page.waitForTimeout(1000)

    // 验证成功提示
    await expect(page.locator('.el-message')).toContainText('成功')
  })
})

test.describe('知识库表单验证', () => {
  test('名称为空时显示验证错误', async ({ page }) => {
    await login(page)

    await page.click('button:has-text("创建知识库")')
    await expect(page.locator('.el-dialog')).toBeVisible()
    await page.waitForTimeout(500)

    // 不填名称，直接提交
    await page.click('.el-dialog__footer button.el-button--primary')
    await page.waitForTimeout(500)

    // 验证有错误提示
    await expect(page.locator('.el-form-item__error').first()).toContainText('请输入知识库名称')
  })
})