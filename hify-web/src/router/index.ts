import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/models',
      name: 'models',
      component: () => import('../views/model/ModelList.vue'),
    },
    {
      path: '/providers',
      name: 'providers',
      component: () => import('../views/provider/ProviderList.vue'),
    },
    {
      path: '/agents',
      name: 'agents',
      component: () => import('../views/agent/AgentList.vue'),
    },
    {
      path: '/knowledge',
      name: 'knowledge',
      component: () => import('../views/PlaceholderView.vue'),
    },
    {
      path: '/workflows',
      name: 'workflows',
      component: () => import('../views/PlaceholderView.vue'),
    },
  ],
})

export default router
