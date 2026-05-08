<script setup lang="ts">
import { NODE_TYPE_LABELS, NODE_TYPE_COLORS } from './types'
import type { NodeType } from '@/api/workflow'

const nodeTypes: NodeType[] = ['START', 'END', 'LLM', 'TOOL', 'CONDITION', 'APPROVAL', 'API_CALL', 'KNOWLEDGE']

function onDragStart(event: DragEvent, type: NodeType) {
  if (event.dataTransfer) {
    event.dataTransfer.setData('application/vueflow-node-type', type)
    event.dataTransfer.effectAllowed = 'move'
  }
}
</script>

<template>
  <div class="node-palette">
    <div class="palette-title">节点</div>
    <div class="palette-list">
      <div
        v-for="t in nodeTypes"
        :key="t"
        class="palette-item"
        :style="{ borderLeftColor: NODE_TYPE_COLORS[t] }"
        draggable="true"
        @dragstart="onDragStart($event, t)"
      >
        <span class="dot" :style="{ backgroundColor: NODE_TYPE_COLORS[t] }" />
        <span class="label">{{ NODE_TYPE_LABELS[t] }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.node-palette {
  width: 180px;
  background: var(--bg-surface);
  border-right: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  height: 100%;
}
.palette-title {
  padding: 16px;
  font-weight: 600;
  font-size: 14px;
  border-bottom: 1px solid var(--border-subtle);
}
.palette-list {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
}
.palette-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 6px;
  background: var(--color-bg-secondary);
  border-left: 3px solid transparent;
  cursor: grab;
  transition: background 0.2s;
}
.palette-item:hover {
  background: var(--bg-hover);
}
.palette-item:active {
  cursor: grabbing;
}
.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}
.label {
  font-size: 13px;
  color: var(--text-primary);
}
</style>
