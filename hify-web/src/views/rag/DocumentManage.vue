<template>
  <div class="document-manage">
    <div class="page-header">
      <div>
        <h2>文档管理</h2>
        <span class="kb-name">知识库: {{ kbName }}</span>
      </div>
      <el-button type="primary" @click="showUploadDialog = true">
        上传文档
      </el-button>
    </div>

    <el-card class="filter-card">
      <el-form :inline="true">
        <el-form-item label="状态">
          <el-select v-model="statusFilter" placeholder="全部" clearable @change="loadData">
            <el-option label="待处理" value="pending" />
            <el-option label="处理中" value="processing" />
            <el-option label="已完成" value="completed" />
            <el-option label="失败" value="failed" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip />
      <el-table-column prop="fileType" label="类型" width="80" align="center">
        <template #default="{ row }">
          <el-tag size="small">{{ row.fileType.toUpperCase() }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="fileSize" label="大小" width="100" align="center">
        <template #default="{ row }">
          {{ formatFileSize(row.fileSize) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="totalChunks" label="分块数" width="80" align="center" />
      <el-table-column prop="errorMessage" label="错误信息" min-width="150" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="上传时间" width="160">
        <template #default="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" v-if="row.status === 'failed'" @click="handleRetry(row)">
            重试
          </el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">
            删除
          </el-button>
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

    <el-dialog v-model="showUploadDialog" title="上传文档" width="400px">
      <el-upload
        drag
        :auto-upload="false"
        :limit="1"
        :on-change="handleFileChange"
        :file-list="fileList"
        accept=".txt,.md"
      >
        <el-icon><UploadFilled /></el-icon>
        <div>将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">支持 TXT、Markdown 格式</div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedFile" @click="handleUpload">上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const tableData = ref([])
const showUploadDialog = ref(false)
const selectedFile = ref(null)
const fileList = ref([])
const statusFilter = ref(null)

const kbId = ref(null)
const kbName = ref('')

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

const loadData = async () => {
  if (!kbId.value) return
  loading.value = true
  try {
    const params = new URLSearchParams({
      page: pagination.page,
      pageSize: pagination.pageSize
    })
    if (statusFilter.value) {
      params.append('status', statusFilter.value)
    }
    const res = await fetch(`/api/rag/knowledge-bases/${kbId.value}/documents?${params}`)
    const data = await res.json()
    tableData.value = data.records || []
    pagination.total = data.total || 0
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const handleFileChange = (file) => {
  selectedFile.value = file.raw
}

const handleUpload = async () => {
  if (!selectedFile.value) return

  const formData = new FormData()
  formData.append('file', selectedFile.value)

  try {
    await fetch(`/api/rag/knowledge-bases/${kbId.value}/documents`, {
      method: 'POST',
      body: formData
    })
    ElMessage.success('上传成功')
    showUploadDialog.value = false
    selectedFile.value = null
    fileList.value = []
    loadData()
  } catch (e) {
    ElMessage.error('上传失败')
  }
}

const handleRetry = async (row) => {
  await fetch(`/api/rag/documents/${row.id}/retry`, { method: 'POST' })
  ElMessage.success('已提交重试')
  loadData()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除文档「${row.fileName}」？`, '提示', { type: 'warning' })
  await fetch(`/api/rag/documents/${row.id}`, { method: 'DELETE' })
  ElMessage.success('删除成功')
  loadData()
}

const formatFileSize = (bytes) => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const getStatusType = (status) => {
  const map = { pending: 'info', processing: 'warning', completed: 'success', failed: 'danger' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { pending: '待处理', processing: '处理中', completed: '完成', failed: '失败' }
  return map[status] || status
}

onMounted(() => {
  // 从 URL 获取 kbId（保持为字符串以避免大整数精度丢失）
  const path = window.location.pathname
  const match = path.match(/\/rag\/kb\/(\d+)\/documents/)
  if (match) {
    kbId.value = match[1]
  }
  if (kbId.value) {
    loadData()
  }
})
</script>

<style scoped>
.document-manage {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 5px 0;
}

.kb-name {
  color: #909399;
  font-size: 14px;
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