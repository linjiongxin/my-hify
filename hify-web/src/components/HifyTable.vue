<script setup lang="ts" generic="T = any">
import { ref, computed, onMounted, watch } from 'vue'

export interface PageParams {
  current: number
  size: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  current?: number
  size?: number
}

export interface TableColumn<T> {
  prop?: string
  label?: string
  width?: string | number
  minWidth?: string | number
  align?: 'left' | 'center' | 'right'
  slot?: string
  formatter?: (row: T, column: any, cellValue: unknown, index: number) => string
}

export interface HifyTableProps<T> {
  columns: TableColumn<T>[]
  api: (params: PageParams) => Promise<PageResult<T>>
  showPagination?: boolean
  pageSize?: number
  immediate?: boolean
}

const props = withDefaults(defineProps<HifyTableProps<T>>(), {
  showPagination: true,
  pageSize: 10,
  immediate: true,
})

const loading = ref(false)
const data = ref<T[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSizeVal = ref(props.pageSize)

const emptyText = computed(() => (loading.value ? '' : '暂无数据'))
const totalPages = computed(() => Math.ceil(total.value / pageSizeVal.value))

async function fetchData() {
  loading.value = true
  try {
    const res = await props.api({
      current: currentPage.value,
      size: pageSizeVal.value,
    })
    data.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function handleCurrentChange(val: number) {
  currentPage.value = val
  fetchData()
}

function handleSizeChange(val: number) {
  currentPage.value = 1
  pageSizeVal.value = val
  fetchData()
}

function refresh() {
  currentPage.value = 1
  fetchData()
}

defineExpose({
  refresh,
})

onMounted(() => {
  if (props.immediate) {
    fetchData()
  }
})

watch(() => props.api, refresh, { deep: false })

// 计算显示的页码按钮
const visiblePages = computed(() => {
  const total = totalPages.value
  const current = currentPage.value
  const pages: (number | '...')[] = []

  if (total <= 7) {
    for (let i = 1; i <= total; i++) pages.push(i)
  } else {
    pages.push(1)
    if (current > 3) pages.push('...')
    const start = Math.max(2, current - 1)
    const end = Math.min(total - 1, current + 1)
    for (let i = start; i <= end; i++) pages.push(i)
    if (current < total - 2) pages.push('...')
    pages.push(total)
  }
  return pages
})
</script>

<template>
  <div class="hify-table">
    <el-table
      v-loading="loading"
      :data="data"
      stripe
      style="width: 100%"
    >
      <el-table-column
        v-for="(col, idx) in columns"
        :key="col.prop || col.slot || idx"
        :prop="col.prop"
        :label="col.label"
        :width="col.width"
        :min-width="col.minWidth"
        :align="col.align || 'left'"
        :formatter="col.formatter"
      >
        <template v-if="col.slot" #default="scope">
          <slot :name="col.slot" :row="scope.row as T" :index="scope.$index" />
        </template>
      </el-table-column>

      <template #empty>
        <el-empty :description="emptyText" />
      </template>
    </el-table>

    <div v-if="showPagination && total > 0" class="pagination-wrapper">
      <div class="hify-pagination">
        <span class="total">共 {{ total }} 条</span>
        <div class="page-sizes">
          <select v-model="pageSizeVal" @change="handleSizeChange(pageSizeVal)">
            <option :value="10">10 条/页</option>
            <option :value="20">20 条/页</option>
            <option :value="50">50 条/页</option>
            <option :value="100">100 条/页</option>
          </select>
        </div>
        <button class="page-btn" :disabled="currentPage <= 1" @click="handleCurrentChange(currentPage - 1)">‹</button>
        <template v-for="p in visiblePages" :key="p">
          <span v-if="p === '...'" class="ellipsis">···</span>
          <button v-else class="page-btn" :class="{ active: p === currentPage }" @click="handleCurrentChange(p as number)">{{ p }}</button>
        </template>
        <button class="page-btn" :disabled="currentPage >= totalPages" @click="handleCurrentChange(currentPage + 1)">›</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.hify-table {
  background-color: var(--bg-surface);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  box-shadow: var(--shadow-sm);
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--space-5);
}

.hify-pagination {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}

.total {
  color: var(--text-secondary);
  margin-right: 12px;
}

.page-sizes select {
  padding: 6px 28px 6px 10px;
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  background: var(--bg-surface);
  cursor: pointer;
  font-size: 14px;
  color: var(--text-regular);
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%23999' d='M2 4l4 4 4-4'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
}

.page-sizes select:focus {
  outline: none;
  border-color: var(--el-color-primary);
}

.page-btn {
  min-width: 32px;
  height: 32px;
  padding: 0;
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  background: var(--bg-surface);
  color: var(--text-regular);
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.page-btn:hover:not(:disabled) {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
}

.page-btn.active {
  background: var(--el-color-primary);
  border-color: var(--el-color-primary);
  color: white;
  font-weight: 500;
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.ellipsis {
  color: var(--text-secondary);
  padding: 0 4px;
  font-size: 14px;
}
</style>
