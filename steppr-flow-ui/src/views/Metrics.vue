<template>
  <div class="space-y-4">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-lg font-semibold text-gray-900">Metrics</h1>
        <p class="text-xs text-gray-500">Real-time performance metrics</p>
      </div>
      <div class="flex items-center space-x-3">
        <span v-if="lastUpdated" class="text-[10px] text-gray-400">
          Updated: {{ formatTime(lastUpdated) }}
        </span>
        <button @click="refresh" class="btn-sm flex items-center space-x-1.5">
          <svg class="w-3.5 h-3.5" :class="{ 'animate-spin': loading }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
          <span>Refresh</span>
        </button>
      </div>
    </div>

    <!-- Health Status Banner -->
    <div class="rounded-lg p-3 flex items-center justify-between" :class="healthBannerClass">
      <div class="flex items-center space-x-2">
        <div class="w-6 h-6 rounded-full flex items-center justify-center" :class="healthIconBgClass">
          <svg v-if="healthStatus === 'healthy'" class="w-3.5 h-3.5 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
          </svg>
          <svg v-else-if="healthStatus === 'warning'" class="w-3.5 h-3.5 text-amber-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01" />
          </svg>
          <svg v-else class="w-3.5 h-3.5 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </div>
        <div>
          <div class="text-xs font-medium" :class="healthTextClass">
            {{ healthStatus === 'healthy' ? 'Healthy' : healthStatus === 'warning' ? 'Warning' : 'Critical' }}
          </div>
          <div class="text-[10px]" :class="healthSubtextClass">
            {{ (dashboard.globalSuccessRate ?? 0).toFixed(1) }}% success
          </div>
        </div>
      </div>
      <div class="text-right">
        <div class="text-lg font-semibold" :class="healthTextClass">{{ totalProcessed }}</div>
        <div class="text-[10px]" :class="healthSubtextClass">processed</div>
      </div>
    </div>

    <!-- Global Stats -->
    <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
      <StatsCard :value="dashboard.totalStarted" label="Started" variant="primary" />
      <StatsCard :value="dashboard.totalCompleted" label="Completed" variant="success" />
      <StatsCard :value="dashboard.totalFailed" label="Failed" variant="danger" />
      <StatsCard :value="dashboard.totalActive" label="Active" variant="info" />
    </div>

    <!-- Rate Cards -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
      <div class="bg-white border border-gray-100 rounded-lg p-3">
        <div class="flex items-center justify-between mb-2">
          <span class="text-[11px] text-gray-400">Success Rate</span>
          <span class="text-sm font-semibold text-emerald-600">{{ (dashboard.globalSuccessRate ?? 0).toFixed(1) }}%</span>
        </div>
        <div class="h-1 bg-gray-100 rounded-full overflow-hidden">
          <div class="h-full bg-emerald-500 rounded-full" :style="{ width: `${dashboard.globalSuccessRate ?? 0}%` }" />
        </div>
      </div>

      <div class="bg-white border border-gray-100 rounded-lg p-3">
        <div class="flex items-center justify-between mb-2">
          <span class="text-[11px] text-gray-400">Failure Rate</span>
          <span class="text-sm font-semibold text-red-600">{{ (dashboard.globalFailureRate ?? 0).toFixed(1) }}%</span>
        </div>
        <div class="h-1 bg-gray-100 rounded-full overflow-hidden">
          <div class="h-full bg-red-500 rounded-full" :style="{ width: `${dashboard.globalFailureRate ?? 0}%` }" />
        </div>
      </div>

      <div class="bg-white border border-gray-100 rounded-lg p-3">
        <div class="flex items-center justify-between mb-2">
          <span class="text-[11px] text-gray-400">Retries / DLQ</span>
          <div class="flex items-center space-x-2 text-xs">
            <span class="font-medium text-amber-600">{{ dashboard.totalRetries ?? 0 }}</span>
            <span class="text-gray-300">/</span>
            <span class="font-medium text-red-600">{{ dashboard.totalDlq ?? 0 }}</span>
          </div>
        </div>
        <div class="flex space-x-1">
          <div class="flex-1 h-1 bg-amber-100 rounded-full"><div class="h-full bg-amber-400 rounded-full w-full" /></div>
          <div class="flex-1 h-1 bg-red-100 rounded-full"><div class="h-full bg-red-400 rounded-full w-full" /></div>
        </div>
      </div>
    </div>

    <!-- Health & Outbox Row -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
      <HealthPanel :health="health" />
      <OutboxPanel :outbox="outbox" />
    </div>

    <!-- Circuit Breakers -->
    <div class="bg-white border border-gray-100 rounded-lg">
      <div class="px-4 py-3 border-b border-gray-50 flex items-center justify-between">
        <div>
          <h2 class="text-sm font-medium text-gray-800">Circuit Breakers</h2>
          <p class="text-[10px] text-gray-400">Broker resilience</p>
        </div>
        <span
          v-if="circuitBreakerConfig"
          class="px-1.5 py-0.5 text-[10px] font-medium rounded"
          :class="circuitBreakerConfig.enabled ? 'bg-emerald-50 text-emerald-600' : 'bg-gray-100 text-gray-500'"
        >
          {{ circuitBreakerConfig.enabled ? 'On' : 'Off' }}
        </span>
      </div>

      <div v-if="circuitBreakers.length === 0" class="p-6 text-center text-xs text-gray-400">
        No circuit breakers registered
      </div>

      <div v-else class="divide-y divide-gray-50">
        <div v-for="cb in circuitBreakers" :key="cb.name" class="px-4 py-3 flex items-center justify-between">
          <div class="flex items-center space-x-2">
            <div class="w-5 h-5 rounded-full flex items-center justify-center" :class="getCircuitBreakerBgClass(cb.state)">
              <svg v-if="cb.state === 'CLOSED'" class="w-3 h-3 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
              </svg>
              <svg v-else-if="cb.state === 'OPEN'" class="w-3 h-3 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
              <svg v-else class="w-3 h-3 text-amber-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01" />
              </svg>
            </div>
            <div>
              <div class="text-xs font-medium text-gray-700">{{ cb.name }}</div>
              <div class="flex items-center space-x-1.5 mt-0.5">
                <span class="px-1 py-0.5 text-[9px] font-medium rounded" :class="getCircuitBreakerStateClass(cb.state)">
                  {{ cb.state }}
                </span>
                <span class="text-[10px] text-gray-400">{{ cb.successfulCalls }}/{{ cb.failedCalls }}</span>
              </div>
            </div>
          </div>

          <div class="flex items-center space-x-4">
            <div class="text-center">
              <div class="text-xs font-medium" :class="cb.failureRate > 50 ? 'text-red-600' : 'text-gray-600'">
                {{ (cb.failureRate ?? 0).toFixed(0) }}%
              </div>
              <div class="text-[9px] text-gray-400">fail</div>
            </div>
            <button
              v-if="cb.state !== 'CLOSED'"
              @click="handleResetCircuitBreaker(cb.name)"
              class="px-2 py-1 text-[10px] font-medium text-white bg-sky-500 rounded hover:bg-sky-600"
            >
              Reset
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Per-Workflow Metrics -->
    <div class="bg-white border border-gray-100 rounded-lg">
      <div class="px-4 py-3 border-b border-gray-50">
        <h2 class="text-sm font-medium text-gray-800">Workflow Performance</h2>
        <p class="text-[10px] text-gray-400">Per-workflow metrics</p>
      </div>

      <div v-if="workflowsSorted.length === 0" class="p-6 text-center text-xs text-gray-400">
        No workflow metrics available
      </div>

      <div v-else class="divide-y divide-gray-50">
        <div v-for="wf in workflowsSorted" :key="wf.topic" class="px-4 py-3">
          <div class="flex items-center justify-between mb-2">
            <div class="flex items-center space-x-2">
              <code class="text-xs font-medium text-gray-700">{{ wf.topic }}</code>
              <span class="px-1 py-0.5 text-[9px] font-medium rounded" :class="getSuccessRateClass(wf.successRate ?? 0)">
                {{ (wf.successRate ?? 0).toFixed(0) }}%
              </span>
            </div>
            <div class="text-xs text-gray-400">{{ formatDuration(wf.avgDurationMs) }}</div>
          </div>

          <!-- Mini Stats Row -->
          <div class="grid grid-cols-6 gap-1.5 text-center mb-2">
            <div class="bg-gray-50 rounded px-1.5 py-1">
              <div class="text-xs font-medium text-gray-600">{{ wf.completed }}</div>
              <div class="text-[8px] text-gray-400">done</div>
            </div>
            <div class="bg-gray-50 rounded px-1.5 py-1">
              <div class="text-xs font-medium text-red-500">{{ wf.failed }}</div>
              <div class="text-[8px] text-gray-400">fail</div>
            </div>
            <div class="bg-gray-50 rounded px-1.5 py-1">
              <div class="text-xs font-medium text-gray-400">{{ wf.cancelled }}</div>
              <div class="text-[8px] text-gray-400">cancel</div>
            </div>
            <div class="bg-gray-50 rounded px-1.5 py-1">
              <div class="text-xs font-medium text-sky-500">{{ wf.active }}</div>
              <div class="text-[8px] text-gray-400">active</div>
            </div>
            <div class="bg-gray-50 rounded px-1.5 py-1">
              <div class="text-xs font-medium text-amber-500">{{ wf.retries }}</div>
              <div class="text-[8px] text-gray-400">retry</div>
            </div>
            <div class="bg-gray-50 rounded px-1.5 py-1">
              <div class="text-xs font-medium text-red-600">{{ wf.dlq }}</div>
              <div class="text-[8px] text-gray-400">dlq</div>
            </div>
          </div>

          <!-- Progress Bar -->
          <div class="h-1 bg-gray-100 rounded-full overflow-hidden flex">
            <div class="h-full bg-emerald-400" :style="{ width: `${getPercentage(wf.completed, wf.started)}%` }" />
            <div class="h-full bg-red-400" :style="{ width: `${getPercentage(wf.failed, wf.started)}%` }" />
            <div class="h-full bg-gray-300" :style="{ width: `${getPercentage(wf.cancelled, wf.started)}%` }" />
            <div class="h-full bg-sky-400" :style="{ width: `${getPercentage(wf.active, wf.started)}%` }" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { useMetricsStore } from '@/stores/metrics'
