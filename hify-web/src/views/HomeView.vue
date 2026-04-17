<script setup lang="ts">
import { Cpu, ChatDotRound, Collection, Share, ArrowRight, Plus } from '@element-plus/icons-vue'

const features = [
  {
    title: '模型管理',
    desc: '配置多模型提供商，统一调用网关。支持 OpenAI、DeepSeek、通义千问等。',
    icon: Cpu,
    color: 'indigo',
  },
  {
    title: 'Agent 配置',
    desc: '选模型、绑工具、设提示词，快速构建符合业务场景的 AI Agent。',
    icon: ChatDotRound,
    color: 'violet',
  },
  {
    title: '知识库 + RAG',
    desc: '上传文档，自动分块与向量检索，让 Agent 拥有企业专属知识。',
    icon: Collection,
    color: 'teal',
  },
  {
    title: '工作流编排',
    desc: '通过 JSON 配置线性执行与条件分支，实现自动化业务流程。',
    icon: Share,
    color: 'cyan',
  },
]

const stats = [
  { label: '模型数', value: '0', trend: '待配置' },
  { label: 'Agent 数', value: '0', trend: '待创建' },
  { label: '知识库', value: '0', trend: '待上传' },
]
</script>

<template>
  <div class="page">
    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">首页</h1>
        <p class="page-desc">欢迎来到 Hify，从这里开始构建你的 AI Agent 平台</p>
      </div>
      <div class="page-header-right">
        <el-button :icon="Plus" type="primary">新建 Agent</el-button>
        <el-button>查看文档</el-button>
      </div>
    </div>

    <!-- 数据概览 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="8" v-for="stat in stats" :key="stat.label">
        <div class="stat-card">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
          <div class="stat-trend">{{ stat.trend }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 功能卡片 -->
    <div class="content-card">
      <h2 class="section-title">快速开始</h2>
      <el-row :gutter="16" class="cards-row">
        <el-col :span="12" v-for="item in features" :key="item.title">
          <div class="feature-card">
            <div class="feature-header">
              <div class="feature-icon" :class="`feature-icon--${item.color}`">
                <el-icon :size="22">
                  <component :is="item.icon" />
                </el-icon>
              </div>
              <div class="feature-title">{{ item.title }}</div>
            </div>
            <div class="feature-desc">{{ item.desc }}</div>
            <div class="feature-action">
              <el-button type="primary" text class="action-btn">
                进入管理 <el-icon class="action-icon"><ArrowRight /></el-icon>
              </el-button>
            </div>
          </div>
        </el-col>
      </el-row>
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

/* 统计卡片 */
.stats-row {
  margin-bottom: var(--space-6);
}

.stat-card {
  background-color: var(--bg-surface);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  box-shadow: var(--shadow-sm);
  transition: var(--transition-shadow);
}

.stat-card:hover {
  box-shadow: var(--shadow-md);
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
  margin-bottom: var(--space-1);
}

.stat-label {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  margin-bottom: var(--space-1);
}

.stat-trend {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

/* 内容卡片 */
.content-card {
  background-color: var(--bg-surface);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  box-shadow: var(--shadow-sm);
}

.section-title {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--space-4);
}

.cards-row {
  margin-top: var(--space-2);
}

.cards-row :deep(.el-col) {
  margin-bottom: var(--space-4);
}

.feature-card {
  height: 100%;
  cursor: pointer;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  background-color: var(--bg-surface);
  transition: border-color var(--duration-fast) var(--ease-in-out),
              box-shadow var(--duration-fast) var(--ease-in-out);
}

.feature-card:hover {
  border-color: var(--border-brand);
  box-shadow: var(--shadow-md);
}

.feature-header {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-3);
}

.feature-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.feature-icon--indigo {
  background-color: var(--color-primary-50);
  color: var(--color-primary-600);
}

.feature-icon--violet {
  background-color: #f3e8ff;
  color: #7c3aed;
}

.feature-icon--teal {
  background-color: var(--color-secondary-50);
  color: var(--color-secondary-600);
}

.feature-icon--cyan {
  background-color: #ecfeff;
  color: #0891b2;
}

.feature-title {
  font-size: var(--text-md);
  font-weight: 600;
  color: var(--text-primary);
}

.feature-desc {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: var(--space-4);
}

.feature-action {
  display: flex;
  justify-content: flex-end;
}

.action-btn {
  padding: 0;
  font-weight: 500;
  background: transparent !important;
  border-color: transparent !important;
  box-shadow: none !important;
  color: var(--color-primary-600) !important;
}

.action-btn:hover {
  background: var(--color-primary-50) !important;
  color: var(--color-primary-700) !important;
}

.action-icon {
  margin-left: 2px;
  transition: transform var(--duration-fast) var(--ease-out);
}

.action-btn:hover .action-icon {
  transform: translateX(2px);
}
</style>
