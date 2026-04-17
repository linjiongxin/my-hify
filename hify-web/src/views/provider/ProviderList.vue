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
  getProviderPage,
  createProvider,
  updateProvider,
  deleteProvider,
  type Provider,
} from '@/api/provider'

const protocolOptions = [
  { label: 'OpenAI 兼容', value: 'openai_compatible' },
  { label: '自定义', value: 'custom' },
]

const authTypeOptions = [
  { label: 'Bearer Token', value: 'BEARER' },
  { label: 'API Key', value: 'API_KEY' },
  { label: '无认证', value: 'NONE' },
  { label: '自定义 Header', value: 'CUSTOM' },
]

const columns = [
  { prop: 'name', label: '名称', minWidth: 160 },
  { prop: 'code', label: '代码', width: 120, align: 'center' as const },
  { prop: 'protocolType', label: '协议', width: 140, align: 'center' as const },
  { prop: 'apiBaseUrl', label: 'Base URL', minWidth: 240 },
  { prop: 'authType', label: '鉴权', width: 130, align: 'center' as const },
  { prop: 'enabled', label: '状态', width: 90, align: 'center' as const, slot: 'status' },
  { prop: 'createdAt', label: '创建时间', width: 180, align: 'center' as const },
  { label: '操作', width: 150, align: 'center' as const, slot: 'action' },
]

const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入代码', trigger: 'blur' }],
  protocolType: [{ required: true, message: '请选择协议类型', trigger: 'change' }],
  apiBaseUrl: [{ required: true, message: '请输入 Base URL', trigger: 'blur' }],
  authType: [{ required: true, message: '请选择鉴权类型', trigger: 'change' }],
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
  return getProviderPage(params)
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
      await deleteProvider(row.id)
      notifySuccess('删除成功')
      tableRef.value?.refresh()
    },
    { title: '删除提供商', message: `确定删除 "${row.name}" 吗？` }
  )
}

async function handleSubmit(data: Provider, isEdit: boolean) {
  const payload = {
    name: data.name,
    code: data.code,
    protocolType: data.protocolType,
    apiBaseUrl: data.apiBaseUrl,
    authType: data.authType,
    apiKey: data.apiKey,
    authConfig: data.authConfig,
    enabled: data.enabled,
    sortOrder: data.sortOrder,
  }
  if (isEdit) {
    await updateProvider(data.id, payload)
    notifySuccess('修改成功')
  } else {
    await createProvider(payload)
    notifySuccess('新增成功')
  }
  dialogRef.value?.finish()
  tableRef.value?.refresh()
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
      title="提供商"
      width="560px"
      :rules="rules"
      @submit="handleSubmit"
    >
      <template #default="{ form }">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入提供商名称" />
        </el-form-item>

        <el-form-item label="代码" prop="code">
          <el-input v-model="form.code" placeholder="如 openai、deepseek" />
        </el-form-item>

        <el-form-item label="协议类型" prop="protocolType">
          <el-select v-model="form.protocolType" placeholder="请选择协议类型" style="width: 100%">
            <el-option
              v-for="opt in protocolOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="Base URL" prop="apiBaseUrl">
          <el-input v-model="form.apiBaseUrl" placeholder="请输入 API 基础地址" />
        </el-form-item>

        <el-form-item label="鉴权类型" prop="authType">
          <el-select v-model="form.authType" placeholder="请选择鉴权类型" style="width: 100%">
            <el-option
              v-for="opt in authTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="API Key" prop="apiKey">
          <el-input
            v-model="form.apiKey"
            type="password"
            show-password
            placeholder="请输入 API Key"
          />
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
