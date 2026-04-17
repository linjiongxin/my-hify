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

const emit = defineEmits<{
  (e: 'update:pageParams', params: PageParams): void
}>()

const loading = ref(false)
const data = ref<T[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSizeVal = computed(() => props.pageSize)

const emptyText = computed(() => (loading.value ? '' : '暂无数据'))

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
  // pageSize is prop-controlled in simple mode, just refetch
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

    <div v-if="showPagination" class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSizeVal"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="handleCurrentChange"
        @size-change="handleSizeChange"
      />
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
</style>
