import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '@/views/Dashboard.vue'
import Executions from '@/views/Executions.vue'
import ExecutionDetail from '@/views/ExecutionDetail.vue'
import Workflows from '@/views/Workflows.vue'
import Metrics from '@/views/Metrics.vue'

const routes = [
  {
    path: '/',
    name: 'Dashboard',
    component: Dashboard
  },
  {
    path: '/executions',
    name: 'Executions',
    component: Executions
  },
  {
    path: '/executions/:id',
    name: 'ExecutionDetail',
    component: ExecutionDetail,
    props: true
  },
  {
    path: '/workflows',
    name: 'Workflows',
    component: Workflows
  },
  {
    path: '/metrics',
    name: 'Metrics',
    component: Metrics
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router