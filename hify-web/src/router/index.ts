import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
    },
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/chat',
      name: 'chat',
      component: () => import('../views/chat/ChatView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/models',
      name: 'models',
      component: () => import('../views/model/ModelList.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/providers',
      name: 'providers',
      component: () => import('../views/provider/ProviderList.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/agents',
      name: 'agents',
      component: () => import('../views/agent/AgentList.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/knowledge',
      name: 'knowledge',
      component: () => import('../views/rag/KnowledgeBaseList.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/rag/knowledge-bases',
      name: 'rag-knowledge-bases',
      component: () => import('../views/rag/KnowledgeBaseList.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/rag/kb/:kbId/documents',
      name: 'rag-documents',
      component: () => import('../views/rag/DocumentManage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/rag/agent-kb',
      name: 'rag-agent-kb',
      component: () => import('../views/rag/AgentRagConfig.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/workflows',
      name: 'workflows',
      component: () => import('../views/workflow/WorkflowList.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/workflows/:id/edit',
      name: 'workflow-edit',
      component: () => import('../views/workflow/WorkflowEditor.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/workflow-instances',
      name: 'workflow-instances',
      component: () => import('../views/workflow/WorkflowInstanceList.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/mcp-servers',
      name: 'mcp-servers',
      component: () => import('../views/mcp/McpServerList.vue'),
      meta: { requiresAuth: true },
    },
  ],
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/chat')
  } else {
    next()
  }
})

export default router
