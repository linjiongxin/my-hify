<template>
  <div class="knowledge-base-list">
    <div class="page-header">
      <h2>知识库管理</h2>
      <el-button type="primary" @click="showCreateDialog = true">
        创建知识库
      </el-button>
    </div>

    <el-card class="filter-card">
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="名称">
          <el-input v-model="queryForm.name" placeholder="搜索知识库名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.enabled" placeholder="全部" clearable>
            <el-option label="启用" :value="true" />
            <el-option label="禁用" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="名称" min-width="150" />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="embeddingModel" label="Embedding 模型" width="150" />
      <el-table-column prop="chunkSize" label="分块大小" width="100" align="center" />
      <el-table-column prop="chunkOverlap" label="重叠大小" width="100" align="center" />
      <el-table-column prop="enabled" label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
            {{ row.enabled ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="160">
        <template #default="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" @click="handleManageDocuments(row)">文档</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pagination.page"
      v-model:page-size="pagination.pageSize"
      :total="pagination.total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      @size-change="loadData"
      @current-change="loadData"
      class="pagination"
    />

    <el-dialog v-model="showCreateDialog" title="创建知识库" width="500px">
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="120px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="createForm.name" placeholder="请输入知识库名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" rows="3" placeholder="可选" />
        </el-form-item>
        <el-form-item label="Embedding 模型">
          <el-select v-model="createForm.embeddingModel" placeholder="选择模型">
            <el-option label="nomic-embed-text (本地)" value="nomic-embed-text" />
            <el-option label="text-embedding-v2 (阿里)" value="text-embedding-v2" />
          </el-select>
        </el-form-item>
        <el-form-item label="分块大小">
          <el-input-number v-model="createForm.chunkSize" :min="100" :max="2000" :step="50" />
        </el-form-item>
        <el-form-item label="分块重叠">
          <el-input-number v-model="createForm.chunkOverlap" :min="0" :max="200" :step="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showEditDialog" title="编辑知识库" width="500px">
      <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="120px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="editForm.name" placeholder="请输入知识库名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" rows="3" />
        </el-form-item>
        <el-form-item label="Embedding 模型">
          <el-select v-model="editForm.embeddingModel">
            <el-option label="nomic-embed-text (本地)" value="nomic-embed-text" />
            <el-option label="text-embedding-v2 (阿里)" value="text-embedding-v2" />
          </el-select>
        </el-form-item>
        <el-form-item label="分块大小">
          <el-input-number v-model="editForm.chunkSize" :min="100" :max="2000" :step="50" />
        </el-form-item>
        <el-form-item label="分块重叠">
          <el-input-number v-model="editForm.chunkOverlap" :min="0" :max="200" :step="10" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="editForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const tableData = ref([])
const showCreateDialog = ref(false)
const showEditDialog = ref(false)

const queryForm = reactive({
  name: '',
  enabled: null
})

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

const createForm = reactive({
  name: '',
  description: '',
  embeddingModel: 'nomic-embed-text',
  chunkSize: 512,
  chunkOverlap: 50
})

const editForm = reactive({
  id: null,
  name: '',
  description: '',
  embeddingModel: '',
  chunkSize: 512,
  chunkOverlap: 50,
  enabled: true
})

const createRules = {
  name: [{ required: true, message: '请输入知识库名称', trigger: 'blur' }]
}

const editRules = {
  name: [{ required: true, message: '请输入知识库名称', trigger: 'blur' }]
}

const createFormRef = ref(null)
const editFormRef = ref(null)

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize,
    }
    if (queryForm.name) params.name = queryForm.name
    if (queryForm.enabled !== null && queryForm.enabled !== '') params.enabled = queryForm.enabled
    const res = await fetch(`/api/rag/knowledge-bases?${new URLSearchParams(params)}`)
    const data = await res.json()
    if (!res.ok) {
      ElMessage.error(data.message || '加载失败')
      return
    }
    tableData.value = data.records || []
    pagination.total = data.total || 0
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  queryForm.name = ''
  queryForm.enabled = null
  loadData()
}

const handleCreate = async () => {
  try {
    await createFormRef.value.validate()
    const res = await fetch('/api/rag/knowledge-bases', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(createForm)
    })
    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: '创建失败' }))
      ElMessage.error(err.message || '创建失败')
      return
    }
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    loadData()
  } catch (e) {
    // validation failed
  }
}

const handleEdit = (row) => {
  editForm.id = row.id
  editForm.name = row.name
  editForm.description = row.description
  editForm.embeddingModel = row.embeddingModel
  editForm.chunkSize = row.chunkSize
  editForm.chunkOverlap = row.chunkOverlap
  editForm.enabled = row.enabled
  showEditDialog.value = true
}

const handleUpdate = async () => {
  try {
    await editFormRef.value.validate()
    await fetch(`/api/rag/knowledge-bases/${editForm.id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(editForm)
    })
    ElMessage.success('更新成功')
    showEditDialog.value = false
    loadData()
  } catch (e) {
    // validation failed
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除知识库「${row.name}」？`, '提示', { type: 'warning' })
  await fetch(`/api/rag/knowledge-bases/${row.id}`, { method: 'DELETE' })
  ElMessage.success('删除成功')
  loadData()
}

const handleManageDocuments = (row) => {
  // 跳转到文档管理页面
  window.location.href = `/rag/kb/${row.id}/documents`
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.knowledge-base-list {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
}

.filter-card {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>