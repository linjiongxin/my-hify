<script setup lang="ts">
import { VueFlow, useVueFlow, type Node, type Edge } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { nextTick } from 'vue'
import type { NodeType } from '@/api/workflow'
import { NODE_TYPE_LABELS, NODE_TYPE_COLORS } from './types'

const FLOW_ID = 'workflow-canvas'

const nodes = defineModel<Node[]>('nodes', { default: () => [] })
const edges = defineModel<Edge[]>('edges', { default: () => [] })

const emit = defineEmits<{
  (e: 'nodeSelect', node: Node | null): void
}>()

const { addNodes, addEdges, onConnect, onNodeClick, onPaneClick, project, findNode } = useVueFlow({ id: FLOW_ID })

onConnect((connection) => {
  addEdges([{
    id: `e-${connection.source}-${connection.target}`,
    source: connection.source,
    target: connection.target,
    data: { condition: undefined, edgeIndex: 0 },
  }])
})

onNodeClick(({ node }) => {
  emit('nodeSelect', node)
})

onPaneClick(() => {
  emit('nodeSelect', null)
})

function onDragOver(event: DragEvent) {
  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
}

function onDrop(event: DragEvent) {
  event.preventDefault()
  const type = event.dataTransfer?.getData('application/vueflow-node-type') as NodeType | undefined
  if (!type) return

  const bounds = (event.currentTarget as HTMLElement).getBoundingClientRect()
  const position = project({
    x: event.clientX - bounds.left,
    y: event.clientY - bounds.top,
  })

  const nodeId = `${type.toLowerCase()}-${Date.now()}`
  const newNode: Node = {
    id: nodeId,
    type: 'default',
    position,
    data: {
      name: NODE_TYPE_LABELS[type],
      type,
      config: {},
    },
    label: NODE_TYPE_LABELS[type],
  }

  addNodes([newNode])

  nextTick(() => {
    const node = findNode(nodeId)
    if (node) {
      emit('nodeSelect', node)
    }
  })
}

function nodeClass(node: Node) {
  const type = node.data?.type as NodeType | undefined
  if (!type) return ''
  return `node-${type.toLowerCase()}`
}

function nodeStyle(node: Node) {
  const type = node.data?.type as NodeType | undefined
  if (!type) return {}
  return {
    '--node-color': NODE_TYPE_COLORS[type],
  }
}
</script>

<template>
  <div class="flow-canvas" @dragover="onDragOver" @drop="onDrop">
    <VueFlow
      :id="FLOW_ID"
      v-model:nodes="nodes"
      v-model:edges="edges"
      :fit-view-on-init="true"
      :node-class="nodeClass"
      :node-style="nodeStyle"
    >
      <Background pattern-color="#e5e7eb" :gap="16" />
      <Controls />
    </VueFlow>
  </div>
</template>

<style>
.flow-canvas {
  flex: 1;
  height: 100%;
  position: relative;
}

/* Vue Flow 节点样式 */
.vue-flow__node {
  border-radius: 8px;
  border: 2px solid var(--node-color, #ccc);
  background: #fff;
  padding: 8px 16px;
  min-width: 120px;
  text-align: center;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: box-shadow 0.2s;
}

.vue-flow__node:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.vue-flow__node.selected {
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.2), 0 4px 16px rgba(0, 0, 0, 0.12);
  border-color: var(--el-color-primary);
}

.vue-flow__node-start {
  border-radius: 24px;
}

.vue-flow__node-end {
  border-radius: 24px;
}

.vue-flow__handle {
  width: 8px;
  height: 8px;
  background: var(--node-color, #ccc);
  border: 2px solid #fff;
}

.vue-flow__edge-path {
  stroke: #9ca3af;
  stroke-width: 2;
}

.vue-flow__edge.selected .vue-flow__edge-path {
  stroke: var(--el-color-primary);
  stroke-width: 3;
}

.vue-flow__connectionline {
  stroke: var(--el-color-primary);
  stroke-width: 2;
}
</style>
