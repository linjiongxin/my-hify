<script setup lang="ts">
import { computed } from 'vue'
import type { Node } from '@vue-flow/core'
import { NODE_TYPE_LABELS, NODE_TYPE_COLORS } from './types'
import type { NodeType } from '@/api/workflow'
import LlmNodeConfig from './node-configs/LlmNodeConfig.vue'
import ToolNodeConfig from './node-configs/ToolNodeConfig.vue'
import ConditionNodeConfig from './node-configs/ConditionNodeConfig.vue'
import ApprovalNodeConfig from './node-configs/ApprovalNodeConfig.vue'
import ApiCallNodeConfig from './node-configs/ApiCallNodeConfig.vue'
import KnowledgeNodeConfig from './node-configs/KnowledgeNodeConfig.vue'
import StartEndNodeConfig from './node-configs/StartEndNodeConfig.vue'

const props = defineProps<{
  selectedNode: Node | null
}>()

const nodeType = computed(() => (props.selectedNode?.data?.type as NodeType) || null)

const configComponent = computed(() => {
  switch (nodeType.value) {
    case 'LLM': return LlmNodeConfig
    case 'TOOL': return ToolNodeConfig
    case 'CONDITION': return ConditionNodeConfig
    case 'APPROVAL': return ApprovalNodeConfig
    case 'API_CALL': return ApiCallNodeConfig
    case 'KNOWLEDGE': return KnowledgeNodeConfig
    case 'START':
    case 'END': return StartEndNodeConfig
    default: return null
  }
})

const typeLabel = computed(() => {
  if (!nodeType.value) return ''
  return NODE_TYPE_LABELS[nodeType.value]
})

const typeColor = computed(() => {
  if (!nodeType.value) return ''
  return NODE_TYPE_COLORS[nodeType.value]
})

const nodeName = computed({
  get() {
    return String(props.selectedNode?.data?.name || '')
  },
  set(val: string) {
    if (props.selectedNode) {
      props.selectedNode.data = { ...props.selectedNode.data, name: val }
      props.selectedNode.label = val
    }
  },
})

const nodeConfig = computed({
  get() {
    return (props.selectedNode?.data?.config as Record<string, unknown>) || {}
  },
  set(val: Record<string, unknown>) {
    if (props.selectedNode) {
      props.selectedNode.data = { ...props.selectedNode.data, config: val }
    }
  },
})
</script>

<template>
  <div class="node-config-panel">
    <div v-if="!selectedNode" class="empty-tip">
      <p>请选择画布中的节点进行配置</p>
    </div>
    <template v-else>
      <div class="panel-header">
        <div class="type-badge" :style="{ backgroundColor: typeColor + '20', color: typeColor, borderColor: typeColor }">
          {{ typeLabel }}
        </div>
      </div>

      <div class="panel-body">
        <el-form label-position="top" size="small">
          <el-form-item label="节点名称">
            <el-input v-model="nodeName" />
          </el-form-item>
          <el-form-item label="节点 ID">
            <el-input :model-value="selectedNode.id" disabled />
          </el-form-item>

          <el-divider />

          <component :is="configComponent" v-if="configComponent" v-model="nodeConfig" />
        </el-form>
      </div>
    </template>
  </div>
</template>

<style scoped>
.node-config-panel {
  width: 300px;
  background: var(--bg-surface);
  border-left: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow-y: auto;
}
.empty-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-secondary);
  font-size: 14px;
}
.panel-header {
  padding: 16px;
  border-bottom: 1px solid var(--border-subtle);
}
.type-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid;
}
.panel-body {
  padding: 16px;
  flex: 1;
}
</style>
