<script setup lang="ts">
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
</script>

<template>
  <el-config-provider :locale="zhCn">
    <default-layout />
  </el-config-provider>
</template>
