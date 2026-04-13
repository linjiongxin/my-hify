<script setup lang="ts">
</script>

<template>
  <div class="home">
    <h1>欢迎来到 Hify</h1>
    <p class="subtitle">面向团队内部的 AI Agent 开发平台</p>

    <el-row :gutter="20" class="cards">
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>模型管理</span>
          </template>
          <div>配置多模型提供商，统一调用网关。</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>Agent 配置</span>
          </template>
          <div>选模型、绑工具、设提示词，快速构建 Agent。</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>知识库 + RAG</span>
          </template>
          <div>上传文档，自动分块与向量检索。</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.home {
  padding: 20px;
}

.subtitle {
  color: #6b7280;
  margin-bottom: 24px;
}

.cards {
  margin-top: 16px;
}

.el-card {
  height: 100%;
}
</style>