import StatsCard from '@/components/StatsCard.vue'
import HealthPanel from '@/components/HealthPanel.vue'
import OutboxPanel from '@/components/OutboxPanel.vue'
import { format } from 'date-fns'

const store = useMetricsStore()

const dashboard = computed(() => store.dashboard)
const loading = computed(() => store.loading)
const lastUpdated = computed(() => store.lastUpdated)
const totalProcessed = computed(() => store.totalProcessed)
const healthStatus = computed(() => store.healthStatus)
const workflowsSorted = computed(() => store.workflowsSorted)
const circuitBreakers = computed(() => store.circuitBreakers)
const circuitBreakerConfig = computed(() => store.circuitBreakerConfig)
const health = computed(() => store.health)
const outbox = computed(() => store.outbox)

const healthBannerClass = computed(() => ({
  'bg-emerald-50 border border-emerald-100': healthStatus.value === 'healthy',
  'bg-amber-50 border border-amber-100': healthStatus.value === 'warning',
  'bg-red-50 border border-red-100': healthStatus.value === 'critical'
}))

const healthIconBgClass = computed(() => ({
  'bg-emerald-100': healthStatus.value === 'healthy',
  'bg-amber-100': healthStatus.value === 'warning',
  'bg-red-100': healthStatus.value === 'critical'
}))

