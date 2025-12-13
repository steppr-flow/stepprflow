import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { metricsApi, circuitBreakerApi, healthApi, outboxApi } from '@/services/api'

export const useMetricsStore = defineStore('metrics', () => {
  // State
  const dashboard = ref({
    totalStarted: 0,
    totalCompleted: 0,
    totalFailed: 0,
    totalCancelled: 0,
    totalActive: 0,
    totalRetries: 0,
    totalDlq: 0,
    globalSuccessRate: 0,
    globalFailureRate: 0,
    workflowMetrics: []
  })

  const loading = ref(false)
  const error = ref(null)
  const lastUpdated = ref(null)

  // Circuit Breakers state
  const circuitBreakers = ref([])
  const circuitBreakerConfig = ref(null)

  // Health state
  const health = ref({
    status: 'UP',
    components: {}
  })

  // Outbox state
  const outbox = ref({
    pending: 0,
    sent: 0,
    failed: 0,
    total: 0,
    sendRate: 0,
    health: 'UP'
  })

  // Computed
  const totalProcessed = computed(() =>
    dashboard.value.totalCompleted + dashboard.value.totalFailed + dashboard.value.totalCancelled
  )

  const healthStatus = computed(() => {
    const rate = dashboard.value.globalSuccessRate
    if (rate >= 95) return 'healthy'
    if (rate >= 80) return 'warning'
    return 'critical'
  })

  const workflowsSorted = computed(() => {
    const metrics = dashboard.value.workflowMetrics
    if (!Array.isArray(metrics)) return []
    return [...metrics].sort((a, b) => (b.started ?? 0) - (a.started ?? 0))
  })

  // Actions
  async function fetchDashboard() {
    try {
      loading.value = true
      error.value = null
      const { data } = await metricsApi.getDashboard()
      console.log('Metrics API response:', data)
      // Merge with defaults to ensure all properties exist
      dashboard.value = {
        totalStarted: data?.totalStarted ?? 0,
        totalCompleted: data?.totalCompleted ?? 0,
        totalFailed: data?.totalFailed ?? 0,
        totalCancelled: data?.totalCancelled ?? 0,
        totalActive: data?.totalActive ?? 0,
        totalRetries: data?.totalRetries ?? 0,
        totalDlq: data?.totalDlq ?? 0,
        globalSuccessRate: data?.globalSuccessRate ?? 0,
        globalFailureRate: data?.globalFailureRate ?? 0,
        workflowMetrics: Array.isArray(data?.workflowMetrics) ? data.workflowMetrics : []
      }
      lastUpdated.value = new Date()
    } catch (e) {
      console.error('Failed to fetch metrics:', e)
      error.value = e.message
    } finally {
      loading.value = false
    }
  }

  async function fetchWorkflowMetrics(topic) {
    try {
      loading.value = true
      const { data } = await metricsApi.getWorkflowMetrics(topic)
      return data
    } catch (e) {
      error.value = e.message
      return null
    } finally {
      loading.value = false
    }
  }

  function clearError() {
    error.value = null
  }

  // Circuit Breaker actions
  async function fetchCircuitBreakers() {
    try {
      const { data } = await circuitBreakerApi.getAll()
      circuitBreakers.value = Array.isArray(data) ? data : []
    } catch (e) {
      console.error('Failed to fetch circuit breakers:', e)
      circuitBreakers.value = []
    }
  }

  async function fetchCircuitBreakerConfig() {
    try {
      const { data } = await circuitBreakerApi.getConfig()
      circuitBreakerConfig.value = data
    } catch (e) {
      console.error('Failed to fetch circuit breaker config:', e)
      circuitBreakerConfig.value = null
    }
  }

  async function resetCircuitBreaker(name) {
    try {
      await circuitBreakerApi.reset(name)
      await fetchCircuitBreakers()
      return true
    } catch (e) {
      console.error('Failed to reset circuit breaker:', e)
      return false
    }
  }

  // Health actions
  async function fetchHealth() {
    try {
      const { data } = await healthApi.getHealth()
      health.value = data || { status: 'UP', components: {} }
    } catch (e) {
      console.error('Failed to fetch health:', e)
      health.value = { status: 'DOWN', components: {} }
    }
  }

  // Outbox actions
  async function fetchOutboxStats() {
    try {
      const { data } = await outboxApi.getStats()
      outbox.value = data || {
        pending: 0,
        sent: 0,
        failed: 0,
        total: 0,
        sendRate: 0,
        health: 'UP'
      }
    } catch (e) {
      console.error('Failed to fetch outbox stats:', e)
      // Keep previous values on error
    }
  }

  return {
    // State
    dashboard,
    loading,
    error,
    lastUpdated,
    circuitBreakers,
    circuitBreakerConfig,
    health,
    outbox,
    // Computed
    totalProcessed,
    healthStatus,
    workflowsSorted,
    // Actions
    fetchDashboard,
    fetchWorkflowMetrics,
    fetchCircuitBreakers,
    fetchCircuitBreakerConfig,
    resetCircuitBreaker,
    fetchHealth,
    fetchOutboxStats,
    clearError
  }
})