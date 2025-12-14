import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { dashboardApi, executionApi, ErrorCodes } from '@/services/api'

export const useWorkflowStore = defineStore('workflow', () => {
  // State
  const stats = ref({
    pending: 0,
    inProgress: 0,
    completed: 0,
    failed: 0,
    retryPending: 0,
    cancelled: 0,
    total: 0
  })

  const executions = ref([])
  const recentExecutions = ref([])
  const workflows = ref([])
  const currentExecution = ref(null)
  const loading = ref(false)

  // Enhanced error state
  const error = ref(null)

  // Pagination
  const pagination = ref({
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0
  })

  // Filters
  const filters = ref({
    topic: '',
    statuses: [],
    search: ''
  })

  // Computed
  const hasMorePages = computed(() =>
    pagination.value.page < pagination.value.totalPages - 1
  )

  const hasError = computed(() => error.value !== null)

  const errorMessage = computed(() => error.value?.message || null)

  const errorCode = computed(() => error.value?.code || null)

  /**
   * Set error from API error or plain message.
   */
  function setError(e) {
    if (e?.isApiError) {
      error.value = {
        code: e.code,
        message: e.message,
        details: e.details,
        isConflict: e.isConflict?.() || false,
        isNotFound: e.isNotFound?.() || false,
        isValidation: e.isValidation?.() || false
      }
    } else {
      error.value = {
        code: 'UNKNOWN_ERROR',
        message: e?.message || 'An unexpected error occurred',
        details: {},
        isConflict: false,
        isNotFound: false,
        isValidation: false
      }
    }
  }

  // Actions
  async function fetchDashboard() {
    try {
      loading.value = true
      clearError()
      const { data } = await dashboardApi.getOverview()
      stats.value = data.stats || stats.value
      recentExecutions.value = data.recentExecutions || []
      workflows.value = data.workflows || []
    } catch (e) {
      setError(e)
    } finally {
      loading.value = false
    }
  }

  async function fetchExecutions(resetPage = false) {
    try {
      loading.value = true
      clearError()
      if (resetPage) {
        pagination.value.page = 0
      }

      const params = {
        page: pagination.value.page,
        size: pagination.value.size,
        sortBy: 'createdAt',
        direction: 'DESC'
      }

      if (filters.value.topic) {
        params.topic = filters.value.topic
      }
      if (filters.value.statuses && filters.value.statuses.length > 0) {
        params.statuses = filters.value.statuses.join(',')
      }

      const { data } = await executionApi.getExecutions(params)
      executions.value = data.content || []
      pagination.value.totalElements = data.totalElements || 0
      pagination.value.totalPages = data.totalPages || 0
    } catch (e) {
      setError(e)
    } finally {
      loading.value = false
    }
  }

  async function fetchExecution(id) {
    try {
      loading.value = true
      clearError()
      const { data } = await executionApi.getExecution(id)
      currentExecution.value = data
      return data
    } catch (e) {
      setError(e)
      return null
    } finally {
      loading.value = false
    }
  }

  async function resumeExecution(id, fromStep = null) {
    try {
      clearError()
      await executionApi.resumeExecution(id, fromStep)
      await fetchExecution(id)
      return { success: true }
    } catch (e) {
      setError(e)
      return {
        success: false,
        error: error.value,
        needsRefresh: e.code === ErrorCodes.CONCURRENT_MODIFICATION
      }
    }
  }

  async function cancelExecution(id) {
    try {
      clearError()
      await executionApi.cancelExecution(id)
      await fetchExecution(id)
      return { success: true }
    } catch (e) {
      setError(e)
      return {
        success: false,
        error: error.value,
        needsRefresh: e.code === ErrorCodes.CONCURRENT_MODIFICATION
      }
    }
  }

  async function updatePayloadField(id, fieldPath, newValue, reason) {
    try {
      clearError()
      const { data } = await executionApi.updatePayloadField(id, fieldPath, newValue, reason)
      currentExecution.value = data
      return { success: true }
    } catch (e) {
      setError(e)
      return {
        success: false,
        error: error.value,
        needsRefresh: e.code === ErrorCodes.CONCURRENT_MODIFICATION
      }
    }
  }

  async function restorePayload(id) {
    try {
      clearError()
      const { data } = await executionApi.restorePayload(id)
      currentExecution.value = data
      return { success: true }
    } catch (e) {
      setError(e)
      return {
        success: false,
        error: error.value,
        needsRefresh: e.code === ErrorCodes.CONCURRENT_MODIFICATION
      }
    }
  }

  function setFilters(newFilters) {
    filters.value = { ...filters.value, ...newFilters }
  }

  function nextPage() {
    if (hasMorePages.value) {
      pagination.value.page++
      fetchExecutions()
    }
  }

  function prevPage() {
    if (pagination.value.page > 0) {
      pagination.value.page--
      fetchExecutions()
    }
  }

  function setPageSize(size) {
    pagination.value.size = size
    pagination.value.page = 0
    fetchExecutions()
  }

  function setPage(page) {
    pagination.value.page = page
    fetchExecutions()
  }

  function clearError() {
    error.value = null
  }

  return {
    // State
    stats,
    executions,
    recentExecutions,
    workflows,
    currentExecution,
    loading,
    error,
    pagination,
    filters,
    // Computed
    hasMorePages,
    hasError,
    errorMessage,
    errorCode,
    // Actions
    fetchDashboard,
    fetchExecutions,
    fetchExecution,
    resumeExecution,
    cancelExecution,
    updatePayloadField,
    restorePayload,
    setFilters,
    nextPage,
    prevPage,
    setPageSize,
    setPage,
    clearError
  }
})
