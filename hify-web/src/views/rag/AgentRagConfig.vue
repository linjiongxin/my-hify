<template>
  <div class="agent-rag-config">
    <div class="page-header">
      <h2>Agent RAG 配置</h2>
    </div>

    <el-row :gutter="20">
      <el-col :span="8">
        <el-card title="选择 Agent">
          <el-form :inline="true">
            <el-form-item label="Agent">
              <el-select v-model="selectedAgentId" placeholder="请选择 Agent" @change="loadAgentBindings">
                <el-option
                  v-for="agent in agentList"
                  :key="agent.id"
                  :label="agent.name"
                  :value="agent.id"
                />
              </el-select>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card class="mt-20" title="已绑定知识库">
          <template #header>
            <span>已绑定知识库</span>
            <el-button size="small" type="primary" @click="showBindDialog = true">
              添加绑定
            </el-button>
          </template>

          <el-table :data="bindings" v-loading="bindingsLoading" stripe>
            <el-table-column prop="kbName" label="知识库" min-width="120" />
            <el-table-column prop="topK" label="Top-K" width="70" align="center" />
            <el-table-column label="相似度阈值" width="100" align="center">
              <template #default="{ row }">
                {{ row.similarityThreshold }}
              </template>
            </el-table-column>
            <el-table-column prop="enabled" label="状态" width="70" align="center">
              <template #default="{ row }">
                <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
                  {{ row.enabled ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button size="small" @click="handleEditBinding(row)">编辑</el-button>
                <el-button size="small" type="danger" @click="handleUnbind(row)">解绑</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-if="!selectedAgentId" description="请先选择 Agent" />
          <el-empty v-else-if="bindings.length === 0" description="暂无绑定" />
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card title="RAG 检索测试">
          <template #header>
            <span>检索测试</span>
          </template>

          <el-input
            v-model="testQuery"
            type="textarea"
            :rows="3"
            placeholder="输入测试查询，验证 RAG 检索效果..."
          />
          <div class="mt-10">
            <el-button type="primary" :disabled="!selectedAgentId || !testQuery" @click="handleTestSearch">
              测试检索
            </el-button>
          </div>

          <div v-if="testResults.length > 0" class="search-results mt-20">
            <h4>检索结果 ({{ testResults.length }} 条)</h4>
            <el-divider />
            <div v-for="(result, index) in testResults" :key="index" class="result-item">
              <div class="result-header">
                <span class="result-index">[{{ index + 1 }}]</span>
                <span class="result-similarity">相似度: {{ result.similarity?.toFixed(4) }}</span>
              </div>
              <div class="result-content">{{ result.content }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="showBindDialog" title="绑定知识库" width="400px">
      <el-form :model="bindForm" label-width="100px">
        <el-form-item label="知识库">
          <el-select v-model="bindForm.kbId" placeholder="选择知识库">
            <el-option
              v-for="kb in availableKbs"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Top-K">
          <el-input-number v-model="bindForm.topK" :min="1" :max="20" />
        </el-form-item>
        <el-form-item label="相似度阈值">
          <el-input-number v-model="bindForm.similarityThreshold" :min="0" :max="1" :step="0.05" :precision="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showBindDialog = false">取消</el-button>
        <el-button type="primary" @click="handleBind">绑定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showEditDialog" title="编辑绑定" width="400px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="Top-K">
          <el-input-number v-model="editForm.topK" :min="1" :max="20" />
        </el-form-item>
        <el-form-item label="相似度阈值">
          <el-input-number v-model="editForm.similarityThreshold" :min="0" :max="1" :step="0.05" :precision="2" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="editForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateBinding">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const agentList = ref([])
const selectedAgentId = ref(null)
const bindings = ref([])
const knowledgeBases = ref([])
const bindingsLoading = ref(false)

const showBindDialog = ref(false)
const showEditDialog = ref(false)

const bindForm = reactive({
  kbId: null,
  topK: 5,
  similarityThreshold: 0.7
})

const editForm = reactive({
  agentId: null,
  kbId: null,
  topK: 5,
  similarityThreshold: 0.7,
  enabled: true
})

const testQuery = ref('')
const testResults = ref([])

const availableKbs = computed(() => {
  const boundKbIds = bindings.value.map(b => b.kbId)
  return knowledgeBases.value.filter(kb => !boundKbIds.includes(kb.id) && kb.enabled)
})

const loadAgents = async () => {
  try {
    const res = await fetch('/api/agent?pageNum=1&pageSize=100')
    const result = await res.json()
    agentList.value = result.data?.records || []
  } catch (e) {
    ElMessage.error('加载 Agent 列表失败')
  }
}

const loadKnowledgeBases = async () => {
  try {
    const res = await fetch('/api/rag/knowledge-bases?enabled=true')
    const data = await res.json()
    knowledgeBases.value = data.records || []
  } catch (e) {
    ElMessage.error('加载知识库列表失败')
  }
}

const loadAgentBindings = async () => {
  if (!selectedAgentId.value) {
    bindings.value = []
    return
  }
  bindingsLoading.value = true
  try {
    const res = await fetch(`/api/rag/agent-kb/agent/${selectedAgentId.value}`)
    bindings.value = await res.json()

    // 补充知识库名称
    bindings.value.forEach(binding => {
      const kb = knowledgeBases.value.find(k => k.id === binding.kbId)
      if (kb) {
        binding.kbName = kb.name
      }
    })
  } catch (e) {
    ElMessage.error('加载绑定信息失败')
  } finally {
    bindingsLoading.value = false
  }
}

const handleBind = async () => {
  if (!bindForm.kbId) {
    ElMessage.warning('请选择知识库')
    return
  }
  try {
    await fetch('/api/rag/agent-kb/bind', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        agentId: selectedAgentId.value,
        kbId: bindForm.kbId,
        topK: bindForm.topK,
        similarityThreshold: bindForm.similarityThreshold
      })
    })
    ElMessage.success('绑定成功')
    showBindDialog.value = false
    loadAgentBindings()
  } catch (e) {
    ElMessage.error('绑定失败')
  }
}

