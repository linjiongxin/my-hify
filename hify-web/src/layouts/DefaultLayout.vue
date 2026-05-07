<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ChatDotRound, Setting, User, Collection, Share, OfficeBuilding, Fold, Expand, ArrowDown, Link } from '@element-plus/icons-vue'

const route = useRoute()
const isCollapsed = ref(false)

const menuItems = [
  { title: '对话', icon: ChatDotRound, path: '/chat' },
  { title: '模型提供商', icon: OfficeBuilding, path: '/providers' },
  { title: '模型管理', icon: Setting, path: '/models' },
  { title: 'Agent 管理', icon: User, path: '/agents' },
  { title: '知识库', icon: Collection, path: '/knowledge' },
  { title: 'Agent 知识库', icon: Link, path: '/rag/agent-kb' },
  { title: '工作流', icon: Share, path: '/workflows' },
]

const breadcrumbMap: Record<string, string> = {
  home: '首页',
  chat: '对话',
  models: '模型配置',
  providers: '模型提供商',
  agents: 'Agent 管理',
  knowledge: '知识库',
  'rag-agent-kb': 'Agent 知识库',
  workflows: '工作流',
}

const breadcrumbs = computed(() => {
  const name = String(route.name || 'home')
  const label = breadcrumbMap[name] || name
  if (name === 'home') {
    return [{ label: '首页', path: '/' }]
  }
  return [
    { label: '首页', path: '/' },
    { label, path: route.path },
  ]
})
</script>

<template>
  <el-container class="default-layout">
    <el-aside :width="isCollapsed ? '72px' : '220px'" class="sidebar">
      <div class="logo" :class="{ collapsed: isCollapsed }">
        <span class="logo-mark">H</span>
        <div v-if="!isCollapsed" class="logo-text-group">
          <span class="logo-text">Hify</span>
          <span class="logo-sub">AI Agent Platform</span>
        </div>
      </div>

      <el-menu
        default-active="/"
        :router="true"
        :collapse="isCollapsed"
        :collapse-transition="false"
        class="el-menu-vertical"
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon class="menu-icon">
            <component :is="item.icon" />
          </el-icon>
          <template #title>
            <span class="menu-title">{{ item.title }}</span>
          </template>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-footer" :class="{ collapsed: isCollapsed }">
        <div class="collapse-btn" @click="isCollapsed = !isCollapsed">
          <el-icon><component :is="isCollapsed ? Expand : Fold" /></el-icon>
        </div>
        <span v-if="!isCollapsed" class="version">v0.1.0</span>
      </div>
    </el-aside>

    <el-container class="main-wrapper">
      <el-header class="header">
        <el-breadcrumb separator="/" class="breadcrumb">
          <el-breadcrumb-item
            v-for="(crumb, idx) in breadcrumbs"
            :key="idx"
            :to="idx < breadcrumbs.length - 1 ? crumb.path : undefined"
          >
            {{ crumb.label }}
          </el-breadcrumb-item>
        </el-breadcrumb>

        <div class="header-meta">
          <div class="user-info">
            <el-avatar :size="32" class="user-avatar">
              <el-icon :size="16"><User /></el-icon>
            </el-avatar>
            <span class="user-name">Admin</span>
            <el-icon class="user-arrow"><ArrowDown /></el-icon>
          </div>
        </div>
      </el-header>
      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.default-layout {
  height: 100%;
}

.sidebar {
  background-color: var(--color-bg-dark);
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--sidebar-border);
  transition: width var(--duration-slow) var(--ease-in-out);
  box-shadow: 4px 0 24px rgba(0, 0, 0, 0.35);
}

.logo {
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 0 var(--space-5);
  gap: var(--space-3);
  border-bottom: 1px solid var(--sidebar-border);
  flex-shrink: 0;
  transition: padding var(--duration-slow) var(--ease-in-out);
}

.logo.collapsed {
  padding: 0;
  justify-content: center;
  gap: 0;
}

