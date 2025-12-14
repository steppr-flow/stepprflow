<template>
  <div class="space-y-5">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-lg font-semibold text-gray-900">Dashboard</h1>
        <p class="text-xs text-gray-500">Monitor workflow executions</p>
      </div>
      <button @click="refresh" class="btn-sm flex items-center space-x-1.5">
        <svg class="w-3.5 h-3.5" :class="{ 'animate-spin': loading }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
        </svg>
        <span>Refresh</span>
      </button>
    </div>

    <!-- Stats Grid -->
    <div class="grid grid-cols-3 lg:grid-cols-6 gap-3">
      <StatsCard
        :value="stats.total"
        label="Total"
        variant="default"
      />
      <StatsCard
        :value="stats.pending"
        label="Pending"
        variant="warning"
      />
      <StatsCard
        :value="stats.inProgress"
        label="In Progress"
        variant="info"
      />
      <StatsCard
        :value="stats.completed"
        label="Completed"
        variant="success"
      />
      <StatsCard
        :value="stats.failed"
        label="Failed"
        variant="danger"
      />
      <StatsCard
        :value="stats.retryPending"
        label="Retry"
        variant="warning"
      />
    </div>

    <!-- Recent Executions & Workflows -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
      <!-- Recent Executions -->
      <div class="card-compact">
        <div class="px-4 py-3 border-b border-gray-100 flex items-center justify-between">
          <h2 class="text-sm font-medium text-gray-800">Recent Executions</h2>
          <router-link to="/executions" class="text-xs text-primary-600 hover:text-primary-700">
            View all →
          </router-link>
        </div>
        <div class="divide-y divide-gray-50">
          <div
            v-for="execution in recentExecutions"
            :key="execution.executionId"
            class="px-4 py-2.5 hover:bg-gray-50 cursor-pointer"
            @click="goToExecution(execution.executionId)"
          >
            <div class="flex items-center justify-between">
              <div class="min-w-0 flex-1">
                <div class="flex items-center space-x-2">
                  <code class="text-xs text-gray-600 truncate">{{ execution.executionId?.substring(0, 8) }}</code>
                  <StatusBadge :status="execution.status" />
                </div>
                <div class="mt-0.5 flex items-center space-x-3 text-[11px] text-gray-400">
                  <span>{{ execution.topic }}</span>
                  <span>{{ execution.currentStep }}/{{ execution.totalSteps }}</span>
                </div>
              </div>
              <div class="text-[11px] text-gray-400">
                {{ formatDate(execution.createdAt) }}
              </div>
            </div>
          </div>
          <div v-if="!recentExecutions.length" class="p-6 text-center text-xs text-gray-400">
            No recent executions
          </div>
        </div>
      </div>

      <!-- Registered Workflows -->
      <div class="card-compact">
        <div class="px-4 py-3 border-b border-gray-100 flex items-center justify-between">
          <h2 class="text-sm font-medium text-gray-800">Registered Workflows</h2>
          <router-link to="/workflows" class="text-xs text-primary-600 hover:text-primary-700">
            View all →
          </router-link>
        </div>
        <div class="divide-y divide-gray-50">
          <div
            v-for="workflow in workflows"
            :key="workflow.topic"
            class="px-4 py-2.5 hover:bg-gray-50"
          >
            <div class="flex items-center justify-between">
              <div>
                <div class="flex items-center space-x-1.5">
                  <div
                    class="w-1.5 h-1.5 rounded-full"
                    :class="workflow.status === 'ACTIVE' ? 'bg-emerald-500' : 'bg-gray-300'"
                  />
                  <code class="text-xs font-medium text-gray-700">{{ workflow.topic }}</code>
                </div>
                <p class="mt-0.5 text-[11px] text-gray-400 truncate max-w-[200px]">{{ workflow.description || 'No description' }}</p>
              </div>
              <div class="text-right">
                <div class="text-lg font-semibold text-gray-600">{{ workflow.stepCount || workflow.steps }}</div>
                <div class="text-[10px] text-gray-400">steps</div>
              </div>
            </div>
          </div>
          <div v-if="!workflows.length" class="p-6 text-center text-xs text-gray-400">
            No workflows registered
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useWorkflowStore } from '@/stores/workflow'
import StatsCard from '@/components/StatsCard.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { formatDistanceToNow } from 'date-fns'

const router = useRouter()
const store = useWorkflowStore()

const stats = computed(() => store.stats)
const recentExecutions = computed(() => store.recentExecutions)
const workflows = computed(() => store.workflows)
const loading = computed(() => store.loading)

let interval

onMounted(() => {
  store.fetchDashboard()
  // Auto-refresh every 5 seconds
  interval = setInterval(() => store.fetchDashboard(), 5000)
})

onUnmounted(() => {
  clearInterval(interval)
})

function refresh() {
  store.fetchDashboard()
}

function goToExecution(id) {
  router.push(`/executions/${id}`)
}

function formatDate(date) {
  if (!date) return '-'
  return formatDistanceToNow(new Date(date), { addSuffix: true })
}
</script>