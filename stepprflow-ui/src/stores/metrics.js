import { defineStore } from 'pinia'
import { ref } from 'vue'
import { metricsApi, circuitBreakerApi, healthApi, outboxApi } from '@/services/api.js'

export const useMetricsStore = defineStore('metrics', () => {
  const dashboard = ref(null)
  const circuitBreakers = ref([])
  const cbConfig = ref(null)
  const health = ref(null)
  const outbox = ref(null)
  const loading = ref(false)
  const error = ref(null)

  function clearError() {
    error.value = null
  }

  async function fetchDashboard() {
    loading.value = true
    error.value = null
    try {
      dashboard.value = await metricsApi.getDashboard()
    } catch (e) {
      error.value = e.message
    } finally {
      loading.value = false
    }
  }

  async function fetchCircuitBreakers() {
    try {
      circuitBreakers.value = await circuitBreakerApi.list()
    } catch (e) {
      error.value = e.message
    }
  }

  async function fetchCbConfig() {
    try {
      cbConfig.value = await circuitBreakerApi.getConfig()
    } catch (e) {
      error.value = e.message
    }
  }

  async function resetCircuitBreaker(name) {
    try {
      await circuitBreakerApi.reset(name)
      await fetchCircuitBreakers()
      return true
    } catch (e) {
      error.value = e.message
      return false
    }
  }

  async function fetchHealth() {
    try {
      health.value = await healthApi.get()
    } catch (e) {
      if (e.status === 503) {
        health.value = e.details
      } else {
        error.value = e.message
      }
    }
  }

  async function fetchOutbox() {
    try {
      outbox.value = await outboxApi.stats()
    } catch (e) {
      error.value = e.message
    }
  }

  async function fetchAll() {
    await Promise.all([
      fetchDashboard(),
      fetchCircuitBreakers(),
      fetchHealth(),
      fetchOutbox()
    ])
  }

  return {
    dashboard,
    circuitBreakers,
    cbConfig,
    health,
    outbox,
    loading,
    error,
    clearError,
    fetchDashboard,
    fetchCircuitBreakers,
    fetchCbConfig,
    resetCircuitBreaker,
    fetchHealth,
    fetchOutbox,
    fetchAll
  }
})
