<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, EditPen, Delete, Share } from '@element-plus/icons-vue'
import HifyTable from '@/components/HifyTable.vue'
import HifyFormDialog from '@/components/HifyFormDialog.vue'
import { useConfirm } from '@/composables/useConfirm'
import { notifySuccess } from '@/utils/notify'
import type { PageResult, PageParams } from '@/components/HifyTable.vue'
import type { FormRules } from 'element-plus'
import {
  getWorkflowPage,
  createWorkflow,
  updateWorkflow,
  deleteWorkflow,
  type Workflow,
  type WorkflowStatus,
} from '@/api/workflow'

const router = useRouter()
const { confirmDelete } = useConfirm()

const statusOptions = [
  { label: '草稿', value: 'draft' },
  { label: '已发布', value: 'published' },
  { label: '已禁用', value: 'disabled' },
]

const statusTagType = (status?: WorkflowStatus) => {
  switch (status) {
    case 'published': return 'success'
    case 'draft': return 'info'
    case 'disabled': return 'danger'
    default: return 'info'
  }
}

const statusLabel = (status?: WorkflowStatus) => {
  switch (status) {
    case 'published': return '已发布'
    case 'draft': return '草稿'
    case 'disabled': return '已禁用'
    default: return status || '-'
  }
}

function formatDateTime(val: unknown) {
  if (!val) return '-'
  const d = new Date(String(val))
  if (Number.isNaN(d.getTime())) return String(val)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const columns = [
  { prop: 'name', label: '名称', minWidth: 160 },
  { prop: 'description', label: '描述', minWidth: 200 },
  { prop: 'status', label: '状态', width: 100, align: 'center' as const, slot: 'status' },
  { prop: 'version', label: '版本', width: 80, align: 'center' as const },
  { prop: 'createdAt', label: '创建时间', width: 180, align: 'center' as const, formatter: (_row: any, _col: any, val: unknown) => formatDateTime(val) },
  { label: '操作', width: 220, align: 'center' as const, slot: 'action' },
]

const rules: FormRules = {
  name: [{ required: true, message: '请输入工作流名称', trigger: 'blur' }],
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

async function fetchApi(params: PageParams): Promise<PageResult<Workflow>> {
  return getWorkflowPage(params)
}

function handleAdd() {
  dialogRef.value?.open({ status: 'draft' })
}

function handleEdit(row: Workflow) {
  dialogRef.value?.open({ ...row })
}

function handleDesign(row: Workflow) {
  if (row.id) {
    router.push(`/workflows/${row.id}/edit`)
  }
}

async function handleDelete(row: Workflow) {
  await confirmDelete(
    async () => {
      await deleteWorkflow(row.id!)
      notifySuccess('删除成功')
      tableRef.value?.refresh()
    },
    { title: '删除工作流', message: `确定删除 "${row.name}" 吗？` }
  )
}

async function handleSubmit(data: Workflow, isEdit: boolean) {
  const payload = {
    name: data.name,
    description: data.description,
    status: data.status,
  }
  if (isEdit) {
    await updateWorkflow(data.id!, payload)
    notifySuccess('修改成功')
  } else {
    await createWorkflow(payload)
    notifySuccess('新增成功')
  }
  dialogRef.value?.finish()
  tableRef.value?.refresh()
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">工作流管理</h1>
        <p class="page-desc">可视化编排 AI 工作流，支持多节点协作与条件分支</p>
      </div>
      <div class="page-header-right">
        <el-button :icon="Plus" type="primary" @click="handleAdd">新增工作流</el-button>
      </div>
    </div>

    <HifyTable ref="tableRef" :columns="columns" :api="fetchApi">
      <template #status="{ row }">
        <el-tag :type="statusTagType(row.status)">
          {{ statusLabel(row.status) }}
        </el-tag>
      </template>

      <template #action="{ row }">
        <el-button :icon="EditPen" type="primary" text @click="handleEdit(row)">编辑</el-button>
        <el-button :icon="Share" type="success" text @click="handleDesign(row)">编排</el-button>
        <el-button :icon="Delete" type="danger" text @click="handleDelete(row)">删除</el-button>
      </template>
    </HifyTable>

    <HifyFormDialog
      ref="dialogRef"
      title="工作流"
      width="520px"
      :rules="rules"
      @submit="handleSubmit"
    >
      <template #default="{ form }">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入工作流名称" />
        </el-form-item>

        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入工作流描述"
          />
        </el-form-item>

        <el-form-item label="状态">
          <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
            <el-option
              v-for="opt in statusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
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