const healthTextClass = computed(() => ({
  'text-emerald-700': healthStatus.value === 'healthy',
  'text-amber-700': healthStatus.value === 'warning',
  'text-red-700': healthStatus.value === 'critical'
}))

const healthSubtextClass = computed(() => ({
  'text-emerald-500': healthStatus.value === 'healthy',
  'text-amber-500': healthStatus.value === 'warning',
  'text-red-500': healthStatus.value === 'critical'
}))

let interval

onMounted(() => {
  store.fetchDashboard()
  store.fetchCircuitBreakers()
  store.fetchCircuitBreakerConfig()
  store.fetchHealth()
  store.fetchOutboxStats()
  interval = setInterval(() => {
    store.fetchDashboard()
    store.fetchCircuitBreakers()
    store.fetchHealth()
    store.fetchOutboxStats()
  }, 5000)
})

onUnmounted(() => {
  clearInterval(interval)
})

function refresh() {
  store.fetchDashboard()
}

function formatTime(date) {
  if (!date) return '-'
  return format(date, 'HH:mm:ss')
}

function formatDuration(ms) {
  if (!ms || ms === 0) return '-'
  if (ms < 1000) return `${ms.toFixed(0)}ms`
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
  return `${(ms / 60000).toFixed(1)}m`
}

function getSuccessRateClass(rate) {
  if (rate >= 95) return 'bg-emerald-50 text-emerald-600'
  if (rate >= 80) return 'bg-amber-50 text-amber-600'
  return 'bg-red-50 text-red-600'
}

function getPercentage(value, total) {
  if (!total || total === 0) return 0
  return (value / total) * 100
}

function getCircuitBreakerBgClass(state) {
  if (state === 'CLOSED') return 'bg-emerald-100'
  if (state === 'OPEN') return 'bg-red-100'
  return 'bg-amber-100'
}

function getCircuitBreakerStateClass(state) {
  if (state === 'CLOSED') return 'bg-emerald-50 text-emerald-600'
  if (state === 'OPEN') return 'bg-red-50 text-red-600'
  return 'bg-amber-50 text-amber-600'
}

async function handleResetCircuitBreaker(name) {
  await store.resetCircuitBreaker(name)
}
</script>