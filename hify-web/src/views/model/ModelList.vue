<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus, EditPen, Delete } from '@element-plus/icons-vue'
import HifyTable from '@/components/HifyTable.vue'
import HifyFormDialog from '@/components/HifyFormDialog.vue'
import { useConfirm } from '@/composables/useConfirm'
import { notifySuccess } from '@/utils/notify'
import type { PageResult, PageParams } from '@/components/HifyTable.vue'
import type { FormRules } from 'element-plus'
import {
  getModelPage,
  createModel,
  updateModel,
  deleteModel,
  type ModelConfig,
} from '@/api/model'
import { getProviderPage, type Provider } from '@/api/provider'

const providerMap = ref<Map<number, string>>(new Map())
const providerOptions = ref<Provider[]>([])

function formatDateTime(val: unknown) {
  if (!val) return '-'
  const d = new Date(String(val))
  if (Number.isNaN(d.getTime())) return String(val)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const columns = [
  {
    prop: 'providerId',
    label: '提供商',
    width: 140,
    align: 'center' as const,
    formatter: (_row: ModelConfig, _col: any, cellValue: unknown) =>
      providerMap.value.get(cellValue as number) || '-',
  },
  { prop: 'name', label: '模型名称', minWidth: 160 },
  { prop: 'modelId', label: '模型 ID', minWidth: 180 },
  { prop: 'defaultModel', label: '默认', width: 90, align: 'center' as const, slot: 'defaultModel' },
  { prop: 'enabled', label: '状态', width: 90, align: 'center' as const, slot: 'status' },
  { prop: 'createdAt', label: '创建时间', width: 180, align: 'center' as const, formatter: (_row: any, _col: any, val: unknown) => formatDateTime(val) },
  { label: '操作', width: 150, align: 'center' as const, slot: 'action' },
]

const rules: FormRules = {
  providerId: [{ required: true, message: '请选择提供商', trigger: 'change' }],
  name: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  modelId: [{ required: true, message: '请输入模型 ID', trigger: 'blur' }],
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

async function loadProviders() {
  try {
    const res = await getProviderPage({ current: 1, size: 999 })
    providerOptions.value = res.records || []
    providerMap.value = new Map(providerOptions.value.map((p) => [p.id, p.name]))
  } catch {
    // ignore
  }
}

async function fetchApi(params: PageParams): Promise<PageResult<ModelConfig>> {
  return getModelPage(params)
}

function handleAdd() {
  dialogRef.value?.open()
}

function handleEdit(row: ModelConfig) {
  dialogRef.value?.open(row)
}

async function handleDelete(row: ModelConfig) {
  await confirmDelete(
    async () => {
      await deleteModel(row.id)
      notifySuccess('删除成功')
      tableRef.value?.refresh()
    },
    { title: '删除模型', message: `确定删除 "${row.name}" 吗？` }
  )
}

async function handleSubmit(data: ModelConfig, isEdit: boolean) {
  const payload = {
    providerId: data.providerId,
    name: data.name,
    modelId: data.modelId,
    maxTokens: data.maxTokens,
    contextWindow: data.contextWindow,
    capabilities: data.capabilities,
    inputPricePer1m: data.inputPricePer1m,
    outputPricePer1m: data.outputPricePer1m,
    defaultModel: data.defaultModel,
    enabled: data.enabled,
    sortOrder: data.sortOrder,
  }
  if (isEdit) {
    await updateModel(data.id, payload)
    notifySuccess('修改成功')
  } else {
    await createModel(payload)
    notifySuccess('新增成功')
  }
  dialogRef.value?.finish()
  tableRef.value?.refresh()
}

onMounted(() => {
  loadProviders()
})
</script>

<template>
  <div class="page">
    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">模型配置</h1>
        <p class="page-desc">管理各提供商下的模型参数与可用性配置</p>
      </div>
      <div class="page-header-right">
        <el-button :icon="Plus" type="primary" @click="handleAdd">新增模型</el-button>
      </div>
    </div>

    <!-- 列表 -->
    <HifyTable ref="tableRef" :columns="columns" :api="fetchApi">
      <template #defaultModel="{ row }">
        <el-tag :type="row.defaultModel ? 'warning' : 'info'" size="small">
          {{ row.defaultModel ? '默认' : '-' }}
        </el-tag>
      </template>

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

    <!-- 表单弹窗 -->
    <HifyFormDialog
      ref="dialogRef"
      title="模型"
      width="560px"
      :rules="rules"
      @submit="handleSubmit"
    >
      <template #default="{ form }">
        <el-form-item label="提供商" prop="providerId">
          <el-select
            v-model="form.providerId"
            placeholder="请选择提供商"
            style="width: 100%"
            :disabled="!!form.id"
          >
            <el-option
              v-for="opt in providerOptions"
              :key="opt.id"
              :label="opt.name"
              :value="opt.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="模型名称" prop="name">
          <el-input v-model="form.name" placeholder="如 GPT-4o" />
        </el-form-item>

        <el-form-item label="模型 ID" prop="modelId">
          <el-input v-model="form.modelId" placeholder="如 gpt-4o" />
        </el-form-item>

        <el-form-item label="最大 Token">
          <el-input-number v-model="form.maxTokens" :min="1" style="width: 100%" />
        </el-form-item>

        <el-form-item label="上下文窗口">
          <el-input-number v-model="form.contextWindow" :min="1" style="width: 100%" />
        </el-form-item>

        <el-form-item label="默认模型">
          <el-switch v-model="form.defaultModel" active-text="是" inactive-text="否" />
        </el-form-item>

        <el-form-item label="启用状态">
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="禁用" />
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
