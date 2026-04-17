<script setup lang="ts">
import { useRoute } from 'vue-router'
import { Plus, RefreshLeft } from '@element-plus/icons-vue'

const route = useRoute()

const pageTitles: Record<string, string> = {
  models: '模型管理',
  agents: 'Agent 管理',
  knowledge: '知识库',
  workflows: '工作流',
}

const pageDescs: Record<string, string> = {
  models: '配置和管理多模型提供商，统一调用网关',
  agents: '创建和配置 AI Agent，绑定模型与工具',
  knowledge: '上传文档构建企业知识库，支持向量检索',
  workflows: '通过 JSON 配置编排自动化业务流程',
}

const name = String(route.name || '')
const title = pageTitles[name] || '页面'
const desc = pageDescs[name] || '功能页面'
</script>

<template>
  <div class="page">
    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">{{ title }}</h1>
        <p class="page-desc">{{ desc }}</p>
      </div>
      <div class="page-header-right">
        <el-button :icon="Plus" type="primary">新建</el-button>
        <el-button :icon="RefreshLeft">刷新</el-button>
      </div>
    </div>

    <!-- 内容卡片 -->
    <div class="content-card">
      <div class="placeholder">
        <el-empty :image-size="120">
          <template #image>
            <div class="empty-illustration">
              <svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
                <rect x="8" y="12" width="48" height="40" rx="4" stroke="currentColor" stroke-width="2" />
                <path d="M8 20H56" stroke="currentColor" stroke-width="2" />
                <circle cx="16" cy="16" r="1.5" fill="currentColor" />
                <circle cx="22" cy="16" r="1.5" fill="currentColor" />
                <circle cx="28" cy="16" r="1.5" fill="currentColor" />
                <path d="M24 36L30 30L36 36" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                <circle cx="38" cy="28" r="3" stroke="currentColor" stroke-width="2" />
              </svg>
            </div>
          </template>
          <template #description>
            <p class="empty-title">页面开发中</p>
            <p class="route-name">{{ route.name }}</p>
          </template>
        </el-empty>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  max-width: 960px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: var(--space-6);
  gap: var(--space-4);
}

.page-header-left {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.page-title {
  font-size: var(--text-2xl);
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.page-desc {
  font-size: var(--text-base);
  color: var(--text-secondary);
  line-height: 1.5;
}

.page-header-right {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex-shrink: 0;
}

.content-card {
  background-color: var(--bg-surface);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  box-shadow: var(--shadow-sm);
}

.placeholder {
  padding: var(--space-10) 0;
}

.empty-illustration {
  width: 120px;
  height: 120px;
  margin: 0 auto var(--space-5);
  color: var(--color-primary-200);
  background-color: var(--color-primary-50);
  border-radius: var(--radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-5);
}

.empty-title {
  font-size: var(--text-lg);
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: var(--space-1);
}

.route-name {
  color: var(--text-tertiary);
  font-size: var(--text-xs);
  margin-top: var(--space-1);
}
</style>
