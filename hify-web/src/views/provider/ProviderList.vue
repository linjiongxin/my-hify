<script setup lang="ts">
import { ref } from 'vue'
import { Plus, EditPen, Delete } from '@element-plus/icons-vue'
import HifyTable from '@/components/HifyTable.vue'
import HifyFormDialog from '@/components/HifyFormDialog.vue'
import { useConfirm } from '@/composables/useConfirm'
import { notifySuccess } from '@/utils/notify'
import type { PageResult, PageParams } from '@/components/HifyTable.vue'
import type { FormRules } from 'element-plus'

type ProviderType = 'OpenAI' | 'Claude' | 'Gemini' | 'Ollama' | 'DeepSeek'

interface Provider {
  id: number
  name: string
  type: ProviderType
  apiKey: string
  baseUrl: string
  status: 1 | 0
  createTime: string
}

const providerOptions = ['OpenAI', 'Claude', 'Gemini', 'Ollama', 'DeepSeek']

const mockList = ref<Provider[]>([
  {
    id: 1,
    name: 'OpenAI 官方',
    type: 'OpenAI',
    apiKey: 'sk-************abcd',
    baseUrl: 'https://api.openai.com/v1',
    status: 1,
    createTime: '2024-01-15 09:30:00',
  },
  {
    id: 2,
    name: 'Anthropic Claude',
    type: 'Claude',
    apiKey: 'sk-ant-************efgh',
    baseUrl: 'https://api.anthropic.com',
    status: 1,
    createTime: '2024-02-08 14:20:00',
  },
  {
    id: 3,
    name: 'Google Gemini',
    type: 'Gemini',
    apiKey: 'AIza****************xyz',
    baseUrl: 'https://generativelanguage.googleapis.com',
    status: 0,
    createTime: '2024-03-12 11:45:00',
  },
  {
    id: 4,
    name: 'Ollama 本地模型',
    type: 'Ollama',
    apiKey: '',
    baseUrl: 'http://localhost:11434',
    status: 1,
    createTime: '2024-04-05 16:00:00',
  },
  {
    id: 5,
    name: 'DeepSeek',
    type: 'DeepSeek',
    apiKey: 'sk-************1234',
    baseUrl: 'https://api.deepseek.com/v1',
    status: 1,
    createTime: '2024-05-20 10:10:00',
  },
])

const columns = [
  { prop: 'name', label: '名称', minWidth: 160 },
  { prop: 'type', label: '类型', width: 120, align: 'center' as const },
  { prop: 'baseUrl', label: 'Base URL', minWidth: 240 },
  { prop: 'status', label: '状态', width: 100, align: 'center' as const, slot: 'status' },
  { prop: 'createTime', label: '创建时间', width: 180, align: 'center' as const },
  { label: '操作', width: 150, align: 'center' as const, slot: 'action' },
]

const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  baseUrl: [{ required: true, message: '请输入 Base URL', trigger: 'blur' }],
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

async function fetchApi(params: PageParams): Promise<PageResult<Provider>> {
  await new Promise((resolve) => setTimeout(resolve, 300))
  const start = (params.current - 1) * params.size
  const end = start + params.size
  const records = mockList.value.slice(start, end)
  return {
    records,
    total: mockList.value.length,
    current: params.current,
    size: params.size,
  }
}

function handleAdd() {
  dialogRef.value?.open()
}

function handleEdit(row: Provider) {
  dialogRef.value?.open(row)
}

async function handleDelete(row: Provider) {
  await confirmDelete(
    async () => {
      mockList.value = mockList.value.filter((item) => item.id !== row.id)
    },
    { title: '删除提供商', message: `确定删除 "${row.name}" 吗？`, successMessage: '删除成功' }
  )
  tableRef.value?.refresh()
}

function handleSubmit(data: Provider, isEdit: boolean) {
  setTimeout(() => {
    if (isEdit) {
      const idx = mockList.value.findIndex((item) => item.id === data.id)
      if (idx > -1) {
        mockList.value[idx] = { ...data }
      }
      notifySuccess('修改成功')
    } else {
      mockList.value.unshift({
        ...data,
        id: Date.now(),
        status: 1,
        createTime: new Date().toLocaleString().replace(/\//g, '-'),
      })
      notifySuccess('新增成功')
    }
    dialogRef.value?.finish()
    tableRef.value?.refresh()
  }, 400)
}
</script>

<template>
  <div class="page">
    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">模型提供商管理</h1>
        <p class="page-desc">配置和管理多模型提供商，统一调用网关</p>
      </div>
      <div class="page-header-right">
        <el-button :icon="Plus" type="primary" @click="handleAdd">新增提供商</el-button>
      </div>
    </div>

    <!-- 列表 -->
    <HifyTable ref="tableRef" :columns="columns" :api="fetchApi">
      <template #status="{ row }">
        <el-tag :type="row.status === 1 ? 'success' : 'info'">
          {{ row.status === 1 ? '启用' : '禁用' }}
        </el-tag>
      </template>

      <template #action="{ row }">
        <el-button :icon="EditPen" type="primary" text @click="handleEdit(row)">编辑</el-button>
        <el-button :icon="Delete" type="danger" text @click="handleDelete(row)">删除</el-button>
      </template>
    </HifyTable>

    <!-- 表单弹窗 -->
    <HifyFormDialog
      ref="dialogRef"
      title="提供商"
      width="560px"
      :rules="rules"
      @submit="handleSubmit"
    >
      <template #default="{ form }">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入提供商名称" />
        </el-form-item>

        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择类型" style="width: 100%">
            <el-option
              v-for="opt in providerOptions"
              :key="opt"
              :label="opt"
              :value="opt"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="API Key" prop="apiKey">
          <el-input v-model="form.apiKey" type="password" show-password placeholder="请输入 API Key" />
        </el-form-item>

        <el-form-item label="Base URL" prop="baseUrl">
          <el-input v-model="form.baseUrl" placeholder="请输入 Base URL" />
        </el-form-item>
      </template>
    </HifyFormDialog>
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
</style>
