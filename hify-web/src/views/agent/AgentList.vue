<script setup lang="ts">
import { ref } from 'vue'
import { Plus, EditPen, Delete } from '@element-plus/icons-vue'
import HifyTable from '@/components/HifyTable.vue'
import HifyFormDialog from '@/components/HifyFormDialog.vue'
import { useConfirm } from '@/composables/useConfirm'
import { notifySuccess } from '@/utils/notify'
import type { PageResult, PageParams } from '@/components/HifyTable.vue'
import type { FormRules } from 'element-plus'
import {
  getAgentPage,
  getAgentDetail,
  createAgent,
  updateAgent,
  deleteAgent,
  getAgentTools,
  replaceAgentTools,
  type Agent,
  type ToolItem,
} from '@/api/agent'
import { getAllEnabledModels } from '@/api/model'

const modelOptions = ref<{ label: string; value: string }[]>([])

async function loadModelOptions() {
  const models = await getAllEnabledModels()
  modelOptions.value = models.map(m => ({ label: m.name, value: m.modelId }))
}

loadModelOptions()

const toolTypeOptions = [
  { label: '内置工具', value: 'builtin' },
  { label: 'MCP 工具', value: 'mcp' },
]

function formatDateTime(val: unknown) {
  if (!val) return '-'
  const d = new Date(String(val))
  if (Number.isNaN(d.getTime())) return String(val)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const columns = [
  { prop: 'name', label: '名称', minWidth: 160 },
  { prop: 'modelId', label: '模型', width: 180, align: 'center' as const },
  { prop: 'description', label: '描述', minWidth: 200 },
  { prop: 'enabled', label: '状态', width: 90, align: 'center' as const, slot: 'status' },
  { prop: 'createdAt', label: '创建时间', width: 180, align: 'center' as const, formatter: (_row: any, _col: any, val: unknown) => formatDateTime(val) },
  { label: '操作', width: 150, align: 'center' as const, slot: 'action' },
]

const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  modelId: [{ required: true, message: '请选择模型', trigger: 'change' }],
}

interface HifyTableExpose {
  refresh: () => void
}
interface HifyFormDialogExpose {
  open: (data?: any) => void
  close: () => void
  finish: () => void
}

const tableRef = ref<HifyTableExpose>()
const dialogRef = ref<HifyFormDialogExpose>()
const { confirmDelete } = useConfirm()

const currentAgentId = ref<number | null>(null)
const toolList = ref<ToolItem[]>([])
const toolDialogVisible = ref(false)
const toolForm = ref<Partial<ToolItem>>({
  toolType: 'builtin',
  enabled: true,
})
const toolFormRef = ref()

async function fetchApi(params: PageParams): Promise<PageResult<Agent>> {
  return getAgentPage(params)
}

function handleAdd() {
  toolList.value = []
  dialogRef.value?.open({ enabled: true, temperature: 0.7, maxTokens: 2048, topP: 1.0 }, false)
}

async function handleEdit(row: Agent) {
  toolList.value = []
  const detail = await getAgentDetail(row.id)
  currentAgentId.value = row.id
  const tools = await getAgentTools(row.id)
  toolList.value = tools || []
  dialogRef.value?.open({ ...detail, id: row.id })
}

async function handleDelete(row: Agent) {
  await confirmDelete(
    async () => {
      await deleteAgent(row.id)
      notifySuccess('删除成功')
      tableRef.value?.refresh()
    },
    { title: '删除 Agent', message: `确定删除 "${row.name}" 吗？` }
  )
}

async function handleSubmit(data: Agent, _isEdit: boolean) {
  const payload = {
    name: data.name,
    modelId: data.modelId,
    description: data.description,
    systemPrompt: data.systemPrompt,
    temperature: data.temperature,
    maxTokens: data.maxTokens,
    topP: data.topP,
    welcomeMessage: data.welcomeMessage,
    enabled: data.enabled,
  }
  if (data.id != null) {
    await updateAgent(data.id, payload)
    notifySuccess('修改成功')
  } else {
    const newId = await createAgent(payload)
    currentAgentId.value = newId
    notifySuccess('新增成功')
  }
  dialogRef.value?.finish()
  tableRef.value?.refresh()
}

function handleOpenToolDialog() {
  toolForm.value = {
    toolType: 'builtin',
    enabled: true,
  }
  toolDialogVisible.value = true
}

async function handleAddTool() {
  if (!toolFormRef.value) return
  try {
    await toolFormRef.value.validate()
    const newTool: ToolItem = {
      toolName: toolForm.value.toolName!,
      toolType: toolForm.value.toolType!,
      toolImpl: toolForm.value.toolImpl!,
      configJson: toolForm.value.configJson,
      enabled: toolForm.value.enabled,
      sortOrder: toolList.value.length,
    }
    toolList.value.push(newTool)
    toolDialogVisible.value = false
  } catch {
    // validation error
  }
}

function handleRemoveTool(index: number) {
  toolList.value.splice(index, 1)
}

async function handleSaveTools() {
  if (!currentAgentId.value) return
  await replaceAgentTools(currentAgentId.value, { tools: toolList.value })
  notifySuccess('工具绑定已更新')
}
</script>