.logo-mark {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--color-primary-500), var(--color-primary-700));
  color: var(--text-inverse);
  font-size: var(--text-lg);
  font-weight: 700;
  border-radius: var(--radius-md);
  flex-shrink: 0;
  box-shadow: 0 0 16px rgba(99, 102, 241, 0.35);
}

.logo-text-group {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
  overflow: hidden;
}

.logo-text {
  font-size: var(--text-xl);
  font-weight: 700;
  letter-spacing: -0.02em;
  background: linear-gradient(90deg, var(--color-primary-400) 0%, var(--color-primary-600) 50%, var(--color-secondary-400) 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  filter: drop-shadow(0 0 8px rgba(99, 102, 241, 0.25));
  white-space: nowrap;
}

.logo-sub {
  font-size: 11px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.45);
  letter-spacing: 0.04em;
  white-space: nowrap;
}

.el-menu-vertical {
  background-color: transparent;
  padding: var(--space-3) 0;
  flex: 1;
  overflow-y: auto;
  border-right: none;
}

.el-menu-vertical :deep(.el-menu-item) {
  color: var(--sidebar-text);
  background: transparent;
  border-radius: 0;
  margin: 0 var(--space-3);
  margin-bottom: var(--space-1);
  height: 46px;
  line-height: 46px;
  padding-left: var(--space-4) !important;
  position: relative;
  transition: background-color var(--duration-fast) var(--ease-in-out),
              color var(--duration-fast) var(--ease-in-out);
}

.el-menu-vertical :deep(.el-menu-item:hover) {
  background-color: var(--sidebar-bg-hover);
  color: var(--sidebar-text-hover);
}

.el-menu-vertical :deep(.el-menu-item.is-active) {
  background-color: var(--sidebar-bg-active);
  color: var(--sidebar-text-active);
  font-weight: 500;
}

.el-menu-vertical :deep(.el-menu-item.is-active::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 18px;
  background: linear-gradient(180deg, var(--color-primary-500), var(--color-primary-700));
  border-radius: 0 2px 2px 0;
  box-shadow: 0 0 8px rgba(99, 102, 241, 0.55);
}

.menu-icon {
  margin-right: var(--space-3);
  color: var(--sidebar-icon);
  font-size: var(--text-lg);
  transition: color var(--duration-fast) var(--ease-in-out);
}

.el-menu-vertical :deep(.el-menu-item.is-active .menu-icon),
.el-menu-vertical :deep(.el-menu-item:hover .menu-icon) {
  color: var(--sidebar-icon-active);
}

.menu-title {
  font-size: var(--text-base);
  letter-spacing: 0.01em;
}

.sidebar-footer {
  height: 56px;
  border-top: 1px solid var(--sidebar-border);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-5);
  transition: padding var(--duration-slow) var(--ease-in-out);
}

.sidebar-footer.collapsed {
  padding: 0;
  justify-content: center;
}

.collapse-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  color: rgba(255, 255, 255, 0.55);
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-in-out),
              color var(--duration-fast) var(--ease-in-out);
}

.collapse-btn:hover {
  background-color: var(--sidebar-bg-hover);
  color: #ffffff;
}

.version {
  font-size: 11px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.35);
  letter-spacing: 0.02em;
}

.main-wrapper {
  background-color: var(--color-bg-secondary);
}

.header {
  height: 64px;
  background-color: var(--bg-surface);
  border-bottom: 1px solid var(--border-subtle);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-6);
  flex-shrink: 0;
}

.breadcrumb {
  font-size: var(--text-base);
}

.breadcrumb :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  font-weight: 600;
  color: var(--text-primary);
}

.header-meta {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.user-info {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  cursor: pointer;
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-md);
  transition: background-color var(--duration-fast) var(--ease-in-out);
}

.user-info:hover {
  background-color: var(--bg-hover);
}

.user-avatar {
  background: var(--btn-gradient-primary);
  color: #ffffff;
  flex-shrink: 0;
}

.user-name {
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--text-primary);
}

.user-arrow {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-left: 2px;
}

.main {
  background-color: var(--color-bg-secondary);
  padding: var(--space-6);
  overflow-y: auto;
}
</style>
