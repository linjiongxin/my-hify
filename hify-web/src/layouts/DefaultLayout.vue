<script setup lang="ts">
const menuItems = [
  { title: '首页', icon: 'el-icon-house', path: '/' },
  { title: '模型管理', icon: 'el-icon-cpu', path: '/models' },
  { title: 'Agent', icon: 'el-icon-bot', path: '/agents' },
  { title: '知识库', icon: 'el-icon-collection', path: '/knowledge' },
  { title: '工作流', icon: 'el-icon-share', path: '/workflows' },
]
</script>

<template>
  <el-container class="default-layout">
    <el-aside width="200px" class="sidebar">
      <div class="logo">Hify</div>
      <el-menu
        default-active="/"
        :router="true"
        class="el-menu-vertical"
        background-color="#1f2937"
        text-color="#e5e7eb"
        active-text-color="#409eff"
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">Hify AI Agent 平台</el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.default-layout {
  height: 100%;
}

.sidebar {
  background-color: #1f2937;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  font-weight: bold;
  border-bottom: 1px solid #374151;
}

.el-menu-vertical {
  border-right: none;
}

.header {
  background-color: #fff;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  font-weight: 500;
  color: #1f2937;
}

.main {
  background-color: #f3f4f6;
}
</style>