<template>
  <div class="page">
    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">Agent 管理</h1>
        <p class="page-desc">配置和管理 AI Agent，支持工具绑定</p>
      </div>
      <div class="page-header-right">
        <el-button :icon="Plus" type="primary" @click="handleAdd">新增 Agent</el-button>
      </div>
    </div>

    <!-- 列表 -->
    <HifyTable ref="tableRef" :columns="columns" :api="fetchApi">
      <template #status="{ row }">
        <el-tag :type="row.enabled ? 'success' : 'info'">
          {{ row.enabled ? '启用' : '禁用' }}
        </el-tag>
      </template>

      <template #action="{ row }">
        <el-button :icon="EditPen" type="primary" text @click="handleEdit(row)">编辑</el-button>
        <el-button :icon="Delete" type="danger" text @click="handleDelete(row)">删除</el-button>
      </template>
    </HifyTable>

    <!-- 表单弹窗（带 Tabs） -->
    <HifyFormDialog
      ref="dialogRef"
      title="Agent"
      width="720px"
      :rules="rules"
      @submit="handleSubmit"
    >
      <template #default="{ form }">
        <el-tabs>
          <el-tab-pane label="基本信息">
            <el-form-item label="名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入 Agent 名称" />
            </el-form-item>

            <el-form-item label="模型" prop="modelId">
              <el-select v-model="form.modelId" placeholder="请选择模型" style="width: 100%">
                <el-option
                  v-for="opt in modelOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="描述">
              <el-input v-model="form.description" type="textarea" placeholder="请输入描述" :rows="2" />
            </el-form-item>

            <el-form-item label="系统提示词">
              <el-input v-model="form.systemPrompt" type="textarea" placeholder="请输入系统提示词" :rows="4" />
            </el-form-item>

            <el-form-item label="温度">
              <el-slider v-model="form.temperature" :min="0" :max="1" :step="0.1" show-stops />
            </el-form-item>

            <el-form-item label="最大 Token">
              <el-input-number v-model="form.maxTokens" :min="1" :max="128000" />
            </el-form-item>

            <el-form-item label="TopP">
              <el-slider v-model="form.topP" :min="0" :max="1" :step="0.1" show-stops />
            </el-form-item>

            <el-form-item label="欢迎语">
              <el-input v-model="form.welcomeMessage" type="textarea" placeholder="请输入欢迎语" :rows="2" />
            </el-form-item>

            <el-form-item label="状态">
              <el-switch v-model="form.enabled" />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane label="工具绑定">
            <div class="tool-binding">
              <div class="tool-header">
                <span class="tool-title">已绑定工具</span>
                <el-button type="primary" size="small" @click="handleOpenToolDialog">添加工具</el-button>
              </div>

              <el-table :data="toolList" empty-text="暂未绑定工具" style="width: 100%">
                <el-table-column prop="toolName" label="名称" min-width="120" />
                <el-table-column prop="toolType" label="类型" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.toolType === 'builtin' ? 'success' : 'warning'" size="small">
                      {{ row.toolType === 'builtin' ? '内置' : 'MCP' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="toolImpl" label="实现标识" min-width="160" />
                <el-table-column prop="enabled" label="启用" width="70" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
                      {{ row.enabled ? '是' : '否' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="80" align="center">
                  <template #default="{ $index }">
                    <el-button type="danger" text size="small" @click="handleRemoveTool($index)">
                      删除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>

              <div v-if="toolList.length > 0" class="tool-footer">
                <el-button type="primary" @click="handleSaveTools">保存工具绑定</el-button>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>
    </HifyFormDialog>

    <!-- 添加工具弹窗 -->
    <el-dialog v-model="toolDialogVisible" title="添加工具" width="480px" align-center destroy-on-close>
      <el-form ref="toolFormRef" :model="toolForm" label-width="100px">
        <el-form-item label="工具名称" prop="toolName" :rules="{ required: true, message: '请输入工具名称', trigger: 'blur' }">
          <el-input v-model="toolForm.toolName" placeholder="如：天气查询" />
        </el-form-item>

        <el-form-item label="工具类型" prop="toolType">
          <el-select v-model="toolForm.toolType" style="width: 100%">
            <el-option
              v-for="opt in toolTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="实现标识" prop="toolImpl" :rules="{ required: true, message: '请输入实现标识', trigger: 'blur' }">
          <el-input v-model="toolForm.toolImpl" placeholder="如：weather 或 mcp-server-name/weather" />
        </el-form-item>

        <el-form-item label="启用">
          <el-switch v-model="toolForm.enabled" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="toolDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddTool">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page {
  max-width: 1200px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: var(--space-6);
  gap: var(--space-4);
}

.page-header-left {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.page-title {
  font-size: var(--text-2xl);
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.page-desc {
  font-size: var(--text-base);
  color: var(--text-secondary);
  line-height: 1.5;
}

.page-header-right {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex-shrink: 0;
}

.tool-binding {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.tool-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.tool-title {
  font-size: var(--text-base);
  font-weight: 500;
  color: var(--text-primary);
}

.tool-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: var(--space-2);
}
</style>
