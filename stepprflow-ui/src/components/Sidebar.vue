<template>
  <aside class="fixed inset-y-0 left-0 w-48 bg-white border-r border-gray-200">
    <!-- Logo -->
    <div class="h-12 flex items-center px-3 border-b border-gray-100">
      <div class="flex items-center space-x-2">
        <div class="w-6 h-6 bg-primary-600 rounded flex items-center justify-center">
          <img :src="logoSrc" alt="Steppr Flow" class="w-5 h-5">
        </div>
        <span class="text-sm font-semibold text-gray-800">Steppr Flow</span>
      </div>
    </div>

    <!-- Navigation -->
    <nav class="p-2 space-y-0.5">
      <router-link
        v-for="item in navigation"
        :key="item.path"
        :to="item.path"
        :data-cy="'nav-' + item.path.replace('/', '') || 'nav-dashboard'"
        class="flex items-center space-x-2 px-2.5 py-1.5 rounded text-xs transition-colors"
        :class="[
          $route.path === item.path
            ? 'bg-primary-50 text-primary-700'
            : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
        ]"
      >
        <component :is="item.icon" class="w-4 h-4" />
        <span class="font-medium">{{ item.name }}</span>
      </router-link>
    </nav>

    <!-- Stats Summary -->
    <div class="absolute bottom-0 left-0 right-0 p-2 border-t border-gray-100 bg-gray-50/50">
      <div class="grid grid-cols-2 gap-1.5">
        <div class="bg-white rounded px-2 py-1.5 text-center border border-gray-100">
          <div class="text-sm font-medium text-emerald-600">{{ stats.completed }}</div>
          <div class="text-[10px] text-gray-400">Completed</div>
        </div>
        <div class="bg-white rounded px-2 py-1.5 text-center border border-gray-100">
          <div class="text-sm font-medium text-red-600">{{ stats.failed }}</div>
          <div class="text-[10px] text-gray-400">Failed</div>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { computed, h } from 'vue'
import { useWorkflowStore } from '@/stores/workflow'
import logoSrc from '@/assets/images/stepprflow-mini-m.png'

const store = useWorkflowStore()
const stats = computed(() => store.stats)

// Icons as render functions
const DashboardIcon = {
  render: () => h('svg', { fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M4 5a1 1 0 011-1h14a1 1 0 011 1v2a1 1 0 01-1 1H5a1 1 0 01-1-1V5zM4 13a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H5a1 1 0 01-1-1v-6zM16 13a1 1 0 011-1h2a1 1 0 011 1v6a1 1 0 01-1 1h-2a1 1 0 01-1-1v-6z' })
  ])
}

const ListIcon = {
  render: () => h('svg', { fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M4 6h16M4 10h16M4 14h16M4 18h16' })
  ])
}

const WorkflowIcon = {
  render: () => h('svg', { fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M9 17V7m0 10a2 2 0 01-2 2H5a2 2 0 01-2-2V7a2 2 0 012-2h2a2 2 0 012 2m0 10a2 2 0 002 2h2a2 2 0 002-2M9 7a2 2 0 012-2h2a2 2 0 012 2m0 10V7m0 10a2 2 0 002 2h2a2 2 0 002-2V7a2 2 0 00-2-2h-2a2 2 0 00-2 2' })
  ])
}

const MetricsIcon = {
  render: () => h('svg', { fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z' })
  ])
}

const navigation = [
  { name: 'Dashboard', path: '/', icon: DashboardIcon },
  { name: 'Executions', path: '/executions', icon: ListIcon },
  { name: 'Workflows', path: '/workflows', icon: WorkflowIcon },
  { name: 'Metrics', path: '/metrics', icon: MetricsIcon }
]
</script>