const handleEditBinding = (row) => {
  editForm.agentId = row.agentId
  editForm.kbId = row.kbId
  editForm.topK = row.topK
  editForm.similarityThreshold = row.similarityThreshold
  editForm.enabled = row.enabled
  showEditDialog.value = true
}

const handleUpdateBinding = async () => {
  try {
    await fetch('/api/rag/agent-kb/update', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        agentId: editForm.agentId,
        kbId: editForm.kbId,
        topK: editForm.topK,
        similarityThreshold: editForm.similarityThreshold
      })
    })
    ElMessage.success('更新成功')
    showEditDialog.value = false
    loadAgentBindings()
  } catch (e) {
    ElMessage.error('更新失败')
  }
}

const handleUnbind = async (row) => {
  await ElMessageBox.confirm('确定解绑该知识库？', '提示', { type: 'warning' })
  try {
    await fetch(`/api/rag/agent-kb/unbind?agentId=${row.agentId}&kbId=${row.kbId}`, {
      method: 'DELETE'
    })
    ElMessage.success('解绑成功')
    loadAgentBindings()
  } catch (e) {
    ElMessage.error('解绑失败')
  }
}

const handleTestSearch = async () => {
  // TODO: 实现 RAG 检索测试 API
  ElMessage.info('测试检索功能开发中')
}

onMounted(() => {
  loadAgents()
  loadKnowledgeBases()
})
</script>

<style scoped>
.agent-rag-config {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
}

.mt-10 {
  margin-top: 10px;
}

.mt-20 {
  margin-top: 20px;
}

.search-results {
  max-height: 400px;
  overflow-y: auto;
}

.result-item {
  padding: 10px;
  border-bottom: 1px solid #eee;
}

.result-item:last-child {
  border-bottom: none;
}

.result-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
  font-size: 12px;
  color: #909399;
}

.result-content {
  color: #333;
  line-height: 1.6;
}
</style>