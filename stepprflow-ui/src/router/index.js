import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue')
  },
  {
    path: '/executions',
    name: 'Executions',
    component: () => import('@/views/Executions.vue')
  },
  {
    path: '/executions/:id',
    name: 'ExecutionDetail',
    component: () => import('@/views/ExecutionDetail.vue'),
    props: true
  },
  {
    path: '/workflows',
    name: 'Workflows',
    component: () => import('@/views/Workflows.vue')
  },
  {
    path: '/metrics',
    name: 'Metrics',
    component: () => import('@/views/Metrics.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
