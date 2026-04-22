<script setup lang="ts" generic="T = any">
import { ref, computed, nextTick, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'

export interface HifyFormDialogProps {
  title?: string
  width?: string | number
  rules?: FormRules
  labelWidth?: string | number
}

const props = withDefaults(defineProps<HifyFormDialogProps>(), {
  title: '表单',
  width: '520px',
  labelWidth: '100px',
})

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
  (e: 'submit', data: T, isEdit: boolean): void
}>()

const visible = defineModel<boolean>({ default: false })

const formRef = ref<FormInstance>()
const formData = ref<T>({} as T)
const submitLoading = ref(false)
const rawData = ref<T | null>(null)

const forceEdit = ref<boolean | undefined>(undefined)

const isEdit = computed(() => {
  if (forceEdit.value !== undefined) {
    return forceEdit.value
  }
  return !!rawData.value
})
const dialogTitle = computed(() => {
  const prefix = isEdit.value ? '编辑' : '新增'
  return `${prefix}${props.title}`
})

function open(data?: T, editing?: boolean) {
  forceEdit.value = editing
  rawData.value = data || null
  visible.value = true
  nextTick(() => {
    formRef.value?.resetFields()
    if (data) {
      formData.value = { ...data }
    } else {
      formData.value = {} as T
    }
  })
}

function close() {
  visible.value = false
}

function handleClosed() {
  submitLoading.value = false
  rawData.value = null
  formData.value = {} as T
  forceEdit.value = undefined
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    submitLoading.value = true
    emit('submit', { ...formData.value } as T, isEdit.value)
  } catch {
    // validation error, do nothing
  }
}

function finish() {
  submitLoading.value = false
  close()
}

watch(visible, (val) => {
  if (!val) {
    nextTick(() => {
      formRef.value?.clearValidate()
    })
  }
})

defineExpose({
  open,
  close,
  finish,
})
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    :width="width"
    align-center
    destroy-on-close
    :close-on-click-modal="false"
    @closed="handleClosed"
  >
    <el-form
      ref="formRef"
      :model="formData as any"
      :rules="rules"
      :label-width="labelWidth"
      class="hify-form"
    >
      <slot :form="formData" :is-edit="isEdit" />
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="close">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          确定
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.hify-form {
  padding: var(--space-2) 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
}

/* 确保 el-select 下拉框在 dialog 之上 */
:deep(.el-select-dropdown) {
  z-index: 3000 !important;
}

:deep(.el-select-dropdown__list) {
  z-index: 3000 !important;
}
</style>
