<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { Node, Edge } from '@vue-flow/core'
import { ArrowLeft, Check, CircleCheck } from '@element-plus/icons-vue'
import NodePalette from '@/components/workflow/NodePalette.vue'
import FlowCanvas from '@/components/workflow/FlowCanvas.vue'
import NodeConfigPanel from '@/components/workflow/NodeConfigPanel.vue'
import { notifySuccess, notifyError } from '@/utils/notify'
import {
  getWorkflowDetail,
  getWorkflowNodes,
  getWorkflowEdges,
  saveWorkflowNodes,
  saveWorkflowEdges,
  updateWorkflow,
  type Workflow,
} from '@/api/workflow'
import { toFlowNode, toFlowEdge, toNodeDTO, toEdgeDTO } from '@/components/workflow/types'

const route = useRoute()
const router = useRouter()
const workflowId = String(route.params.id)

const workflow = ref<Workflow | null>(null)
const nodes = ref<Node[]>([])
const edges = ref<Edge[]>([])
const selectedNode = ref<Node | null>(null)
const loading = ref(false)
const saving = ref(false)

function handleNodeSelect(node: Node | null) {
  selectedNode.value = node
}

async function loadWorkflow() {
  loading.value = true
  try {
    const [detail, nodeList, edgeList] = await Promise.all([
      getWorkflowDetail(workflowId),
      getWorkflowNodes(workflowId),
      getWorkflowEdges(workflowId),
    ])
    workflow.value = detail
    nodes.value = nodeList.map(toFlowNode)
    edges.value = edgeList.map(toFlowEdge)
  } catch (err: any) {
    notifyError(err.message || '加载工作流失败')
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    const nodeDTOs = (nodes.value as any[]).map(n => toNodeDTO(n, workflowId))
    const edgeDTOs = (edges.value as any[]).map(e => toEdgeDTO(e, workflowId))
    await Promise.all([
      saveWorkflowNodes(workflowId, nodeDTOs),
      saveWorkflowEdges(workflowId, edgeDTOs),
    ])
    notifySuccess('保存成功')
  } catch (err: any) {
    notifyError(err.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handlePublish() {
  try {
    await updateWorkflow(workflowId, { status: 'published' })
    if (workflow.value) {
      workflow.value.status = 'published'
    }
    notifySuccess('发布成功')
  } catch (err: any) {
    notifyError(err.message || '发布失败')
  }
}

function goBack() {
  router.push('/workflows')
}

onMounted(() => {
  loadWorkflow()
})
</script>

<template>
  <div class="workflow-editor">
    <!-- 顶部工具栏 -->
    <div class="editor-header">
      <div class="header-left">
        <el-button :icon="ArrowLeft" text @click="goBack">返回</el-button>
        <div class="workflow-title">
          <span class="title-text">{{ workflow?.name || '加载中...' }}</span>
          <el-tag v-if="workflow?.status" size="small" :type="workflow.status === 'published' ? 'success' : workflow.status === 'draft' ? 'info' : 'danger'">
            {{ workflow.status === 'published' ? '已发布' : workflow.status === 'draft' ? '草稿' : '已禁用' }}
          </el-tag>
        </div>
      </div>
      <div class="header-right">
        <el-button :icon="Check" :loading="saving" @click="handleSave">保存</el-button>
        <el-button :icon="CircleCheck" type="primary" @click="handlePublish">发布</el-button>
      </div>
    </div>

    <!-- 三栏主体 -->
    <div v-loading="loading" class="editor-body">
      <NodePalette />
      <FlowCanvas v-model:nodes="nodes" v-model:edges="edges" @node-select="handleNodeSelect" />
      <NodeConfigPanel :selected-node="selectedNode" />
    </div>
  </div>
</template>

<style scoped>
.workflow-editor {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px);
  margin: calc(-1 * var(--space-6));
}

.editor-header {
  height: 56px;
  background: var(--bg-surface);
  border-bottom: 1px solid var(--border-subtle);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-5);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.workflow-title {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.title-text {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.editor-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}
</style>
